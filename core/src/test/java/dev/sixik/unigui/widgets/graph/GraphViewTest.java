package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.event.GraphNodeClickEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.style.StyleKeyRegistry;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Граф: добавление и удаление, раскладка, наведение, события, рёбра и стили GraphView.
 *
 * <p>Размер виджета 200x100. Радиус узла по умолчанию 6, запас
 * {@code inset = 6 + 1.5 + 4 = 11.5}, поэтому область построения — {@code x in [11.5, 188.5]},
 * {@code y in [11.5, 88.5]}.</p>
 */
class GraphViewTest {
    /** Допуск координат: float-вычисления с несколькими сложениями. */
    private static final float EPSILON = 1.0e-3f;
    private static final MutableColor RED = MutableColor.rgba(1.0f, 0.0f, 0.0f, 1.0f);

    private static DrawList render(GraphView graph) {
        graph.measure(new LayoutContext(200.0f, 100.0f));
        graph.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        DrawList list = new DrawList();
        graph.render(new DefaultRenderContext(list));
        return list;
    }

    private static GraphView twoNodes() {
        GraphView graph = new GraphView().preferredSize(200.0f, 100.0f)
                .addNode("a", 0.0f, 0.5f)
                .addNode("b", 1.0f, 0.5f);
        render(graph);
        return graph;
    }

    private static int count(DrawList list, DrawCommandType type) {
        int result = 0;
        for (DrawCommand command : list.commands()) {
            if (command.type() == type) result++;
        }
        return result;
    }

    private static boolean hasColor(DrawList list, MutableColor color) {
        for (DrawCommand command : list.commands()) {
            if (command.paint() == null) continue;
            var c = command.paint().color();
            if (Math.abs(c.r() - color.r()) < 1e-4f && Math.abs(c.g() - color.g()) < 1e-4f
                    && Math.abs(c.b() - color.b()) < 1e-4f && Math.abs(c.a() - color.a()) < 1e-4f) {
                return true;
            }
        }
        return false;
    }

    @Test
    void addNode_sameIdTwice_movesNodeInsteadOfDuplicating() {
        GraphView graph = new GraphView().addNode("a", 0.1f, 0.1f).addNode("a", 0.9f, 0.9f);

        assertEquals(1, graph.nodeCount());
    }

    @Test
    void addNode_nullId_isIgnored() {
        GraphView graph = new GraphView().addNode(null, 0.5f, 0.5f);

        assertEquals(0, graph.nodeCount());
    }

    @Test
    void removeNode_dropsItsEdgesToo() {
        GraphView graph = new GraphView().addNode("a", 0.1f, 0.5f).addNode("b", 0.5f, 0.5f)
                .addNode("c", 0.9f, 0.5f).addEdge("a", "b").addEdge("b", "c").addEdge("a", "c");

        graph.removeNode("b");

        assertEquals(2, graph.nodeCount());
        assertEquals(1, graph.edgeCount());
    }

    @Test
    void clearGraph_removesEverything() {
        GraphView graph = twoNodes().addEdge("a", "b");

        graph.clearGraph();

        assertEquals(0, graph.nodeCount());
        assertEquals(0, graph.edgeCount());
        render(graph);
    }

    @Test
    void edge_toMissingNode_waitsAndAppearsOnceNodeIsAdded() {
        GraphView graph = new GraphView().preferredSize(200.0f, 100.0f).addNode("a", 0.0f, 0.5f).addEdge("a", "b");
        int lines = count(render(graph), DrawCommandType.LINE);

        graph.addNode("b", 1.0f, 0.5f);

        assertEquals(lines + 1, count(render(graph), DrawCommandType.LINE));
    }

    @Test
    void edge_directed_addsArrowheadOverUndirectedEdge() {
        GraphView graph = twoNodes().addEdge("a", "b");
        int directed = render(graph).commands().size();

        graph.directed(false);

        assertTrue(directed > render(graph).commands().size(), "the arrowhead is an extra command");
    }

    @Test
    void edge_touchingNodes_isNotDrawn() {
        GraphView far = twoNodes().addEdge("a", "b");
        GraphView touching = new GraphView().preferredSize(200.0f, 100.0f)
                .addNode("a", 0.5f, 0.5f).addNode("b", 0.5f, 0.5f).addEdge("a", "b");

        int farLines = count(render(far), DrawCommandType.LINE);
        int touchingLines = count(render(touching), DrawCommandType.LINE);

        assertEquals(farLines - 1, touchingLines);
    }

    @Test
    void edge_selfLoop_rendersWithoutFailure() {
        GraphView graph = twoNodes().addEdge("a", "a");

        render(graph);
    }

