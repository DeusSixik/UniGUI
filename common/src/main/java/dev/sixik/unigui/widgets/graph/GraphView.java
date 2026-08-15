package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.GraphNodeClickEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.GraphViewRenderer;
import dev.sixik.unigui.widgets.render.GraphViewState;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.function.Function;

public final class GraphView extends WidgetBase {
    public static final float DEFAULT_PREFERRED_WIDTH = 240.0f;
    public static final float DEFAULT_PREFERRED_HEIGHT = 130.0f;

    private static final float DEFAULT_NODE_RADIUS = 5.0f;
    private static final float NODE_HIT_RADIUS = 8.0f;

    private final List<Node> nodes = new ObjectArrayList<>();
    private final List<Edge> edges = new ObjectArrayList<>();
    private final MutableColor nodeColor = new MutableColor(0.25f, 0.78f, 1.0f, 0.92f);
    private final MutableColor hoveredNodeColor = new MutableColor(1.0f, 0.78f, 0.28f, 1.0f);
    private final MutableColor edgeColor = new MutableColor(0.55f, 0.68f, 0.82f, 0.70f);
    private final MutableColor labelColor = new MutableColor(0.75f, 0.90f, 1.0f, 1.0f);
    private final MutableColor tooltipBackground = new MutableColor(0.025f, 0.030f, 0.040f, 0.96f);
    private final MutableColor tooltipBorder = new MutableColor(0.25f, 0.78f, 1.0f, 0.92f);
    private boolean nodeLabelsVisible = true;
    private NodeLabelPlacement nodeLabelPlacement = NodeLabelPlacement.RIGHT;
    private GraphNodeRenderer nodeRenderer;
    private GraphNodeLabelRenderer nodeLabelRenderer;
    private GraphNodeTooltipRenderer nodeTooltipRenderer;
    private GraphViewRenderer renderer;
    private Function<NodePoint, String> nodeLabelProvider = NodePoint::id;
    private Function<NodePoint, String> nodeTooltipProvider = node -> node.id();
    private int hoveredNodeIndex = -1;
    private float preferredWidth = DEFAULT_PREFERRED_WIDTH;
    private float preferredHeight = DEFAULT_PREFERRED_HEIGHT;

