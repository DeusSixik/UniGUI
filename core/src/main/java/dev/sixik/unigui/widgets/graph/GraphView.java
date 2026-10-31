package dev.sixik.unigui.widgets.graph;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

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
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.impl.widget.WidgetBase;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.function.Function;

@AiDoc(summary = "Static or interactive diagram graph widget displaying connected nodes, edges, labels, and tooltips with custom renderers.")
@WikiPage(
        title = "GraphView",
        slug = "graph-view",
        category = "<key:graph.category>",
        summary = "<key:graph.view.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:graph.view.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:graph.view.example.basic.title>",
        code = "GraphView graph = new GraphView();\ngraph.addNode(\"a\", 0.2f, 0.5f);\ngraph.addNode(\"b\", 0.8f, 0.5f);\ngraph.addEdge(\"a\", \"b\");"
)
@WikiLink(
        label = "NodeGraph",
        target = "page:node-graph",
        description = "<key:node.graph.summary>"
)
@XmlWidgetName("GraphView")
public final class GraphView extends WidgetBase {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.GRAPH_VIEW;

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
    private Function<NodePoint, String> nodeLabelProvider = NodePoint::id;
    private Function<NodePoint, String> nodeTooltipProvider = node -> node.id();
    private int hoveredNodeIndex = -1;
    private float preferredWidth = DEFAULT_PREFERRED_WIDTH;
    private float preferredHeight = DEFAULT_PREFERRED_HEIGHT;

