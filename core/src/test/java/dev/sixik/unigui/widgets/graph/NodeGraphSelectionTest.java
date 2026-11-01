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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Клики и выделение: провод под нодой не перехватывает клик, а выделение нод, проводов и регионов исключает друг друга.
 *
 * <p>Сцена: {@code src} слева, {@code sink} справа, между ними горизонтальный провод на высоте 35, а поверх провода
 * лежит нода {@code blocker}.</p>
 */
class NodeGraphSelectionTest {
    private static final float WIRE_Y = 35.0f;

    private NodeGraph graph;
    private NodeGraphItem blocker;

    @BeforeEach
    void setUp() {
        graph = new NodeGraph().selectionMode(NodeGraphSelectionMode.MULTIPLE);
        graph.portTypes().register(PortType.data("vec3", 0xFFF5D547));
        graph.nodeTypes()
                .register(NodeType.of("src", "Source", "Test", id -> NodeBuilder.node(id)
                        .header("Source").output("out", "Out", "vec3").build()))
                .register(NodeType.of("sink", "Sink", "Test", id -> NodeBuilder.node(id)
                        .header("Sink").input("in", "In", "vec3").build()))
                .register(NodeType.of("fn", "Blocker", "Test", id -> NodeBuilder.node(id)
                        .header("Blocker").flowRow("exec", "then").input("in", "In", "vec3")
                        .output("out", "Out", "vec3").build()));
        graph.spawn("src", "src", 0.0f, 0.0f);
        graph.spawn("sink", "sink", 500.0f, 0.0f);
        blocker = graph.spawn("fn", "blocker", 250.0f, 10.0f);
        graph.addConnection("wire", "src", "out", "sink", "in");
        graph.measure(new LayoutContext(900.0f, 700.0f));
        graph.arrange(new MutableRect(0.0f, 0.0f, 900.0f, 700.0f));
    }

    private void press(float x, float y) {
        graph.handle(new PointerPressedEvent(graph, x, y, x, y, 1, PointerButton.PRIMARY));
    }

    private void release(float x, float y) {
        graph.handle(new PointerReleasedEvent(graph, x, y, x, y, 1, PointerButton.PRIMARY));
    }

    private void click(float x, float y) {
        press(x, y);
        release(x, y);
    }

    private void move(float x, float y) {
        graph.handle(new PointerMovedEvent(graph, x, y, x, y, 1));
    }

    private void key(int code, int modifiers) {
        graph.handle(new KeyPressedEvent(graph, code, 0, modifiers));
    }

    /** Точка внутри blocker, через которую проходит провод. */
    private float underNodeX() {
        return blocker.x() + 50.0f;
    }

    // ---- провод под нодой ----

    @Test
    void sceneGeometry_theWireReallyRunsUnderTheBlocker() {
        assertTrue(blocker.x() < 300.0f && blocker.x() + blocker.arrangedWidth() > 300.0f);
        assertTrue(blocker.y() < WIRE_Y && blocker.y() + blocker.arrangedHeight() > WIRE_Y);
        assertEquals(1, graph.connections().size());
    }

    @Test
    void clickOnNodeAboveAWire_selectsTheNode() {
        click(underNodeX(), WIRE_Y);

        assertTrue(blocker.selected());
        assertTrue(graph.selectedConnectionIds().isEmpty(), "the wire under the node must not take the click");
    }

    @Test
    void hoverOverNodeAboveAWire_highlightsTheNodeNotTheWire() {
        move(underNodeX(), WIRE_Y);

        assertEquals("blocker", graph.hoveredItemId());
        assertEquals("", graph.hoveredConnectionId());
    }

    @Test
    void hoverOnTheWireBetweenNodes_highlightsTheWire() {
        move(200.0f, WIRE_Y);

        assertEquals("wire", graph.hoveredConnectionId());
        assertEquals("", graph.hoveredItemId());
    }

    @Test
    void clickOnTheWireBetweenNodes_stillSelectsTheWire() {
        click(200.0f, WIRE_Y);

        assertEquals(List.of("wire"), graph.selectedConnectionIds());
        assertTrue(graph.selectedItemIds().isEmpty());
    }

    @Test
    void dragFromAnAboveWireNode_movesTheNodeNotTheWire() {
        press(underNodeX(), WIRE_Y);
        move(underNodeX() + 40.0f, WIRE_Y + 60.0f);
        release(underNodeX() + 40.0f, WIRE_Y + 60.0f);

        assertEquals(290.0f, blocker.x(), 1.0e-3f);
        assertEquals(70.0f, blocker.y(), 1.0e-3f);
        assertNotNull(graph.connection("wire"));
    }

    // ---- исключающее выделение мышью ----

    @Test
    void nodeThenWire_deleteRemovesOnlyTheWire() {
        click(underNodeX(), WIRE_Y);
        assertTrue(blocker.selected());

        click(200.0f, WIRE_Y);

        assertFalse(blocker.selected(), "selecting a wire deselects the node");
        assertEquals(List.of("wire"), graph.selectedConnectionIds());
        key(KeyCodes.DELETE, 0);
        assertNull(graph.connection("wire"));
        assertNotNull(graph.item("blocker"), "the node must survive deleting the wire");
    }

