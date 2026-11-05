package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.xaml.bind.XamlBindingContext;
import dev.sixik.unigui.xaml.bind.XamlCommandRegistry;
import dev.sixik.unigui.xaml.bind.XamlMutableObservableValue;
import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.component.ComponentEvent;
import dev.sixik.unigui.xaml.component.ComponentHost;
import dev.sixik.unigui.xaml.component.ComponentInstance;
import dev.sixik.unigui.xaml.component.Components;
import dev.sixik.unigui.xaml.component.Contract;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.snapshot.Snapshotter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComponentTest {
    private static final String CARD = """
            <Component x:Name="TestCard" xmlns:x="https://unigui.dev/xaml">
              <Component.Contract>
                <Param Name="Title" Type="string" Default="Untitled"/>
                <Param Name="Price" Type="int" Default="0"/>
                <Param Name="ProductId" Type="string"/>
                <Event Name="Buy" Direction="Out" Args="ProductId:string"/>
                <Event Name="Invalidate" Direction="In" Coalesce="true" Args="state:int"/>
                <Event Name="Rename" Direction="In" Args="title:string"/>
              </Component.Contract>
              <VBox>
                <Label id="title" text="{Param Title}"/>
                <Label id="price" text="{Param Price}"/>
                <Button id="buy" text="Buy" onClick="{Raise Buy}"/>
              </VBox>
            </Component>
            """;

    private final List<String> registered = new ArrayList<>();

    private ComponentDefinition register(String id, String source) {
        ComponentDefinition definition = Components.register(id, source);
        registered.add(id);
        return definition;
    }

    @AfterEach
    void cleanUp() {
        registered.forEach(Components::unregister);
        registered.clear();
    }

    // ------------------------------------------------------------------ контракт

    @Test
    void contractIsReadFromTheDocument() {
        ComponentDefinition definition = register("t:card", CARD);
        assertEquals("TestCard", definition.tag().local());
        Contract contract = definition.contract();
        assertEquals(List.of("Title", "Price", "ProductId"),
                contract.params().stream().map(Contract.Param::name).toList());
        assertEquals("Untitled", contract.param("Title").orElseThrow().value());
        assertEquals(0, contract.param("Price").orElseThrow().value());
        assertEquals(Contract.Direction.OUT, contract.event("Buy").orElseThrow().direction());
        assertTrue(contract.event("Invalidate").orElseThrow().coalesce());
        assertEquals("ProductId", contract.event("Buy").orElseThrow().args().get(0).name());
    }

    private static void assertRejected(String contract, String body, String fragment, int line) {
        String source = "<Component x:Name=\"Bad\" xmlns:x=\"https://unigui.dev/xaml\">\n"
                + "<Component.Contract>\n" + contract + "\n</Component.Contract>\n" + body + "</Component>";
        XamlLoadException failure = assertThrows(XamlLoadException.class,
                () -> ComponentDefinition.parse("t:bad", source), fragment);
        assertTrue(failure.getMessage().contains(fragment), "message was: " + failure.getMessage());
        if (line > 0) {
            assertEquals(line, failure.diagnostics().get(0).line(), "line of: " + failure.getMessage());
        }
    }

    @Test
    void contractMistakesAreReportedWithPositions() {
        String body = "<VBox/>";
        assertRejected("<Param Type=\"string\"/>", body, "requires Name", 3);
        assertRejected("<Param Name=\"A\"/>", body, "requires Type", 3);
        assertRejected("<Param Name=\"A\" Type=\"money\"/>", body, "Unknown parameter type 'money'", 3);
        assertRejected("<Param Name=\"A\" Type=\"string\"/>\n<Param Name=\"A\" Type=\"int\"/>", body,
                "Duplicate name 'A'", 4);
        assertRejected("<Param Name=\"width\" Type=\"float\"/>", body, "clashes with a built-in widget property", 3);
        assertRejected("<Event Name=\"Construct\"/>", body, "reserved lifecycle event", 3);
        assertRejected("<Event Name=\"E\" Direction=\"Sideways\"/>", body, "Use In or Out", 3);
        assertRejected("<Event Name=\"E\" Direction=\"Out\" Coalesce=\"true\"/>", body,
                "Coalesce only applies to incoming", 3);
        assertRejected("<Event Name=\"E\" Args=\"x\"/>", body, "must look like name:type", 3);
        assertRejected("<Param Name=\"N\" Type=\"int\" Default=\"many\"/>", body, "not a valid int", 3);
        assertRejected("<Param Name=\"O\" Type=\"object\" Default=\"x\"/>", body, "cannot have a text Default", 3);
        assertRejected("<Param Name=\"A\" Type=\"string\" Extra=\"1\"/>", body, "Unknown attribute 'Extra'", 3);
        assertRejected("<Thing/>", body, "can hold <Param>, <Var> and <Event>", 3);
        assertRejected("", "", "has no body", 0);
        assertRejected("", "<VBox/><VBox/>", "exactly one root widget", 0);
    }

    @Test
    void documentMustBeAComponent() {
        XamlLoadException failure = assertThrows(XamlLoadException.class,
                () -> ComponentDefinition.parse("t:x", "<VBox/>"));
        assertTrue(failure.getMessage().contains("must start with <Component>"), failure.getMessage());
        assertThrows(XamlLoadException.class, () -> ComponentDefinition.parse("t:x", "<Component/>"));
    }

    @Test
    void tagCannotShadowABuiltInWidgetOrAnotherComponent() {
        String shadow = CARD.replace("TestCard", "Button");
        assertThrows(IllegalArgumentException.class, () -> Components.register("t:shadow", shadow));
        register("t:one", CARD);
        IllegalArgumentException clash = assertThrows(IllegalArgumentException.class,
                () -> Components.register("t:two", CARD));
        assertTrue(clash.getMessage().contains("t:one"), clash.getMessage());
    }

    // ------------------------------------------------------------------ создание из Java

    @Test
    void parametersReachTheBodyAndFollowLaterChanges() {
        register("t:card", CARD);
        ComponentInstance card = Xaml.component("t:card").create(Args.of("Title", "Sword", "Price", 50));
        Widget root = card.widget();
        assertEquals("Sword", Xaml.get(root, "title", Label.class).text());
        assertEquals("50", Xaml.get(root, "price", Label.class).text(), "an int parameter feeds a text property");

        card.set("Title", "Axe").set("Price", 7);
        assertEquals("Axe", Xaml.get(root, "title", Label.class).text());
        assertEquals("7", Xaml.get(root, "price", Label.class).text());
    }

    @Test
    void defaultsApplyWhenNothingIsPassed() {
        register("t:card", CARD);
        ComponentInstance card = Xaml.component("t:card").create();
        assertEquals("Untitled", Xaml.get(card.widget(), "title", Label.class).text());
        assertEquals("0", Xaml.get(card.widget(), "price", Label.class).text());
    }

    @Test
    void creationChecksNamesAndTypesAgainstTheContract() {
        register("t:card", CARD);
        var component = Xaml.component("t:card");
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> component.create(Args.of("Titel", "x")));
        assertTrue(unknown.getMessage().contains("no parameter 'Titel'"), unknown.getMessage());
        assertTrue(unknown.getMessage().contains("Title, Price, ProductId"), "lists what exists");
        IllegalArgumentException wrongType = assertThrows(IllegalArgumentException.class,
                () -> component.create(Args.of("Price", "free")));
        assertTrue(wrongType.getMessage().contains("Price"), wrongType.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Xaml.component("t:missing"));
    }

    // ------------------------------------------------------------------ события

    @Test
    void raisedEventCarriesArgumentsFromSameNamedParameters() {
        register("t:card", CARD);
        ComponentInstance card = Xaml.component("t:card").create(Args.of("ProductId", "sword_01"));
        List<Args> bought = new ArrayList<>();
        card.onEvent("Buy", bought::add);

        Xaml.get(card.widget(), "buy", Button.class).click();
        assertEquals(1, bought.size());
        assertEquals("sword_01", bought.get(0).get("ProductId"));
    }

    @Test
    void eventsAreCheckedAgainstTheirDeclaredDirection() {
        register("t:card", CARD);
        ComponentInstance card = Xaml.component("t:card").create();
        IllegalArgumentException wrongWay = assertThrows(IllegalArgumentException.class,
                () -> card.send("Buy", Args.empty()));
        assertTrue(wrongWay.getMessage().contains("outgoing"), wrongWay.getMessage());
        assertThrows(IllegalArgumentException.class, () -> card.onEvent("Invalidate", args -> { }));
        assertThrows(IllegalArgumentException.class, () -> card.emit("Invalidate", Args.empty()));
        assertThrows(IllegalArgumentException.class, () -> card.send("Nope", Args.empty()));
        assertThrows(IllegalArgumentException.class, () -> card.send("Rename", Args.of("name", "x")),
                "unknown argument");
        assertThrows(IllegalArgumentException.class, () -> card.send("Rename", Args.of("title", 5)),
                "argument of the wrong type");
    }

    @Test
    void incomingEventsRunOnFlushAndCoalescedOnesKeepOnlyTheLast() {
        register("t:card", CARD);
        ComponentInstance card = Xaml.component("t:card").create();
        List<Object> invalidated = new ArrayList<>();
        List<Object> renamed = new ArrayList<>();
        card.handle("Invalidate", args -> invalidated.add(args.get("state")));
        card.handle("Rename", args -> renamed.add(args.get("title")));

        card.send("Invalidate", Args.of("state", 1));
        card.send("Rename", Args.of("title", "A"));
        card.send("Invalidate", Args.of("state", 2));
        card.send("Invalidate", Args.of("state", 3));
        card.send("Rename", Args.of("title", "B"));
        assertEquals(5 - 2, card.pendingEvents(), "two Invalidate calls merged into the last one");
        assertTrue(invalidated.isEmpty(), "nothing runs before the frame");

        card.flush();
        assertEquals(List.of(3), invalidated);
        assertEquals(List.of("A", "B"), renamed, "ordinary events are not merged");
        assertEquals(0, card.pendingEvents());
    }

    @Test
    void tickingTheHostDeliversQueuedEvents() {
        register("t:card", CARD);
        ComponentInstance card = Xaml.component("t:card").create();
        int[] seen = {0};
        card.handle("Invalidate", args -> seen[0]++);
        card.send("Invalidate", Args.of("state", 1));
        card.widget().tick(new dev.sixik.unigui.api.core.FrameContext(1, 0.016f, 0.0f,
                dev.sixik.unigui.api.core.FramePhase.ANIMATION));
        assertEquals(1, seen[0]);
    }

    @Test
    void anEventHandlerThatKeepsSendingItselfIsStopped() {
        register("t:card", CARD);
        ComponentInstance card = Xaml.component("t:card").create();
        card.handle("Rename", args -> card.send("Rename", Args.of("title", "again")));
        card.send("Rename", Args.of("title", "go"));
        IllegalStateException storm = assertThrows(IllegalStateException.class, card::flush);
        assertTrue(storm.getMessage().contains("keeps producing"), storm.getMessage());
        assertEquals(0, card.pendingEvents(), "the queue is cleared so the next frame starts clean");
    }

    @Test
    void lifecycleEventsFireAroundTheLifetime() {
        List<String> log = new ArrayList<>();
        register("t:card", CARD).behavior(instance -> {
            instance.handle("Construct", args -> log.add("construct:" + instance.get("Title")));
            instance.handle("Destruct", args -> log.add("destruct"));
        });
        ComponentInstance card = Xaml.component("t:card").create(Args.of("Title", "Sword"));
        assertEquals(List.of("construct:Sword"), log, "Construct sees the parameters already applied");
        card.widget().dispose();
        assertEquals(List.of("construct:Sword", "destruct"), log);
        assertTrue(card.disposed());
        card.send("Rename", Args.of("title", "late")); // после освобождения события молча отбрасываются
        assertEquals(0, card.pendingEvents());
    }

    @Test
    void subscriptionsCanBeCancelled() {
        register("t:card", CARD);
        ComponentInstance card = Xaml.component("t:card").create();
        int[] calls = {0};
        var subscription = card.handle("Rename", args -> calls[0]++);
        card.send("Rename", Args.of("title", "a"));
        card.flush();
        subscription.unsubscribe();
        card.send("Rename", Args.of("title", "b"));
        card.flush();
        assertEquals(1, calls[0]);
    }

    // ------------------------------------------------------------------ использование как тега

    @Test
    void componentWorksAsATagWithAttributeParameters() {
        register("t:card", CARD);
        Widget root = Xaml.load("<VBox><TestCard id=\"c\" Title=\"Shield\" Price=\"9\"/></VBox>");
        ComponentHost host = Xaml.get(root, "c", ComponentHost.class);
        assertEquals("Shield", host.instance().get("Title"));
        assertEquals(9, host.instance().get("Price"));
        assertEquals("Shield", Xaml.get(host, "title", Label.class).text());
        assertEquals("9", Xaml.get(host, "price", Label.class).text());
    }

    @Test
    void tagUsageIsValidatedLikeAnyOtherWidget() {
        register("t:card", CARD);
        XamlLoadException unknown = assertThrows(XamlLoadException.class,
                () -> Xaml.load("<TestCard Titel=\"x\"/>"));
        assertTrue(unknown.getMessage().contains("Unknown attribute 'Titel'"), unknown.getMessage());
        XamlLoadException badValue = assertThrows(XamlLoadException.class,
                () -> Xaml.load("<TestCard Price=\"free\"/>"));
        assertTrue(badValue.getMessage().contains("Price"), badValue.getMessage());
        XamlLoadException children = assertThrows(XamlLoadException.class,
                () -> Xaml.load("<TestCard><Label/></TestCard>"));
        assertTrue(children.getMessage().contains("cannot contain child"), children.getMessage());
    }

    @Test
    void outgoingEventBindsToACommandOnTheTag() {
        register("t:card", CARD);
        List<String> log = new ArrayList<>();
        XamlCommandRegistry commands = XamlCommandRegistry.empty().register("shop.buy", (source, event) -> {
            ComponentEvent component = (ComponentEvent) event;
            log.add(component.name() + ":" + component.args().get("ProductId"));
        });
        Widget root = Xaml.load("<TestCard id=\"c\" ProductId=\"p7\" Buy=\"{Command shop.buy}\"/>",
                LoadOptions.DEFAULT.commands(commands));
        Xaml.get(root, "buy", Button.class).click();
        assertEquals(List.of("Buy:p7"), log);

        XamlLoadException unknown = assertThrows(XamlLoadException.class,
                () -> Xaml.load("<TestCard Buy=\"nope\"/>", LoadOptions.DEFAULT.commands(commands)));
        assertTrue(unknown.getMessage().contains("Unknown XAML command 'nope'"), unknown.getMessage());
    }

    @Test
    void parameterOnTheTagCanBeBoundToOuterData() {
        register("t:card", CARD);
        XamlBindingContext data = XamlBindingContext.empty();
        XamlMutableObservableValue<String> name = data.mutable("product.name", String.class, "Bow");
        Widget root = Xaml.load("<TestCard Title=\"{Binding product.name}\"/>",
                LoadOptions.DEFAULT.bindingContext(data));
        assertEquals("Bow", Xaml.get(root, "title", Label.class).text());
        name.set("Crossbow");
        assertEquals("Crossbow", Xaml.get(root, "title", Label.class).text(), "outer change reaches the body");
    }

    @Test
    void commandsOfTheOuterDocumentStayAvailableInsideTheBody() {
        register("t:action", """
                <Component x:Name="ActionBox" xmlns:x="https://unigui.dev/xaml">
                  <Button id="b" text="Go" onClick="outer.go"/>
                </Component>
                """);
        int[] calls = {0};
        XamlCommandRegistry commands = XamlCommandRegistry.empty().register("outer.go", (s, e) -> calls[0]++);
        Widget root = Xaml.load("<ActionBox/>", LoadOptions.DEFAULT.commands(commands));
        Xaml.get(root, "b", Button.class).click();
        assertEquals(1, calls[0]);
    }

    @Test
    void anInstanceIsFoundByItsKeyAndUpdatedAlone() {
        register("t:card", CARD);
        Widget root = Xaml.load("""
                <VBox>
                  <TestCard key="a" Title="Sword"/>
                  <TestCard key="b" Title="Axe"/>
                </VBox>
                """);
        ComponentInstance second = Components.byKey(root, "b").orElseThrow();
        second.set("Title", "Mace");
        assertEquals("Sword", Components.byKey(root, "a").orElseThrow().get("Title"), "the other card is untouched");
        assertEquals("Mace", second.get("Title"));
        assertTrue(Components.byKey(root, "missing").isEmpty());
        assertTrue(Components.byKey(root, "").isEmpty());
    }

    @Test
    void keyIsSavedWithTheUsage() {
        register("t:card", CARD);
        Widget root = Xaml.load("<TestCard key=\"k1\" Title=\"Sword\"/>");
        String text = XamlFormat.INSTANCE.write(new Snapshotter(Xaml.defaultSchema()).snapshot(root).document());
        assertTrue(text.contains("key=\"k1\""), text);
    }

    @Test
    void keyIsReservedInTheContract() {
        assertThrows(XamlLoadException.class, () -> ComponentDefinition.parse("t:k", """
                <Component x:Name="KeyCard" xmlns:x="https://unigui.dev/xaml">
                  <Component.Contract><Param Name="key" Type="string"/></Component.Contract>
                  <VBox/>
                </Component>
                """));
    }

    // ------------------------------------------------------------------ тело

    @Test
    void bodyMistakesPointAtTheComponentAndTheLine() {
        register("t:broken", """
                <Component x:Name="BrokenCard" xmlns:x="https://unigui.dev/xaml">
                  <Component.Contract>
                    <Param Name="Title" Type="string"/>
                    <Param Name="Count" Type="int"/>
                  </Component.Contract>
                  <VBox>
                    <Label text="{Param Missing}"/>
                  </VBox>
                </Component>
                """);
        XamlLoadException failure = assertThrows(XamlLoadException.class, () -> Xaml.component("t:broken").create());
        assertTrue(failure.getMessage().contains("In component 't:broken'"), failure.getMessage());
        assertTrue(failure.getMessage().contains("no parameter 'Missing'"), failure.getMessage());
        assertTrue(failure.getMessage().contains("Declared: Title, Count"), failure.getMessage());
    }

    @Test
    void incompatibleParameterTypeIsRejectedWhereItIsUsed() {
        register("t:typed", """
                <Component x:Name="TypedCard" xmlns:x="https://unigui.dev/xaml">
                  <Component.Contract><Param Name="Flag" Type="bool"/></Component.Contract>
                  <Label width="{Param Flag}"/>
                </Component>
                """);
        XamlLoadException failure = assertThrows(XamlLoadException.class, () -> Xaml.component("t:typed").create());
        assertTrue(failure.getMessage().contains("Parameter 'Flag' is bool"), failure.getMessage());
    }

    @Test
    void paramAndRaiseOutsideAComponentAreUnknownExtensions() {
        XamlLoadException failure = assertThrows(XamlLoadException.class,
                () -> Xaml.load("<Label text=\"{Param Title}\"/>"));
        assertTrue(failure.getMessage().contains("Unknown markup extension 'Param'"), failure.getMessage());
    }

    @Test
    void raisingAnIncomingEventFromTheBodyIsRejected() {
        register("t:incoming", """
                <Component x:Name="IncomingCard" xmlns:x="https://unigui.dev/xaml">
                  <Component.Contract><Event Name="Ping" Direction="In"/></Component.Contract>
                  <Button onClick="{Raise Ping}"/>
                </Component>
                """);
        XamlLoadException failure = assertThrows(XamlLoadException.class,
                () -> Xaml.component("t:incoming").create());
        assertTrue(failure.getMessage().contains("is incoming"), failure.getMessage());
    }

    @Test
    void componentsNestAndPassParametersDown() {
        register("t:card", CARD);
        register("t:row", """
                <Component x:Name="TestRow" xmlns:x="https://unigui.dev/xaml">
                  <Component.Contract><Param Name="ItemName" Type="string"/></Component.Contract>
                  <VBox><TestCard id="inner" Title="{Param ItemName}"/></VBox>
                </Component>
                """);
        ComponentInstance row = Xaml.component("t:row").create(Args.of("ItemName", "Mace"));
        ComponentHost inner = Xaml.get(row.widget(), "inner", ComponentHost.class);
        assertEquals("Mace", inner.instance().get("Title"));
        assertEquals("Mace", Xaml.get(inner, "title", Label.class).text());
        row.set("ItemName", "Spear");
        assertEquals("Spear", Xaml.get(inner, "title", Label.class).text(), "changes travel through both levels");
    }

    @Test
    void aComponentThatContainsItselfFailsInsteadOfOverflowingTheStack() {
        register("t:loop", """
                <Component x:Name="LoopCard" xmlns:x="https://unigui.dev/xaml">
                  <VBox><LoopCard/></VBox>
                </Component>
                """);
        XamlLoadException failure = assertThrows(XamlLoadException.class, () -> Xaml.component("t:loop").create());
        assertTrue(failure.getMessage().contains("nested"), failure.getMessage());
    }

    @Test
    void objectParametersTakeJavaValuesAndRefuseText() {
        register("t:obj", """
                <Component x:Name="ObjCard" xmlns:x="https://unigui.dev/xaml">
                  <Component.Contract><Param Name="Item" Type="object"/></Component.Contract>
                  <Label id="l" text="{Param Item}"/>
                </Component>
                """);
        record Product(String name) {
            @Override
            public String toString() {
                return "Product " + name;
            }
        }
        ComponentInstance card = Xaml.component("t:obj").create(Args.of("Item", new Product("Gem")));
        assertEquals("Product Gem", Xaml.get(card.widget(), "l", Label.class).text());
        assertThrows(XamlLoadException.class, () -> Xaml.load("<ObjCard Item=\"text\"/>"));
    }

    // ------------------------------------------------------------------ реестр и схема

    @Test
    void reRegisteringReplacesTheComponent() {
        register("t:card", CARD);
        register("t:card", CARD.replace("Untitled", "Replaced"));
        assertEquals("Replaced", Xaml.component("t:card").create().get("Title"));
    }

    @Test
    void unregisteringRemovesTheTagFromNewDocuments() {
        register("t:card", CARD);
        assertNotNull(Xaml.load("<TestCard/>"));
        Components.unregister("t:card");
        assertThrows(XamlLoadException.class, () -> Xaml.load("<TestCard/>"));
        assertThrows(IllegalArgumentException.class, () -> Xaml.component("t:card"));
    }

    @Test
    void theSchemaDescribesAComponentLikeAnyWidget() {
        register("t:card", CARD);
        var type = Xaml.defaultSchema().type("TestCard").orElseThrow();
        assertEquals("Components", type.category());
        assertTrue(type.property("Title").isPresent());
        assertTrue(type.property("Buy").isPresent(), "an outgoing event becomes a command property");
        assertFalse(type.property("Invalidate").isPresent(), "incoming events are not document properties");
        assertTrue(type.property("Title").orElseThrow().bindable());
        assertTrue(type.property("width").isPresent(), "common widget properties still apply");
    }

    // ------------------------------------------------------------------ сохранение

    @Test
    void aComponentUsageSavesBackAndReloadsToTheSameValues() {
        register("t:card", CARD);
        Widget original = Xaml.load("<VBox><TestCard id=\"c\" Title=\"Shield\" Price=\"9\"/></VBox>");
        Snapshotter.Result saved = new Snapshotter(Xaml.defaultSchema()).snapshot(original);
        String text = XamlFormat.INSTANCE.write(saved.document());
        assertTrue(text.contains("<TestCard"), text);
        assertTrue(text.contains("Title=\"Shield\""), text);
        assertTrue(text.contains("Price=\"9\""), text);
        assertFalse(text.contains("<Label"), "the body belongs to the component, not to the usage: " + text);

        Widget reloaded = Xaml.load(text);
        ComponentHost host = Xaml.get(reloaded, "c", ComponentHost.class);
        assertEquals("Shield", host.instance().get("Title"));
        assertEquals(9, host.instance().get("Price"));
        String again = XamlFormat.INSTANCE.write(new Snapshotter(Xaml.defaultSchema()).snapshot(reloaded).document());
        assertEquals(text, again, "saving is a fixed point");
    }

    @Test
    void defaultParametersAreNotWrittenOut() {
        register("t:card", CARD);
        Widget widget = Xaml.load("<TestCard id=\"c\"/>");
        String text = XamlFormat.INSTANCE.write(new Snapshotter(Xaml.defaultSchema()).snapshot(widget).document());
        assertFalse(text.contains("Title="), text);
        assertSame(VBox.class, Xaml.get(widget, "buy", Button.class).parent().getClass());
    }
}
