package dev.sixik.unigui.xaml;

import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.component.ComponentHost;
import dev.sixik.unigui.xaml.component.ContractEdits;
import dev.sixik.unigui.xaml.component.ParamType;
import dev.sixik.unigui.xaml.component.Promotions;
import dev.sixik.unigui.xaml.component.Renames;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.load.DocumentValidator;
import dev.sixik.unigui.xaml.logic.NodeCatalog;
import dev.sixik.unigui.xaml.preview.ComponentPreview;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Сценарии редактора контракта: выбор типов, события в обе стороны, переименование и привязка свойств. */
final class ContractWorkflowTest {
    private static String component(String contract, String body, String graph) {
        return "<Component x:Name=\"Card\" xmlns:x=\"https://unigui.dev/xaml\">\n<Component.Contract>\n" + contract
                + "\n</Component.Contract>\n" + body + "\n<Component.Graph>\n" + graph
                + "\n</Component.Graph>\n</Component>";
    }

    private static boolean clean(String text) {
        return new DocumentValidator(Xaml.defaultSchema()).validate(text).valid();
    }

    private static ComponentPreview show(String text) {
        ComponentPreview preview = new ComponentPreview(Xaml.defaultSchema(), null);
        var result = preview.show(EditorSession.create(text).document());
        assertTrue(result.shown(), result.message());
        return preview;
    }

    private static <T> T widget(ComponentPreview preview, EditorSession ignored, String id, Class<T> type) {
        return type.cast(find(preview.host().orElseThrow(), id));
    }

    private static dev.sixik.unigui.api.widget.Widget find(dev.sixik.unigui.api.widget.Widget widget, String id) {
        if (id.equals(widget.id())) return widget;
        for (dev.sixik.unigui.api.widget.Widget child : widget.children()) {
            var found = find(child, id);
            if (found != null) return found;
        }
        return null;
    }

    // ------------------------------------------------------------------ типы

    @Test
    void theTypeListComesFromTheRegistry() {
        assertTrue(ParamType.names().containsAll(List.of("string", "int", "float", "double", "bool", "color")));
        assertFalse(ParamType.textualNames().contains("object"), "an object has no text form");
    }

    @Test
    void changingTheTypeDropsADefaultThatNoLongerFits() {
        EditorSession session = EditorSession.create(component(
                "<Param Name=\"A\" Type=\"string\" Default=\"hello\"/><Param Name=\"B\" Type=\"string\" Default=\"5\"/>",
                "<Label/>", ""));
        var root = session.document().root();
        assertTrue(session.apply(ContractEdits.setType(root, "Param", "A", "int")), "the old default must not block it");
        assertFalse(session.text().contains("hello"), session.text());
        assertTrue(session.apply(ContractEdits.setType(root, "Param", "B", "int")));
        assertTrue(session.text().contains("Default=\"5\""), "a default that still fits stays: " + session.text());
    }

    @Test
    void attributeOptionsReplaceTyping() {
        ComponentDefinition definition = ComponentDefinition.parse("t", component(
                "<Param Name=\"Title\" Type=\"string\"/><Var Name=\"Seen\" Type=\"int\"/>", "<Label/>", ""));
        var contract = definition.contract();
        assertEquals(List.of("==", "!=", "<", "<=", ">", ">="),
                NodeCatalog.attributeOptions(contract, "Math.Compare", "Op", Map.of(), List.of(), List.of()));
        assertEquals(List.of("Title"),
                NodeCatalog.attributeOptions(contract, "Param.Get", "Name", Map.of(), List.of(), List.of()));
        assertEquals(List.of("Seen"),
                NodeCatalog.attributeOptions(contract, "Var.Set", "Name", Map.of(), List.of(), List.of()));
        assertEquals(List.of("a", "b"),
                NodeCatalog.attributeOptions(contract, "Widget.Set", "Target", Map.of(), List.of("a", "b"), List.of()));
        assertEquals(List.of("false", "true"), NodeCatalog.attributeOptions(contract, "Value.Literal", "Value",
                Map.of("ValueType", "bool"), List.of(), List.of()));
        assertTrue(NodeCatalog.attributeOptions(contract, "Value.Literal", "Value", Map.of("ValueType", "string"),
                List.of(), List.of()).isEmpty(), "free text stays free");
    }