    @AiDoc(summary = "Constructs a GraphView diagram widget with default styles.")
    @WikiMethod(summary = "<key:graph.view.method.ctorDefault>")
    public GraphView() {
        nodeColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        hoveredNodeColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        edgeColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        labelColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        tooltipBackground.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        tooltipBorder.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    @AiDoc(summary = "Adds a node with ID and normalized (0..1) or absolute coordinates.")
    @WikiMethod(summary = "<key:graph.view.method.addNode>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView addNode(@WikiParameter(name = "id", description = "<key:graph.view.param.nodeId>") String id, @WikiParameter(name = "x", description = "<key:graph.view.param.x>") float x, @WikiParameter(name = "y", description = "<key:graph.view.param.y>") float y) {
        nodes.add(new Node(id, x, y));
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Adds a directed edge connection between two node IDs.")
    @WikiMethod(summary = "<key:graph.view.method.addEdge>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView addEdge(@WikiParameter(name = "from", description = "<key:graph.view.param.fromNode>") String from, @WikiParameter(name = "to", description = "<key:graph.view.param.toNode>") String to) {
        edges.add(new Edge(from, to));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Clears all nodes and edges from the diagram.")
    @WikiMethod(summary = "<key:graph.view.method.clearGraph>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView clearGraph() {
        nodes.clear();
        edges.clear();
        hoveredNodeIndex = -1;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns the default node circle fill color.")
    @WikiMethod(summary = "<key:graph.view.method.nodeColor>")
    @WikiReturn("<key:graph.view.return.color>")
    public MutableColor nodeColor() {
        return nodeColor;
    }

    @AiDoc(summary = "Returns the hover fill color for nodes.")
    @WikiMethod(summary = "<key:graph.view.method.hoveredNodeColor>")
    @WikiReturn("<key:graph.view.return.color>")
    public MutableColor hoveredNodeColor() {
        return hoveredNodeColor;
    }

    @AiDoc(summary = "Returns the connection edge line color.")
    @WikiMethod(summary = "<key:graph.view.method.edgeColor>")
    @WikiReturn("<key:graph.view.return.color>")
    public MutableColor edgeColor() {
        return edgeColor;
    }

    @AiDoc(summary = "Returns the node label text color.")
    @WikiMethod(summary = "<key:graph.view.method.labelColor>")
    @WikiReturn("<key:graph.view.return.color>")
    public MutableColor labelColor() {
        return labelColor;
    }

    @AiDoc(summary = "Returns the background fill color of node tooltip popups.")
    @WikiMethod(summary = "<key:graph.view.method.tooltipBackground>")
    @WikiReturn("<key:graph.view.return.color>")
    public MutableColor tooltipBackground() {
        return tooltipBackground;
    }

    @AiDoc(summary = "Returns the border outline color of node tooltip popups.")
    @WikiMethod(summary = "<key:graph.view.method.tooltipBorder>")
    @WikiReturn("<key:graph.view.return.color>")
    public MutableColor tooltipBorder() {
        return tooltipBorder;
    }

    @XmlAttribute(value = "nodeLabelsVisible", category = "Appearance", defaultValue = "true", description = "Whether node labels are rendered.")
    @AiDoc(summary = "Sets whether text labels are rendered alongside node points.")
    @WikiMethod(summary = "<key:graph.view.method.nodeLabelsVisible>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView nodeLabelsVisible(@WikiParameter(name = "nodeLabelsVisible", description = "<key:graph.view.param.nodeLabelsVisible>") boolean nodeLabelsVisible) {
        if (this.nodeLabelsVisible == nodeLabelsVisible) return this;
        this.nodeLabelsVisible = nodeLabelsVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @XmlAttribute(value = "nodeLabelPlacement", category = "Appearance", defaultValue = "right", description = "Placement of labels relative to graph nodes.")
    @AiDoc(summary = "Sets relative placement of text labels around node points.")
    @WikiMethod(summary = "<key:graph.view.method.nodeLabelPlacement>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView nodeLabelPlacement(@WikiParameter(name = "nodeLabelPlacement", description = "<key:graph.view.param.nodeLabelPlacement>") NodeLabelPlacement nodeLabelPlacement) {
        this.nodeLabelPlacement = nodeLabelPlacement == null ? NodeLabelPlacement.RIGHT : nodeLabelPlacement;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Configures a function returning the text label for a given node point.")
    @WikiMethod(summary = "<key:graph.view.method.nodeLabelProvider>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView nodeLabelProvider(@WikiParameter(name = "nodeLabelProvider", description = "<key:graph.view.param.nodeLabelProvider>") java.util.function.Function<NodePoint, String> nodeLabelProvider) {
        this.nodeLabelProvider = nodeLabelProvider == null ? NodePoint::id : nodeLabelProvider;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Configures a function returning tooltip text for hovered nodes.")
    @WikiMethod(summary = "<key:graph.view.method.nodeTooltipProvider>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView nodeTooltipProvider(@WikiParameter(name = "nodeTooltipProvider", description = "<key:graph.view.param.nodeTooltipProvider>") java.util.function.Function<NodePoint, String> nodeTooltipProvider) {
        this.nodeTooltipProvider = nodeTooltipProvider == null ? node -> node.id() : nodeTooltipProvider;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Sets a custom renderer callback for drawing node points.")
    @WikiMethod(summary = "<key:graph.view.method.nodeRenderer>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView nodeRenderer(@WikiParameter(name = "nodeRenderer", description = "<key:graph.view.param.nodeRenderer>") GraphNodeRenderer nodeRenderer) {
        this.nodeRenderer = nodeRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Sets a custom renderer callback for drawing node labels.")
    @WikiMethod(summary = "<key:graph.view.method.nodeLabelRenderer>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView nodeLabelRenderer(@WikiParameter(name = "nodeLabelRenderer", description = "<key:graph.view.param.nodeLabelRenderer>") GraphNodeLabelRenderer nodeLabelRenderer) {
        this.nodeLabelRenderer = nodeLabelRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Sets a custom renderer callback for drawing node hover tooltips.")
    @WikiMethod(summary = "<key:graph.view.method.nodeTooltipRenderer>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView nodeTooltipRenderer(@WikiParameter(name = "nodeTooltipRenderer", description = "<key:graph.view.param.nodeTooltipRenderer>") GraphNodeTooltipRenderer nodeTooltipRenderer) {
        this.nodeTooltipRenderer = nodeTooltipRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns the preferred width of this graph view.")
    @WikiMethod(summary = "<key:graph.view.method.preferredWidthGet>")
    @WikiReturn("<key:graph.view.return.preferredWidth>")
    public float preferredWidth() {
        return preferredWidth;
    }

    @XmlAttribute(value = "preferredWidth", category = "Layout", defaultValue = "240", description = "Intrinsic graph width before layout constraints are applied.")
    @AiDoc(summary = "Sets the preferred width of this graph view.")
    @WikiMethod(summary = "<key:graph.view.method.preferredWidthSet>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView preferredWidth(@WikiParameter(name = "preferredWidth", description = "<key:graph.view.param.preferredWidth>") float preferredWidth) {
        float normalized = positiveOr(preferredWidth, DEFAULT_PREFERRED_WIDTH);
        if (this.preferredWidth == normalized) return this;
        this.preferredWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns the preferred height of this graph view.")
    @WikiMethod(summary = "<key:graph.view.method.preferredHeightGet>")
    @WikiReturn("<key:graph.view.return.preferredHeight>")
    public float preferredHeight() {
        return preferredHeight;
    }

    @XmlAttribute(value = "preferredHeight", category = "Layout", defaultValue = "130", description = "Intrinsic graph height before layout constraints are applied.")
    @AiDoc(summary = "Sets the preferred height of this graph view.")
    @WikiMethod(summary = "<key:graph.view.method.preferredHeightSet>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView preferredHeight(@WikiParameter(name = "preferredHeight", description = "<key:graph.view.param.preferredHeight>") float preferredHeight) {
        float normalized = positiveOr(preferredHeight, DEFAULT_PREFERRED_HEIGHT);
        if (this.preferredHeight == normalized) return this;
        this.preferredHeight = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Sets both preferred width and height dimensions.")
    @WikiMethod(summary = "<key:graph.view.method.preferredSize>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView preferredSize(@WikiParameter(name = "width", description = "<key:graph.view.param.preferredWidth>") float width, @WikiParameter(name = "height", description = "<key:graph.view.param.preferredHeight>") float height) {
        return preferredWidth(width).preferredHeight(height);
    }

    @AiDoc(summary = "Registers a listener called when a node point is clicked.")
    @WikiMethod(summary = "<key:graph.view.method.onNodeClick>")
    @WikiReturn("<key:graph.view.return.subscription>")
    public EventSubscription onNodeClick(@WikiParameter(name = "listener", description = "<key:graph.view.param.listener>") EventListener<? super GraphNodeClickEvent> listener) {
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
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        if (renderCustomVisual(draw)) {
            return;
        }
        draw.rect(layoutBounds().x(), layoutBounds().y(),
                layoutBounds().width(), layoutBounds().height(), Paint.stroke(edgeColor, 1.0f));
        List<NodePoint> points = computeNodePoints();
        for (Edge edge : edges) {
            NodePoint from = findNode(points, edge.from());
            NodePoint to = findNode(points, edge.to());
            if (from != null && to != null) {
                draw.addLine(from.x(), from.y(), to.x(), to.y(), edgeColor, 1.0f);
            }
        }
        NodePoint hovered = null;
        for (NodePoint point : points) {
            if (point.hovered()) hovered = point;
            renderNode(draw, point);
            if (nodeLabelsVisible && nodeLabelPlacement != NodeLabelPlacement.NONE) {
                renderNodeLabel(draw, point);
            }
        }
        if (hovered != null) {
            renderNodeTooltip(draw, hovered);
        }
    }

    private static NodePoint findNode(List<NodePoint> points, String id) {
        for (NodePoint point : points) {
            if (point.id().equals(id)) return point;
        }
        return null;
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

    @AiDoc(summary = "Specifies relative label placement position around a node: TOP, BOTTOM, LEFT, RIGHT, or CENTER.")
    public enum NodeLabelPlacement {
        NONE,
        CENTER,
        RIGHT,
        ABOVE,
        BELOW
    }

    @AiDoc(summary = "Represents a point node with normalized or world coordinates in a GraphView diagram.")
    public record Node(String id, float x, float y) {
    }

    @AiDoc(summary = "Represents an edge connection link between two node identifiers.")
    public record Edge(String from, String to) {
    }

    @AiDoc(summary = "Geometry point data for a rendered node with hover state.")
    public record NodePoint(int index, String id, float normalizedX, float normalizedY, float x, float y, boolean hovered) {
    }

    @FunctionalInterface
    @AiDoc(summary = "Callback interface for custom rendering of graph node points.")
    public interface GraphNodeRenderer {
        void render(DrawScope draw, NodePoint node);
    }

    @FunctionalInterface
    @AiDoc(summary = "Callback interface for custom rendering of graph node text labels.")
    public interface GraphNodeLabelRenderer {
        void render(DrawScope draw, NodePoint node);
    }

    @FunctionalInterface
    @AiDoc(summary = "Callback interface for custom rendering of graph node tooltip popups.")
    public interface GraphNodeTooltipRenderer {
        void render(DrawScope draw, NodePoint node);
    }
}
