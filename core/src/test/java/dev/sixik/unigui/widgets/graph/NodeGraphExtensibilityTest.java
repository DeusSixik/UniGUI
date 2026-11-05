package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.graph.node.NodeBuilder;
import dev.sixik.unigui.widgets.graph.node.NodeType;
import dev.sixik.unigui.widgets.graph.node.PortShape;
import dev.sixik.unigui.widgets.graph.node.PortType;
import dev.sixik.unigui.widgets.graph.runtime.GraphExecutor;
import dev.sixik.unigui.widgets.graph.runtime.GraphModel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Граф можно наследовать: классы не final, у фабричных классов открыты конструкторы, а ключевые шаги отрисовки,
 * ввода и меню доступны наследнику.
 */
class NodeGraphExtensibilityTest {
    private static final class CustomGraph extends NodeGraph {
        final List<String> calls = new ArrayList<>();

        @Override
        protected void renderBackground(DrawScope draw) {
            calls.add("background");
            super.renderBackground(draw);
        }

        @Override
        protected void renderForeground(DrawScope draw) {
            calls.add("foreground");
            super.renderForeground(draw);
        }

        @Override
        protected ContextMenu buildRegionMenu(NodeGraphRegion region) {
            ContextMenu menu = super.buildRegionMenu(region);
            menu.item("Custom action", () -> calls.add("custom"));
            return menu;
        }
    }

    private static final class CustomItem extends NodeGraphItem {
        CustomItem(String id) {
            super(id);
        }
    }

    private static final class CustomRegion extends NodeGraphRegion {
        CustomRegion(String id) {
            super(id, "Custom", 100.0f, 100.0f, 300.0f, 200.0f);
        }
    }

    private static final class CustomType extends NodeType {
        CustomType(String id, Function<String, NodeGraphItem> factory) {
            super(id, "Custom type", "Custom", "", factory);
        }
    }

    private static final class CustomPortType extends PortType {
        CustomPortType() {
            super("custom", "Custom", 0xFF00FF00, PortShape.CIRCLE, false);
        }
    }

    private static final class CountingExecutor extends GraphExecutor {
        int fires;

        @Override
        public dev.sixik.unigui.widgets.graph.runtime.GraphResult fire(GraphModel model, String startNodeId) {
            fires++;
            return super.fire(model, startNodeId);
        }
    }

    @Test
    void theGraphClassesCanBeExtended() {
        List<Class<?>> mustBeOpen = List.of(NodeGraph.class, NodeGraphItem.class, NodeGraphPort.class,
                NodeGraphRegion.class, NodeGraphConnection.class, NodeGraphHistory.class, NodePalette.class,
                NodePaletteModel.class, RegionEditor.class, GraphView.class,
                dev.sixik.unigui.widgets.graph.node.NodeBuilder.class, NodeType.class, PortType.class,
                dev.sixik.unigui.widgets.graph.node.NodeTypeRegistry.class,
                dev.sixik.unigui.widgets.graph.node.PortTypeRegistry.class,
                dev.sixik.unigui.widgets.graph.node.NodeStyle.class,
                dev.sixik.unigui.widgets.graph.node.HeaderModule.class,
                dev.sixik.unigui.widgets.graph.node.PortRowModule.class,
                dev.sixik.unigui.widgets.graph.node.RerouteModule.class,
                dev.sixik.unigui.widgets.graph.node.SeparatorModule.class,
                dev.sixik.unigui.widgets.graph.node.WidgetModule.class,
                GraphExecutor.class, GraphModel.class);
        for (Class<?> type : mustBeOpen) {
            assertFalse(Modifier.isFinal(type.getModifiers()), type.getSimpleName() + " should not be final");
        }
    }

    @Test
    void aSubclassedGraph_overridesRenderingSteps() {
        CustomGraph graph = new CustomGraph();
        graph.addRegion("r", "Region", 10.0f, 10.0f, 200.0f, 120.0f);
        graph.measure(new LayoutContext(800.0f, 600.0f));
        graph.arrange(new MutableRect(0.0f, 0.0f, 800.0f, 600.0f));

        graph.render(new DefaultRenderContext(new DrawList()));

        assertTrue(graph.calls.contains("background"));
        assertTrue(graph.calls.contains("foreground"));
    }

    @Test
    void aSubclassedGraph_extendsTheRegionMenu() {
        CustomGraph graph = new CustomGraph();
        graph.addRegion("r", "Setup", 100.0f, 100.0f, 400.0f, 250.0f);
        OverlayLayer layer = new OverlayLayer();
        layer.content(graph);
        layer.applyQueuedMutations();
        layer.setUiContextInternal(new DefaultUIContext());
        layer.measure(new LayoutContext(1000.0f, 800.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 1000.0f, 800.0f));

        graph.handle(new PointerPressedEvent(graph, 250.0f, 110.0f, 250.0f, 110.0f, 1, PointerButton.SECONDARY));

        layer.applyQueuedMutations();
        ContextMenu menu = null;
        for (var child : layer.children()) {
            if (child instanceof ContextMenu candidate && candidate.opened()) menu = candidate;
        }
        assertNotNull(menu);
        int last = menu.itemCount() - 1;
        assertEquals("Custom action", menu.itemButton(last).text());
        menu.itemButton(last).click();
        assertTrue(graph.calls.contains("custom"));
    }

    @Test
    void subclassesOfItemsAndRegions_liveInAGraph() {
        NodeGraph graph = new NodeGraph();
        CustomItem item = new CustomItem("n");
        CustomRegion region = new CustomRegion("r");

        graph.addItem(item);
        graph.addRegion(region);

        assertEquals(item, graph.item("n"));
        assertEquals(region, graph.region("r"));
        assertInstanceOf(CustomRegion.class, graph.regions().get(0));
    }

    @Test
    void subclassedTypesCanBeRegisteredAndSpawned() {
        NodeGraph graph = new NodeGraph();
        graph.portTypes().register(new CustomPortType());
        graph.nodeTypes().register(new CustomType("custom.node", id -> new CustomItem(id)));

        NodeGraphItem spawned = graph.spawn("custom.node", "c", 5.0f, 6.0f);

        assertInstanceOf(CustomItem.class, spawned);
        assertEquals("Custom", graph.portTypes().get("custom").displayName());
    }

    @Test
    void aSubclassedExecutorStillRuns() {
        CountingExecutor executor = new CountingExecutor();
        NodeGraph graph = new NodeGraph();
        graph.nodeTypes().register(NodeType.of("start", "Start", "T", id -> NodeBuilder.node(id)
                .header("Start").flowRow(null, "then").build()));
        graph.spawn("start", "s", 0.0f, 0.0f);
        executor.register("start", dev.sixik.unigui.widgets.graph.runtime.NodeBehavior.exec(context -> {
        }));

        executor.fire(GraphModel.from(graph), "s");

        assertEquals(1, executor.fires);
    }
}