    @Test
    void wireThenNode_deleteRemovesOnlyTheNode() {
        click(200.0f, WIRE_Y);
        assertEquals(List.of("wire"), graph.selectedConnectionIds());

        click(underNodeX(), WIRE_Y);

        assertTrue(graph.selectedConnectionIds().isEmpty(), "selecting a node deselects the wire");
        key(KeyCodes.DELETE, 0);
        assertNull(graph.item("blocker"));
        assertNotNull(graph.connection("wire"), "the wire must survive deleting an unrelated node");
    }

    @Test
    void clickOnEmptySpace_deselectsEverything() {
        graph.addRegion("area", "Area", 20.0f, 300.0f, 300.0f, 200.0f);
        click(underNodeX(), WIRE_Y);

        click(800.0f, 600.0f);

        assertTrue(graph.selectedItemIds().isEmpty());
        assertTrue(graph.selectedConnectionIds().isEmpty());
        assertNull(graph.selectedRegion());
    }

    @Test
    void regionHeader_replacesNodeAndWireSelection() {
        NodeGraphRegion region = graph.addRegion("area", "Area", 20.0f, 300.0f, 300.0f, 200.0f);
        click(underNodeX(), WIRE_Y);

        click(60.0f, 310.0f);

        assertTrue(region.selected());
        assertTrue(graph.selectedItemIds().isEmpty());
        assertTrue(graph.selectedConnectionIds().isEmpty());
        click(200.0f, WIRE_Y);
        assertFalse(region.selected(), "selecting a wire deselects the region");
        assertEquals(List.of("wire"), graph.selectedConnectionIds());
        click(underNodeX(), WIRE_Y);
        assertTrue(graph.selectedConnectionIds().isEmpty());
        assertNull(graph.selectedRegion());
    }

    @Test
    void deleteOnARegion_keepsTheNodesAndWires() {
        graph.addRegion("area", "Area", 20.0f, 300.0f, 300.0f, 200.0f);
        click(60.0f, 310.0f);

        key(KeyCodes.DELETE, 0);

        assertEquals(0, graph.regions().size());
        assertEquals(3, graph.items().size());
        assertEquals(1, graph.connections().size());
    }

    // ---- исключающее выделение через API ----

    @Test
    void api_allowsMixedSelectionAndTheSnapshotKeepsIt() {
        // Программное API ничего не сбрасывает: snapshot хранит выделение нод и проводов одновременно.
        graph.selectItem(blocker);
        graph.selectConnection("wire");

        assertTrue(blocker.selected());
        assertEquals(List.of("wire"), graph.selectedConnectionIds());
        NodeGraphSnapshot snapshot = graph.snapshot();
        assertEquals(List.of("blocker"), snapshot.selectedItemIds());
        assertEquals(List.of("wire"), snapshot.selectedConnectionIds());
    }

    @Test
    void api_additiveItemSelectionStillKeepsSeveralNodes() {
        NodeGraphItem sink = graph.item("sink");

        graph.selectItem(blocker, true);
        graph.selectItem(sink, true);

        assertEquals(2, graph.selectedItemIds().size());
    }

    @Test
    void api_selectAllClearsWireAndRegionSelection() {
        NodeGraphRegion region = graph.addRegion("area", "Area", 20.0f, 300.0f, 300.0f, 200.0f);
        graph.selectRegion(region);

        key(KeyCodes.A, KeyModifiers.CONTROL);

        assertFalse(region.selected());
        assertEquals(3, graph.selectedItemIds().size());
        graph.selectConnection("wire");
        key(KeyCodes.A, KeyModifiers.CONTROL);
        assertTrue(graph.selectedConnectionIds().isEmpty());
    }

    @Test
    void selectionEvents_reportTheClearedKind() {
        List<String> connectionEvents = new ArrayList<>();
        List<String> itemEvents = new ArrayList<>();
        graph.onConnectionSelectionChanged(event -> connectionEvents.add(event.newSelection().toString()));
        graph.onSelectionChanged(event -> itemEvents.add(event.newSelection().toString()));

        click(underNodeX(), WIRE_Y);
        click(200.0f, WIRE_Y);
        click(underNodeX(), WIRE_Y);

        assertEquals(List.of("[blocker]", "[]", "[blocker]"), itemEvents);
        assertEquals(List.of("[wire]", "[]"), connectionEvents);
    }

    @Test
    void groupingNodes_afterSelectingAWire_leavesOnlyTheRegionSelected() {
        graph.selectItem(blocker);
        graph.selectConnection("wire");

        NodeGraphRegion region = graph.groupSelected();

        assertTrue(region.selected());
        assertTrue(graph.selectedItemIds().isEmpty());
        assertTrue(graph.selectedConnectionIds().isEmpty());
    }

    @Test
    void paste_afterSelectingAWire_deleteLeavesTheWireAlone() {
        graph.selectItem(blocker);
        graph.copySelection();
        graph.selectConnection("wire");

        graph.paste(600.0f, 300.0f);
        key(KeyCodes.DELETE, 0);

        assertNotNull(graph.connection("wire"), "pasting selects the pasted nodes and deselects the wire");
        assertEquals(3, graph.items().size(), "only the pasted node was deleted");
    }
}