    // ------------------------------------------------------------------ события

    @Test
    void anOutgoingEventCanBeHandledInTheGraph() {
        ComponentPreview preview = show(component(
                "<Param Name=\"Title\" Type=\"string\"/><Event Name=\"Buy\" Direction=\"Out\"/>", "<Label/>", """
                <Node Id="on" Type="Event.Buy"/>
                <Node Id="set" Type="Param.Set" Name="Title" value="bought"/>
                <Wire From="on.then" To="set.exec"/>"""));
        var instance = preview.host().orElseThrow().instance();
        List<Args> outside = new ArrayList<>();
        instance.onEvent("Buy", outside::add);
        instance.emit("Buy", Args.empty());
        assertEquals("bought", instance.get("Title"), "the On node runs when the event is raised");
        assertEquals(1, outside.size(), "the parent still gets the event");
    }

    @Test
    void anIncomingEventCanBeRaisedFromTheGraph() {
        ComponentPreview preview = show(component(
                "<Param Name=\"Title\" Type=\"string\"/><Event Name=\"Go\" Direction=\"In\"/>"
                        + "<Event Name=\"Reset\" Direction=\"In\"/>", "<Label/>", """
                <Node Id="go" Type="Event.Go"/>
                <Node Id="raise" Type="Raise.Reset"/>
                <Node Id="on" Type="Event.Reset"/>
                <Node Id="set" Type="Param.Set" Name="Title" value="reset"/>
                <Wire From="go.then" To="raise.exec"/>
                <Wire From="on.then" To="set.exec"/>"""));
        preview.send("Go", Args.empty());
        assertEquals("reset", preview.host().orElseThrow().instance().get("Title"));
    }

    @Test
    void anEventThatRaisesItselfStopsWithAReason() {
        ComponentPreview preview = show(component("<Event Name=\"Ping\" Direction=\"Out\"/>", "<Label/>", """
                <Node Id="on" Type="Event.Ping"/>
                <Node Id="raise" Type="Raise.Ping"/>
                <Wire From="on.then" To="raise.exec"/>"""));
        var instance = preview.host().orElseThrow().instance();
        RuntimeException failure = assertThrows(RuntimeException.class, () -> instance.emit("Ping", Args.empty()));
        assertTrue(String.valueOf(failure.getMessage()).contains("Ping") || failure.getCause() != null, failure.toString());
    }

    @Test
    void customEventsAreTitledLikeContractEvents() {
        var contract = ComponentDefinition.parse("t", component("", "<Label/>", "")).contract();
        assertEquals("On Ready", NodeCatalog.titleOf(contract, "Event.Custom", Map.of("Name", "Ready")));
        assertEquals("Raise Ready", NodeCatalog.titleOf(contract, "Event.Call", Map.of("Name", "Ready")));
    }

    @Test
    void renamingACustomEventUpdatesEveryNode() {
        EditorSession session = EditorSession.create(component("", "<Label/>", """
                <Node Id="on" Type="Event.Custom" Name="Ready"/>
                <Node Id="call" Type="Event.Call" Name="Ready"/>"""));
        assertTrue(Renames.customEvent(session, "Ready", "Loaded"), session.diagnostics(EditorSession.Channel.EDIT).toString());
        assertFalse(session.text().contains("Ready"), session.text());
        assertTrue(clean(session.text()));
    }

