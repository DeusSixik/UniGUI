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
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.xaml.XamlProperty;
import dev.sixik.unigui.xaml.XamlWidget;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.WidgetStyles;
import dev.sixik.unigui.widgets.display.OverridableColor;
import dev.sixik.unigui.widgets.display.PlotSupport;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.function.Function;

/**
 * Диаграмма связей: узлы в нормализованных координатах, соединённые направленными рёбрами.
 *
 * <h2>Координаты</h2>
 * <p>Узел задаётся нормализованными координатами {@code (x, y)} в диапазоне [0, 1] от области построения. Область
 * построения — границы виджета, сжатые на радиус узла с запасом, поэтому узел с координатой {@code 0} или {@code 1}
 * не обрезается краем. Значения вне [0, 1] допустимы и выводят узел за область.</p>
 *
 * <h2>Рёбра</h2>
 * <p>Ребро соединяет два узла по id; ребро к ещё не добавленному узлу ждёт его появления. Линия обрезается по
 * окружностям узлов, чтобы не заходить внутрь них; при {@link #directed(boolean)} на конце рисуется стрелка. Рёбра,
 * связанные с наведённым узлом, подсвечиваются.</p>
 *
 * <h2>Стиль</h2>
 * <p>Оформление берётся из ключей {@code graph.*}, {@code plot.tooltip.*}, {@code background.*}, {@code border.*} и
 * {@code text.*}; значения по умолчанию — {@link WidgetStyles.Plot#GRAPH_VIEW}. Цвета, изменённые на экземпляре,
 * приоритетнее стиля.</p>
 */
