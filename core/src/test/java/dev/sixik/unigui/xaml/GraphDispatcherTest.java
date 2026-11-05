package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.component.ComponentInstance;
import dev.sixik.unigui.xaml.component.Components;
import dev.sixik.unigui.xaml.component.ParamType;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.logic.GraphFunction;
import dev.sixik.unigui.xaml.logic.GraphFunctions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphDispatcherTest {
    private final List<String> components = new ArrayList<>();
    private final List<String> functions = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        components.forEach(Components::unregister);
        functions.forEach(GraphFunctions::unregister);
    }

    private void register(String id, String source) {
        Components.register(id, source);
        components.add(id);
    }

    private void function(GraphFunction function) {
        GraphFunctions.register(function);
        functions.add(function.name());
    }

    private static String component(String name, String contract, String body, String graph) {
        return "<Component x:Name=\"" + name + "\" xmlns:x=\"https://unigui.dev/xaml\">\n"
                + "<Component.Contract>\n" + contract + "\n</Component.Contract>\n"
                + body + "\n<Component.Graph>\n" + graph + "\n</Component.Graph>\n</Component>";
    }

    private static final String LABEL = "<Label id=\"label\" text=\"x\"/>";

    private static void tick(ComponentInstance instance, float seconds) {
        instance.widget().tick(new FrameContext(1, seconds, 0.0f, FramePhase.ANIMATION));
    }

    // ------------------------------------------------------------------ переменные

    @Test
    void variablesKeepStateBetweenRunsAndStayPrivate() {
        register("d:vars", component("VarBox", """
                <Param Name="Shown" Type="int" Default="0"/>
                <Var Name="clicks" Type="int" Default="10"/>
                <Event Name="Click" Direction="In"/>""", LABEL, """
                <Node Id="in" Type="Event.Click"/>
                <Node Id="get" Type="Var.Get" Name="clicks"/>
                <Node Id="inc" Type="Math.Add" b="1"/>
                <Node Id="store" Type="Var.Set" Name="clicks"/>
                <Node Id="show" Type="Param.Set" Name="Shown"/>
                <Node Id="now" Type="Var.Get" Name="clicks"/>
                <Wire From="in.then" To="store.exec"/>
                <Wire From="get.value" To="inc.a"/>
                <Wire From="inc.result" To="store.value"/>
                <Wire From="store.then" To="show.exec"/>
                <Wire From="now.value" To="show.value"/>"""));
        ComponentInstance box = Xaml.component("d:vars").create();
        for (int i = 0; i < 3; i++) {
            box.send("Click", Args.empty());
            box.flush();
        }
        assertEquals(13, box.get("Shown"), "the variable started at 10 and was incremented three times");
        assertThrows(IllegalArgumentException.class, () -> box.get("clicks"), "a variable is not a parameter");
        assertTrue(Components.find("d:vars").orElseThrow().contract().var("clicks").isPresent());
    }

    // ------------------------------------------------------------------ пользовательские события

    @Test
    void customEventsAreReusableSubroutinesWithArguments() {
        register("d:custom", component("CustomBox", """
                <Param Name="Total" Type="int" Default="0"/>
                <Event Name="Go" Direction="In"/>""", LABEL, """
                <Node Id="in" Type="Event.Go"/>
                <Node Id="bumpA" Type="Event.Call" Name="bump" by="5"/>
                <Node Id="bumpB" Type="Event.Call" Name="bump" by="7"/>
                <Node Id="bump" Type="Event.Custom" Name="bump" Args="by:int"/>
                <Node Id="total" Type="Param.Get" Name="Total"/>
                <Node Id="sum" Type="Math.Add"/>
                <Node Id="set" Type="Param.Set" Name="Total"/>
                <Wire From="in.then" To="bumpA.exec"/>
                <Wire From="bumpA.then" To="bumpB.exec"/>
                <Wire From="bump.then" To="set.exec"/>
                <Wire From="total.value" To="sum.a"/>
                <Wire From="bump.by" To="sum.b"/>
                <Wire From="sum.result" To="set.value"/>"""));
        ComponentInstance box = Xaml.component("d:custom").create();
        box.send("Go", Args.empty());
        box.flush();
        assertEquals(12, box.get("Total"));
    }

    @Test
    void aCustomEventThatCallsItselfIsStopped() {
        register("d:recur", component("RecurBox", "<Event Name=\"Go\" Direction=\"In\"/>", LABEL, """
                <Node Id="in" Type="Event.Go"/>
                <Node Id="start" Type="Event.Call" Name="again"/>
                <Node Id="again" Type="Event.Custom" Name="again"/>
                <Node Id="inner" Type="Event.Call" Name="again"/>
                <Wire From="in.then" To="start.exec"/>
                <Wire From="again.then" To="inner.exec"/>"""));
        ComponentInstance box = Xaml.component("d:recur").create();
        box.send("Go", Args.empty());
        RuntimeException failure = assertThrows(RuntimeException.class, box::flush);
        assertTrue(failure.getMessage().contains("levels deep"), failure.getMessage());
    }

    // ------------------------------------------------------------------ время

    private static final String DELAY_CONTRACT = """
            <Param Name="Title" Type="string" Default="idle"/>
            <Param Name="Count" Type="int" Default="0"/>
            <Event Name="Go" Direction="In" Args="n:int"/>
            <Event Name="Stop" Direction="In"/>""";

    private static final String DELAY_GRAPH = """
            <Node Id="in" Type="Event.Go"/>
            <Node Id="wait" Type="Flow.Delay" seconds="2"/>
            <Node Id="late" Type="Param.Set" Name="Title" value="late"/>
            <Node Id="keep" Type="Param.Set" Name="Count"/>
            <Node Id="stop" Type="Event.Stop"/>
            <Node Id="cancel" Type="Flow.CancelDelay" Target="wait"/>
            <Wire From="in.then" To="wait.exec"/>
            <Wire From="wait.then" To="late.exec"/>
            <Wire From="late.then" To="keep.exec"/>
            <Wire From="in.n" To="keep.value"/>
            <Wire From="stop.then" To="cancel.exec"/>""";

    @Test
    void delayContinuesTheChainWhenEnoughTimeHasPassedAndKeepsEventArguments() {
        register("d:delay", component("DelayBox", DELAY_CONTRACT, LABEL, DELAY_GRAPH));
        ComponentInstance box = Xaml.component("d:delay").create();
        box.send("Go", Args.of("n", 42));
        box.flush();
        assertEquals("idle", box.get("Title"), "nothing happens before the delay is over");

        box.advance(1.0f);
        assertEquals("idle", box.get("Title"));
        box.advance(1.5f);
        assertEquals("late", box.get("Title"));
        assertEquals(42, box.get("Count"), "the argument of the starting event survives the pause");

        box.advance(10.0f);
        assertEquals("late", box.get("Title"), "the delay fires once");
    }

    @Test
    void startingADelayAgainRestartsItAndCancelStopsIt() {
        register("d:retrigger", component("RetriggerBox", DELAY_CONTRACT, LABEL, DELAY_GRAPH));
        ComponentInstance box = Xaml.component("d:retrigger").create();
        box.send("Go", Args.of("n", 1));
        box.flush();
        box.advance(1.5f);
        box.send("Go", Args.of("n", 2));
        box.flush();
        box.advance(1.5f);
        assertEquals("idle", box.get("Title"), "the second start replaced the first wait");
        box.advance(0.6f);
        assertEquals(2, box.get("Count"));

        ComponentInstance other = Xaml.component("d:retrigger").create();
        other.send("Go", Args.of("n", 3));
        other.flush();
        other.send("Stop", Args.empty());
        other.flush();
        other.advance(5.0f);
        assertEquals("idle", other.get("Title"), "a cancelled delay never continues");
    }

    @Test
    void theHostTickMovesTimeForward() {
        register("d:tick", component("TickBox", DELAY_CONTRACT, LABEL, DELAY_GRAPH));
        ComponentInstance box = Xaml.component("d:tick").create();
        box.send("Go", Args.of("n", 9));
        tick(box, 1.0f); // flush, then time advances by one second
        assertEquals("idle", box.get("Title"));
        tick(box, 1.5f);
        assertEquals("late", box.get("Title"));
    }

    @Test
    void aDisposedComponentDropsItsPendingDelays() {
        register("d:gone", component("GoneBox", DELAY_CONTRACT, LABEL, DELAY_GRAPH));
        ComponentInstance box = Xaml.component("d:gone").create();
        box.send("Go", Args.of("n", 1));
        box.flush();
        box.widget().dispose();
        box.advance(10.0f);
        assertEquals("idle", box.get("Title"));
    }

    // ------------------------------------------------------------------ компоненты и диспетчеры

    private static final String CARD = """
            <Component x:Name="ShopCard" xmlns:x="https://unigui.dev/xaml">
              <Component.Contract>
                <Param Name="Title" Type="string" Default=""/>
                <Param Name="ProductId" Type="string" Default=""/>
                <Event Name="Buy" Direction="Out" Args="ProductId:string"/>
              </Component.Contract>
              <HBox>
                <Label id="name" text="{Param Title}"/>
                <Button id="buy" text="Buy" onClick="{Raise Buy}"/>
              </HBox>
            </Component>""";

    private static final String SHOP_CONTRACT = """
            <Event Name="Show" Direction="In" Args="id:string,title:string"/>
            <Event Name="Hide" Direction="In"/>
            <Event Name="Clear" Direction="In"/>""";

    private static final String SHOP_BODY = "<VBox id=\"list\"/>";

    private static final String SHOP_GRAPH = """
            <Node Id="show" Type="Event.Show"/>
            <Node Id="make" Type="Component.Create" Component="d:card" Title="" ProductId=""/>
            <Node Id="add" Type="Widget.AddChild" Target="list"/>
            <Node Id="bind" Type="Dispatcher.Bind" Event="Buy" Handler="onBuy"/>
            <Node Id="onBuy" Type="Event.Custom" Name="onBuy" Args="ProductId:string"/>
            <Node Id="record" Type="Call.d.record"/>
            <Node Id="clear" Type="Event.Clear"/>
            <Node Id="wipe" Type="Widget.Clear" Target="list"/>
            <Wire From="show.then" To="make.exec"/>
            <Wire From="show.id" To="make.ProductId"/>
            <Wire From="show.title" To="make.Title"/>
            <Wire From="make.then" To="add.exec"/>
            <Wire From="make.instance" To="add.child"/>
            <Wire From="add.then" To="bind.exec"/>
            <Wire From="make.instance" To="bind.target"/>
            <Wire From="onBuy.then" To="record.exec"/>
            <Wire From="onBuy.ProductId" To="record.id"/>
            <Wire From="onBuy.sender" To="record.card"/>
            <Wire From="clear.then" To="wipe.exec"/>""";

    private List<String> purchases;
    private List<Object> senders;

    private ComponentInstance shop() {
        purchases = new ArrayList<>();
        senders = new ArrayList<>();
        function(GraphFunction.define("d.record").param("id", ParamType.STRING).param("card", ParamType.OBJECT)
                .body(args -> {
                    purchases.add((String) args.get("id"));
                    senders.add(args.get("card"));
                    return null;
                }));
        register("d:card", CARD);
        register("d:shop", component("ShopBox", SHOP_CONTRACT, SHOP_BODY, SHOP_GRAPH));
        return Xaml.component("d:shop").create();
    }

    private static PanelWidget list(ComponentInstance shop) {
        return (PanelWidget) Xaml.get(shop.widget(), "list");
    }

    private static void apply(Widget widget) {
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) apply(child);
    }

    @Test
    void aGraphCreatesCardsAddsThemAndHearsTheirPurchases() {
        ComponentInstance shop = shop();
        shop.send("Show", Args.of("id", "sword", "title", "Sword"));
        shop.send("Show", Args.of("id", "axe", "title", "Axe"));
        shop.flush();

        PanelWidget list = list(shop);
        assertEquals(2, list.children().size(), "two cards were created and added by the graph");

        Button buy = (Button) Xaml.find(list.children().get(1), "buy").orElseThrow();
        buy.click();
        assertEquals(List.of("axe"), purchases, "the card's outgoing event reached the shop graph with its argument");
        assertTrue(senders.get(0) instanceof ComponentInstance, "the handler also knows which card raised it");
        assertEquals("axe", ((ComponentInstance) senders.get(0)).get("ProductId"));

        ((Button) Xaml.find(list.children().get(0), "buy").orElseThrow()).click();
        assertEquals(List.of("axe", "sword"), purchases);
    }

    @Test
    void clearingRemovesAndDisposesTheCreatedCards() {
        ComponentInstance shop = shop();
        shop.send("Show", Args.of("id", "a", "title", "A"));
        shop.flush();
        PanelWidget list = list(shop);
        Widget card = list.children().get(0);
        Button buy = (Button) Xaml.find(card, "buy").orElseThrow();

        shop.send("Clear", Args.empty());
        shop.flush();
        assertEquals(0, list.children().size());
        buy.click();
        assertTrue(purchases.isEmpty(), "a removed card no longer reaches the shop");
    }

    @Test
    void disposingTheShopDisposesItsCards() {
        ComponentInstance shop = shop();
        shop.send("Show", Args.of("id", "a", "title", "A"));
        shop.flush();
        Widget card = list(shop).children().get(0);
        Button buy = (Button) Xaml.find(card, "buy").orElseThrow();
        shop.widget().dispose();
        buy.click();
        assertTrue(purchases.isEmpty());
    }

    @Test
    void unbindingStopsTheNotifications() {
        purchases = new ArrayList<>();
        function(GraphFunction.define("d.record").param("id", ParamType.STRING).param("card", ParamType.OBJECT)
                .body(args -> {
                    purchases.add((String) args.get("id"));
                    return null;
                }));
        register("d:card", CARD);
        register("d:unbind", component("UnbindBox", """
                <Event Name="Show" Direction="In"/>
                <Event Name="Silence" Direction="In"/>
                <Var Name="card" Type="object"/>""", SHOP_BODY, """
                <Node Id="show" Type="Event.Show"/>
                <Node Id="make" Type="Component.Create" Component="d:card" ProductId="p1"/>
                <Node Id="add" Type="Widget.AddChild" Target="list"/>
                <Node Id="bind" Type="Dispatcher.Bind" Event="Buy" Handler="onBuy"/>
                <Node Id="keep" Type="Var.Set" Name="card"/>
                <Node Id="onBuy" Type="Event.Custom" Name="onBuy" Args="ProductId:string"/>
                <Node Id="record" Type="Call.d.record"/>
                <Node Id="silence" Type="Event.Silence"/>
                <Node Id="saved" Type="Var.Get" Name="card"/>
                <Node Id="unbind" Type="Dispatcher.Unbind" Event="Buy"/>
                <Wire From="show.then" To="make.exec"/>
                <Wire From="make.then" To="add.exec"/>
                <Wire From="make.instance" To="add.child"/>
                <Wire From="add.then" To="bind.exec"/>
                <Wire From="make.instance" To="bind.target"/>
                <Wire From="bind.then" To="keep.exec"/>
                <Wire From="make.instance" To="keep.value"/>
                <Wire From="onBuy.then" To="record.exec"/>
                <Wire From="onBuy.ProductId" To="record.id"/>
                <Wire From="onBuy.sender" To="record.card"/>
                <Wire From="silence.then" To="unbind.exec"/>
                <Wire From="saved.value" To="unbind.target"/>"""));
        ComponentInstance shop = Xaml.component("d:unbind").create();
        shop.send("Show", Args.empty());
        shop.flush();
        Button buy = (Button) Xaml.find(list(shop).children().get(0), "buy").orElseThrow();
        buy.click();
        assertEquals(1, purchases.size());

        shop.send("Silence", Args.empty());
        shop.flush();
        buy.click();
        assertEquals(1, purchases.size(), "after Dispatcher.Unbind the shop no longer hears the card");
    }

    @Test
    void aProgramIsRecompiledWhenAMissingComponentGetsRegistered() {
        register("d:late", component("LateShop", "<Event Name=\"Go\" Direction=\"In\"/>", SHOP_BODY, """
                <Node Id="in" Type="Event.Go"/>
                <Node Id="make" Type="Component.Create" Component="d:later"/>
                <Wire From="in.then" To="make.exec"/>"""));
        XamlLoadException missing = assertThrows(XamlLoadException.class, () -> Xaml.component("d:late").create());
        assertTrue(missing.getMessage().contains("component 'd:later' which is not registered"), missing.getMessage());

        register("d:later", CARD.replace("ShopCard", "LaterCard"));
        ComponentInstance shop = Xaml.component("d:late").create();
        shop.send("Go", Args.empty());
        shop.flush();
    }

    // ------------------------------------------------------------------ ошибки

    private void assertRejected(String contract, String graph, String fragment) {
        ComponentDefinition definition = ComponentDefinition.parse("d:probe",
                component("ProbeBox", contract, LABEL, graph));
        XamlLoadException failure = assertThrows(XamlLoadException.class, definition::program, fragment);
        assertTrue(failure.getMessage().contains(fragment), "expected '" + fragment + "' in: " + failure.getMessage());
        assertTrue(failure.diagnostics().get(0).hasLocation(), failure.getMessage());
    }

    @Test
    void newNodeKindsAreCheckedBeforeAnythingRuns() {
        String contract = "<Event Name=\"Go\" Direction=\"In\"/><Var Name=\"n\" Type=\"int\"/>";
        assertRejected(contract, "<Node Id=\"a\" Type=\"Var.Get\" Name=\"ghost\"/>", "variable 'ghost'");
        assertRejected(contract, "<Node Id=\"a\" Type=\"Var.Get\"/>", "requires Name");
        assertRejected(contract, "<Node Id=\"a\" Type=\"Event.Call\" Name=\"nope\"/>", "custom event 'nope'");
        assertRejected(contract, "<Node Id=\"a\" Type=\"Event.Custom\"/>", "requires Name");
        assertRejected(contract, "<Node Id=\"a\" Type=\"Event.Custom\" Name=\"Go\"/>", "clashes with an event");
        assertRejected(contract, "<Node Id=\"a\" Type=\"Event.Custom\" Name=\"x\"/>"
                + "<Node Id=\"b\" Type=\"Event.Custom\" Name=\"x\"/>", "declared twice");
        assertRejected(contract, "<Node Id=\"a\" Type=\"Event.Custom\" Name=\"x\" Args=\"sender:int\"/>", "reserved or repeated");
        assertRejected(contract, "<Node Id=\"a\" Type=\"Flow.CancelDelay\" Target=\"zzz\"/>", "Flow.Delay");
        assertRejected(contract, "<Node Id=\"a\" Type=\"Component.Create\" Component=\"d:none\"/>", "not registered");
        assertRejected(contract, "<Node Id=\"a\" Type=\"Widget.AddChild\"/>", "requires Target");
        assertRejected(contract, "<Node Id=\"a\" Type=\"Dispatcher.Bind\" Event=\"Buy\" Handler=\"none\"/>", "not a custom event");
        assertRejected(contract, "<Node Id=\"a\" Type=\"Widget.Clear\"/>", "requires Target");
    }

    @Test
    void componentReferencesCannotBeUsedAsNumbersOrText() {
        register("d:card", CARD);
        assertRejected("<Event Name=\"Go\" Direction=\"In\"/>", """
                <Node Id="in" Type="Event.Go"/>
                <Node Id="make" Type="Component.Create" Component="d:card"/>
                <Node Id="sum" Type="Math.Add"/>
                <Wire From="in.then" To="make.exec"/>
                <Wire From="make.instance" To="sum.a"/>""", "joins component to float");
        assertRejected("<Event Name=\"Go\" Direction=\"In\"/>", """
                <Node Id="a" Type="Dispatcher.Bind" Event="Buy" Handler="h" target="x"/>
                <Node Id="h" Type="Event.Custom" Name="h"/>""", "cannot be written as text");
    }

    @Test
    void contractVariablesAreValidatedLikeParameters() {
        for (String[] bad : new String[][]{
                {"<Var Name=\"a\" Type=\"int\"/><Var Name=\"a\" Type=\"int\"/>", "Duplicate name"},
                {"<Param Name=\"a\" Type=\"int\"/><Var Name=\"a\" Type=\"int\"/>", "Duplicate name"},
                {"<Var Name=\"a\" Type=\"money\"/>", "Unknown parameter type"},
                {"<Var Type=\"int\"/>", "requires Name"},
                {"<Var Name=\"Construct\" Type=\"int\"/>", "reserved lifecycle"}}) {
            XamlLoadException failure = assertThrows(XamlLoadException.class,
                    () -> ComponentDefinition.parse("d:x", component("BadVars", bad[0], LABEL, "")), bad[0]);
            assertTrue(failure.getMessage().contains(bad[1]), bad[0] + " -> " + failure.getMessage());
        }
        assertSame(ParamType.INT, ComponentDefinition.parse("d:ok",
                component("OkVars", "<Var Name=\"width\" Type=\"int\"/>", LABEL, "")).contract().var("width").orElseThrow().type(),
                "a variable may share a name with a widget property because it is not one");
        assertFalse(Components.ids().contains("d:ok"));
    }
}