    @Test
    void renamingAnArgumentKeepsItsWiresAndValues() {
        EditorSession session = EditorSession.create(component(
                "<Param Name=\"Title\" Type=\"string\"/><Event Name=\"Buy\" Direction=\"In\" Args=\"id:string\"/>"
                        + "<Event Name=\"Sold\" Direction=\"Out\" Args=\"id:string\"/>", "<Label/>", """
                <Node Id="on" Type="Event.Buy"/>
                <Node Id="set" Type="Param.Set" Name="Title"/>
                <Wire From="on.then" To="set.exec"/>
                <Wire From="on.id" To="set.value"/>"""));
        assertTrue(Renames.argument(session, "Buy", "item:string", "id", "item"),
                session.diagnostics(EditorSession.Channel.EDIT).toString());
        assertTrue(session.text().contains("From=\"on.item\""), session.text());
        assertTrue(clean(session.text()), session.text());
    }

    @Test
    void removingAnArgumentRemovesItsWires() {
        EditorSession session = EditorSession.create(component(
                "<Param Name=\"Title\" Type=\"string\" Default=\"x\"/><Event Name=\"Buy\" Direction=\"In\" Args=\"id:string\"/>",
                "<Label/>", """
                <Node Id="on" Type="Event.Buy"/>
                <Node Id="set" Type="Param.Set" Name="Title" value="y"/>
                <Wire From="on.then" To="set.exec"/>
                <Wire From="on.id" To="set.value"/>"""));
        assertTrue(Renames.arguments(session, "Buy", ""), session.diagnostics(EditorSession.Channel.EDIT).toString());
        assertFalse(session.text().contains("on.id"), session.text());
        assertTrue(session.text().contains("on.then"), "the flow wire stays: " + session.text());
    }

    // ------------------------------------------------------------------ привязка свойств

    private static final String CARD = component(
            "<Event Name=\"Go\" Direction=\"In\"/><Event Name=\"Sell\" Direction=\"Out\"/>",
            "<VBox><Label id=\"title\" text=\"Hello\"/><Button id=\"buy\" onClick=\"{Raise Sell}\"/></VBox>", "");

    private static UiNode node(EditorSession session, String id) {
        return session.document().root().children().get(0).children().stream()
                .filter(child -> id.equals(child.attributeText("id", ""))).findFirst().orElseThrow();
    }

    @Test
    void aPropertyBecomesAParameterWithItsCurrentValue() {
        EditorSession session = EditorSession.create(CARD);
        UiNode title = node(session, "title");
        assertTrue(Promotions.supported(session, title, "text"));
        assertEquals("TitleText", Promotions.promote(session, title, "text", Promotions.Kind.PARAM).orElseThrow());
        assertTrue(session.text().contains("<Param Name=\"TitleText\" Type=\"string\" Default=\"Hello\"/>"), session.text());
        assertTrue(session.text().contains("text=\"{Param TitleText}\""), session.text());
        assertEquals("TitleText", Promotions.binding(title, "text").orElseThrow());
        assertFalse(Promotions.supported(session, title, "text"), "it is bound already");

        ComponentPreview preview = show(session.text());
        assertEquals("Hello", widget(preview, session, "title", Label.class).text(), "the look does not change");
    }

    @Test
    void aPromotionIsOneUndoStep() {
        EditorSession session = EditorSession.create(CARD);
        Promotions.promote(session, node(session, "title"), "text", Promotions.Kind.VARIABLE).orElseThrow();
        assertTrue(session.undo());
        assertFalse(session.text().contains("TitleText"), session.text());
        assertTrue(session.text().contains("text=\"Hello\""), session.text());
        assertFalse(session.undo(), "one step undoes the contract entry and the binding together");
    }

