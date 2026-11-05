package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.widgets.graph.node.NodeBuilder;
import dev.sixik.unigui.widgets.graph.node.NodeType;
import dev.sixik.unigui.widgets.graph.node.PortType;
import dev.sixik.unigui.widgets.graph.node.RerouteModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeGraphEditingTest {
    private static final float EPS = 1.0e-3f;

    private NodeGraph graph;

    @BeforeEach
    void setUp() {
        graph = new NodeGraph().selectionMode(NodeGraphSelectionMode.MULTIPLE);
        graph.portTypes().register(PortType.data("vec3", 0xFFF5D547));
        graph.nodeTypes()
                .register(NodeType.of("fn", "Function", "Test", id -> NodeBuilder.node(id)
                        .header("Function")
                        .flowRow("exec", "then")
                        .input("in", "In", "vec3")
                        .output("out", "Out", "vec3")
                        .build()))
                .register(NodeType.of("src", "Source", "Test", id -> NodeBuilder.node(id)
                        .header("Source")
                        .output("out", "Out", "vec3")
                        .build()));
    }

    private void layout() {
        graph.measure(new LayoutContext(900.0f, 700.0f));
        graph.arrange(new MutableRect(0.0f, 0.0f, 900.0f, 700.0f));
    }

    private void select(NodeGraphItem... items) {
        graph.clearSelection();
        for (NodeGraphItem item : items) {
            graph.selectItem(item, true);
        }
    }

    private void key(int code, int modifiers) {
        graph.handle(new KeyPressedEvent(graph, code, 0, modifiers));
    }

    // ---- история ----

    @Test
    void history_recordsNothingOutsideTransactions() {
        graph.spawn("fn", "a", 0.0f, 0.0f);

        assertFalse(graph.history().canUndo());
        assertFalse(graph.undo());
    }

    @Test
    void history_undoRedoOfAddRemoveAndConnectInOneStep() {
        graph.transact("Build", () -> {
            graph.spawn("src", "s", 0.0f, 0.0f);
            graph.spawn("fn", "f", 300.0f, 0.0f);
            graph.addConnection("w", "s", "out", "f", "in");
        });

        assertEquals("Build", graph.history().undoName());
        assertTrue(graph.undo());
        assertEquals(0, graph.items().size());
        assertEquals(0, graph.connections().size());
        assertTrue(graph.redo());
        assertEquals(2, graph.items().size());
        assertEquals(1, graph.connections().size());
        assertEquals(1, graph.item("f").port("in").connectionCount());
    }

    @Test
    void history_undoDelete_bringsBackNodeWithItsWires() {
        graph.spawn("src", "s", 0.0f, 0.0f);
        graph.spawn("fn", "f", 300.0f, 0.0f);
        graph.addConnection("w", "s", "out", "f", "in");
        select(graph.item("f"));

        key(KeyCodes.DELETE, 0);

        assertNull(graph.item("f"));
        assertEquals(0, graph.connections().size());
        assertTrue(graph.undo());
        assertNotNull(graph.item("f"));
        assertNotNull(graph.connection("w"));
        assertEquals(1, graph.item("s").port("out").connectionCount());
        assertTrue(graph.redo());
        assertNull(graph.item("f"));
    }

    @Test
    void history_undoRestoresWireEvictedByPortCapacity() {
        graph.spawn("src", "a", 0.0f, 0.0f);
        graph.spawn("src", "b", 0.0f, 100.0f);
        graph.spawn("fn", "f", 300.0f, 0.0f);
        graph.addConnection("first", "a", "out", "f", "in");

        graph.transact("Connect", () -> graph.addConnection("second", "b", "out", "f", "in"));

        assertNull(graph.connection("first"));
        assertTrue(graph.undo());
        assertNotNull(graph.connection("first"));
        assertNull(graph.connection("second"));
        assertTrue(graph.redo());
        assertNull(graph.connection("first"));
        assertNotNull(graph.connection("second"));
    }

    @Test
    void history_newStepDiscardsRedoAndLimitTrimsOldest() {
        graph.history().limit(2);
        graph.transact("one", () -> graph.spawn("src", "s1", 0.0f, 0.0f));
        graph.transact("two", () -> graph.spawn("src", "s2", 0.0f, 50.0f));
        graph.transact("three", () -> graph.spawn("src", "s3", 0.0f, 100.0f));

        assertEquals("three", graph.history().undoName());
        graph.undo();
        graph.undo();
        assertFalse(graph.history().canUndo(), "oldest step was trimmed by the limit");
        assertEquals(1, graph.items().size());

        graph.transact("four", () -> graph.spawn("src", "s4", 0.0f, 150.0f));

        assertFalse(graph.history().canRedo());
    }

    @Test
    void history_isClearedWhenTheGraphIsRebuilt() {
        graph.transact("Build", () -> graph.spawn("src", "s", 0.0f, 0.0f));
        NodeGraphSnapshot snapshot = graph.snapshot();

        graph.restoreSnapshot(snapshot, null);

        assertFalse(graph.history().canUndo());
        assertFalse(graph.undo());
        graph.transact("Build again", () -> graph.spawn("src", "t", 0.0f, 0.0f));
        graph.clearItems();
        assertFalse(graph.history().canUndo());
    }

    @Test
    void history_collapseIsUndoable() {
        NodeGraphItem node = graph.spawn("fn", "f", 0.0f, 0.0f);
        layout();

        graph.transact("Collapse", () -> node.collapsed(true));
        assertTrue(node.collapsed());
        graph.undo();
        assertFalse(node.collapsed());
        graph.redo();
        assertTrue(node.collapsed());
    }

    @Test
    void history_undoShortcutAndRedoShortcuts() {
        NodeGraphItem node = graph.spawn("fn", "f", 0.0f, 0.0f);
        select(node);
        key(KeyCodes.DELETE, 0);
        assertNull(graph.item("f"));

        key(KeyCodes.Z, KeyModifiers.CONTROL);
        assertNotNull(graph.item("f"));
        key(KeyCodes.Y, KeyModifiers.CONTROL);
        assertNull(graph.item("f"));
        key(KeyCodes.Z, KeyModifiers.CONTROL);
        key(KeyCodes.DELETE, 0);
        key(KeyCodes.Z, KeyModifiers.CONTROL);
        key(KeyCodes.Z, KeyModifiers.CONTROL | KeyModifiers.SHIFT);
        assertNull(graph.item("f"), "Ctrl+Shift+Z redoes");
    }

    // ---- интерактивное перетаскивание ----

    @Test
    void drag_nodeIsOneUndoStepAndRestoresPosition() {
        NodeGraphItem node = graph.spawn("fn", "f", 100.0f, 100.0f);
        layout();
        float grabX = 100.0f + 20.0f;
        float grabY = 100.0f + node.arrangedHeight() * 0.5f;

        graph.handle(new PointerPressedEvent(graph, grabX, grabY, grabX, grabY, 1, PointerButton.PRIMARY));
        graph.handle(new PointerMovedEvent(graph, grabX + 30.0f, grabY + 10.0f, grabX + 30.0f, grabY + 10.0f, 1));
        graph.handle(new PointerMovedEvent(graph, grabX + 60.0f, grabY + 25.0f, grabX + 60.0f, grabY + 25.0f, 1));
        graph.handle(new PointerReleasedEvent(graph, grabX + 60.0f, grabY + 25.0f, grabX + 60.0f, grabY + 25.0f, 1,
                PointerButton.PRIMARY));

        assertEquals(160.0f, node.x(), EPS);
        assertEquals(125.0f, node.y(), EPS);
        assertEquals("Move node", graph.history().undoName());
        graph.undo();
        assertEquals(100.0f, node.x(), EPS);
        assertEquals(100.0f, node.y(), EPS);
        assertFalse(graph.history().canUndo(), "two pointer moves collapse into one step");
        graph.redo();
        assertEquals(160.0f, node.x(), EPS);
    }

    @Test
    void drag_clickWithoutMovingRecordsNothing() {
        NodeGraphItem node = graph.spawn("fn", "f", 100.0f, 100.0f);
        layout();
        float x = 120.0f;
        float y = 100.0f + node.arrangedHeight() * 0.5f;

        graph.handle(new PointerPressedEvent(graph, x, y, x, y, 1, PointerButton.PRIMARY));
        graph.handle(new PointerReleasedEvent(graph, x, y, x, y, 1, PointerButton.PRIMARY));

        assertFalse(graph.history().canUndo());
    }

    @Test
    void snapToGrid_dragEndsOnGridLines() {
        graph.gridSize(24.0f).snapToGrid(true);
        NodeGraphItem node = graph.spawn("fn", "f", 96.0f, 96.0f);
        layout();
        float x = 110.0f;
        float y = 96.0f + node.arrangedHeight() * 0.5f;

        graph.handle(new PointerPressedEvent(graph, x, y, x, y, 1, PointerButton.PRIMARY));
        graph.handle(new PointerMovedEvent(graph, x + 31.0f, y + 17.0f, x + 31.0f, y + 17.0f, 1));
        graph.handle(new PointerReleasedEvent(graph, x + 31.0f, y + 17.0f, x + 31.0f, y + 17.0f, 1, PointerButton.PRIMARY));

        assertEquals(0.0f, node.x() % 24.0f, EPS);
        assertEquals(0.0f, node.y() % 24.0f, EPS);
        assertEquals(120.0f, node.x(), EPS);
        assertEquals(120.0f, node.y(), EPS);
    }

    @Test
    void collapseArrowPress_isUndoable() {
        NodeGraphItem node = graph.spawn("fn", "f", 100.0f, 100.0f);
        layout();
        float arrowX = 100.0f + node.arrangedWidth() - node.style().padding() - 4.0f;
        float headerY = 100.0f + node.style().headerHeight() * 0.5f;

        graph.handle(new PointerPressedEvent(graph, arrowX, headerY, arrowX, headerY, 1, PointerButton.PRIMARY));

        assertTrue(node.collapsed());
        assertEquals("Collapse node", graph.history().undoName());
        graph.undo();
        assertFalse(node.collapsed());
    }

    // ---- копирование и вставка ----

    @Test
    void copyPaste_clonesNodesAndInternalWiresOnly() {
        graph.spawn("src", "s", 100.0f, 100.0f);
        graph.spawn("fn", "f", 400.0f, 100.0f);
        graph.spawn("src", "outsider", 100.0f, 400.0f);
        graph.spawn("fn", "other", 400.0f, 400.0f);
        graph.addConnection("inner", "s", "out", "f", "in");
        graph.addConnection("outer", "outsider", "out", "other", "in");
        select(graph.item("s"), graph.item("f"));

        NodeGraphClipboard copied = graph.copySelection();
        List<NodeGraphItem> pasted = graph.paste(500.0f, 500.0f);

        assertEquals(2, copied.nodeCount());
        assertEquals(1, copied.wireCount());
        assertEquals(2, pasted.size());
        assertEquals(6, graph.items().size());
        assertEquals(500.0f, pasted.get(0).x(), EPS);
        assertEquals(500.0f + 300.0f, pasted.get(1).x(), EPS);
        assertEquals(3, graph.connections().size());
        assertTrue(pasted.get(0).selected() && pasted.get(1).selected());
        assertFalse(graph.item("s").selected());
        graph.undo();
        assertEquals(4, graph.items().size());
        assertEquals(2, graph.connections().size());
    }

    @Test
    void copy_skipsContentWidgetNodesAndUnknownTypes() {
        graph.addItem(new NodeGraphItem("legacy", new dev.sixik.unigui.widgets.display.Label("x"), 0.0f, 0.0f));
        NodeGraphItem custom = NodeBuilder.node("custom").header("Custom").build();
        custom.typeId("not-registered");
        graph.addItem(custom);
        graph.spawn("src", "s", 0.0f, 0.0f);
        select(graph.item("legacy"), custom, graph.item("s"));

        NodeGraphClipboard copied = graph.copySelection();

        assertEquals(1, copied.nodeCount());
    }

    @Test
    void paste_keepsCollapsedStateAndFreshIds() {
        NodeGraphItem node = graph.spawn("fn", "f", 0.0f, 0.0f);
        node.collapsed(true);
        select(node);
        graph.copySelection();

        List<NodeGraphItem> pasted = graph.paste(50.0f, 50.0f);

        assertTrue(pasted.get(0).collapsed());
        assertFalse(pasted.get(0).id().equals("f"));
        assertNotNull(graph.item(pasted.get(0).id()));
    }

    @Test
    void duplicate_offsetsAndLeavesClipboardAlone() {
        NodeGraphItem node = graph.spawn("src", "s", 100.0f, 100.0f);
        select(node);

        List<NodeGraphItem> copies = graph.duplicateSelection();

        assertEquals(1, copies.size());
        assertEquals(124.0f, copies.get(0).x(), EPS);
        assertEquals(124.0f, copies.get(0).y(), EPS);
        assertTrue(graph.clipboard().isEmpty());
        assertEquals(2, graph.items().size());
    }

    @Test
    void copyPasteShortcuts_workThroughTheKeyboard() {
        graph.spawn("src", "s", 100.0f, 100.0f);
        select(graph.item("s"));

        key(KeyCodes.C, KeyModifiers.CONTROL);
        key(KeyCodes.V, KeyModifiers.CONTROL);
        key(KeyCodes.D, KeyModifiers.CONTROL);

        assertEquals(3, graph.items().size());
    }

    @Test
    void paste_intoAnotherGraphThatSharesTypes() {
        graph.spawn("src", "s", 0.0f, 0.0f);
        select(graph.item("s"));
        NodeGraphClipboard copied = graph.copySelection();
        NodeGraph other = new NodeGraph();
        other.nodeTypes().register(graph.nodeTypes().get("src"));

        List<NodeGraphItem> pasted = other.paste(copied, 10.0f, 20.0f);

        assertEquals(1, pasted.size());
        assertEquals(1, other.items().size());
        assertEquals(10.0f, pasted.get(0).x(), EPS);
    }

    // ---- выравнивание, группа, показ ----

    @Test
    void align_leftTopAndCenter() {
        NodeGraphItem a = graph.spawn("src", "a", 100.0f, 50.0f);
        NodeGraphItem b = graph.spawn("src", "b", 260.0f, 200.0f);
        layout();
        select(a, b);

        assertTrue(graph.alignSelected(NodeGraphAlign.LEFT));
        assertEquals(100.0f, b.x(), EPS);
        assertTrue(graph.alignSelected(NodeGraphAlign.TOP));
        assertEquals(50.0f, b.y(), EPS);
        graph.undo();
        assertEquals(200.0f, b.y(), EPS, "TOP alignment undone");
        graph.undo();
        assertEquals(260.0f, b.x(), EPS, "LEFT alignment undone");
        assertFalse(graph.alignSelected(NodeGraphAlign.LEFT) && false);
    }

    @Test
    void align_needsTwoNodes() {
        NodeGraphItem a = graph.spawn("src", "a", 100.0f, 50.0f);
        select(a);

        assertFalse(graph.alignSelected(NodeGraphAlign.LEFT));
        assertFalse(graph.distributeSelected(true));
    }

    @Test
    void distribute_spreadsMiddleNodesEvenly() {
        NodeGraphItem a = graph.spawn("src", "a", 0.0f, 0.0f);
        NodeGraphItem b = graph.spawn("src", "b", 50.0f, 0.0f);
        NodeGraphItem c = graph.spawn("src", "c", 600.0f, 0.0f);
        layout();
        select(a, b, c);

        assertTrue(graph.distributeSelected(true));

        float gapAB = b.x() - (a.x() + a.arrangedWidth());
        float gapBC = c.x() - (b.x() + b.arrangedWidth());
        assertEquals(gapAB, gapBC, 0.01f);
        assertEquals(0.0f, a.x(), EPS);
        assertEquals(600.0f, c.x(), EPS);
    }

    @Test
    void group_wrapsSelectedNodesInARegionThatCarriesThem() {
        NodeGraphItem a = graph.spawn("src", "a", 100.0f, 100.0f);
        NodeGraphItem b = graph.spawn("src", "b", 300.0f, 160.0f);
        layout();
        select(a, b);

        NodeGraphRegion region = graph.groupSelected("Setup");

        assertNotNull(region);
        assertEquals("Setup", region.title());
        assertTrue(region.containsItem(a));
        assertTrue(region.containsItem(b));
        assertTrue(region.selected());
        assertFalse(a.selected());
        graph.undo();
        assertEquals(0, graph.regions().size());
    }

    @Test
    void group_withoutSelectionDoesNothing() {
        assertNull(graph.groupSelected());
        assertEquals(0, graph.regions().size());
    }

    @Test
    void frame_fitsSelectionIntoTheWindow() {
        NodeGraphItem node = graph.spawn("src", "a", 1000.0f, 800.0f);
        graph.spawn("src", "far", -2000.0f, -2000.0f);
        layout();
        select(node);

        graph.frameSelection();

        float centerX = graph.viewport().x() + (node.x() + node.arrangedWidth() * 0.5f) * graph.viewport().zoom();
        float centerY = graph.viewport().y() + (node.y() + node.arrangedHeight() * 0.5f) * graph.viewport().zoom();
        assertEquals(450.0f, centerX, 0.5f);
        assertEquals(350.0f, centerY, 0.5f);
        assertTrue(graph.viewport().zoom() <= 1.5f + EPS);
    }

    @Test
    void frameAll_zoomsOutToContainEverything() {
        NodeGraphItem a = graph.spawn("src", "a", 0.0f, 0.0f);
        NodeGraphItem b = graph.spawn("src", "b", 3000.0f, 2000.0f);
        layout();

        graph.frameAll();

        float zoom = graph.viewport().zoom();
        assertTrue(zoom < 0.5f, "zoom " + zoom);
        float left = graph.viewport().x() + a.x() * zoom;
        float right = graph.viewport().x() + (b.x() + b.arrangedWidth()) * zoom;
        float bottom = graph.viewport().y() + (b.y() + b.arrangedHeight()) * zoom;
        assertTrue(left >= 0.0f && right <= 900.0f && bottom <= 700.0f);
    }

    @Test
    void frameShortcuts_runWithoutErrorsOnEmptyGraph() {
        layout();

        key(KeyCodes.HOME, 0);
        key(KeyCodes.F, 0);

        assertEquals(1.0f, graph.viewport().zoom(), EPS);
    }

    // ---- reroute ----

    @Test
    void insertReroute_splitsTheWireAndKeepsTheTypeColoring() {
        graph.spawn("src", "s", 0.0f, 0.0f);
        graph.spawn("fn", "f", 400.0f, 0.0f);
        graph.addConnection("w", "s", "out", "f", "in");

        NodeGraphItem reroute = graph.insertReroute("w", 250.0f, 120.0f);

        assertNotNull(reroute);
        assertNull(graph.connection("w"));
        assertEquals(2, graph.connections().size());
        assertTrue(reroute.module(RerouteModule.class) != null && !reroute.module(RerouteModule.class).flow());
        assertEquals(1, reroute.port(RerouteModule.INPUT).connectionCount());
        assertEquals(1, reroute.port(RerouteModule.OUTPUT).connectionCount());
        assertEquals(1, graph.item("f").port("in").connectionCount());
        assertEquals("vec3", NodeGraphEditing.effectiveType(graph, reroute.port(RerouteModule.OUTPUT)));
        assertEquals(RerouteModule.DATA_TYPE_ID, reroute.typeId());
    }

    @Test
    void insertReroute_onExecWireCreatesAnExecReroute() {
        graph.spawn("fn", "a", 0.0f, 0.0f);
        graph.spawn("fn", "b", 400.0f, 0.0f);
        graph.addConnection("flow", "a", "then", "b", "exec");

        NodeGraphItem reroute = graph.insertReroute("flow", 250.0f, 0.0f);

        assertEquals(RerouteModule.FLOW_TYPE_ID, reroute.typeId());
        assertTrue(reroute.module(RerouteModule.class).flow());
        assertEquals("flow", NodeGraphEditing.effectiveType(graph, reroute.port(RerouteModule.OUTPUT)));
        assertEquals(2, graph.connections().size());
    }

    @Test
    void insertReroute_isOneUndoStep() {
        graph.spawn("src", "s", 0.0f, 0.0f);
        graph.spawn("fn", "f", 400.0f, 0.0f);
        graph.addConnection("w", "s", "out", "f", "in");

        graph.insertReroute("w", 250.0f, 120.0f);
        graph.undo();

        assertEquals(2, graph.items().size());
        assertNotNull(graph.connection("w"));
        assertEquals(1, graph.connections().size());
        assertEquals("Insert reroute", graph.history().redoName());
    }

    @Test
    void reroute_chainsPassTypeThroughAndSurviveSnapshot() {
        graph.spawn("src", "s", 0.0f, 0.0f);
        graph.spawn("fn", "f", 600.0f, 0.0f);
        graph.addConnection("w", "s", "out", "f", "in");
        NodeGraphItem first = graph.insertReroute("w", 200.0f, 50.0f);
        String secondWire = graph.connections().stream()
                .filter(c -> c.fromItemId().equals(first.id())).findFirst().orElseThrow().id();
        NodeGraphItem second = graph.insertReroute(secondWire, 400.0f, 50.0f);

        assertEquals("vec3", NodeGraphEditing.effectiveType(graph, second.port(RerouteModule.OUTPUT)));

        NodeGraph restored = new NodeGraph();
        restored.portTypes().register(PortType.data("vec3", 0xFFF5D547));
        restored.nodeTypes().register(graph.nodeTypes().get("src")).register(graph.nodeTypes().get("fn"));
        restored.restoreSnapshot(graph.snapshot(), null);

        assertEquals(4, restored.items().size());
        assertEquals(3, restored.connections().size());
        assertNotNull(restored.item(second.id()).port(RerouteModule.OUTPUT));
    }

    @Test
    void rerouteKey_insertsIntoSelectedWire() {
        graph.spawn("src", "s", 0.0f, 0.0f);
        graph.spawn("fn", "f", 400.0f, 0.0f);
        graph.addConnection("w", "s", "out", "f", "in");
        layout();
        graph.selectConnection("w");

        key(KeyCodes.R, 0);

        assertNull(graph.connection("w"));
        assertEquals(3, graph.items().size());
    }

    @Test
    void groupKey_createsRegionFromSelection() {
        NodeGraphItem node = graph.spawn("src", "s", 100.0f, 100.0f);
        layout();
        select(node);

        key(KeyCodes.C, 0);

        assertEquals(1, graph.regions().size());
    }

    @Test
    void builtInRerouteTypesAreInTheCatalog() {
        List<String> ids = new ArrayList<>();
        graph.nodeTypes().all().forEach(type -> ids.add(type.id()));

        assertTrue(ids.contains(RerouteModule.DATA_TYPE_ID));
        assertTrue(ids.contains(RerouteModule.FLOW_TYPE_ID));
    }
}
