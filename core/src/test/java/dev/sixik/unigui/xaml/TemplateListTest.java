package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.data.VirtualListView;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.xaml.bind.XamlBindingContext;
import dev.sixik.unigui.xaml.bind.XamlMutableObservableValue;
import dev.sixik.unigui.xaml.bind.XamlObservableList;
import dev.sixik.unigui.xaml.component.ComponentInstance;
import dev.sixik.unigui.xaml.component.Components;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.DocumentValidator;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.snapshot.Snapshotter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TemplateListTest {
    record Product(String id, String name, int price) {
    }

    record Wrapper(Product product) {
    }

    private static final String CARD = """
            <Component x:Name="RowCard" xmlns:x="https://unigui.dev/xaml">
              <Component.Contract>
                <Param Name="Title" Type="string"/>
                <Param Name="Price" Type="int"/>
              </Component.Contract>
              <Label id="title" text="{Param Title}"/>
            </Component>
            """;

    private static final String LIST = """
            <VirtualListView id="list" itemHeight="20" overscan="0" items="{Binding catalog}">
              <VirtualListView.ItemTemplate>
                <RowCard key="{Item id}" Title="{Item name}" Price="{Item price}"/>
              </VirtualListView.ItemTemplate>
            </VirtualListView>
            """;

    private XamlObservableList<Product> catalog;
    private XamlBindingContext data;

    @BeforeEach
    void setUp() {
        Components.register("t:rowcard", CARD);
        catalog = new XamlObservableList<>();
        for (int i = 0; i < 1000; i++) catalog.add(new Product("p" + i, "Item " + i, i));
        data = XamlBindingContext.empty().list("catalog", catalog);
    }

    @AfterEach
    void tearDown() {
        Components.unregister("t:rowcard");
    }

    private VirtualListView load(String source) {
        VirtualListView list = Xaml.load(source, VirtualListView.class, LoadOptions.DEFAULT.bindingContext(data));
        list.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 60.0f));
        return list;
    }

    private static String titleOf(Widget root, String key) {
        return Components.byKey(root, key).map(card -> (String) card.get("Title")).orElse(null);
    }

    // ------------------------------------------------------------------ список

    @Test
    void onlyVisibleRowsAreBuiltFromTheTemplate() {
        VirtualListView list = load(LIST);
        assertEquals(1000, list.itemCount());
        assertTrue(list.realizedCount() <= 4, "only the visible rows exist, not 1000: " + list.realizedCount());
        assertEquals("Item 0", titleOf(list, "p0"));
        assertEquals("Item 2", titleOf(list, "p2"));
        assertEquals(null, titleOf(list, "p500"), "off-screen rows do not exist yet");
        assertEquals(2, ((Integer) Components.byKey(list, "p2").orElseThrow().get("Price")).intValue(),
                "{Item price} fed an int parameter");
    }

    @Test
    void appendingKeepsTheRowsThatAreAlreadyShown() {
        catalog.setAll(List.of(new Product("a", "A", 1), new Product("b", "B", 2)));
        VirtualListView list = load(LIST);
        ComponentInstance first = Components.byKey(list, "a").orElseThrow();

        catalog.add(new Product("c", "C", 3));
        list.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 60.0f));

        assertEquals(3, list.itemCount());
        assertSame(first, Components.byKey(list, "a").orElseThrow(), "append does not rebuild existing rows");
        assertEquals("C", titleOf(list, "c"));
    }

    @Test
    void removingAndReplacingRefreshesTheRows() {
        catalog.setAll(List.of(new Product("a", "A", 1), new Product("b", "B", 2), new Product("c", "C", 3)));
        VirtualListView list = load(LIST);

        catalog.remove(0);
        list.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 60.0f));
        assertEquals(2, list.itemCount());
        assertEquals(null, titleOf(list, "a"));
        assertEquals("B", titleOf(list, "b"));

        catalog.set(0, new Product("b", "B renamed", 9));
        list.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 60.0f));
        assertEquals("B renamed", titleOf(list, "b"), "a replaced element shows its new data");
    }

    @Test
    void clearingEmptiesTheView() {
        VirtualListView list = load(LIST);
        catalog.clear();
        list.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 60.0f));
        assertEquals(0, list.itemCount());
        assertEquals(0, list.realizedCount());
    }

    @Test
    void aPlainListWorksToo() {
        XamlBindingContext plain = XamlBindingContext.empty().list("catalog", List.of(new Product("x", "Only", 5)));
        VirtualListView list = Xaml.load(LIST, VirtualListView.class, LoadOptions.DEFAULT.bindingContext(plain));
        list.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 60.0f));
        assertEquals("Only", titleOf(list, "x"));
    }

    @Test
    void eachViewKeepsItsOwnSource() {
        VirtualListView list = load(LIST);
        XamlBindingContext other = XamlBindingContext.empty().list("catalog",
                List.of(new Product("m", "M", 1), new Product("n", "N", 2)));
        VirtualListView second = Xaml.load(LIST, VirtualListView.class, LoadOptions.DEFAULT.bindingContext(other));
        second.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 60.0f));
        assertEquals(2, second.itemCount());
        assertEquals(1000, list.itemCount(), "the first view is independent");
    }

    // ------------------------------------------------------------------ путь элемента

    @Test
    void itemPathsReadMapsNestedRecordsAndWholeValues() {
        XamlBindingContext mixed = XamlBindingContext.empty().list("catalog", List.of(
                Map.of("id", "m1", "name", "From map"),
                new Wrapper(new Product("w1", "Nested", 3))));
        VirtualListView list = Xaml.load("""
                <VirtualListView itemHeight="20" overscan="0" items="{Binding catalog}">
                  <VirtualListView.ItemTemplate>
                    <Label text="{Item}"/>
                  </VirtualListView.ItemTemplate>
                </VirtualListView>
                """, VirtualListView.class, LoadOptions.DEFAULT.bindingContext(mixed));
        list.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 60.0f));
        assertEquals(2, list.realizedCount());

        VirtualListView nested = Xaml.load("""
                <VirtualListView itemHeight="20" overscan="0" items="{Binding catalog}">
                  <VirtualListView.ItemTemplate>
                    <RowCard key="{Item product.id}" Title="{Item product.name}"/>
                  </VirtualListView.ItemTemplate>
                </VirtualListView>
                """, VirtualListView.class,
                LoadOptions.DEFAULT.bindingContext(XamlBindingContext.empty().list("catalog",
                        List.of(new Wrapper(new Product("w1", "Nested", 3))))));
        nested.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 60.0f));
        assertEquals("Nested", titleOf(nested, "w1"));
    }

    @Test
    void aMissingItemPropertyFailsWithAClearMessage() {
        VirtualListView list = Xaml.load("""
                <VirtualListView itemHeight="20" overscan="0" items="{Binding catalog}">
                  <VirtualListView.ItemTemplate>
                    <RowCard Title="{Item nmae}"/>
                  </VirtualListView.ItemTemplate>
                </VirtualListView>
                """, VirtualListView.class, LoadOptions.DEFAULT.bindingContext(data));
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> list.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 60.0f)));
        assertTrue(failure.getMessage().contains("no property 'nmae'"), failure.getMessage());
    }

    // ------------------------------------------------------------------ ошибки в документе

    @Test
    void mistakesInTheTemplateAreFoundWhenTheDocumentLoads() {
        XamlLoadException unknownType = assertThrows(XamlLoadException.class, () -> load("""
                <VirtualListView items="{Binding catalog}">
                  <VirtualListView.ItemTemplate><NoSuchWidget/></VirtualListView.ItemTemplate>
                </VirtualListView>"""));
        assertTrue(unknownType.getMessage().contains("Unknown widget type 'NoSuchWidget'"), unknownType.getMessage());

        XamlLoadException unknownAttribute = assertThrows(XamlLoadException.class, () -> load("""
                <VirtualListView items="{Binding catalog}">
                  <VirtualListView.ItemTemplate><RowCard Titel="x"/></VirtualListView.ItemTemplate>
                </VirtualListView>"""));
        assertTrue(unknownAttribute.getMessage().contains("Unknown attribute 'Titel'"), unknownAttribute.getMessage());
        assertEquals(2, unknownAttribute.diagnostics().get(0).line(), "the error points into the template");
    }

    @Test
    void theTemplateSlotTakesExactlyOneElement() {
        assertThrows(XamlLoadException.class, () -> load("""
                <VirtualListView>
                  <VirtualListView.ItemTemplate><Label/><Label/></VirtualListView.ItemTemplate>
                </VirtualListView>"""));
        assertThrows(XamlLoadException.class, () -> load("""
                <VirtualListView>
                  <VirtualListView.ItemTemplate></VirtualListView.ItemTemplate>
                </VirtualListView>"""));
    }

    @Test
    void itemOutsideATemplateIsRejected() {
        XamlLoadException failure = assertThrows(XamlLoadException.class,
                () -> Xaml.load("<Label text=\"{Item name}\"/>"));
        assertTrue(failure.getMessage().contains("only be used inside an ItemTemplate"), failure.getMessage());
    }

    @Test
    void itemsCannotBeWrittenAsText() {
        XamlLoadException failure = assertThrows(XamlLoadException.class,
                () -> Xaml.load("<VirtualListView items=\"abc\"/>"));
        assertTrue(failure.getMessage().contains("cannot be written as text"), failure.getMessage());
    }

    @Test
    void theValidatorAcceptsTemplatesAndFlagsTheirMistakes() {
        DocumentValidator validator = new DocumentValidator(Xaml.defaultSchema());
        assertTrue(validator.validate(LIST).valid(), validator.validate(LIST).diagnostics().toString());
        var bad = validator.validate(LIST.replace("Title=", "Titel="));
        assertFalse(bad.valid());
        assertTrue(bad.diagnostics().get(0).message().contains("Unknown attribute 'Titel'"));
    }

    // ------------------------------------------------------------------ привязки внутри шаблона

    @Test
    void bindingsInsideARowAreReleasedWhenTheRowIsDisposed() {
        XamlBindingContext live = XamlBindingContext.empty().list("catalog", List.of(new Product("a", "A", 1)));
        XamlMutableObservableValue<String> status = live.mutable("status", String.class, "ready");
        VirtualListView list = Xaml.load("""
                <VirtualListView itemHeight="20" overscan="0" items="{Binding catalog}">
                  <VirtualListView.ItemTemplate>
                    <Label id="status" text="{Binding status}"/>
                  </VirtualListView.ItemTemplate>
                </VirtualListView>
                """, VirtualListView.class, LoadOptions.DEFAULT.bindingContext(live));
        list.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 60.0f));
        Label label = Xaml.get(list, "status", Label.class);
        assertEquals("ready", label.text());

        status.set("busy");
        assertEquals("busy", label.text(), "a live row follows its binding");

        list.children().stream().filter(child -> child instanceof dev.sixik.unigui.xaml.load.TemplateItem)
                .forEach(Widget::dispose);
        status.set("gone");
        assertEquals("busy", label.text(), "a disposed row no longer listens");
    }

    // ------------------------------------------------------------------ сохранение

    @Test
    void theTemplateIsSavedAndReloadedUnchanged() {
        VirtualListView list = Xaml.load(LIST, VirtualListView.class, LoadOptions.DEFAULT.bindingContext(data));
        Snapshotter.Result saved = new Snapshotter(Xaml.defaultSchema()).snapshot(list);
        String text = XamlFormat.INSTANCE.write(saved.document());
        assertTrue(text.contains("VirtualListView.ItemTemplate"), text);
        assertTrue(text.contains("{Item name}"), text);
        assertFalse(text.contains("TemplateItem"), "built rows are not written: " + text);

        // Выражение привязки запоминается загрузчиком, поэтому источник данных сохраняется выражением, а не данными.
        assertTrue(text.contains("items=\"{Binding catalog}\""), text);
        VirtualListView reloaded = Xaml.load(text, VirtualListView.class, LoadOptions.DEFAULT.bindingContext(data));
        reloaded.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 60.0f));
        assertEquals("Item 1", titleOf(reloaded, "p1"));
        String again = XamlFormat.INSTANCE.write(new Snapshotter(Xaml.defaultSchema()).snapshot(reloaded).document());
        assertEquals(text, again, "saving is a fixed point");
    }

    // ------------------------------------------------------------------ наблюдаемый список

    @Test
    void observableListReportsEachKindOfChange() {
        XamlObservableList<String> list = new XamlObservableList<>(List.of("a", "b"));
        List<XamlObservableList.Change> seen = new ArrayList<>();
        var subscription = list.onChanged(seen::add);

        list.add("c");
        list.add(0, "z");
        list.set(1, "A");
        list.remove(2);
        list.addAll(List.of("x", "y"));
        list.setAll(List.of("only"));
        list.clear();
        list.clear(); // пустой список не шумит

        assertEquals(List.of(
                new XamlObservableList.Change(XamlObservableList.Kind.ADDED, 2, 1),
                new XamlObservableList.Change(XamlObservableList.Kind.ADDED, 0, 1),
                new XamlObservableList.Change(XamlObservableList.Kind.REPLACED, 1, 1),
                new XamlObservableList.Change(XamlObservableList.Kind.REMOVED, 2, 1),
                new XamlObservableList.Change(XamlObservableList.Kind.ADDED, 3, 2),
                new XamlObservableList.Change(XamlObservableList.Kind.RESET, 0, 1),
                new XamlObservableList.Change(XamlObservableList.Kind.RESET, 0, 0)), seen);

        subscription.unsubscribe();
        list.add("silent");
        assertEquals(7, seen.size());
    }
}
