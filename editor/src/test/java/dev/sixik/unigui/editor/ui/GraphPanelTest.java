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
}
