package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.event.NodeGraphItemMoveEndedEvent;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.graph.NodeGraph;
import dev.sixik.unigui.widgets.graph.NodeGraphItem;
import dev.sixik.unigui.widgets.graph.NodeGraphPortRef;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.DocumentSource;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.logic.GraphDefinition;
import dev.sixik.unigui.xaml.logic.GraphEdits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphPanelTest {
    private static final String SOURCE = """
            <Component x:Name="Card" xmlns:x="https://unigui.dev/xaml">
              <Component.Contract>
                <Param Name="Title" Type="string" Default="idle"/>
                <Event Name="Refresh" Direction="In"/>
              </Component.Contract>
              <Label id="title" text="{Param Title}"/>
              <Component.Graph>
                <Node Id="refresh" Type="Event.Refresh" X="0" Y="0"/>
                <Node Id="set" Type="Param.Set" Name="Title" X="200" Y="0"/>
                <Wire From="refresh.then" To="set.exec"/>
              </Component.Graph>
            </Component>""";

    private EditorSession session;
    private XamlEditorDocument document;
    private EditorShell shell;

    @BeforeEach
    void setUp() {
        session = EditorSession.open(DocumentSource.memory("card", "Card", SOURCE));
        document = new XamlEditorDocument("card", "Card", session);
        shell = ComponentEditor.shell(document);
    }

    private GraphPanel panel() {
        shell.dock().findPane(ComponentEditor.GRAPH).content();
        return (GraphPanel) shell.panel(ComponentEditor.GRAPH).orElseThrow();
    }

    private NodeGraph graph() {
        return panel().view();
    }

    private UiNode root() {
        return session.document().root();
    }

    private GraphDefinition definition() {
        return GraphEdits.read(root());
    }

    /** Конец перетаскивания приходит от указателя; тест подаёт то же событие напрямую (метод защищённый). */
    private void dispatch(NodeGraphItemMoveEndedEvent event) {
        try {
            java.lang.reflect.Method method = NodeGraph.class.getDeclaredMethod("dispatch", dev.sixik.unigui.api.event.Event.class);
            method.setAccessible(true);
            method.invoke(graph(), event);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static NodeGraphPortRef ref(String item, String port) {
        return new NodeGraphPortRef(item, port);
    }

    private static Button find(Widget widget, String id) {
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        if (id.equals(widget.id()) && widget instanceof Button button) return button;
        for (Widget child : widget.children()) {
            Button found = find(child, id);
            if (found != null) return found;
        }
        return null;
    }

    @Test
    void theCanvasShowsTheNodesAndWiresOfTheDocument() {
        assertEquals(Map.of("nodes", 2, "wires", 1), panel().counts());
        NodeGraphItem set = graph().item("set");
        assertNotNull(set.port("exec"));
        assertNotNull(set.port("value"));
        assertNotNull(set.port("then"));
        assertNotNull(graph().item("refresh").port("then"));
    }

    @Test
    void theBuiltInPaletteAddsANodeToTheDocumentAndUndoRemovesIt() {
        panel();
        assertNotNull(graph().nodeTypes().get("Math_Add"), "the palette is filled from the catalog");
        assertNotNull(graph().nodeTypes().get("Param_Set_Title"), "parameter nodes carry their name");
        assertNull(graph().nodeTypes().get("reroute"), "reroute nodes are not part of the document");

        NodeGraphItem created = graph().spawn("Math_Add", 100.0f, 100.0f);
        assertNotNull(created);
        assertEquals(3, definition().nodes().size());
        assertEquals(3, graph().items().size());
        GraphDefinition.Node node = definition().nodes().stream().filter(n -> n.id().equals(created.id())).findFirst().orElseThrow();
        assertEquals("Math.Add", node.type());
        assertEquals(100.0f, node.x());

        // Провод к созданной ноде работает: её идентификатор не ломает запись «нода.пин».
        graph().addConnection("c", ref(created.id(), "result"), ref("set", "value"));
        assertEquals(2, definition().wires().size());

        document.undo();
        document.undo();
        assertEquals(2, definition().nodes().size());
        assertEquals(2, graph().items().size(), "the canvas follows the undo");
    }

    @Test
    void aParameterNodeFromThePaletteKeepsItsName() {
        panel();
        NodeGraphItem created = graph().spawn("Param_Get_Title", 10.0f, 10.0f);
        GraphDefinition.Node node = definition().nodes().stream().filter(n -> n.id().equals(created.id())).findFirst().orElseThrow();
        assertEquals("Title", node.attributes().get("Name"));
    }

    @Test
    void aWireDrawnOnTheCanvasBecomesAnEditAndUndoRemovesIt() {
        assertTrue(session.apply(GraphEdits.addNode(root(), "add", "Math.Add", Map.of(), 100, 100)));
        graph().addConnection("new", ref("add", "result"), ref("set", "value"));
        assertEquals(2, definition().wires().size());
        assertTrue(session.text().contains("From=\"add.result\""), session.text());

        document.undo();
        assertEquals(1, definition().wires().size());
        assertEquals(1, graph().connections().size(), "the canvas follows the undo");
    }

    @Test
    void aWireTheGraphWouldRejectCannotBeDrawn() {
        String before = session.text();
        // поток выполнения нельзя подать в вход данных
        assertThrows(IllegalArgumentException.class,
                () -> graph().addConnection("bad", ref("refresh", "then"), ref("set", "value")));
        assertEquals(before, session.text());
        assertEquals(1, graph().connections().size());
    }

    @Test
    void removingANodeOnTheCanvasRemovesItAndItsWiresFromTheDocument() {
        graph().removeItem("set");
        assertEquals(1, definition().nodes().size());
        assertEquals(0, definition().wires().size());
        assertNull(graph().item("set"));

        document.undo();
        document.undo();
        assertEquals(2, definition().nodes().size());
        assertEquals(2, graph().items().size());
    }

    @Test
    void movingANodeWritesItsPosition() {
        NodeGraphItem set = graph().item("set");
        set.position(320.0f, 40.0f);
        dispatch(new NodeGraphItemMoveEndedEvent(graph(), "set", 200.0f, 0.0f, 320.0f, 40.0f, 0));
        GraphDefinition.Node node = definition().nodes().stream().filter(n -> n.id().equals("set")).findFirst().orElseThrow();
        assertEquals(320.0f, node.x());
        assertEquals(40.0f, node.y());
    }

    @Test
    void aPinValueEditedOnTheNodeWritesTheAttribute() {
        graph().item("set").value("value", "hello");
        graph().item("set").notifyValueChanged("value");
        GraphDefinition.Node node = definition().nodes().stream().filter(n -> n.id().equals("set")).findFirst().orElseThrow();
        assertEquals("hello", node.attributes().get("value"));
    }

    @Test
    void aNodeThatCannotBeResolvedStaysOnTheCanvasMarkedAsAnError() {
        // Удаление параметра ломает ноду Param.Set, которая на него ссылается.
        assertTrue(session.apply(dev.sixik.unigui.xaml.component.ContractEdits.remove(root(), "Param", "Title")));
        NodeGraphItem broken = graph().item("set");
        assertNotNull(broken, "the node is still visible so it can be fixed or deleted");
        assertEquals(2, broken.issueLevel());
        assertTrue(broken.ports().isEmpty());
        assertTrue(panel().problemText().contains("Title"), panel().problemText());
    }

    // ------------------------------------------------------------------ отладка

    private void runRefresh() {
        assertTrue(session.apply(GraphEdits.setAttribute(root(), "set", "value", "done")));
        shell.dock().findPane(UiEditor.VIEWPORT).content();
        dev.sixik.unigui.xaml.component.ComponentHost host =
                (dev.sixik.unigui.xaml.component.ComponentHost) ((ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow())
                        .previewRoot();
        host.instance().send("Refresh", dev.sixik.unigui.xaml.component.Args.empty());
        host.instance().flush();
    }

    @Test
    void theDebugBarWalksThroughTheLastRunAndSelectsEachNode() {
        Widget content = shell.dock().findPane(ComponentEditor.GRAPH).content();
        assertTrue(panel().debugText().startsWith("No runs yet"), panel().debugText());

        runRefresh();
        assertTrue(panel().debugText().startsWith("Run from refresh: 2 steps"), panel().debugText());

        find(content, "graph.debug.next").click();
        assertTrue(panel().debugText().startsWith("1/2 Event.Refresh refresh"), panel().debugText());
        assertEquals(java.util.List.of("refresh"), graph().selectedItemIds());

        find(content, "graph.debug.next").click();
        assertTrue(panel().debugText().startsWith("2/2 Param.Set set"), panel().debugText());
        assertEquals(java.util.List.of("set"), graph().selectedItemIds());

        find(content, "graph.debug.next").click();
        assertTrue(panel().debugText().startsWith("2/2"), "stepping stops at the last step");

        find(content, "graph.debug.prev").click();
        assertTrue(panel().debugText().startsWith("1/2"), panel().debugText());
    }

    @Test
    void clearingTheRecordResetsTheDebugBar() {
        Widget content = shell.dock().findPane(ComponentEditor.GRAPH).content();
        runRefresh();
        find(content, "graph.debug.clear").click();
        assertTrue(panel().debugText().startsWith("No runs yet"), panel().debugText());
    }

    // ------------------------------------------------------------------ точки останова

    private dev.sixik.unigui.xaml.logic.GraphDebugger debugger() {
        shell.dock().findPane(UiEditor.VIEWPORT).content();
        var host = (dev.sixik.unigui.xaml.component.ComponentHost)
                ((ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow()).previewRoot();
        assertNotNull(host);
        return componentPreviewOf(host);
    }

    /** Отладчик предпросмотра: его держит панель Viewport через общий ComponentView; достаём его по хосту. */
    private dev.sixik.unigui.xaml.logic.GraphDebugger componentPreviewOf(dev.sixik.unigui.xaml.component.ComponentHost host) {
        try {
            var field = ViewportPanel.class.getDeclaredField("componentView");
            field.setAccessible(true);
            ComponentView view = (ComponentView) field.get(shell.panel(UiEditor.VIEWPORT).orElseThrow());
            return view.preview().orElseThrow().debugger();
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    @Test
    void aBreakpointIsMarkedAndStopsTheRunBeforeItsNode() {
        Widget content = shell.dock().findPane(ComponentEditor.GRAPH).content();
        assertTrue(session.apply(GraphEdits.setAttribute(root(), "set", "value", "done")));
        graph().selectItem("set");
        find(content, "graph.debug.break").click();
        assertEquals(1, graph().item("set").issueLevel(), "the node carries the breakpoint mark");
        assertTrue(debugger().isBreakpoint("set"));

        shell.dock().findPane(UiEditor.VIEWPORT).content();
        var host = (dev.sixik.unigui.xaml.component.ComponentHost)
                ((ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow()).previewRoot();
        host.instance().send("Refresh", dev.sixik.unigui.xaml.component.Args.empty());
        host.instance().flush();

        assertEquals("idle", host.instance().get("Title"), "the node with the breakpoint has not run");
        assertTrue(panel().debugText().startsWith("Paused before Param.Set set"), panel().debugText());
        assertEquals(java.util.List.of("set"), graph().selectedItemIds());

        find(content, "graph.debug.continue").click();
        assertEquals("done", host.instance().get("Title"));
        assertTrue(panel().debugText().startsWith("Run from refresh"), panel().debugText());
    }

    @Test
    void stepNodeWalksOneNodeAtATime() {
        Widget content = shell.dock().findPane(ComponentEditor.GRAPH).content();
        assertTrue(session.apply(GraphEdits.setAttribute(root(), "set", "value", "done")));
        graph().selectItem("refresh");
        find(content, "graph.debug.break").click();

        shell.dock().findPane(UiEditor.VIEWPORT).content();
        var host = (dev.sixik.unigui.xaml.component.ComponentHost)
                ((ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow()).previewRoot();
        host.instance().send("Refresh", dev.sixik.unigui.xaml.component.Args.empty());
        host.instance().flush();
        assertTrue(panel().debugText().startsWith("Paused before Event.Refresh refresh"), panel().debugText());

        find(content, "graph.debug.stepnode").click();
        assertTrue(panel().debugText().startsWith("Paused before Param.Set set"), panel().debugText());
        assertEquals("idle", host.instance().get("Title"));
        find(content, "graph.debug.stepnode").click();
        assertEquals("done", host.instance().get("Title"));
        assertTrue(panel().debugText().startsWith("Run from refresh"), panel().debugText());
    }

    @Test
    void deletingANodeDropsItsBreakpoint() {
        Widget content = shell.dock().findPane(ComponentEditor.GRAPH).content();
        // Без значения у обязательного входа граф не запускается и экземпляра нет: сначала делаем граф полным.
        assertTrue(session.apply(GraphEdits.setAttribute(root(), "set", "value", "done")));
        var debugger = debugger();
        graph().selectItem("set");
        find(content, "graph.debug.break").click();
        assertTrue(debugger.isBreakpoint("set"));
        graph().removeItem("set");
        assertFalse(debugger.isBreakpoint("set"));
    }

    // ------------------------------------------------------------------ типизированные значения пинов

    @Test
    void pinValuesUseEditorsOfTheirTypeAndAreWrittenCanonically() {
        assertTrue(session.apply(GraphEdits.addNode(root(), "add", "Math.Add", Map.of(), 0, 100)));
        assertTrue(session.apply(GraphEdits.addNode(root(), "not", "Logic.Not", Map.of(), 200, 100)));

        NodeGraphItem add = graph().item("add");
        add.value("a", 3.0);
        add.notifyValueChanged("a");
        assertEquals("3", definition().nodes().stream().filter(n -> n.id().equals("add")).findFirst().orElseThrow()
                .attributes().get("a"), "a float 3.0 is written as 3");
        add.value("b", 2.5);
        add.notifyValueChanged("b");
        assertEquals("2.5", definition().nodes().stream().filter(n -> n.id().equals("add")).findFirst().orElseThrow()
                .attributes().get("b"));

        NodeGraphItem not = graph().item("not");
        not.value("value", true);
        not.notifyValueChanged("value");
        assertEquals("true", definition().nodes().stream().filter(n -> n.id().equals("not")).findFirst().orElseThrow()
                .attributes().get("value"));
        assertTrue(graph().item("not").editor("value").widget() instanceof dev.sixik.unigui.widgets.interaction.Checkbox,
                "a bool pin gets a checkbox, not a text field");
    }

    @Test
    void savedValuesComeBackInTheirEditors() {
        assertTrue(session.apply(GraphEdits.addNode(root(), "add", "Math.Add", Map.of("a", "4"), 0, 100)));
        assertEquals(4.0, ((Number) graph().item("add").editor("a").value()).doubleValue());
    }

    // ------------------------------------------------------------------ атрибуты из списка

    private static dev.sixik.unigui.api.widget.Widget findId(dev.sixik.unigui.api.widget.Widget widget, String id) {
        if (id.equals(widget.id())) return widget;
        for (dev.sixik.unigui.api.widget.Widget child : widget.children()) {
            var found = findId(child, id);
            if (found != null) return found;
        }
        return null;
    }

    private dev.sixik.unigui.widgets.interaction.ComboBox attribute(String node, String attribute) {
        for (var widget : graph().item(node).allWidgets()) {
            var found = findId(widget, "graph.attr." + node + "." + attribute);
            if (found != null) return (dev.sixik.unigui.widgets.interaction.ComboBox) found;
        }
        throw new AssertionError("no attribute list " + node + "." + attribute);
    }

    private void addNode(String id, String type, java.util.Map<String, String> attributes) {
        assertTrue(session.apply(GraphEdits.addNode(root(), id, type, attributes, 0.0f, 200.0f)));
    }

    @Test
    void aComparisonOperatorIsPickedFromAList() {
        addNode("cmp", "Math.Compare", java.util.Map.of("Op", "=="));
        var op = attribute("cmp", "Op");
        assertEquals(java.util.List.of("==", "!=", "<", "<=", ">", ">="), op.items());
        assertEquals("==", op.selectedItem());
        op.selectedIndex(op.items().indexOf(">="));
        assertTrue(session.text().contains("Op=\"&gt;=\"") || session.text().contains("Op=\">=\""), session.text());
    }

    @Test
    void aParameterNodePicksItsNameFromTheContract() {
        addNode("get", "Param.Get", java.util.Map.of("Name", "Title"));
        assertEquals(java.util.List.of("Title"), attribute("get", "Name").items());
    }

    @Test
    void changingTheLiteralTypeDropsAValueThatNoLongerFits() {
        addNode("lit", "Value.Literal", java.util.Map.of("ValueType", "string", "Value", "hello"));
        var type = attribute("lit", "ValueType");
        type.selectedIndex(type.items().indexOf("int"));
        assertTrue(session.text().contains("ValueType=\"int\""), session.text());
        assertTrue(!session.text().contains("hello"), session.text());
    }

    @Test
    void runsInPlayAreShownByTheDebugBar() {
        panel();
        assertTrue(session.apply(GraphEdits.setAttribute(root(), "set", "value", "x")));
        var viewport = (ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow();
        shell.dock().findPane(UiEditor.VIEWPORT).content();
        viewport.playing(true);
        var host = (dev.sixik.unigui.xaml.component.ComponentHost) viewport.playRoot();
        host.instance().send("Refresh", dev.sixik.unigui.xaml.component.Args.empty());
        host.instance().flush();
        assertTrue(panel().debugText().startsWith("Run from"), panel().debugText());
    }

    // ------------------------------------------------------------------ клавиша Delete

    private static void attach(dev.sixik.unigui.api.widget.Widget widget) {
        if (widget instanceof dev.sixik.unigui.widgets.containers.PanelWidget panel) panel.applyQueuedMutations();
        for (dev.sixik.unigui.api.widget.Widget child : widget.children()) attach(child);
    }

    /** Нажатие идёт настоящим маршрутом: корень окна, затем цель, затем обратно к корню. */
    private void press(dev.sixik.unigui.api.widget.Widget target, int key) {
        shell.dock().selectPane(ComponentEditor.GRAPH);
        attach(shell.root());
        dev.sixik.unigui.impl.event.DefaultRoutedEventDispatcher.INSTANCE.dispatch(
                new dev.sixik.unigui.api.event.KeyPressedEvent(target, key, 0, 0));
    }

    @Test
    void deleteInTheGraphRemovesTheNodeAndNotTheWidgetSelectedInTheDocument() {
        session.select(root().children().get(0));
        panel();
        graph().selectItem("set");
        press(graph(), dev.sixik.unigui.api.input.KeyCodes.DELETE);

        assertEquals(1, definition().nodes().size(), session.text());
        assertEquals("refresh", definition().nodes().get(0).id());
        assertEquals(1, graph().items().size(), "only the selected node leaves the canvas");
        assertTrue(session.text().contains("id=\"title\""), "the body widget stays: " + session.text());
    }

    @Test
    void deleteWithAListInsideANodeFocusedStillActsOnTheGraph() {
        addNode("cmp", "Math.Compare", java.util.Map.of("Op", "=="));
        session.select(root().children().get(0));
        graph().selectItem("cmp");
        press(attribute("cmp", "Op"), dev.sixik.unigui.api.input.KeyCodes.DELETE);

        assertEquals(2, definition().nodes().size(), session.text());
        assertTrue(session.text().contains("id=\"title\""), session.text());
    }

    @Test
    void aDocumentThatStopsParsingDoesNotWipeTheCanvas() {
        panel();
        int before = graph().items().size();
        session.replaceText(session.text().replace("<Label id=\"title\" text=\"{Param Title}\"/>", ""));
        assertEquals(before, graph().items().size(), "the nodes stay visible while the body is missing");
        assertTrue(!panel().problemText().isEmpty(), "the reason is shown");
    }

    @Test
    void theRootWidgetOfAComponentCannotBeDeleted() {
        session.select(root().children().get(0));
        assertTrue(!UiEdits.hasMovableSelection(session));
        assertTrue(!UiEdits.deleteSelected(session), "a component must keep its body");
        assertTrue(session.text().contains("id=\"title\""), session.text());
    }

    @Test
    void deleteStillRemovesAWidgetWhenTheGraphIsNotFocused() {
        session.replaceText(session.text().replace("<Label id=\"title\" text=\"{Param Title}\"/>",
                "<VBox id=\"body\"><Label id=\"title\" text=\"{Param Title}\"/></VBox>"));
        session.select(root().children().get(0).children().get(0));
        var inspector = shell.dock().findPane(UiEditor.INSPECTOR).content();
        attach(shell.root());
        dev.sixik.unigui.impl.event.DefaultRoutedEventDispatcher.INSTANCE.dispatch(
                new dev.sixik.unigui.api.event.KeyPressedEvent(inspector, dev.sixik.unigui.api.input.KeyCodes.DELETE, 0, 0));
        assertTrue(!session.text().contains("id=\"title\""), "the shell shortcut still works elsewhere: " + session.text());
    }
}