    @Test
    void aVariableDrivesTheBoundPropertyFromTheGraph() {
        EditorSession session = EditorSession.create(CARD);
        String name = Promotions.promote(session, node(session, "title"), "text", Promotions.Kind.VARIABLE).orElseThrow();
        assertTrue(session.text().contains("{Var " + name + "}"), session.text());
        assertTrue(clean(session.text()), session.text());
        Renames.variable(session, name, "Caption");
        assertTrue(session.text().contains("{Var Caption}"), "a rename follows the binding: " + session.text());

        String wired = session.text().replace("</Component.Graph>", """
                <Node Id="go" Type="Event.Go"/>
                <Node Id="set" Type="Var.Set" Name="Caption" value="Changed"/>
                <Wire From="go.then" To="set.exec"/>
                </Component.Graph>""");
        ComponentPreview preview = show(wired);
        assertEquals("Hello", widget(preview, session, "title", Label.class).text());
        preview.send("Go", Args.empty());
        assertEquals("Changed", widget(preview, session, "title", Label.class).text());
    }

    @Test
    void unbindingRestoresAPlainValue() {
        EditorSession session = EditorSession.create(CARD);
        UiNode title = node(session, "title");
        Promotions.promote(session, title, "text", Promotions.Kind.PARAM).orElseThrow();
        assertTrue(Promotions.unbind(session, title, "text"));
        assertTrue(session.text().contains("text=\"Hello\""), session.text());
        assertTrue(Promotions.binding(title, "text").isEmpty());
    }

    @Test
    void identifiersActionsAndScreensCannotBePromoted() {
        EditorSession session = EditorSession.create(CARD);
        assertFalse(Promotions.supported(session, node(session, "title"), "id"));
        assertFalse(Promotions.supported(session, node(session, "buy"), "onClick"));
        EditorSession screen = EditorSession.create("<VBox><Label id=\"a\" text=\"x\"/></VBox>");
        assertFalse(Promotions.supported(screen, screen.document().root().children().get(0), "text"),
                "a screen has no contract");
    }

    @Test
    void repeatedPromotionsGetDistinctNames() {
        EditorSession session = EditorSession.create(component("<Param Name=\"TitleText\" Type=\"string\"/>",
                "<VBox><Label id=\"title\" text=\"Hello\"/></VBox>", ""));
        assertEquals("TitleText2",
                Promotions.promote(session, node(session, "title"), "text", Promotions.Kind.PARAM).orElseThrow());
    }

    // ------------------------------------------------------------------ свойства без своего типа

    private static final String PANEL = component("<Event Name=\"Go\" Direction=\"In\"/>",
            "<VBox id=\"body\"><Box id=\"panel\" width=\"100\" height=\"20\"/><Label id=\"title\" text=\"Hi\"/></VBox>", "");

    private static void layout(dev.sixik.unigui.api.widget.Widget root) {
        root.measure(new dev.sixik.unigui.api.layout.LayoutContext(800.0f, 600.0f));
        root.arrange(new dev.sixik.unigui.api.math.MutableRect(0.0f, 0.0f, 800.0f, 600.0f));
    }

    private static String wired(EditorSession session, String kind, String name, String value) {
        return session.text().replace("</Component.Graph>", """
                <Node Id="go" Type="Event.Go"/>
                <Node Id="set" Type="%s.Set" Name="%s" value="%s"/>
                <Wire From="go.then" To="set.exec"/>
                </Component.Graph>""".formatted(kind, name, value));
    }

    @Test
    void everyValuePropertyOfAWidgetCanBePromoted() {
        EditorSession session = EditorSession.create(PANEL);
        UiNode panel = node(session, "panel");
        var info = session.schema().type(panel.type()).orElseThrow();
        List<String> refused = new ArrayList<>();
        for (var property : info.properties()) {
            boolean action = property.kind() == dev.sixik.unigui.xaml.schema.ValueKind.BINDING_OR_ACTION;
            boolean identity = property.name().equals("id") || property.name().equals("class");
            if (!action && !identity && !Promotions.supported(session, panel, property.name())) refused.add(property.name());
        }
        assertEquals(List.of(), refused);
    }

