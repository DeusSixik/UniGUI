package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.widgets.data.VirtualListView;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.xaml.bind.XamlBindingContext;
import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.component.ComponentInstance;
import dev.sixik.unigui.xaml.component.Components;
import dev.sixik.unigui.xaml.component.ParamType;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.struct.StructType;
import dev.sixik.unigui.xaml.struct.StructValue;
import dev.sixik.unigui.xaml.struct.Structs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StructTest {
    record Product(String id, String name, int price, boolean sale) {
    }

    record Line(Product product, int quantity) {
    }

    private final List<String> structs = new ArrayList<>();
    private final List<String> components = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        components.forEach(Components::unregister);
        structs.forEach(Structs::unregister);
    }

    private StructType product() {
        StructType type = Structs.registerRecord(Product.class);
        structs.add(type.name());
        return type;
    }

    private void register(String id, String source) {
        Components.register(id, source);
        components.add(id);
    }

    // ------------------------------------------------------------------ записи Java

    @Test
    void aJavaRecordBecomesAStructWithTypedFields() {
        StructType type = product();
        assertEquals("Product", type.name());
        assertEquals(List.of("id", "name", "price", "sale"), type.fields().stream().map(StructType.Field::name).toList());
        assertSame(ParamType.STRING, type.field("name").orElseThrow().type());
        assertSame(ParamType.INT, type.field("price").orElseThrow().type());
        assertSame(ParamType.BOOL, type.field("sale").orElseThrow().type());
        assertTrue(type.isRecord());
        assertEquals("struct:Product", type.paramType().name());
        assertSame(type.paramType(), ParamType.parse("struct:Product"));
    }

    @Test
    void valuesAreRealRecordsThatCanBeReadAndCopied() {
        StructType type = product();
        Object made = type.create(Args.of("id", "p1", "name", "Sword", "price", 50));
        assertEquals(new Product("p1", "Sword", 50, false), made, "missing fields take their type default");
        assertEquals("Sword", type.read(made, "name"));
        assertEquals(50, type.read(made, "price"));

        Object cheaper = type.with(made, "price", 40);
        assertEquals(new Product("p1", "Sword", 40, false), cheaper);
        assertEquals(new Product("p1", "Sword", 50, false), made, "the original is untouched");
    }

    @Test
    void nestedRecordsReferToTheirStructs() {
        product();
        StructType line = Structs.registerRecord(Line.class);
        structs.add(line.name());
        assertEquals("struct:Product", line.field("product").orElseThrow().type().name());
        Object made = line.create(Args.of("product", new Product("a", "A", 1, false), "quantity", 3));
        assertEquals(new Line(new Product("a", "A", 1, false), 3), made);
    }

    @Test
    void mistakesAreReportedClearly() {
        StructType type = product();
        assertThrows(IllegalArgumentException.class, () -> type.create(Args.of("colour", "red")));
        IllegalArgumentException wrong = assertThrows(IllegalArgumentException.class,
                () -> type.create(Args.of("price", "expensive")));
        assertTrue(wrong.getMessage().contains("Product.price"), wrong.getMessage());
        assertThrows(IllegalArgumentException.class, () -> type.read(new Product("a", "A", 1, false), "colour"));
        assertThrows(IllegalArgumentException.class, () -> type.read("not a product", "name"));
        assertThrows(IllegalArgumentException.class, () -> StructType.ofRecord(String.class));
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class, () -> ParamType.parse("struct:Nope"));
        assertTrue(unknown.getMessage().contains("Unknown struct 'Nope'"), unknown.getMessage());
    }

    // ------------------------------------------------------------------ структуры из документа

    @Test
    void aStructCanBeDeclaredInADocument() {
        StructType type = Structs.register("""
                <Struct x:Name="Player" xmlns:x="https://unigui.dev/xaml">
                  <Field Name="nick" Type="string"/>
                  <Field Name="level" Type="int"/>
                </Struct>""");
        structs.add("Player");
        assertEquals(StructValue.class, type.valueClass());

        Object made = type.create(Args.of("nick", "Steve", "level", 7));
        assertTrue(made instanceof StructValue);
        assertEquals("Steve", type.read(made, "nick"));
        Object promoted = type.with(made, "level", 8);
        assertNotEquals(made, promoted);
        assertEquals(made, type.create(Args.of("nick", "Steve", "level", 7)), "values compare by content");
        assertEquals(8, ((StructValue) promoted).get("level"));
    }

    @Test
    void structDocumentsAreValidatedWithPositions() {
        for (String[] bad : new String[][]{
                {"<Thing/>", "must start with <Struct>"},
                {"<Struct/>", "requires x:Name"},
                {"<Struct x:Name=\"A\" xmlns:x=\"https://unigui.dev/xaml\"><Field Type=\"int\"/></Struct>", "requires Name"},
                {"<Struct x:Name=\"A\" xmlns:x=\"https://unigui.dev/xaml\"><Field Name=\"f\"/></Struct>", "requires Type"},
                {"<Struct x:Name=\"A\" xmlns:x=\"https://unigui.dev/xaml\"><Field Name=\"f\" Type=\"money\"/></Struct>", "Unknown parameter type"},
                {"<Struct x:Name=\"A\" xmlns:x=\"https://unigui.dev/xaml\"><Field Name=\"f\" Type=\"int\"/>"
                        + "<Field Name=\"f\" Type=\"int\"/></Struct>", "declared twice"},
                {"<Struct x:Name=\"A\" xmlns:x=\"https://unigui.dev/xaml\"><Other/></Struct>", "can hold <Field>"}}) {
            XamlLoadException failure = assertThrows(XamlLoadException.class, () -> StructType.parse(bad[0]), bad[0]);
            assertTrue(failure.getMessage().contains(bad[1]), bad[0] + " -> " + failure.getMessage());
        }
    }

    // ------------------------------------------------------------------ в компонентах и списках

    @Test
    void aStructCanBeAParameterAndAListRowReadsItsFields() {
        product();
        register("s:holder", """
                <Component x:Name="ProductHolder" xmlns:x="https://unigui.dev/xaml">
                  <Component.Contract><Param Name="Item" Type="struct:Product"/></Component.Contract>
                  <Label id="l" text="{Param Item}"/>
                </Component>""");
        ComponentInstance holder = Xaml.component("s:holder").create(Args.of("Item", new Product("p", "Bow", 9, false)));
        assertEquals(new Product("p", "Bow", 9, false), holder.get("Item"));
        assertThrows(IllegalArgumentException.class, () -> holder.set("Item", "plainly wrong"));

        StructType player = StructType.define("Row", List.of(new StructType.Field("title", ParamType.STRING)));
        Structs.register(player);
        structs.add("Row");
        XamlBindingContext data = XamlBindingContext.empty().list("rows",
                List.of(player.create(Args.of("title", "From a struct document"))));
        VirtualListView list = Xaml.load("""
                <VirtualListView itemHeight="20" items="{Binding rows}">
                  <VirtualListView.ItemTemplate><Label id="row" text="{Item title}"/></VirtualListView.ItemTemplate>
                </VirtualListView>""", VirtualListView.class, LoadOptions.DEFAULT.bindingContext(data));
        list.arrange(new MutableRect(0, 0, 200, 60));
        assertEquals("From a struct document", ((Label) Xaml.get(list, "row")).text());
    }

    // ------------------------------------------------------------------ в графах

    @Test
    void graphsMakeBreakAndCopyStructs() {
        product();
        register("s:shop", """
                <Component x:Name="StructShop" xmlns:x="https://unigui.dev/xaml">
                  <Component.Contract>
                    <Param Name="Title" Type="string" Default="-"/>
                    <Event Name="Offer" Direction="In" Args="product:struct:Product"/>
                    <Event Name="Discounted" Direction="Out" Args="product:struct:Product"/>
                  </Component.Contract>
                  <Label id="label" text="x"/>
                  <Component.Graph>
                    <Node Id="in" Type="Event.Offer"/>
                    <Node Id="open" Type="Struct.Break" Struct="Product"/>
                    <Node Id="show" Type="Param.Set" Name="Title"/>
                    <Node Id="cut" Type="Struct.With" Struct="Product" Field="price" newValue="1"/>
                    <Node Id="send" Type="Raise.Discounted"/>
                    <Wire From="in.product" To="open.value"/>
                    <Wire From="in.then" To="show.exec"/>
                    <Wire From="open.name" To="show.value"/>
                    <Wire From="in.product" To="cut.value"/>
                    <Wire From="show.then" To="send.exec"/>
                    <Wire From="cut.result" To="send.product"/>
                  </Component.Graph>
                </Component>""");
        ComponentInstance shop = Xaml.component("s:shop").create();
        List<Object> discounted = new ArrayList<>();
        shop.onEvent("Discounted", args -> discounted.add(args.get("product")));

        shop.send("Offer", Args.of("product", new Product("p1", "Sword", 50, false)));
        shop.flush();
        assertEquals("Sword", shop.get("Title"), "Struct.Break exposed the field");
        assertEquals(List.of(new Product("p1", "Sword", 1, false)), discounted, "Struct.With produced a copy");
    }

    @Test
    void structMakeBuildsAValueFromPins() {
        product();
        register("s:make", """
                <Component x:Name="StructMake" xmlns:x="https://unigui.dev/xaml">
                  <Component.Contract>
                    <Event Name="Go" Direction="In" Args="name:string"/>
                    <Event Name="Made" Direction="Out" Args="product:struct:Product"/>
                  </Component.Contract>
                  <Label id="label" text="x"/>
                  <Component.Graph>
                    <Node Id="in" Type="Event.Go"/>
                    <Node Id="make" Type="Struct.Make" Struct="Product" id="fixed" price="12" sale="true"/>
                    <Node Id="send" Type="Raise.Made"/>
                    <Wire From="in.name" To="make.name"/>
                    <Wire From="in.then" To="send.exec"/>
                    <Wire From="make.value" To="send.product"/>
                  </Component.Graph>
                </Component>""");
        ComponentInstance made = Xaml.component("s:make").create();
        List<Object> seen = new ArrayList<>();
        made.onEvent("Made", args -> seen.add(args.get("product")));
        made.send("Go", Args.of("name", "Axe"));
        made.flush();
        assertEquals(List.of(new Product("fixed", "Axe", 12, true)), seen);
    }

    private void assertRejected(String graph, String fragment) {
        ComponentDefinition definition = ComponentDefinition.parse("s:probe", """
                <Component x:Name="StructProbe" xmlns:x="https://unigui.dev/xaml">
                  <Component.Contract>
                    <Event Name="Go" Direction="In" Args="product:struct:Product,count:int"/>
                  </Component.Contract>
                  <Label/>
                  <Component.Graph>""" + graph + "</Component.Graph></Component>");
        XamlLoadException failure = assertThrows(XamlLoadException.class, definition::program, fragment);
        assertTrue(failure.getMessage().contains(fragment), "expected '" + fragment + "' in: " + failure.getMessage());
        assertTrue(failure.diagnostics().get(0).hasLocation(), failure.getMessage());
    }

    @Test
    void structNodesAreCheckedBeforeAnythingRuns() {
        product();
        Structs.register("<Struct x:Name=\"Other\" xmlns:x=\"https://unigui.dev/xaml\"><Field Name=\"a\" Type=\"int\"/></Struct>");
        structs.add("Other");
        assertRejected("<Node Id=\"n\" Type=\"Struct.Break\" Struct=\"Ghost\"/>", "struct 'Ghost' which is not registered");
        assertRejected("<Node Id=\"n\" Type=\"Struct.Break\"/>", "requires Struct");
        assertRejected("<Node Id=\"n\" Type=\"Struct.With\" Struct=\"Product\" Field=\"colour\"/>", "has no field 'colour'");
        assertRejected("<Node Id=\"in\" Type=\"Event.Go\"/><Node Id=\"n\" Type=\"Struct.Break\" Struct=\"Other\"/>"
                + "<Wire From=\"in.product\" To=\"n.value\"/>", "joins struct:Product to struct:Other");
        assertRejected("<Node Id=\"in\" Type=\"Event.Go\"/><Node Id=\"n\" Type=\"Math.Add\"/>"
                + "<Wire From=\"in.product\" To=\"n.a\"/>", "joins struct:Product to float");
        assertRejected("<Node Id=\"n\" Type=\"Struct.Break\" Struct=\"Product\"/>", "value");
    }

    @Test
    void aGraphIsRecompiledWhenAMissingStructGetsRegistered() {
        // Контракт читается при регистрации, поэтому структура нужна к этому моменту; граф же сверяется лениво.
        product();
        register("s:late", """
                <Component x:Name="LateStruct" xmlns:x="https://unigui.dev/xaml">
                  <Component.Contract><Event Name="Go" Direction="In"/></Component.Contract>
                  <Label/>
                  <Component.Graph>
                    <Node Id="in" Type="Event.Go"/>
                    <Node Id="n" Type="Struct.Make" Struct="Later"/>
                  </Component.Graph>
                </Component>""");
        XamlLoadException missing = assertThrows(XamlLoadException.class, () -> Xaml.component("s:late").create());
        assertTrue(missing.getMessage().contains("struct 'Later'"), missing.getMessage());
        Structs.register("<Struct x:Name=\"Later\" xmlns:x=\"https://unigui.dev/xaml\"><Field Name=\"a\" Type=\"int\"/></Struct>");
        structs.add("Later");
        assertNotNull(Xaml.component("s:late").create(), "the graph compiles once the struct exists");
    }
}