@AiDoc(summary = "Static or interactive diagram graph widget displaying connected nodes, edges, labels, and tooltips with custom renderers.")
@WikiPage(
        title = "GraphView",
        slug = "graph-view",
        category = "<key:graph.widget.category>",
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
@XamlWidget("GraphView")
public class GraphView extends WidgetBase {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.GRAPH_VIEW;

    /**
     * Ширина по умолчанию в UI-пикселях.
     */
    public static final float DEFAULT_PREFERRED_WIDTH = 240.0f;
    /**
     * Высота по умолчанию в UI-пикселях.
     */
    public static final float DEFAULT_PREFERRED_HEIGHT = 130.0f;

    /**
     * Прибавка к радиусу наведённого узла в UI-пикселях.
     */
    private static final float HOVER_RADIUS_BONUS = 1.5f;
    /**
     * Зазор между окружностью узла и концом ребра в UI-пикселях.
     */
    private static final float EDGE_GAP = 1.0f;
    /**
     * Минимальный радиус попадания указателя в узел в UI-пикселях.
     */
    private static final float MIN_HIT_RADIUS = 8.0f;
    /**
     * Зазор между узлом и подписью в UI-пикселях.
     */
    private static final float LABEL_GAP = 3.0f;
    /**
     * Зазор между узлом и подсказкой в UI-пикселях.
     */
    private static final float TOOLTIP_GAP = 6.0f;
    /**
     * Доля ширины стрелки от её длины.
     */
    private static final float ARROW_HALF_WIDTH = 0.45f;
    /**
     * Запас вокруг узла внутри виджета в UI-пикселях.
     */
    private static final float EDGE_INSET = 4.0f;

    private final List<Node> nodes = new ObjectArrayList<>();
    private final List<Edge> edges = new ObjectArrayList<>();
    /**
     * Индекс узла по id; поддерживается в {@link #rebuildIndex()}.
     */
    private final Object2IntOpenHashMap<String> indexById = new Object2IntOpenHashMap<>();
    /**
     * Индексы концов рёбер в {@link #nodes}; {@code -1} — узел ещё не добавлен.
     */
    private int[] edgeFrom = new int[0];
    private int[] edgeTo = new int[0];

    private final OverridableColor nodeColor = new OverridableColor(
            new MutableColor(0.25f, 0.78f, 1.0f, 0.95f), this::repaint);
    private final OverridableColor hoveredNodeColor = new OverridableColor(
            new MutableColor(1.0f, 0.78f, 0.28f, 1.0f), this::repaint);
    private final OverridableColor nodeBorderColor = new OverridableColor(
            new MutableColor(0.0f, 0.0f, 0.0f, 0.40f), this::repaint);
    private final OverridableColor edgeColor = new OverridableColor(
            new MutableColor(0.55f, 0.68f, 0.82f, 0.70f), this::repaint);
    private final OverridableColor hoveredEdgeColor = new OverridableColor(
            new MutableColor(1.0f, 0.78f, 0.28f, 0.95f), this::repaint);
    private final OverridableColor labelColor = new OverridableColor(
            new MutableColor(0.75f, 0.90f, 1.0f, 1.0f), this::repaint);
    private final OverridableColor tooltipBackground = new OverridableColor(
            new MutableColor(0.025f, 0.030f, 0.040f, 0.96f), this::repaint);
    private final OverridableColor tooltipBorder = new OverridableColor(
            new MutableColor(0.25f, 0.78f, 1.0f, 0.92f), this::repaint);

    private boolean nodeLabelsVisible = true;
    private boolean directed = true;
    private NodeLabelPlacement nodeLabelPlacement = NodeLabelPlacement.RIGHT;
    private GraphNodeRenderer nodeRenderer;
    private GraphNodeLabelRenderer nodeLabelRenderer;
    private GraphNodeTooltipRenderer nodeTooltipRenderer;
    private Function<NodePoint, String> nodeLabelProvider = NodePoint::id;
    private Function<NodePoint, String> nodeTooltipProvider = node -> node.id();
    private int hoveredNodeIndex = -1;
    private float preferredWidth = DEFAULT_PREFERRED_WIDTH;
    private float preferredHeight = DEFAULT_PREFERRED_HEIGHT;

    // ---- значения стиля, разрешённые на проходе отрисовки ----
    private boolean backgroundVisible;
    private boolean borderVisible;
    private ColorView background;
    private ColorView border;
    private float borderWidth = 1.0f;
    private float surfaceRadius;
    private ColorView textColor;
    private float textSize = 9.0f;
    private ColorView resolvedNodeColor;
    private ColorView resolvedHoverNodeColor;
    private ColorView resolvedNodeBorder;
    private float nodeRadius = 6.0f;
    private ColorView resolvedEdgeColor;
    private ColorView resolvedHoverEdgeColor;
    private float edgeWidth = 1.5f;
    private float arrowSize = 7.0f;
    private ColorView resolvedTooltipBackground;
    private ColorView resolvedTooltipBorder;
    private ColorView resolvedTooltipText;

    // ---- кэш раскладки: экранные координаты центров узлов ----
    private float[] screenX = new float[0];
    private float[] screenY = new float[0];
    private boolean layoutValid;
    private int graphVersion;
    private int keyVersion = -1;
    private float keyX;
    private float keyY;
    private float keyW;
    private float keyH;
    private float keyRadius;

    // ---- кэш подписей ----
    private String[] labelStrings = new String[0];
    private RichText[] labelTexts = new RichText[0];
    private float labelSize = -1.0f;

    @AiDoc(summary = "Constructs a GraphView diagram widget with default styles.")
    @WikiMethod(summary = "<key:graph.view.method.ctorDefault>")
    public GraphView() {
        indexById.defaultReturnValue(-1);
    }

    /**
     * Возвращает стандартное оформление диаграммы; уступает теме и локальным стилям.
     *
     * @return {@link WidgetStyles.Plot#GRAPH_VIEW}
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.Plot.GRAPH_VIEW;
    }

    private void repaint() {
        invalidate(InvalidationFlags.VISUAL);
    }

    private void onGraphChanged() {
        graphVersion++;
        layoutValid = false;
        hoveredNodeIndex = -1;
        invalidate(InvalidationFlags.VISUAL);
    }

    // ------------------------------------------------------------------ граф

    @AiDoc(summary = "Adds a node with ID and normalized (0..1) coordinates; an existing ID is moved instead of duplicated.")
    @WikiMethod(summary = "<key:graph.view.method.addNode>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView addNode(@WikiParameter(name = "id", description = "<key:graph.view.param.nodeId>") String id, @WikiParameter(name = "x", description = "<key:graph.view.param.x>") float x, @WikiParameter(name = "y", description = "<key:graph.view.param.y>") float y) {
        if (id == null) return this;
        int existing = indexById.getInt(id);
        if (existing >= 0) {
            nodes.set(existing, new Node(id, x, y));
        } else {
            nodes.add(new Node(id, x, y));
            rebuildIndex();
        }
        onGraphChanged();
        return this;
    }

    @AiDoc(summary = "Adds a directed edge connection between two node IDs; the edge waits for a missing node to be added.")
    @WikiMethod(summary = "<key:graph.view.method.addEdge>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView addEdge(@WikiParameter(name = "from", description = "<key:graph.view.param.fromNode>") String from, @WikiParameter(name = "to", description = "<key:graph.view.param.toNode>") String to) {
        if (from == null || to == null) return this;
        edges.add(new Edge(from, to));
        rebuildIndex();
        onGraphChanged();
        return this;
    }

    /**
     * Удаляет узел и все рёбра, связанные с ним.
     *
     * @param id идентификатор узла
     * @return этот виджет
     */
    @AiDoc(summary = "Removes a node and every edge that references its identifier.", section = "Graph Data", sectionOrder = 10, order = 30)
    @WikiMethod(summary = "Removes a node and all incident edges.", section = "Graph Data", sectionOrder = 10, order = 30)
    @WikiReturn("This graph view for fluent configuration.")
    public GraphView removeNode(@WikiParameter(name = "id", description = "Node identifier to remove; null or unknown identifiers are ignored.") String id) {
        int index = id == null ? -1 : indexById.getInt(id);
        if (index < 0) return this;
        nodes.remove(index);
        edges.removeIf(edge -> edge.from().equals(id) || edge.to().equals(id));
        rebuildIndex();
        onGraphChanged();
        return this;
    }

    @AiDoc(summary = "Clears all nodes and edges from the diagram.")
    @WikiMethod(summary = "<key:graph.view.method.clearGraph>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView clearGraph() {
        nodes.clear();
        edges.clear();
        rebuildIndex();
        onGraphChanged();
        return this;
    }

    /**
     * Возвращает число узлов.
     *
     * @return число узлов
     */
    @WikiMethod(summary = "Returns the number of nodes currently in the graph.", section = "Graph Data", sectionOrder = 10, order = 40)
    @WikiReturn("The current number of nodes.")
    public int nodeCount() {
        return nodes.size();
    }

    /**
     * Возвращает число рёбер, включая ожидающие отсутствующий узел.
     *
     * @return число рёбер
     */
    @WikiMethod(summary = "Returns the number of edges, including edges waiting for missing endpoints.", section = "Graph Data", sectionOrder = 10, order = 50)
    @WikiReturn("The current number of stored edges.")
    public int edgeCount() {
        return edges.size();
    }

    /**
     * Пересобирает индекс узлов по id и индексы концов рёбер.
     *
     * <p>Вызывается при каждом изменении графа; стоимость {@code O(N + E)}. Благодаря индексу отрисовка и поиск
     * ребра по id не перебирают узлы: {@code O(1)} на ребро вместо {@code O(N)}.</p>
     */
    private void rebuildIndex() {
        indexById.clear();
        for (int i = 0; i < nodes.size(); i++) indexById.put(nodes.get(i).id(), i);
        int e = edges.size();
        if (edgeFrom.length < e) {
            edgeFrom = new int[e];
            edgeTo = new int[e];
        }
        for (int i = 0; i < e; i++) {
            edgeFrom[i] = indexById.getInt(edges.get(i).from());
            edgeTo[i] = indexById.getInt(edges.get(i).to());
        }
    }

    // ------------------------------------------------------------------ цвета и настройки

    @AiDoc(summary = "Returns the default node circle fill color.")
    @WikiMethod(summary = "<key:graph.view.method.nodeColor>")
    @WikiReturn("<key:graph.view.return.color>")
    public MutableColor nodeColor() {
        return nodeColor.value();
    }

    @AiDoc(summary = "Returns the hover fill color for nodes.")
    @WikiMethod(summary = "<key:graph.view.method.hoveredNodeColor>")
    @WikiReturn("<key:graph.view.return.color>")
    public MutableColor hoveredNodeColor() {
        return hoveredNodeColor.value();
    }

    /**
     * Возвращает цвет обводки узла.
     *
     * @return изменяемый цвет; изменение переопределяет стиль {@code graph.node.border-color}
     */
    @WikiMethod(summary = "Returns the mutable color used to outline graph nodes.", section = "Appearance", sectionOrder = 20, order = 10)
    @WikiReturn("The mutable RGBA node-border color.")
    public MutableColor nodeBorderColor() {
        return nodeBorderColor.value();
    }

    @AiDoc(summary = "Returns the connection edge line color.")
    @WikiMethod(summary = "<key:graph.view.method.edgeColor>")
    @WikiReturn("<key:graph.view.return.color>")
    public MutableColor edgeColor() {
        return edgeColor.value();
    }

    /**
     * Возвращает цвет рёбер, связанных с наведённым узлом.
     *
     * @return изменяемый цвет; изменение переопределяет стиль {@code graph.edge.hover-color}
     */
    @WikiMethod(summary = "Returns the mutable color used for edges connected to the hovered node.", section = "Appearance", sectionOrder = 20, order = 20)
    @WikiReturn("The mutable RGBA hovered-edge color.")
    public MutableColor hoveredEdgeColor() {
        return hoveredEdgeColor.value();
    }

    @AiDoc(summary = "Returns the node label text color.")
    @WikiMethod(summary = "<key:graph.view.method.labelColor>")
    @WikiReturn("<key:graph.view.return.color>")
    public MutableColor labelColor() {
        return labelColor.value();
    }

    @AiDoc(summary = "Returns the background fill color of node tooltip popups.")
    @WikiMethod(summary = "<key:graph.view.method.tooltipBackground>")
    @WikiReturn("<key:graph.view.return.color>")
    public MutableColor tooltipBackground() {
        return tooltipBackground.value();
    }

    @AiDoc(summary = "Returns the border outline color of node tooltip popups.")
    @WikiMethod(summary = "<key:graph.view.method.tooltipBorder>")
    @WikiReturn("<key:graph.view.return.color>")
    public MutableColor tooltipBorder() {
        return tooltipBorder.value();
    }

    /**
     * Включает или выключает стрелки на концах рёбер.
     *
     * @param directed {@code true} — рисовать стрелку у узла-назначения
     * @return этот виджет
     */
    @XamlProperty(value = "directed", category = "Appearance", defaultValue = "true", description = "Whether edges are drawn with an arrowhead at the target node.")
    @AiDoc(summary = "Enables or disables target arrowheads for edges.", section = "Appearance", sectionOrder = 20, order = 10)
    @WikiMethod(summary = "Enables or disables arrowheads at edge targets.", section = "Appearance", sectionOrder = 20, order = 30)
    @WikiReturn("This graph view for fluent configuration.")
    public GraphView directed(@WikiParameter(name = "directed", description = "True to draw an arrowhead at each target node.") boolean directed) {
        if (this.directed == directed) return this;
        this.directed = directed;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Сообщает, рисуются ли стрелки на рёбрах.
     *
     * @return {@code true}, если рёбра направленные
     */
    @WikiMethod(summary = "Returns whether edges are rendered as directed connections.", section = "Appearance", sectionOrder = 20, order = 40)
    @WikiReturn("True when edge target arrowheads are enabled.")
    public boolean directed() {
        return directed;
    }

    @XamlProperty(value = "nodeLabelsVisible", category = "Appearance", defaultValue = "true", description = "Whether node labels are rendered.")
    @AiDoc(summary = "Sets whether text labels are rendered alongside node points.")
    @WikiMethod(summary = "<key:graph.view.method.nodeLabelsVisible>")
    @WikiReturn("<key:graph.view.return.self>")
    public GraphView nodeLabelsVisible(@WikiParameter(name = "nodeLabelsVisible", description = "<key:graph.view.param.nodeLabelsVisible>") boolean nodeLabelsVisible) {
        if (this.nodeLabelsVisible == nodeLabelsVisible) return this;
        this.nodeLabelsVisible = nodeLabelsVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @XamlProperty(value = "nodeLabelPlacement", category = "Appearance", defaultValue = "right", description = "Placement of labels relative to graph nodes.")
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

    @XamlProperty(value = "preferredWidth", category = "Layout", defaultValue = "240", description = "Intrinsic graph width before layout constraints are applied.")
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

    @XamlProperty(value = "preferredHeight", category = "Layout", defaultValue = "130", description = "Intrinsic graph height before layout constraints are applied.")
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

    // ------------------------------------------------------------------ раскладка

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        setDesiredSize(resolveDesiredSize(context, preferredWidth, preferredHeight));
    }

    /**
     * Читает значения стиля в поля, используемые на проходе отрисовки.
     *
     * <p>Приоритет каждого значения: экземпляр ({@link OverridableColor}), локальный стиль, тема, стандартное
     * оформление компонента.</p>
     */
    private void resolveStyle() {
        backgroundVisible = styleValue(StyleKeys.BACKGROUND_VISIBLE, false);
        borderVisible = styleValue(StyleKeys.BORDER_VISIBLE, false);
        background = styleValue(StyleKeys.BACKGROUND_COLOR, (ColorView) null);
        border = styleValue(StyleKeys.BORDER_COLOR, (ColorView) null);
        borderWidth = Math.max(0.0f, styleValue(StyleKeys.BORDER_WIDTH, 1.0f));
        surfaceRadius = Math.max(0.0f, styleValue(StyleKeys.RADIUS, 0.0f));
        textSize = Math.max(4.0f, styleValue(StyleKeys.TEXT_SIZE, 9.0f));
        textColor = labelColor.resolve(styleValue(StyleKeys.TEXT_COLOR, (ColorView) null));
        resolvedNodeColor = nodeColor.resolve(styleValue(StyleKeys.GRAPH_NODE_COLOR, (ColorView) null));
        resolvedHoverNodeColor = hoveredNodeColor.resolve(styleValue(StyleKeys.GRAPH_NODE_HOVER_COLOR, (ColorView) null));
        resolvedNodeBorder = nodeBorderColor.resolve(styleValue(StyleKeys.GRAPH_NODE_BORDER_COLOR, (ColorView) null));
        nodeRadius = Math.max(1.0f, styleValue(StyleKeys.GRAPH_NODE_RADIUS, 6.0f));
        resolvedEdgeColor = edgeColor.resolve(styleValue(StyleKeys.GRAPH_EDGE_COLOR, (ColorView) null));
        resolvedHoverEdgeColor = hoveredEdgeColor.resolve(styleValue(StyleKeys.GRAPH_EDGE_HOVER_COLOR, (ColorView) null));
        edgeWidth = Math.max(0.5f, styleValue(StyleKeys.GRAPH_EDGE_WIDTH, 1.5f));
        arrowSize = Math.max(0.0f, styleValue(StyleKeys.GRAPH_ARROW_SIZE, 7.0f));
        resolvedTooltipBackground = tooltipBackground.resolve(
                styleValue(StyleKeys.PLOT_TOOLTIP_BACKGROUND_COLOR, (ColorView) null));
        resolvedTooltipBorder = tooltipBorder.resolve(styleValue(StyleKeys.PLOT_TOOLTIP_BORDER_COLOR, (ColorView) null));
        resolvedTooltipText = labelColor.resolve(styleValue(StyleKeys.PLOT_TOOLTIP_TEXT_COLOR, (ColorView) null));
    }

    /**
     * Приводит экранные координаты узлов в соответствие с границами, радиусом и составом графа.
     *
     * <p>Формулы для узла с нормализованными координатами {@code (nx, ny)}:
     * <pre>
     *   inset = nodeRadius + HOVER_RADIUS_BONUS + EDGE_INSET
     *   area  = bounds shrunk by inset on every side
     *   x     = area.x + nx * area.width
     *   y     = area.y + ny * area.height
     * </pre>
     * Если ничего не менялось, метод только сравнивает числа и не выделяет память.
     */
    private void prepareLayout() {
        RectView bounds = layoutBounds();
        if (layoutValid && keyVersion == graphVersion && keyX == bounds.x() && keyY == bounds.y()
                && keyW == bounds.width() && keyH == bounds.height() && keyRadius == nodeRadius) {
            return;
        }
        int n = nodes.size();
        if (screenX.length < n) {
            screenX = new float[n];
            screenY = new float[n];
        }
        float inset = nodeRadius + HOVER_RADIUS_BONUS + EDGE_INSET;
        float areaX = bounds.x() + inset;
        float areaY = bounds.y() + inset;
        float areaW = Math.max(0.0f, bounds.width() - inset * 2.0f);
        float areaH = Math.max(0.0f, bounds.height() - inset * 2.0f);
        for (int i = 0; i < n; i++) {
            Node node = nodes.get(i);
            screenX[i] = areaX + node.x() * areaW;
            screenY[i] = areaY + node.y() * areaH;
        }
        keyVersion = graphVersion;
        keyX = bounds.x();
        keyY = bounds.y();
        keyW = bounds.width();
        keyH = bounds.height();
        keyRadius = nodeRadius;
        layoutValid = true;
    }

    // ------------------------------------------------------------------ отрисовка

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE) return;
        RectView bounds = layoutBounds();
        if (bounds.width() <= 0.0f || bounds.height() <= 0.0f) return;
        DrawScope draw = new DrawScope(context, transform(), bounds);
        if (renderCustomVisual(draw)) return;
        resolveStyle();
        PlotSupport.drawSurface(draw, bounds,
                backgroundVisible ? background : null,
                borderVisible ? border : null,
                borderWidth, surfaceRadius);
        prepareLayout();
        if (nodes.isEmpty()) return;

        renderEdges(draw, false);
        if (hoveredNodeIndex >= 0) renderEdges(draw, true);
        for (int i = 0; i < nodes.size(); i++) {
            renderNode(draw, i);
        }
        if (nodeLabelsVisible && nodeLabelPlacement != NodeLabelPlacement.NONE) {
            for (int i = 0; i < nodes.size(); i++) renderNodeLabel(draw, i);
        }
        if (hoveredNodeIndex >= 0 && hoveredNodeIndex < nodes.size()) {
            renderNodeTooltip(draw, hoveredNodeIndex);
        }
    }

    /**
     * Рисует рёбра одного вида: обычные или связанные с наведённым узлом.
     *
     * <p>Геометрия ребра от узла {@code A} к узлу {@code B} (радиусы {@code rA}, {@code rB}):
     * <pre>
     *   d     = B - A,   L = |d|,   u = d / L
     *   start = A + u * (rA + EDGE_GAP)
     *   end   = B - u * (rB + EDGE_GAP)
     *   arrow = треугольник: вершина end, основание на end - u * arrowSize, полуширина arrowSize * 0.45
     * </pre>
     * Линия обрывается у основания стрелки, чтобы толстая линия не выступала за остриё. Ребро, чьи узлы
     * соприкасаются ({@code L <= rA + rB + 2 * EDGE_GAP}), не рисуется.
     *
     * @param draw      область рисования
     * @param highlight {@code true} — только рёбра наведённого узла, цветом подсветки; {@code false} — остальные
     */
    private void renderEdges(DrawScope draw, boolean highlight) {
        for (int e = 0; e < edges.size(); e++) {
            int from = edgeFrom[e];
            int to = edgeTo[e];
            if (from < 0 || to < 0) continue;
            boolean attached = from == hoveredNodeIndex || to == hoveredNodeIndex;
            if (attached != highlight) continue;
            ColorView color = highlight ? resolvedHoverEdgeColor : resolvedEdgeColor;
            if (from == to) {
                float cx = screenX[from];
                float cy = screenY[from] - nodeRadius * 1.7f;
                draw.addCircle(cx, cy, nodeRadius * 0.9f, color, 20, edgeWidth);
                continue;
            }
            float dx = screenX[to] - screenX[from];
            float dy = screenY[to] - screenY[from];
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            float startTrim = nodeRadius + EDGE_GAP;
            float endTrim = nodeRadius + EDGE_GAP;
            if (length <= startTrim + endTrim) continue;
            float ux = dx / length;
            float uy = dy / length;
            float startX = screenX[from] + ux * startTrim;
            float startY = screenY[from] + uy * startTrim;
            float endX = screenX[to] - ux * endTrim;
            float endY = screenY[to] - uy * endTrim;
            boolean arrow = directed && arrowSize > 0.0f && length > startTrim + endTrim + arrowSize;
            float lineEndX = arrow ? endX - ux * arrowSize * 0.5f : endX;
            float lineEndY = arrow ? endY - uy * arrowSize * 0.5f : endY;
            draw.addLine(startX, startY, lineEndX, lineEndY, color, edgeWidth);
            if (arrow) {
                float baseX = endX - ux * arrowSize;
                float baseY = endY - uy * arrowSize;
                float half = arrowSize * ARROW_HALF_WIDTH;
                // Перпендикуляр к направлению ребра: (-uy, ux).
                draw.addTriangleFilled(new DrawPoint(endX, endY),
                        new DrawPoint(baseX - uy * half, baseY + ux * half),
                        new DrawPoint(baseX + uy * half, baseY - ux * half), color);
            }
        }
    }

    private void renderNode(DrawScope draw, int index) {
        boolean hovered = index == hoveredNodeIndex;
        if (nodeRenderer != null) {
            nodeRenderer.render(draw, nodePoint(index, hovered));
            return;
        }
        float radius = hovered ? nodeRadius + HOVER_RADIUS_BONUS : nodeRadius;
        float x = screenX[index];
        float y = screenY[index];
        draw.addCircleFilled(x, y, radius, hovered ? resolvedHoverNodeColor : resolvedNodeColor, 20);
        draw.addCircle(x, y, radius, resolvedNodeBorder, 20, 1.0f);
    }

    private void renderNodeLabel(DrawScope draw, int index) {
        if (nodeLabelRenderer != null) {
            nodeLabelRenderer.render(draw, nodePoint(index, index == hoveredNodeIndex));
            return;
        }
        RichText text = labelText(index);
        if (text == null) return;
        RenderContext context = draw.context();
        float width = PlotSupport.textWidth(context, text);
        float height = PlotSupport.textHeight(context, text);
        RectView bounds = layoutBounds();
        float x = screenX[index];
        float y = screenY[index];
        float gap = nodeRadius + LABEL_GAP;
        float left = bounds.x() + 1.0f;
        float right = bounds.x() + bounds.width() - 1.0f;
        float tx;
        float ty;
        switch (nodeLabelPlacement) {
            case CENTER -> {
                tx = x - width * 0.5f;
                ty = y - height * 0.5f;
            }
            case ABOVE -> {
                tx = x - width * 0.5f;
                ty = y - gap - height;
            }
            case BELOW -> {
                tx = x - width * 0.5f;
                ty = y + gap;
            }
            default -> {
                tx = x + gap;
                ty = y - height * 0.5f;
                // Справа не хватает места — переносим подпись влево от узла вместо обрезания краем.
                if (tx + width > right) tx = x - gap - width;
            }
        }
        tx = PlotSupport.clamp(tx, left, right - width);
        ty = PlotSupport.clamp(ty, bounds.y() + 1.0f, bounds.y() + bounds.height() - height - 1.0f);
        PlotSupport.drawText(draw, text, tx, ty, width, height, textColor, Alignment.CENTER, Alignment.CENTER);
    }

    /**
     * Возвращает кэшированный текст подписи узла.
     *
     * <p>Поставщик подписи вызывается каждый кадр, но {@link RichText} пересоздаётся только если строка изменилась
     * или изменился размер шрифта.</p>
     *
     * @param index индекс узла
     * @return текст подписи или {@code null}, если подпись пуста
     */
    private RichText labelText(int index) {
        int n = nodes.size();
        if (labelStrings.length < n || labelSize != textSize) {
            labelStrings = new String[n];
            labelTexts = new RichText[n];
            labelSize = textSize;
        }
        String label = nodeLabelProvider.apply(nodePoint(index, index == hoveredNodeIndex));
        if (label == null || label.isEmpty()) return null;
        if (!label.equals(labelStrings[index])) {
            labelStrings[index] = label;
            labelTexts[index] = PlotSupport.text(label, textSize);
        }
        return labelTexts[index];
    }

    private void renderNodeTooltip(DrawScope draw, int index) {
        NodePoint point = nodePoint(index, true);
        if (nodeTooltipRenderer != null) {
            nodeTooltipRenderer.render(draw, point);
            return;
        }
        String text = nodeTooltipProvider.apply(point);
        if (text == null || text.isEmpty()) return;
        PlotSupport.drawTooltip(draw, PlotSupport.text(text, textSize), screenX[index], screenY[index],
                nodeRadius + HOVER_RADIUS_BONUS + TOOLTIP_GAP, layoutBounds(),
                resolvedTooltipBackground, resolvedTooltipBorder, resolvedTooltipText);
    }

    // ------------------------------------------------------------------ события

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
            if (hoveredNodeIndex != -1) {
                hoveredNodeIndex = -1;
                invalidate(InvalidationFlags.VISUAL);
            }
        } else if (event instanceof PointerPressedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
            int hit = hitNode(pointer.rootX(), pointer.rootY());
            if (hit >= 0) {
                clickNode(nodePoint(hit, true));
                event.cancel();
            }
        }
    }

    /**
     * Находит ближайший к указателю узел в радиусе попадания.
     *
     * <p>Радиус попадания: {@code max(nodeRadius + 2, MIN_HIT_RADIUS)}. Если в радиус попадает несколько узлов,
     * выбирается ближайший к указателю, а не первый добавленный.</p>
     *
     * @param rootX X указателя
     * @param rootY Y указателя
     * @return индекс узла или {@code -1}
     */
    int hitNode(float rootX, float rootY) {
        if (!layoutValid) return -1;
        float radius = Math.max(nodeRadius + 2.0f, MIN_HIT_RADIUS);
        float best = radius * radius;
        int found = -1;
        for (int i = 0; i < nodes.size(); i++) {
            float dx = rootX - screenX[i];
            float dy = rootY - screenY[i];
            float distance = dx * dx + dy * dy;
            if (distance <= best) {
                best = distance;
                found = i;
            }
        }
        return found;
    }

    private NodePoint nodePoint(int index, boolean hovered) {
        Node node = nodes.get(index);
        return new NodePoint(index, node.id(), node.x(), node.y(), screenX[index], screenY[index], hovered);
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
    public record NodePoint(int index, String id, float normalizedX, float normalizedY, float x, float y,
                            boolean hovered) {
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