    @Test
    void aSizeBecomesAVariableAndTheGraphResizesTheWidget() {
        EditorSession session = EditorSession.create(PANEL);
        String name = Promotions.promote(session, node(session, "panel"), "width", Promotions.Kind.VARIABLE).orElseThrow();
        assertEquals("PanelWidth", name);
        assertTrue(session.text().contains("<Var Name=\"PanelWidth\" Type=\"string\" Default=\"100\"/>"), session.text());
        assertTrue(clean(session.text()), session.text());

        ComponentPreview preview = show(wired(session, "Var", name, "240"));
        var host = preview.host().orElseThrow();
        var panel = find(host, "panel");
        layout(host);
        assertEquals(100.0f, panel.layoutBounds().width(), 0.5f, "the default keeps the look");
        preview.send("Go", Args.empty());
        layout(host);
        assertEquals(240.0f, panel.layoutBounds().width(), 0.5f, "the graph resized the widget");
    }

    private static void settle(dev.sixik.unigui.api.widget.Widget widget) {
        if (widget instanceof dev.sixik.unigui.impl.widget.WidgetBase base) {
            base.clearInvalidationDirect(dev.sixik.unigui.api.core.InvalidationFlags.ALL);
        }
        for (dev.sixik.unigui.api.widget.Widget child : widget.children()) settle(child);
    }

    /** Экран перекладывает окно только по флагу LAYOUT, поэтому сам вызов measure в тесте ничего не доказывает. */
    private static void assertRequestsLayout(String property, String value) {
        EditorSession session = EditorSession.create(PANEL);
        String name = Promotions.promote(session, node(session, "panel"), property, Promotions.Kind.VARIABLE).orElseThrow();
        ComponentPreview preview = show(wired(session, "Var", name, value));
        var host = preview.host().orElseThrow();
        layout(host);
        settle(host);
        assertFalse(dev.sixik.unigui.api.core.InvalidationFlags.has(host.subtreeInvalidationFlags(),
                dev.sixik.unigui.api.core.InvalidationFlags.LAYOUT), "the tree starts clean");
        preview.send("Go", Args.empty());
        assertTrue(dev.sixik.unigui.api.core.InvalidationFlags.has(host.subtreeInvalidationFlags(),
                dev.sixik.unigui.api.core.InvalidationFlags.LAYOUT), property + " must ask the screen for a new layout");
    }

    @Test
    void changesFromTheGraphReachTheRealLayoutPass() {
        assertRequestsLayout("width", "240");
        assertRequestsLayout("padding", "4 8");
        assertRequestsLayout("visibility", "Collapsed");
        assertRequestsLayout("alignSelf", "End");
    }

    @Test
    void anEnumBecomesAParameterAndTheGraphChangesIt() {
        EditorSession session = EditorSession.create(PANEL);
        UiNode title = node(session, "title");
        String name = Promotions.promote(session, title, "visibility", Promotions.Kind.PARAM).orElseThrow();
        assertTrue(clean(session.text()), session.text());

        ComponentPreview preview = show(wired(session, "Param", name, "Collapsed"));
        var label = find(preview.host().orElseThrow(), "title");
        assertEquals(dev.sixik.unigui.api.widget.Visibility.VISIBLE, label.visibility());
        preview.send("Go", Args.empty());
        assertEquals(dev.sixik.unigui.api.widget.Visibility.COLLAPSED, label.visibility());
    }

    @Test
    void insetsBecomeAVariable() {
        EditorSession session = EditorSession.create(PANEL);
        String name = Promotions.promote(session, node(session, "panel"), "padding", Promotions.Kind.VARIABLE).orElseThrow();
        ComponentPreview preview = show(wired(session, "Var", name, "4 8"));
        preview.send("Go", Args.empty());
        var style = ((dev.sixik.unigui.impl.widget.WidgetBase) find(preview.host().orElseThrow(), "panel")).layoutStyle();
        assertEquals(8.0f, style.padding().left(), 0.01f, style.padding().toString());
    }