    public GraphView() {
        nodeColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        hoveredNodeColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        edgeColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        labelColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        tooltipBackground.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        tooltipBorder.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public GraphView addNode(String id, float x, float y) {
        nodes.add(new Node(id, x, y));
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public GraphView addEdge(String from, String to) {
        edges.add(new Edge(from, to));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public GraphView clearGraph() {
        nodes.clear();
        edges.clear();
        hoveredNodeIndex = -1;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor nodeColor() {
        return nodeColor;
    }

    public MutableColor hoveredNodeColor() {
        return hoveredNodeColor;
    }

    public MutableColor edgeColor() {
        return edgeColor;
    }

    public MutableColor labelColor() {
        return labelColor;
    }

    public MutableColor tooltipBackground() {
        return tooltipBackground;
    }

    public MutableColor tooltipBorder() {
        return tooltipBorder;
    }

    public GraphView nodeLabelsVisible(boolean nodeLabelsVisible) {
        if (this.nodeLabelsVisible == nodeLabelsVisible) return this;
        this.nodeLabelsVisible = nodeLabelsVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public GraphView nodeLabelPlacement(NodeLabelPlacement nodeLabelPlacement) {
        this.nodeLabelPlacement = nodeLabelPlacement == null ? NodeLabelPlacement.RIGHT : nodeLabelPlacement;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public GraphView nodeLabelProvider(Function<NodePoint, String> nodeLabelProvider) {
        this.nodeLabelProvider = nodeLabelProvider == null ? NodePoint::id : nodeLabelProvider;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public GraphView nodeTooltipProvider(Function<NodePoint, String> nodeTooltipProvider) {
        this.nodeTooltipProvider = nodeTooltipProvider == null ? node -> node.id() : nodeTooltipProvider;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public GraphView nodeRenderer(GraphNodeRenderer nodeRenderer) {
        this.nodeRenderer = nodeRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public GraphView nodeLabelRenderer(GraphNodeLabelRenderer nodeLabelRenderer) {
        this.nodeLabelRenderer = nodeLabelRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public GraphView nodeTooltipRenderer(GraphNodeTooltipRenderer nodeTooltipRenderer) {
        this.nodeTooltipRenderer = nodeTooltipRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public GraphViewRenderer renderer() {
        return renderer;
    }

    public GraphView renderer(GraphViewRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public GraphView useDefaultRenderer() {
        return renderer(null);
    }

    public float preferredWidth() {
        return preferredWidth;
    }

    public GraphView preferredWidth(float preferredWidth) {
        float normalized = positiveOr(preferredWidth, DEFAULT_PREFERRED_WIDTH);
        if (this.preferredWidth == normalized) return this;
        this.preferredWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float preferredHeight() {
        return preferredHeight;
    }

    public GraphView preferredHeight(float preferredHeight) {
        float normalized = positiveOr(preferredHeight, DEFAULT_PREFERRED_HEIGHT);
        if (this.preferredHeight == normalized) return this;
        this.preferredHeight = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public GraphView preferredSize(float width, float height) {
        return preferredWidth(width).preferredHeight(height);
    }

    public EventSubscription onNodeClick(EventListener<? super GraphNodeClickEvent> listener) {
        return on(GraphNodeClickEvent.TYPE, listener);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        setDesiredSize(resolveDesiredSize(context, preferredWidth, preferredHeight));
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled() || visibility() != Visibility.VISIBLE) return;
        if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.CAPTURE) return;

        if (event instanceof PointerMovedEvent pointer) {
            int next = hitNode(pointer.rootX(), pointer.rootY());
            if (next != hoveredNodeIndex) {
                hoveredNodeIndex = next;
                invalidate(InvalidationFlags.VISUAL);
            }
        } else if (event instanceof PointerExitedEvent) {
            clearHoveredNode();
        } else if (event instanceof PointerPressedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
            int hit = hitNode(pointer.rootX(), pointer.rootY());
            if (hit >= 0) {
                NodePoint node = nodePoint(hit, true);
                if (node != null) {
                    clickNode(node);
                    event.cancel();
                }
            }
        }
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE) return;
        if (layoutBounds().width() <= 0.0f || layoutBounds().height() <= 0.0f) return;
        effectiveRenderer().render(new DrawScope(context, transform(), layoutBounds()), snapshot());
    }

    private GraphViewRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(GraphViewRenderer.class, WidgetsRender.graphView()) : renderer;
    }

    private GraphViewState snapshot() {
        return new GraphViewState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                computeNodePoints(),
                edges,
                nodeLabelsVisible,
                nodeLabelPlacement,
                DEFAULT_NODE_RADIUS,
                nodeColor.copy(),
                hoveredNodeColor.copy(),
                edgeColor.copy(),
                labelColor.copy(),
                tooltipBackground.copy(),
                tooltipBorder.copy(),
                nodeRenderer,
                nodeLabelRenderer,
                nodeTooltipRenderer,
                nodeLabelProvider,
                nodeTooltipProvider);
    }

    private void renderNode(DrawScope draw, NodePoint node) {
        if (nodeRenderer != null) {
            nodeRenderer.render(draw, node);
            return;
        }
        float radius = node.hovered() ? DEFAULT_NODE_RADIUS + 1.5f : DEFAULT_NODE_RADIUS;
        draw.addCircleFilled(node.x(), node.y(), radius, node.hovered() ? hoveredNodeColor : nodeColor, 16);
        draw.addCircle(node.x(), node.y(), radius + 1.0f,
                MutableColor.rgba(0.0f, 0.0f, 0.0f, node.hovered() ? 0.70f : 0.35f), 16, 1.0f);
    }

    private void renderNodeLabel(DrawScope draw, NodePoint node) {
        if (nodeLabelRenderer != null) {
            nodeLabelRenderer.render(draw, node);
            return;
        }
        String text = nodeLabelProvider.apply(node);
        if (text == null || text.isEmpty()) return;
        float textWidth = Math.max(14.0f, text.length() * 6.0f);
        float textHeight = 10.0f;
        float tx = switch (nodeLabelPlacement) {
            case CENTER -> node.x() - textWidth * 0.5f;
            case ABOVE, BELOW -> node.x() - textWidth * 0.5f;
            case RIGHT, NONE -> node.x() + DEFAULT_NODE_RADIUS + 3.0f;
        };
        float ty = switch (nodeLabelPlacement) {
            case CENTER -> node.y() - textHeight * 0.5f;
            case ABOVE -> node.y() - DEFAULT_NODE_RADIUS - textHeight - 3.0f;
            case BELOW -> node.y() + DEFAULT_NODE_RADIUS + 3.0f;
            case RIGHT, NONE -> node.y() - textHeight * 0.5f;
        };
        TextEngine.draw(draw.context(), RichText.plain(text), tx, ty, textWidth, textHeight,
                Paint.fill(labelColor), draw.transform(), Alignment.CENTER, Alignment.CENTER);
    }

    private void renderNodeTooltip(DrawScope draw, NodePoint node) {
        if (nodeTooltipRenderer != null) {
            nodeTooltipRenderer.render(draw, node);
            return;
        }
        String text = nodeTooltipProvider.apply(node);
        if (text == null || text.isEmpty()) return;
        float tooltipWidth = Math.max(44.0f, text.length() * 6.0f + 10.0f);
        float tooltipHeight = 16.0f;
        float tx = clamp(node.x() - tooltipWidth * 0.5f,
                layoutBounds().x(), layoutBounds().x() + layoutBounds().width() - tooltipWidth);
        float ty = Math.max(layoutBounds().y(), node.y() - DEFAULT_NODE_RADIUS - tooltipHeight - 8.0f);
        draw.roundedRect(tx, ty, tooltipWidth, tooltipHeight, 3.0f, Paint.fill(tooltipBackground));
        draw.roundedRect(tx, ty, tooltipWidth, tooltipHeight, 3.0f, Paint.stroke(tooltipBorder, 1.0f));
        draw.addText(text, tx + 5.0f, ty + 3.0f, tooltipWidth - 10.0f, tooltipHeight - 4.0f, labelColor);
    }

    private List<NodePoint> computeNodePoints() {
        if (nodes.isEmpty()) return List.of();
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        List<NodePoint> points = new ObjectArrayList<>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            points.add(new NodePoint(i, node.id(), node.x(), node.y(),
                    x + node.x() * width, y + node.y() * height, i == hoveredNodeIndex));
        }
        return points;
    }

    private int hitNode(float rootX, float rootY) {
        float radiusSquared = NODE_HIT_RADIUS * NODE_HIT_RADIUS;
        for (NodePoint node : computeNodePoints()) {
            float dx = rootX - node.x();
            float dy = rootY - node.y();
            if (dx * dx + dy * dy <= radiusSquared) {
                return node.index();
            }
        }
        return -1;
    }

    private NodePoint nodePoint(int index, boolean hovered) {
        for (NodePoint node : computeNodePoints()) {
            if (node.index() == index) {
                return new NodePoint(node.index(), node.id(), node.normalizedX(), node.normalizedY(), node.x(), node.y(), hovered);
            }
        }
        return null;
    }

    private GraphNodeClickEvent clickNode(NodePoint node) {
        GraphNodeClickEvent event = new GraphNodeClickEvent(this,
                node.index(), node.id(), node.normalizedX(), node.normalizedY(), node.x(), node.y());
        UIContext context = uiContext();
        if (context == null) {
            emit(event);
        } else {
            context.routedEvents().dispatch(event);
        }
        return event;
    }

    private void clearHoveredNode() {
        if (hoveredNodeIndex == -1) return;
        hoveredNodeIndex = -1;
        invalidate(InvalidationFlags.VISUAL);
    }

    private Node node(String id) {
        for (Node node : nodes) {
            if (node.id().equals(id)) return node;
        }
        return null;
    }

    private static float clamp(float value, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    public enum NodeLabelPlacement {
        NONE,
        CENTER,
        RIGHT,
        ABOVE,
        BELOW
    }

    public record Node(String id, float x, float y) {
    }

    public record Edge(String from, String to) {
    }

    public record NodePoint(int index, String id, float normalizedX, float normalizedY, float x, float y, boolean hovered) {
    }

    @FunctionalInterface
    public interface GraphNodeRenderer {
        void render(DrawScope draw, NodePoint node);
    }

    @FunctionalInterface
    public interface GraphNodeLabelRenderer {
        void render(DrawScope draw, NodePoint node);
    }

    @FunctionalInterface
    public interface GraphNodeTooltipRenderer {
        void render(DrawScope draw, NodePoint node);
    }
}