    @Test
    void hitNode_pointerNearNode_returnsItsIndex() {
        GraphView graph = twoNodes();

        // Узел a стоит в x = 11.5, y = 50 (нормализованные 0 и 0.5, сдвинутые на запас).
        assertEquals(0, graph.hitNode(11.5f, 50.0f));
        assertEquals(1, graph.hitNode(188.5f, 50.0f));
        assertEquals(-1, graph.hitNode(100.0f, 50.0f));
    }

    @Test
    void hitNode_overlappingNodes_choosesTheClosest() {
        GraphView graph = new GraphView().preferredSize(200.0f, 100.0f)
                .addNode("far", 0.50f, 0.5f)
                .addNode("near", 0.53f, 0.5f);
        render(graph);
        float nearX = 11.5f + 0.53f * 177.0f;

        assertEquals(1, graph.hitNode(nearX + 0.5f, 50.0f));
    }

    @Test
    void hitNode_beforeFirstRender_isEmpty() {
        GraphView graph = new GraphView().addNode("a", 0.5f, 0.5f);

        assertEquals(-1, graph.hitNode(50.0f, 50.0f));
    }

    @Test
    void click_node_firesEventWithIdAndNormalizedCoordinates() {
        GraphView graph = twoNodes();
        AtomicReference<GraphNodeClickEvent> received = new AtomicReference<>();
        graph.onNodeClick(received::set);
        PointerPressedEvent press = new PointerPressedEvent(graph, 188.5f, 50.0f, 188.5f, 50.0f, 0,
                PointerButton.PRIMARY);

        graph.handle(press);

        GraphNodeClickEvent event = received.get();
        assertNotNull(event);
        assertEquals(1, event.index());
        assertEquals("b", event.id());
        assertEquals(1.0f, event.normalizedX(), EPSILON);
        assertEquals(0.5f, event.normalizedY(), EPSILON);
        assertEquals(188.5f, event.x(), EPSILON);
        assertTrue(press.isCancelled());
    }

    @Test
    void click_emptySpace_isIgnored() {
        GraphView graph = twoNodes();
        AtomicReference<GraphNodeClickEvent> received = new AtomicReference<>();
        graph.onNodeClick(received::set);
        PointerPressedEvent press = new PointerPressedEvent(graph, 100.0f, 50.0f, 100.0f, 50.0f, 0,
                PointerButton.PRIMARY);

        graph.handle(press);

        assertEquals(null, received.get());
        assertFalse(press.isCancelled());
    }

    @Test
    void hover_nodeThenExit_highlightsAndClears() {
        GraphView graph = twoNodes().addEdge("a", "b");
        int plain = render(graph).commands().size();

        graph.handle(new PointerMovedEvent(graph, 11.5f, 50.0f, 11.5f, 50.0f, 0));
        int hovered = render(graph).commands().size();
        graph.handle(new PointerExitedEvent(graph, 300.0f, 300.0f, 300.0f, 300.0f, 0));
        int cleared = render(graph).commands().size();

        // Наведение добавляет подсвеченные рёбра и подсказку, выход возвращает прежний кадр.
        assertTrue(hovered > plain);
        assertEquals(plain, cleared);
    }

    @Test
    void style_nodeColorKey_isUsedWhenNothingOverridesIt() {
        GraphView graph = twoNodes();
        graph.style(style -> style.put(StyleKeys.GRAPH_NODE_COLOR, RED));

        assertTrue(hasColor(render(graph), RED));
    }

    @Test
    void style_instanceNodeColor_winsOverStyleKey() {
        GraphView graph = twoNodes();
        graph.style(style -> style.put(StyleKeys.GRAPH_NODE_COLOR, RED));
        graph.nodeColor().set(0.0f, 1.0f, 0.0f, 1.0f);

        assertFalse(hasColor(render(graph), RED));
    }

    @Test
    void style_nodeRadiusKey_changesLayoutInset() {
        GraphView graph = twoNodes();
        graph.style(style -> style.put(StyleKeys.GRAPH_NODE_RADIUS, 20.0f));
        render(graph);

        // inset = 20 + 1.5 + 4 = 25.5: узел a сдвигается от края вместе с радиусом.
        assertEquals(0, graph.hitNode(25.5f, 50.0f));
    }

    @Test
    void style_edgeKeysAreRegisteredForEditors() {
        StyleKeyRegistry registry = StyleKeyRegistry.builtIns();

        assertTrue(registry.descriptor(StyleKeys.GRAPH_NODE_COLOR).isPresent());
        assertTrue(registry.descriptor(StyleKeys.GRAPH_EDGE_WIDTH).isPresent());
        assertTrue(registry.descriptor(StyleKeys.GRAPH_ARROW_SIZE).isPresent());
    }
}