    @Test
    void aWrongTextValueFailsTheRunAndNamesTheVariable() {
        EditorSession session = EditorSession.create(PANEL);
        String name = Promotions.promote(session, node(session, "title"), "visibility", Promotions.Kind.VARIABLE).orElseThrow();
        ComponentPreview preview = show(wired(session, "Var", name, "Sideways"));
        var label = find(preview.host().orElseThrow(), "title");
        RuntimeException failure = assertThrows(RuntimeException.class, () -> preview.send("Go", Args.empty()));
        assertTrue(String.valueOf(failure.getMessage()).contains(name), failure.toString());
        assertEquals(dev.sixik.unigui.api.widget.Visibility.VISIBLE, label.visibility(), "the widget keeps its value");
    }

    @Test
    void aWrongDefaultIsReportedByTheValidator() {
        String text = component("<Param Name=\"Mode\" Type=\"string\" Default=\"Sideways\"/>",
                "<Label visibility=\"{Param Mode}\"/>", "");
        assertFalse(clean(text));
    }

    @Test
    void aStringStillDoesNotFitANumberProperty() {
        String text = component("<Param Name=\"Title\" Type=\"string\"/>", "<Slider value=\"{Param Title}\"/>", "");
        assertFalse(clean(text), "typed properties keep their check");
        assertFalse(ParamType.needsText(Float.class));
        assertFalse(ParamType.needsText(Boolean.class));
        assertFalse(ParamType.needsText(String.class));
    }

    @Test
    void anExistingEntryBindsOnlyToAPropertyOfItsType() {
        EditorSession session = EditorSession.create(component(
                "<Param Name=\"Size\" Type=\"int\"/><Var Name=\"Mode\" Type=\"string\"/>"
                        + "<Event Name=\"Sell\" Direction=\"Out\"/><Event Name=\"Go\" Direction=\"In\"/>",
                "<VBox><Label id=\"title\" text=\"x\"/><Slider id=\"bar\"/><Button id=\"buy\"/></VBox>", ""));
        assertTrue(Promotions.canBind(session, node(session, "bar"), "value", Promotions.Kind.PARAM, "Size"));
        assertTrue(Promotions.canBind(session, node(session, "title"), "visibility", Promotions.Kind.VARIABLE, "Mode"),
                "text entries fit properties without a type of their own");
        assertFalse(Promotions.canBind(session, node(session, "bar"), "value", Promotions.Kind.VARIABLE, "Mode"),
                "a string does not fit a number");
        assertFalse(Promotions.canBind(session, node(session, "title"), "id", Promotions.Kind.VARIABLE, "Mode"));
        assertFalse(Promotions.canBind(session, node(session, "title"), "text", Promotions.Kind.PARAM, "Missing"));

        assertTrue(Promotions.bind(session, node(session, "bar"), "value", Promotions.Kind.PARAM, "Size"));
        assertTrue(session.text().contains("value=\"{Param Size}\""), session.text());
        assertTrue(Promotions.raise(session, node(session, "buy"), "onClick", "Sell"));
        assertTrue(session.text().contains("onClick=\"{Raise Sell}\""), session.text());
        assertFalse(Promotions.raise(session, node(session, "buy"), "onClick", "Go"), "incoming events are not raised by the body");
        assertTrue(clean(session.text()), session.text());
    }

    @Test
    void theHierarchyOfAComponentShowsOnlyWidgets() {
        EditorSession session = EditorSession.create(component("<Param Name=\"Title\" Type=\"string\"/>",
                "<VBox id=\"body\"/>", "<Node Id=\"a\" Type=\"Event.Construct\"/>"));
        List<String> types = dev.sixik.unigui.xaml.edit.Hierarchy.rows(session.document(), session.schema()).stream()
                .map(dev.sixik.unigui.xaml.edit.Hierarchy.Row::typeName).toList();
        assertEquals(List.of("Component", "VBox"), types);
    }

    @Test
    void aHostIsReachableFromThePreview() {
        assertTrue(show(CARD).host().orElseThrow() instanceof ComponentHost);
    }
}
