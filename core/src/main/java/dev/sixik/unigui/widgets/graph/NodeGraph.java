package dev.sixik.unigui.widgets.graph;


import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.docs.*;
import dev.sixik.unigui.api.event.*;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.input.HitTestCoordinateMapper;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.*;
import dev.sixik.unigui.api.render.*;
import dev.sixik.unigui.api.viewport.Viewport2D;
import dev.sixik.unigui.api.widget.RenderedBoundsMapper;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.xaml.XamlProperty;
import dev.sixik.unigui.xaml.XamlWidget;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.graph.node.*;
import dev.sixik.unigui.widgets.graph.runtime.GraphExecutor;
import dev.sixik.unigui.widgets.graph.runtime.GraphIssue;
import dev.sixik.unigui.widgets.graph.runtime.GraphModel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.*;
import java.util.function.LongSupplier;

@AiDoc(summary = "Interactive 2D node graph canvas supporting movable nodes, pin connections, pan, zoom, grid snapping, and selection.")
@WikiPage(
        title = "NodeGraph",
        slug = "node-graph",
        category = "<key:graph.widget.category>",
        summary = "<key:node.graph.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:node.graph.example.basic.title>",
        code = "NodeGraph graph = new NodeGraph();\nNodeGraphItem itemA = graph.addItem(\"nodeA\", new Label(\"A\"), 50, 50);\nitemA.addPort(\"out\", NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 10);\nNodeGraphItem itemB = graph.addItem(\"nodeB\", new Label(\"B\"), 200, 50);\nitemB.addPort(\"in\", NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 10);\ngraph.addConnection(\"c1\", \"nodeA\", \"out\", \"nodeB\", \"in\");"
)
@WikiLink(
        label = "GraphView",
        target = "page:graph-view",
        description = "<key:graph.view.summary>"
)
@WikiLink(
        label = "NodeGraphItem",
        target = "page:node-graph-item",
        description = "<key:node.graph.item.summary>"
)
@WikiLink(
        label = "NodeGraphConnection",
        target = "page:node-graph-connection",
        description = "<key:node.graph.connection.summary>"
)
@XamlWidget("NodeGraph")
public class NodeGraph extends WidgetBase implements HitTestCoordinateMapper, RenderedBoundsMapper {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.NODE_GRAPH;

    public static final float DEFAULT_PREFERRED_WIDTH = 320.0f;
    public static final float DEFAULT_PREFERRED_HEIGHT = 200.0f;

    private static final float MIN_ITEM_SIZE = 1.0f;
    private static final float PORT_RADIUS = 5.0f;
    private static final float PORT_HIT_RADIUS = 7.0f;
    private static final float CONNECTION_HIT_RADIUS = 6.0f;
    private static final float RESIZE_HANDLE_SIZE = 10.0f;
    private static final float MIN_RESIZE_SIZE = 12.0f;
    private static final float DEFAULT_ITEM_CONTENT_PADDING = 0.0f;

    private final ObjectArrayList<NodeGraphItem> items = new ObjectArrayList<>();
    private final ObjectArrayList<NodeGraphItem> logicalItems = new ObjectArrayList<>();
    private final ObjectArrayList<NodeGraphConnection> connections = new ObjectArrayList<>();
    private final List<NodeGraphItem> itemsView = Collections.unmodifiableList(items);
    private final List<NodeGraphConnection> connectionsView = Collections.unmodifiableList(connections);
    private NodeGraphItem[] itemSnapshot = new NodeGraphItem[0];
    private NodeGraphItem[] logicalItemSnapshot = new NodeGraphItem[0];
    private NodeGraphConnection[] connectionSnapshot = new NodeGraphConnection[0];
    private boolean itemSnapshotDirty = true;
    private boolean logicalItemSnapshotDirty = true;
    private boolean connectionSnapshotDirty = true;
    private List<Widget> childrenView = Collections.emptyList();
    private boolean childrenViewDirty = true;
    private final MutableColor backgroundColor = new MutableColor(0.055f, 0.065f, 0.082f, 1.0f);
    private final MutableColor gridColor = new MutableColor(0.20f, 0.25f, 0.33f, 0.45f);
    private final MutableColor majorGridColor = new MutableColor(0.27f, 0.35f, 0.47f, 0.55f);
    private final MutableColor itemBorderColor = new MutableColor(0.28f, 0.36f, 0.48f, 0.78f);
    private final MutableColor hoveredItemBorderColor = new MutableColor(0.50f, 0.64f, 0.84f, 0.95f);
    private final MutableColor selectedItemBorderColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private final MutableColor portColor = new MutableColor(0.42f, 0.55f, 0.72f, 1.0f);
    private final MutableColor hoveredPortColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private final MutableColor connectionColor = new MutableColor(0.56f, 0.70f, 0.88f, 0.78f);
    private final MutableColor selectedConnectionColor = new MutableColor(1.0f, 0.72f, 0.25f, 1.0f);
    private final MutableColor connectionPreviewColor = new MutableColor(0.25f, 0.78f, 1.0f, 0.92f);
    private final MutableColor invalidConnectionPreviewColor = new MutableColor(1.0f, 0.20f, 0.20f, 0.92f);
    private final MutableColor selectionBoxFillColor = new MutableColor(0.25f, 0.78f, 1.0f, 0.16f);
    private final MutableColor selectionBoxBorderColor = new MutableColor(0.25f, 0.78f, 1.0f, 0.80f);
    private final MutableColor resizeHandleColor = new MutableColor(0.25f, 0.78f, 1.0f, 0.95f);

    private final ObjectArrayList<NodeGraphRegion> regions = new ObjectArrayList<>();
    private final List<NodeGraphRegion> regionsView = Collections.unmodifiableList(regions);
    private final PortTypeRegistry portTypes = new PortTypeRegistry();
    private final NodeTypeRegistry nodeTypes = new NodeTypeRegistry();
    private final MutableColor errorBorderColor = new MutableColor(0.88f, 0.32f, 0.32f, 1.0f);
    private final MutableColor warningBorderColor = new MutableColor(0.92f, 0.66f, 0.20f, 1.0f);
    private final MutableColor shadowColor = new MutableColor(0.0f, 0.0f, 0.0f, 0.28f);
    private final MutableColor minimapBackgroundColor = new MutableColor(0.07f, 0.07f, 0.08f, 0.88f);
    private final MutableColor minimapBorderColor = new MutableColor(0.45f, 0.47f, 0.55f, 0.9f);
    private final MutableColor minimapViewportColor = new MutableColor(1.0f, 1.0f, 1.0f, 0.85f);
    private boolean minimapEnabled;
    private float minimapWidth = 160.0f;
    private float minimapHeight = 100.0f;
    private float minimapMargin = 12.0f;
    private int nextItemId = 1;
    private int nextRegionId = 1;
    private static final float COMMENT_ICON_ROOM = 22.0f;
    private static final float COMMENT_MIN_WIDTH = 160.0f;
    private static final float COMMENT_MAX_WIDTH = 320.0f;
    private static final float COMMENT_PADDING = 6.0f;
    private final NodeGraphHistory history = new NodeGraphHistory(this);
    private static final float DOUBLE_CLICK_DISTANCE = 6.0f;
    private int doubleClickMillis = 350;
    private LongSupplier clock = System::currentTimeMillis;
    private String lastWireClickId = "";
    private long lastWireClickMillis;
    private float lastWireClickX;
    private float lastWireClickY;
    private NodePalette palette;
    private boolean nodePaletteEnabled = true;
    private boolean nodeContextMenuEnabled = true;
    private boolean snapToGrid;
    private NodeGraphClipboard clipboard = NodeGraphClipboard.EMPTY;
    private float lastPointerRootX = Float.NaN;
    private float lastPointerRootY = Float.NaN;
    private final ItemLayout itemLayout = new ItemLayout();
    private final ItemPaint itemPaint = new ItemPaint();
    private final float[] wirePoints = new float[NodeGraphWires.BUFFER_SIZE];
    private int wireCount;
    private NodeGraphConnectionPolicy connectionPolicy = NodeGraphConnectionPolicy.DEFAULT;
    private NodeGraphSelectionMode selectionMode = NodeGraphSelectionMode.SINGLE;
    private final Viewport2D viewport = new Viewport2D();
    private float gridSize = 24.0f;
    private boolean clippingEnabled = true;
    private boolean itemDraggingEnabled = true;
    private boolean panningEnabled = true;
    private boolean zoomEnabled = true;
    private boolean scaleContentWithZoom = true;
    private boolean lassoSelectionEnabled = true;
    private boolean resizeEnabled = true;
    private boolean keyboardEditingEnabled = true;
    private boolean consumeWheelWhileHovered = true;
    private boolean wheelPanningEnabled = true;
    private float wheelPanStep = 32.0f;
    private float itemContentPadding = DEFAULT_ITEM_CONTENT_PADDING;
    private boolean bringToFrontOnSelect = true;
    private String hoveredItemId = "";
    private NodeGraphRegion hoveredRegion;
    private NodeGraphPortRef hoveredPort = new NodeGraphPortRef("", "");
    private String hoveredConnectionId = "";
    private int nextConnectionId = 1;
    private float preferredWidth = DEFAULT_PREFERRED_WIDTH;
    private float preferredHeight = DEFAULT_PREFERRED_HEIGHT;

    private DragState dragState;
    private ConnectionDragState connectionDragState;
    private LassoState lassoState;

    @AiDoc(summary = "Constructs a new NodeGraph instance with default viewport, grid, and themes.")
    @WikiMethod(summary = "<key:node.graph.method.ctorDefault>")
    public NodeGraph() {
        backgroundColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        gridColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        majorGridColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        itemBorderColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        hoveredItemBorderColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        selectedItemBorderColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        portColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        hoveredPortColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        connectionColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        selectedConnectionColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        connectionPreviewColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        invalidConnectionPreviewColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        selectionBoxFillColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        selectionBoxBorderColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        resizeHandleColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        focusable(true);
        nodeTypes.register(RerouteModule.dataType()).register(RerouteModule.flowType());
    }

    @AiDoc(summary = "Creates and places a new node item with the given ID, content widget, and canvas coordinates.")
    @WikiMethod(summary = "<key:node.graph.method.addItemFull>")
    @WikiReturn("<key:node.graph.return.createdItem>")
    public NodeGraphItem addItem(@WikiParameter(name = "id", description = "<key:node.graph.param.itemId>") String id, @WikiParameter(name = "content", description = "<key:node.graph.param.content>") Widget content, @WikiParameter(name = "x", description = "<key:node.graph.param.x>") float x, @WikiParameter(name = "y", description = "<key:node.graph.param.y>") float y) {
        NodeGraphItem item = new NodeGraphItem(id, content, x, y);
        addItem(item);
        return item;
    }

    @AiDoc(summary = "Adds an existing node item to the graph.")
    @WikiMethod(summary = "<key:node.graph.method.addItem>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph addItem(@WikiParameter(name = "item", description = "<key:node.graph.param.item>") NodeGraphItem item) {
        if (item == null || items.contains(item)) return this;
        if (item.owner() != null && item.owner() != this) {
            throw new IllegalArgumentException("NodeGraphItem already belongs to another NodeGraph");
        }
        items.add(item);
        logicalItems.add(item);
        markItemsDirty();
        markLogicalItemsDirty();
        attach(item);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        history.record(NodeGraphHistory.Operation.of(() -> removeItem(item), () -> addItem(item)));
        return this;
    }

    @AiDoc(summary = "Removes the specified node item and all its connected lines.")
    @WikiMethod(summary = "<key:node.graph.method.removeItem>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph removeItem(@WikiParameter(name = "item", description = "<key:node.graph.param.item>") NodeGraphItem item) {
        if (item == null || !items.remove(item)) return this;
        logicalItems.remove(item);
        markItemsDirty();
        markLogicalItemsDirty();
        removeConnectionsForItem(item.id());
        String removedId = item.id();
        detach(item);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        dispatch(new NodeGraphItemRemovedEvent(this, removedId));
        // Провода ноды записались раньше (removeConnectionsForItem), поэтому при отмене нода вернётся первой.
        history.record(NodeGraphHistory.Operation.of(() -> addItem(item), () -> removeItem(item)));
        return this;
    }

    @AiDoc(summary = "Removes the node item with the specified ID.")
    @WikiMethod(summary = "<key:node.graph.method.removeItemId>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph removeItem(@WikiParameter(name = "id", description = "<key:node.graph.param.itemId>") String id) {
        NodeGraphItem item = item(id);
        return item == null ? this : removeItem(item);
    }

    @AiDoc(summary = "Removes all items and connections from the graph.")
    @WikiMethod(summary = "<key:node.graph.method.clearItems>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph clearItems() {
        // Шаги истории держат ссылки на уничтожаемые ноды и провода, поэтому после очистки они бесполезны.
        history.clear();
        for (NodeGraphItem item : itemSnapshot()) {
            List<Widget> widgets = item.allWidgets();
            detach(item);
            for (Widget widget : widgets) {
                widget.dispose();
            }
        }
        items.clear();
        logicalItems.clear();
        markItemsDirty();
        markLogicalItemsDirty();
        for (NodeGraphConnection connection : connectionSnapshot()) {
            detach(connection);
        }
        connections.clear();
        markConnectionsDirty();
        hoveredItemId = "";
        hoveredPort = new NodeGraphPortRef("", "");
        hoveredConnectionId = "";
        dragState = null;
        connectionDragState = null;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns inner padding around item content widgets.")
    @WikiMethod(summary = "<key:node.graph.method.itemContentPaddingGet>")
    @WikiReturn("<key:node.graph.return.padding>")
    public float itemContentPadding() {
        return itemContentPadding;
    }

    @XamlProperty(value = "itemContentPadding", category = "Layout", defaultValue = "0", description = "Padding applied around item content during node layout.")
    @AiDoc(summary = "Sets inner padding around item content widgets.")
    @WikiMethod(summary = "<key:node.graph.method.itemContentPaddingSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph itemContentPadding(@WikiParameter(name = "padding", description = "<key:node.graph.param.padding>") float padding) {
        float normalized = Float.isFinite(padding) ? Math.max(0.0f, padding) : DEFAULT_ITEM_CONTENT_PADDING;
        if (itemContentPadding == normalized) return this;
        itemContentPadding = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns an unmodifiable list of all node items.")
    @WikiMethod(summary = "<key:node.graph.method.items>")
    @WikiReturn("<key:node.graph.return.items>")
    public List<NodeGraphItem> items() {
        return itemsView;
    }

    @AiDoc(summary = "Returns the item with the given ID, or null if not found.")
    @WikiMethod(summary = "<key:node.graph.method.item>")
    @WikiReturn("<key:node.graph.return.item>")
    public NodeGraphItem item(@WikiParameter(name = "id", description = "<key:node.graph.param.itemId>") String id) {
        if (id == null) return null;
        Object[] rawItems = items.elements();
        for (int itemIndex = 0, itemSize = items.size(); itemIndex < itemSize; itemIndex++) {
            NodeGraphItem item = (NodeGraphItem) rawItems[itemIndex];
            if (item.id().equals(id)) return item;
        }
        return null;
    }

    @AiDoc(summary = "Resolves a port reference to its corresponding port object.")
    @WikiMethod(summary = "<key:node.graph.method.port>")
    @WikiReturn("<key:node.graph.return.port>")
    public NodeGraphPort port(@WikiParameter(name = "ref", description = "<key:node.graph.param.portRef>") NodeGraphPortRef ref) {
        if (ref == null || ref.empty()) return null;
        NodeGraphItem item = item(ref.itemId());
        return item == null ? null : item.port(ref.portId());
    }

    @AiDoc(summary = "Creates and adds a connection between specified item and port IDs.")
    @WikiMethod(summary = "<key:node.graph.method.addConnectionIds>")
    @WikiReturn("<key:node.graph.return.createdConnection>")
    public NodeGraphConnection addConnection(@WikiParameter(name = "id", description = "<key:node.graph.param.connectionId>") String id,
                                             @WikiParameter(name = "fromItemId", description = "<key:node.graph.param.fromItemId>") String fromItemId, @WikiParameter(name = "fromPortId", description = "<key:node.graph.param.fromPortId>") String fromPortId,
                                             @WikiParameter(name = "toItemId", description = "<key:node.graph.param.toItemId>") String toItemId, @WikiParameter(name = "toPortId", description = "<key:node.graph.param.toPortId>") String toPortId) {
        return addConnection(new NodeGraphConnection(
                normalizeConnectionId(id),
                new NodeGraphPortRef(fromItemId, fromPortId),
                new NodeGraphPortRef(toItemId, toPortId)));
    }

    @AiDoc(summary = "Creates and adds a connection between port references.")
    @WikiMethod(summary = "<key:node.graph.method.addConnectionRefs>")
    @WikiReturn("<key:node.graph.return.createdConnection>")
    public NodeGraphConnection addConnection(@WikiParameter(name = "id", description = "<key:node.graph.param.connectionId>") String id, @WikiParameter(name = "from", description = "<key:node.graph.param.fromRef>") NodeGraphPortRef from, @WikiParameter(name = "to", description = "<key:node.graph.param.toRef>") NodeGraphPortRef to) {
        return addConnection(new NodeGraphConnection(normalizeConnectionId(id), from, to));
    }

    @AiDoc(summary = "Adds an existing connection instance to the graph.")
    @WikiMethod(summary = "<key:node.graph.method.addConnectionInstance>")
    @WikiReturn("<key:node.graph.return.createdConnection>")
    public NodeGraphConnection addConnection(@WikiParameter(name = "connection", description = "<key:node.graph.param.connection>") NodeGraphConnection connection) {
        if (connection == null) return null;
        if (connection.owner() != null && connection.owner() != this) {
            throw new IllegalArgumentException("NodeGraphConnection already belongs to another NodeGraph");
        }
        if (connection(connection.id()) != null) {
            throw new IllegalArgumentException("Duplicate NodeGraph connection id: " + connection.id());
        }
        NodeGraphConnectionValidation validation = validateConnection(connection.from(), connection.to());
        if (!validation.valid()) {
            throw new IllegalArgumentException(validation.reason());
        }
        enforceCapacity(connection.from());
        enforceCapacity(connection.to());
        connections.add(connection);
        markConnectionsDirty();
        attach(connection);
        invalidate(InvalidationFlags.VISUAL);
        dispatch(new NodeGraphConnectionCreatedEvent(this, connection.id(), connection.from(), connection.to(), connection.type()));
        history.record(NodeGraphHistory.Operation.of(() -> removeConnection(connection), () -> addConnection(connection)));
        return connection;
    }

    @AiDoc(summary = "Removes the connection with the given ID.")
    @WikiMethod(summary = "<key:node.graph.method.removeConnectionId>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph removeConnection(@WikiParameter(name = "id", description = "<key:node.graph.param.connectionId>") String id) {
        NodeGraphConnection connection = connection(id);
        return connection == null ? this : removeConnection(connection);
    }

    @AiDoc(summary = "Removes the specified connection instance.")
    @WikiMethod(summary = "<key:node.graph.method.removeConnection>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph removeConnection(@WikiParameter(name = "connection", description = "<key:node.graph.param.connection>") NodeGraphConnection connection) {
        if (connection == null || !connections.remove(connection)) return this;
        markConnectionsDirty();
        detach(connection);
        invalidate(InvalidationFlags.VISUAL);
        dispatch(new NodeGraphConnectionRemovedEvent(this, connection.id(), connection.from(), connection.to()));
        history.record(NodeGraphHistory.Operation.of(() -> addConnection(connection), () -> removeConnection(connection)));
        return this;
    }

    @AiDoc(summary = "Removes all currently selected connections.")
    @WikiMethod(summary = "<key:node.graph.method.removeSelectedConnections>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph removeSelectedConnections() {
        for (NodeGraphConnection connection : connectionSnapshot()) {
            if (connection.selected()) {
                removeConnection(connection);
            }
        }
        return this;
    }

    @AiDoc(summary = "Returns the connection with the given ID, or null if not found.")
    @WikiMethod(summary = "<key:node.graph.method.connection>")
    @WikiReturn("<key:node.graph.return.connection>")
    public NodeGraphConnection connection(@WikiParameter(name = "id", description = "<key:node.graph.param.connectionId>") String id) {
        if (id == null) return null;
        Object[] rawConnections = connections.elements();
        for (int connectionIndex = 0, connectionSize = connections.size(); connectionIndex < connectionSize; connectionIndex++) {
            NodeGraphConnection connection = (NodeGraphConnection) rawConnections[connectionIndex];
            if (connection.id().equals(id)) return connection;
        }
        return null;
    }

    @AiDoc(summary = "Returns an unmodifiable list of all connections.")
    @WikiMethod(summary = "<key:node.graph.method.connections>")
    @WikiReturn("<key:node.graph.return.connections>")
    public List<NodeGraphConnection> connections() {
        return connectionsView;
    }

    @AiDoc(summary = "Returns true if a connection already exists between the given ports.")
    @WikiMethod(summary = "<key:node.graph.method.hasConnection>")
    @WikiReturn("<key:node.graph.return.hasConnection>")
    public boolean hasConnection(@WikiParameter(name = "from", description = "<key:node.graph.param.fromRef>") NodeGraphPortRef from, @WikiParameter(name = "to", description = "<key:node.graph.param.toRef>") NodeGraphPortRef to) {
        Object[] rawConnections = connections.elements();
        for (int connectionIndex = 0, connectionSize = connections.size(); connectionIndex < connectionSize; connectionIndex++) {
            NodeGraphConnection connection = (NodeGraphConnection) rawConnections[connectionIndex];
            if (connection.from().equals(from) && connection.to().equals(to)) {
                return true;
            }
        }
        return false;
    }

    @AiDoc(summary = "Validates whether a connection can be established according to the active connection policy.")
    @WikiMethod(summary = "<key:node.graph.method.validateConnection>")
    @WikiReturn("<key:node.graph.return.validation>")
    public NodeGraphConnectionValidation validateConnection(@WikiParameter(name = "from", description = "<key:node.graph.param.fromRef>") NodeGraphPortRef from, @WikiParameter(name = "to", description = "<key:node.graph.param.toRef>") NodeGraphPortRef to) {
        NodeGraphConnectionPolicy policy = connectionPolicy == null ? NodeGraphConnectionPolicy.DEFAULT : connectionPolicy;
        NodeGraphConnectionValidation validation = policy.validate(this, from, to);
        return validation == null ? NodeGraphConnectionValidation.invalid("Connection rejected") : validation;
    }

    /**
     * @return максимальный интервал двойного клика по проводу в миллисекундах; {@code 0} — выключено
     */
    public int doubleClickMillis() {
        return doubleClickMillis;
    }

    /**
     * Двойной клик по проводу вставляет в него reroute. Граф сам измеряет интервал: события указателя не несут
     * счётчика кликов.
     *
     * @param millis интервал между двумя нажатиями; {@code 0} выключает вставку по двойному клику
     * @return этот граф
     */
    public NodeGraph doubleClickMillis(int millis) {
        this.doubleClickMillis = Math.max(0, millis);
        return this;
    }

    /**
     * Подменяет часы двойного клика; нужен тестам, чтобы не зависеть от реального времени.
     */
    NodeGraph clock(LongSupplier clock) {
        this.clock = clock == null ? System::currentTimeMillis : clock;
        return this;
    }

    /**
     * @return {@code true}, если ПКМ, пробел и отпускание провода в пустом месте открывают палитру нод
     */
    public boolean nodePaletteEnabled() {
        return nodePaletteEnabled;
    }

    /**
     * Включает или выключает палитру нод: ПКМ по пустому месту и Space открывают её, провод, отпущенный в пустом
     * месте, открывает её с типами, совместимыми с этим проводом. Палитра работает только внутри
     * {@code OverlayLayer}.
     *
     * @param nodePaletteEnabled {@code true}, чтобы включить
     * @return этот граф
     */
    public NodeGraph nodePaletteEnabled(boolean nodePaletteEnabled) {
        this.nodePaletteEnabled = nodePaletteEnabled;
        if (!nodePaletteEnabled && palette != null) palette.close();
        return this;
    }

    /**
     * @return палитра нод этого графа; создаётся при первом обращении и ищет по {@link #nodeTypes()}
     */
    public NodePalette nodePalette() {
        if (palette == null) palette = new NodePalette(nodeTypes);
        return palette;
    }

    /**
     * Открывает палитру нод в точке. Выбранная нода создаётся под этой точкой и выделяется.
     *
     * @param rootX X в root-координатах
     * @param rootY Y в root-координатах
     * @return {@code true}, если палитра открыта; {@code false}, если палитра выключена или граф не лежит в
     * {@code OverlayLayer}
     */
    public boolean openNodePalette(float rootX, float rootY) {
        OverlayLayer layer = nodePaletteHost();
        if (layer == null) return false;
        float worldX = viewport.screenToWorldX(rootX - layoutBounds().x());
        float worldY = viewport.screenToWorldY(rootY - layoutBounds().y());
        NodePalette opened = nodePalette();
        opened.expandAll(false).filter(null).onPick(type -> history.transact("Add node", () -> {
            NodeGraphItem created = spawn(type.id(), worldX, worldY);
            if (created != null) selectOnly(created);
        }));
        showPalette(layer, opened, rootX, rootY);
        return true;
    }

    /**
     * Открывает палитру с типами, у которых есть вход, совместимый с выходом {@code from}. Выбранная нода создаётся в
     * точке и подключается к этому выходу.
     *
     * @param from  выход, из которого тянули провод
     * @param rootX X в root-координатах
     * @param rootY Y в root-координатах
     * @return {@code true}, если палитра открыта
     */
    public boolean openNodePaletteFromPort(NodeGraphPortRef from, float rootX, float rootY) {
        NodeGraphPort source = port(from);
        OverlayLayer layer = nodePaletteHost();
        if (source == null || layer == null) return false;
        float worldX = viewport.screenToWorldX(rootX - layoutBounds().x());
        float worldY = viewport.screenToWorldY(rootY - layoutBounds().y());
        String sourceType = source.type();
        NodePalette opened = nodePalette();
        // Отфильтрованный список короткий: группы сразу развёрнуты, чтобы не прятать подходящие ноды.
        opened.expandAll(true).filter(type -> type.acceptsConnectionFrom(sourceType, portTypes)).onPick(type ->
                history.transact("Add node", () -> {
                    NodeGraphItem created = spawn(type.id(), worldX, worldY);
                    if (created == null) return;
                    String inputId = type.firstCompatibleInput(sourceType, portTypes);
                    if (inputId != null) {
                        try {
                            addConnection("", from, new NodeGraphPortRef(created.id(), inputId));
                        } catch (IllegalArgumentException rejected) {
                            // Политика соединений может отклонить пару; нода всё равно остаётся созданной.
                        }
                    }
                    selectOnly(created);
                }));
        showPalette(layer, opened, rootX, rootY);
        return true;
    }

    /**
     * Открывает палитру там, где последний раз был курсор над графом, а если его не было — в центре графа.
     */
    protected boolean openNodePaletteAtPointer() {
        float rootX = Float.isNaN(lastPointerRootX) ? layoutBounds().x() + layoutBounds().width() * 0.5f : lastPointerRootX;
        float rootY = Float.isNaN(lastPointerRootY) ? layoutBounds().y() + layoutBounds().height() * 0.5f : lastPointerRootY;
        return openNodePalette(rootX, rootY);
    }

    private void showPalette(OverlayLayer layer, NodePalette opened, float rootX, float rootY) {
        layer.applyQueuedMutations();
        if (opened.parent() != layer) layer.addOverlay(opened);
        opened.openAt(rootX, rootY);
        invalidate(InvalidationFlags.VISUAL);
    }

    private OverlayLayer nodePaletteHost() {
        return nodePaletteEnabled ? overlayHost() : null;
    }

    protected OverlayLayer overlayHost() {
        for (Widget current = parent(); current != null; current = current.parent()) {
            if (current instanceof OverlayLayer layer) return layer;
        }
        return null;
    }

    /**
     * Оставляет выделенной только одну ноду.
     */
    protected void selectOnly(NodeGraphItem item) {
        clearSelection();
        clearConnectionSelection();
        selectRegion(null);
        selectItem(item);
    }

    /**
     * Снимает неизменяемую модель графа без виджетов для проверки и исполнения.
     *
     * @return модель на момент вызова
     */
    public GraphModel model() {
        return GraphModel.from(this);
    }

    /**
     * Проверяет граф и помечает ноды с проблемами (красная или янтарная рамка).
     *
     * @param executor исполнитель, чьи поведения определяют чистые ноды и известные типы
     * @return найденные проблемы
     */
    public List<GraphIssue> validate(GraphExecutor executor) {
        List<GraphIssue> issues = executor.validate(model());
        showIssues(issues);
        return issues;
    }

    /**
     * Показывает проблемы на нодах, заменяя прежние пометки.
     *
     * @param issues проблемы; ноды без проблем теряют пометку
     * @return этот граф
     */
    public NodeGraph showIssues(List<GraphIssue> issues) {
        Map<String, List<GraphIssue>> byNode = new HashMap<>();
        for (GraphIssue issue : issues) {
            byNode.computeIfAbsent(issue.itemId(), id -> new ArrayList<>()).add(issue);
        }
        for (NodeGraphItem item : items) {
            List<GraphIssue> own = byNode.get(item.id());
            if (own == null) {
                item.issues(0, null);
                continue;
            }
            boolean error = own.stream().anyMatch(issue -> issue.severity() == GraphIssue.Severity.ERROR);
            item.issues(error ? 2 : 1, own.stream().map(GraphIssue::message).toList());
        }
        return this;
    }

    /**
     * Снимает все пометки проблем с нод.
     *
     * @return этот граф
     */
    public NodeGraph clearIssues() {
        return showIssues(List.of());
    }

    /**
     * @return история изменений для undo/redo
     */
    public NodeGraphHistory history() {
        return history;
    }

    /**
     * Отменяет последний записанный шаг.
     *
     * @return {@code true}, если шаг был отменён
     */
    public boolean undo() {
        return history.undo();
    }

    /**
     * Повторяет последний отменённый шаг.
     *
     * @return {@code true}, если шаг был повторён
     */
    public boolean redo() {
        return history.redo();
    }

    /**
     * Выполняет изменения как один шаг истории.
     *
     * @param name   название шага
     * @param action изменения графа
     * @return этот граф
     */
    public NodeGraph transact(String name, Runnable action) {
        history.transact(name, action);
        return this;
    }

    /**
     * @return {@code true}, если ноды и регионы притягиваются к сетке при перетаскивании
     */
    public boolean snapToGrid() {
        return snapToGrid;
    }

    /**
     * Включает притягивание нод и регионов к сетке {@link #gridSize()} при перетаскивании мышью.
     *
     * <p>По умолчанию привязки нет, и она включается, пока зажат Ctrl. Если включить её здесь, Ctrl работает
     * наоборот: привязка действует всегда, а при зажатом Ctrl временно отключается.</p>
     *
     * @param snapToGrid {@code true}, чтобы привязка действовала всегда
     * @return этот граф
     */
    public NodeGraph snapToGrid(boolean snapToGrid) {
        this.snapToGrid = snapToGrid;
        return this;
    }

    /**
     * Копирует выделенные ноды зарегистрированных типов и провода между ними в буфер графа.
     *
     * @return буфер; пустой, если копировать нечего
     */
    public NodeGraphClipboard copySelection() {
        NodeGraphClipboard copied = NodeGraphEditing.copy(this);
        if (!copied.isEmpty()) clipboard = copied;
        return copied;
    }

    /**
     * @return буфер, заполненный последним {@link #copySelection()}
     */
    public NodeGraphClipboard clipboard() {
        return clipboard;
    }

    /**
     * Вставляет буфер графа левым верхним углом группы в точку.
     *
     * @param x X в world-координатах
     * @param y Y в world-координатах
     * @return вставленные ноды, они же выделены
     */
    public List<NodeGraphItem> paste(float x, float y) {
        return NodeGraphEditing.paste(this, clipboard, x, y);
    }

    /**
     * Вставляет произвольный буфер, например скопированный в другом графе.
     *
     * @param source буфер
     * @param x      X в world-координатах
     * @param y      Y в world-координатах
     * @return вставленные ноды, они же выделены
     */
    public List<NodeGraphItem> paste(NodeGraphClipboard source, float x, float y) {
        return NodeGraphEditing.paste(this, source, x, y);
    }

    /**
     * Вставляет буфер графа под курсором; если курсор ещё не двигался над графом, со сдвигом от оригинала.
     *
     * @return вставленные ноды
     */
    public List<NodeGraphItem> pasteAtPointer() {
        if (clipboard.isEmpty()) return new ObjectArrayList<>();
        if (Float.isNaN(lastPointerRootX)) {
            return paste(clipboard.minX() + 24.0f, clipboard.minY() + 24.0f);
        }
        return paste(viewport.screenToWorldX(lastPointerRootX - layoutBounds().x()),
                viewport.screenToWorldY(lastPointerRootY - layoutBounds().y()));
    }

    /**
     * Дублирует выделенные ноды со сдвигом, не трогая буфер.
     *
     * @return созданные ноды, они же выделены
     */
    public List<NodeGraphItem> duplicateSelection() {
        return NodeGraphEditing.duplicate(this);
    }

    /**
     * Выравнивает выделенные ноды по общей рамке одним шагом истории.
     *
     * @param mode способ выравнивания
     * @return {@code true}, если хоть одна нода сдвинулась
     */
    public boolean alignSelected(NodeGraphAlign mode) {
        return NodeGraphEditing.align(this, mode);
    }

    /**
     * Равномерно распределяет выделенные ноды между крайними.
     *
     * @param horizontal {@code true} — по горизонтали, {@code false} — по вертикали
     * @return {@code true}, если хоть одна нода сдвинулась
     */
    public boolean distributeSelected(boolean horizontal) {
        return NodeGraphEditing.distribute(this, horizontal);
    }

    /**
     * Оборачивает выделенные ноды в регион «Comment».
     *
     * @return созданный регион или {@code null}, если ничего не выделено
     */
    public NodeGraphRegion groupSelected() {
        return groupSelected("Comment");
    }

    /**
     * Оборачивает выделенные ноды в регион с заданным заголовком.
     *
     * @param title заголовок региона
     * @return созданный регион или {@code null}, если ничего не выделено
     */
    public NodeGraphRegion groupSelected(String title) {
        return NodeGraphEditing.group(this, title);
    }

    /**
     * Подбирает положение и зум так, чтобы в окне были все ноды и регионы.
     */
    public NodeGraph frameAll() {
        NodeGraphEditing.frameAll(this);
        return this;
    }

    /**
     * Подбирает положение и зум под выделенное; без выделения — под весь граф.
     */
    public NodeGraph frameSelection() {
        NodeGraphEditing.frameSelection(this);
        return this;
    }

    /**
     * Вставляет reroute в провод.
     *
     * @param connectionId id провода
     * @param worldX       X центра reroute в world-координатах
     * @param worldY       Y центра reroute в world-координатах
     * @return созданный reroute или {@code null}, если провода нет
     */
    public NodeGraphItem insertReroute(String connectionId, float worldX, float worldY) {
        return NodeGraphEditing.insertReroute(this, connection(connectionId), worldX, worldY);
    }

    /**
     * Вставляет reroute в середину провода по его текущей отрисованной форме.
     *
     * @param connection провод
     * @return созданный reroute или {@code null}, если порты провода не найдены
     */
    public NodeGraphItem insertRerouteAtMidpoint(NodeGraphConnection connection) {
        if (connection == null || !wireCurve(connection)) return null;
        int middle = wireCount / 2;
        float rootX = wirePoints[middle * 2];
        float rootY = wirePoints[middle * 2 + 1];
        return NodeGraphEditing.insertReroute(this, connection,
                viewport.screenToWorldX(rootX - layoutBounds().x()),
                viewport.screenToWorldY(rootY - layoutBounds().y()));
    }

    private NodeGraphConnection firstSelectedConnection() {
        for (NodeGraphConnection connection : connections) {
            if (connection.selected()) return connection;
        }
        return null;
    }

    /**
     * @return реестр типов портов: цвета, формы и правила совместимости соединений
     */
    public PortTypeRegistry portTypes() {
        return portTypes;
    }

    /**
     * @return каталог типов нод для меню добавления и восстановления из сохранения
     */
    public NodeTypeRegistry nodeTypes() {
        return nodeTypes;
    }

    /**
     * Создаёт ноду зарегистрированного типа с автоматическим id и добавляет её в граф.
     *
     * @param typeId id типа из {@link #nodeTypes()}
     * @param x      X в world-координатах
     * @param y      Y в world-координатах
     * @return созданная нода или {@code null}, если тип не зарегистрирован
     */
    public NodeGraphItem spawn(String typeId, float x, float y) {
        NodeType type = nodeTypes.get(typeId);
        if (type == null) return null;
        return spawn(typeId, generateItemId(type.id()), x, y);
    }

    /**
     * Подбирает свободный id ноды вида {@code prefix-N}.
     *
     * @param prefix начало id, обычно id типа ноды
     * @return id, которого нет среди нод графа
     */
    public String generateItemId(String prefix) {
        String base = prefix == null || prefix.isBlank() ? "node" : prefix;
        String id;
        do {
            id = base + "-" + nextItemId++;
        } while (item(id) != null);
        return id;
    }

    /**
     * Создаёт ноду зарегистрированного типа с заданным id и добавляет её в граф.
     *
     * @param typeId id типа из {@link #nodeTypes()}
     * @param itemId id создаваемой ноды
     * @param x      X в world-координатах
     * @param y      Y в world-координатах
     * @return созданная нода или {@code null}, если тип не зарегистрирован
     */
    public NodeGraphItem spawn(String typeId, String itemId, float x, float y) {
        NodeType type = nodeTypes.get(typeId);
        if (type == null) return null;
        NodeGraphItem created = type.create(itemId);
        created.position(x, y);
        addItem(created);
        return created;
    }

    /**
     * Создаёт регион-комментарий и добавляет его на холст.
     *
     * @param id     id региона; пустая строка даёт автоматический id
     * @param title  заголовок
     * @param x      X в world-координатах
     * @param y      Y в world-координатах
     * @param width  ширина
     * @param height высота
     * @return созданный регион
     */
    public NodeGraphRegion addRegion(String id, String title, float x, float y, float width, float height) {
        String normalized = id == null ? "" : id.trim();
        if (normalized.isEmpty()) {
            do {
                normalized = "region-" + nextRegionId++;
            } while (region(normalized) != null);
        }
        NodeGraphRegion region = new NodeGraphRegion(normalized, title, x, y, width, height);
        addRegion(region);
        return region;
    }

    /**
     * Добавляет готовый регион на холст.
     *
     * @param region регион; {@code null} игнорируется
     * @return этот граф
     * @throws IllegalArgumentException если id занят или регион принадлежит другому графу
     */
    public NodeGraph addRegion(NodeGraphRegion region) {
        if (region == null || regions.contains(region)) return this;
        if (region.owner() != null && region.owner() != this) {
            throw new IllegalArgumentException("NodeGraphRegion already belongs to another NodeGraph");
        }
        if (region(region.id()) != null) {
            throw new IllegalArgumentException("Duplicate NodeGraph region id: " + region.id());
        }
        regions.add(region);
        region.owner(this);
        for (Widget widget : List.of(region.titleLabel(), region.commentLabel())) {
            adoptModuleWidget(widget);
        }
        childrenViewDirty = true;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        dispatch(new NodeGraphRegionChangedEvent(this, region.id(), NodeGraphRegionChangedEvent.Kind.ADDED,
                region.x(), region.y(), region.width(), region.height()));
        history.record(NodeGraphHistory.Operation.of(() -> removeRegion(region), () -> addRegion(region)));
        return this;
    }

    /**
     * @param region удаляемый регион
     * @return этот граф
     */
    public NodeGraph removeRegion(NodeGraphRegion region) {
        if (region == null || !regions.remove(region)) return this;
        // Виджеты региона не освобождаются: отмена удаления вернёт тот же регион.
        releaseRegion(region, false);
        childrenViewDirty = true;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        dispatch(new NodeGraphRegionChangedEvent(this, region.id(), NodeGraphRegionChangedEvent.Kind.REMOVED,
                region.x(), region.y(), region.width(), region.height()));
        history.record(NodeGraphHistory.Operation.of(() -> addRegion(region), () -> removeRegion(region)));
        return this;
    }

    /**
     * @param id id региона
     * @return этот граф
     */
    public NodeGraph removeRegion(String id) {
        NodeGraphRegion region = region(id);
        return region == null ? this : removeRegion(region);
    }

    /**
     * @param id id региона
     * @return регион или {@code null}
     */
    public NodeGraphRegion region(String id) {
        if (id == null) return null;
        for (int i = 0, size = regions.size(); i < size; i++) {
            NodeGraphRegion region = regions.get(i);
            if (region.id().equals(id)) return region;
        }
        return null;
    }

    /**
     * @return регионы снизу вверх, только для чтения
     */
    public List<NodeGraphRegion> regions() {
        return regionsView;
    }

    /**
     * Удаляет все регионы; ноды остаются.
     *
     * @return этот граф
     */
    public NodeGraph clearRegions() {
        for (NodeGraphRegion region : regions.toArray(new NodeGraphRegion[0])) {
            regions.remove(region);
            releaseRegion(region, true);
        }
        childrenViewDirty = true;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * @return выделенный регион или {@code null}
     */
    public NodeGraphRegion selectedRegion() {
        for (int i = 0, size = regions.size(); i < size; i++) {
            if (regions.get(i).selected()) return regions.get(i);
        }
        return null;
    }

    /**
     * Выделяет регион и снимает остальные выделения регионов.
     *
     * @param region регион или {@code null}, чтобы снять выделение
     * @return этот граф
     */
    public NodeGraph selectRegion(NodeGraphRegion region) {
        for (int i = 0, size = regions.size(); i < size; i++) {
            regions.get(i).selectedInternal(regions.get(i) == region);
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * @return {@code true}, если в углу холста рисуется миникарта
     */
    public boolean minimapEnabled() {
        return minimapEnabled;
    }

    /**
     * Включает миникарту в правом нижнем углу: обзор всех нод и регионов, клик и перетаскивание
     * по ней двигают видимое окно.
     *
     * @param minimapEnabled {@code true}, чтобы показать
     * @return этот граф
     */
    public NodeGraph minimapEnabled(boolean minimapEnabled) {
        if (this.minimapEnabled == minimapEnabled) return this;
        this.minimapEnabled = minimapEnabled;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * @param width  ширина миникарты в пикселях
     * @param height высота миникарты в пикселях
     * @return этот граф
     */
    public NodeGraph minimapSize(float width, float height) {
        this.minimapWidth = positiveOr(width, this.minimapWidth);
        this.minimapHeight = positiveOr(height, this.minimapHeight);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * @return live-цвет фона миникарты
     */
    public MutableColor minimapBackgroundColor() {
        return minimapBackgroundColor;
    }

    /**
     * @return live-цвет рамки миникарты
     */
    public MutableColor minimapBorderColor() {
        return minimapBorderColor;
    }

    /**
     * @return live-цвет рамки видимого окна на миникарте
     */
    public MutableColor minimapViewportColor() {
        return minimapViewportColor;
    }

    /**
     * @param listener слушатель сворачивания и разворачивания нод
     * @return подписка
     */
    public EventSubscription onItemCollapsed(EventListener<? super NodeGraphItemCollapsedEvent> listener) {
        return on(NodeGraphItemCollapsedEvent.TYPE, listener);
    }

    /**
     * @param listener слушатель изменений регионов
     * @return подписка
     */
    public EventSubscription onRegionChanged(EventListener<? super NodeGraphRegionChangedEvent> listener) {
        return on(NodeGraphRegionChangedEvent.TYPE, listener);
    }

    void itemCollapsedChanged(NodeGraphItem item) {
        boolean now = item.collapsed();
        history.record(NodeGraphHistory.Operation.of(() -> item.collapsed(!now), () -> item.collapsed(now)));
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        dispatch(new NodeGraphItemCollapsedEvent(this, item.id(), item.collapsed()));
    }

    void itemValueChanged(NodeGraphItem item, String portId) {
        ValueEditor editor = item.editor(portId);
        dispatch(new NodeGraphValueChangedEvent(this, item.id(), portId,
                editor == null ? null : editor.value(), editor == null ? "" : editor.serialize()));
    }

    /**
     * @param listener слушатель изменений значений входов нод
     * @return подписка
     */
    public EventSubscription onValueChanged(EventListener<? super NodeGraphValueChangedEvent> listener) {
        return on(NodeGraphValueChangedEvent.TYPE, listener);
    }

    void itemHostedChanged() {
        childrenViewDirty = true;
    }

    void regionHostedChanged() {
        childrenViewDirty = true;
    }

    void adoptModuleWidget(Widget widget) {
        if (widget instanceof WidgetBase base) {
            base.setParentInternal(this);
            base.setUiContextInternal(uiContext());
        }
    }

    private void releaseRegion(NodeGraphRegion region, boolean dispose) {
        for (Widget widget : List.of(region.titleLabel(), region.commentLabel())) {
            if (widget instanceof WidgetBase base) {
                base.setParentInternal(null);
                base.setUiContextInternal(null);
            }
            if (dispose) widget.dispose();
        }
        region.selectedInternal(false);
        region.owner(null);
    }

    @AiDoc(summary = "Captures a full serializable snapshot of the node graph.")
    @WikiMethod(summary = "<key:node.graph.method.snapshot>")
    @WikiReturn("<key:node.graph.return.snapshot>")
    public NodeGraphSnapshot snapshot() {
        List<NodeGraphItemSnapshot> itemSnapshots = new ObjectArrayList<>(items.size());
        Object[] rawItems = items.elements();
        for (int itemIndex = 0, itemSize = items.size(); itemIndex < itemSize; itemIndex++) {
            NodeGraphItem item = (NodeGraphItem) rawItems[itemIndex];
            ObjectArrayList<NodeGraphPort> ports = item.rawPorts();
            List<NodeGraphPortSnapshot> portSnapshots = new ObjectArrayList<>(ports.size());
            Object[] rawPorts = ports.elements();
            for (int portIndex = 0, portSize = ports.size(); portIndex < portSize; portIndex++) {
                NodeGraphPort port = (NodeGraphPort) rawPorts[portIndex];
                portSnapshots.add(new NodeGraphPortSnapshot(
                        port.id(),
                        port.kind(),
                        port.side(),
                        port.offset(),
                        port.type(),
                        port.enabled(),
                        port.visible()));
            }
            itemSnapshots.add(new NodeGraphItemSnapshot(
                    item.id(),
                    item.modular() ? "" : item.content().getClass().getName(),
                    item.x(),
                    item.y(),
                    item.width(),
                    item.height(),
                    item.selectable(),
                    item.movable(),
                    item.resizable(),
                    item.visible(),
                    portSnapshots,
                    item.typeId(),
                    item.collapsed(),
                    item.saveState()));
        }

        List<NodeGraphRegionSnapshot> regionSnapshots = new ObjectArrayList<>(regions.size());
        for (int i = 0, size = regions.size(); i < size; i++) {
            NodeGraphRegion region = regions.get(i);
            MutableColor color = region.color();
            int argb = (Math.round(color.a() * 255.0f) << 24) | (Math.round(color.r() * 255.0f) << 16)
                    | (Math.round(color.g() * 255.0f) << 8) | Math.round(color.b() * 255.0f);
            regionSnapshots.add(new NodeGraphRegionSnapshot(region.id(), region.title(), region.comment(), argb,
                    region.x(), region.y(), region.width(), region.height(), region.movable(), region.resizable()));
        }

        List<NodeGraphConnectionSnapshot> connectionSnapshots = new ObjectArrayList<>(connections.size());
        Object[] rawConnections = connections.elements();
        for (int connectionIndex = 0, connectionSize = connections.size(); connectionIndex < connectionSize; connectionIndex++) {
            NodeGraphConnection connection = (NodeGraphConnection) rawConnections[connectionIndex];
            connectionSnapshots.add(new NodeGraphConnectionSnapshot(
                    connection.id(),
                    connection.from(),
                    connection.to(),
                    connection.enabled(),
                    connection.type()));
        }

        return new NodeGraphSnapshot(
                viewportX(),
                viewportY(),
                zoom(),
                itemSnapshots,
                connectionSnapshots,
                selectedItemIds(),
                selectedConnectionIds(),
                regionSnapshots);
    }

    @AiDoc(summary = "Restores graph items, connections, and viewport from a serialized snapshot using a widget resolver.")
    @WikiMethod(summary = "<key:node.graph.method.restoreSnapshot>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph restoreSnapshot(@WikiParameter(name = "snapshot", description = "<key:node.graph.param.snapshot>") NodeGraphSnapshot snapshot, @WikiParameter(name = "resolver", description = "<key:node.graph.param.resolver>") NodeGraphWidgetResolver resolver) {
        if (snapshot == null) return this;
        NodeGraphWidgetResolver safeResolver = resolver == null ? (itemId, contentType) -> null : resolver;
        clearItems();
        clearRegions();

        for (NodeGraphRegionSnapshot regionSnapshot : snapshot.regions()) {
            NodeGraphRegion region = addRegion(regionSnapshot.id(), regionSnapshot.title(),
                    regionSnapshot.x(), regionSnapshot.y(), regionSnapshot.width(), regionSnapshot.height());
            region.comment(regionSnapshot.comment())
                    .color(regionSnapshot.argb())
                    .movable(regionSnapshot.movable())
                    .resizable(regionSnapshot.resizable());
        }

        for (NodeGraphItemSnapshot itemSnapshot : snapshot.items()) {
            NodeType restoredType = itemSnapshot.contentType().isEmpty() ? nodeTypes.get(itemSnapshot.typeId()) : null;
            if (restoredType != null) {
                NodeGraphItem modular = restoredType.create(itemSnapshot.id());
                modular.position(itemSnapshot.x(), itemSnapshot.y())
                        .size(itemSnapshot.width(), itemSnapshot.height())
                        .selectable(itemSnapshot.selectable())
                        .movable(itemSnapshot.movable())
                        .resizable(itemSnapshot.resizable())
                        .visible(itemSnapshot.visible());
                modular.collapsed(itemSnapshot.collapsed());
                modular.loadState(itemSnapshot.state());
                addItem(modular);
                for (NodeGraphPortSnapshot portSnapshot : itemSnapshot.ports()) {
                    NodeGraphPort port = modular.port(portSnapshot.id());
                    if (port != null) {
                        port.enabled(portSnapshot.enabled()).visible(portSnapshot.visible());
                    }
                }
                continue;
            }
            Widget content = safeResolver.resolve(itemSnapshot.id(), itemSnapshot.contentType());
            if (content == null) continue;
            NodeGraphItem item = addItem(itemSnapshot.id(), content, itemSnapshot.x(), itemSnapshot.y());
            item.size(itemSnapshot.width(), itemSnapshot.height())
                    .selectable(itemSnapshot.selectable())
                    .movable(itemSnapshot.movable())
                    .resizable(itemSnapshot.resizable())
                    .visible(itemSnapshot.visible());
            for (NodeGraphPortSnapshot portSnapshot : itemSnapshot.ports()) {
                NodeGraphPort port = item.addPort(
                        portSnapshot.id(),
                        portSnapshot.kind(),
                        portSnapshot.side(),
                        portSnapshot.offset());
                port.type(portSnapshot.type())
                        .enabled(portSnapshot.enabled())
                        .visible(portSnapshot.visible());
            }
        }

        viewport(snapshot.viewportX(), snapshot.viewportY(), snapshot.zoom());

        for (NodeGraphConnectionSnapshot connectionSnapshot : snapshot.connections()) {
            NodeGraphConnection connection = new NodeGraphConnection(
                    connectionSnapshot.id(),
                    connectionSnapshot.from(),
                    connectionSnapshot.to())
                    .enabled(connectionSnapshot.enabled())
                    .type(connectionSnapshot.type());
            try {
                addConnection(connection);
            } catch (IllegalArgumentException ignored) {
                // Snapshot may reference items that were not resolved by the caller.
            }
        }

        clearSelectionInternal(true);
        for (String itemId : snapshot.selectedItemIds()) {
            NodeGraphItem item = item(itemId);
            if (item != null) {
                item.selectedInternal(true);
            }
        }
        clearConnectionSelectionInternal();
        for (String connectionId : snapshot.selectedConnectionIds()) {
            NodeGraphConnection connection = connection(connectionId);
            if (connection != null) {
                connection.selectedInternal(true);
            }
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public List<Widget> children() {
        if (childrenViewDirty) {
            NodeGraphItem[] snapshot = itemSnapshot();
            List<Widget> children = new ObjectArrayList<>(snapshot.length);
            for (NodeGraphItem item : snapshot) {
                children.addAll(item.hostedWidgets());
            }
            for (int i = 0, size = regions.size(); i < size; i++) {
                children.addAll(regions.get(i).widgets());
            }
            childrenView = children.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(children);
            childrenViewDirty = false;
        }
        return childrenView;
    }

    private NodeGraphItem[] itemSnapshot() {
        if (itemSnapshotDirty) {
            itemSnapshot = items.toArray(new NodeGraphItem[items.size()]);
            itemSnapshotDirty = false;
        }
        return itemSnapshot;
    }

    private NodeGraphItem[] logicalItemSnapshot() {
        if (logicalItemSnapshotDirty) {
            logicalItemSnapshot = logicalItems.toArray(new NodeGraphItem[logicalItems.size()]);
            logicalItemSnapshotDirty = false;
        }
        return logicalItemSnapshot;
    }

    private NodeGraphConnection[] connectionSnapshot() {
        if (connectionSnapshotDirty) {
            connectionSnapshot = connections.toArray(new NodeGraphConnection[connections.size()]);
            connectionSnapshotDirty = false;
        }
        return connectionSnapshot;
    }

    private void markItemsDirty() {
        itemSnapshotDirty = true;
        childrenViewDirty = true;
    }

    private void markLogicalItemsDirty() {
        logicalItemSnapshotDirty = true;
    }

    private void markConnectionsDirty() {
        connectionSnapshotDirty = true;
    }

    @Override
    public HitTestPoint mapHitTestPointForChild(Widget child, float x, float y) {
        if (!scaleContentWithZoom || zoom() == 1.0f || child == null) return null;
        float[] pivot = zoomPivot(child);
        if (pivot == null) return null;
        return new HitTestPoint(
                pivot[0] + (x - pivot[0]) / zoom(),
                pivot[1] + (y - pivot[1]) / zoom());
    }

    /**
     * @return корневые координаты точки масштабирования для виджета ноды или региона; {@code null}, если виджет чужой
     */
    private float[] zoomPivot(Widget child) {
        NodeGraphItem item = itemForContent(child);
        if (item != null) {
            return new float[]{worldToRootX(item.x()), worldToRootY(item.y())};
        }
        for (Widget current = child; current != null && current != this; current = current.parent()) {
            NodeGraphRegion region = regionForContent(current);
            if (region != null) {
                return new float[]{worldToRootX(region.x()), worldToRootY(region.y())};
            }
        }
        return null;
    }

    @Override
    public RectView renderedBoundsForChild(Widget child, RectView bounds) {
        if (!scaleContentWithZoom || zoom() == 1.0f || child == null || bounds == null) return null;
        float[] pivot = zoomPivot(child);
        if (pivot == null) return null;
        return new MutableRect(
                pivot[0] + (bounds.x() - pivot[0]) * zoom(),
                pivot[1] + (bounds.y() - pivot[1]) * zoom(),
                bounds.width() * zoom(),
                bounds.height() * zoom());
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        for (NodeGraphItem item : itemSnapshot()) {
            for (Widget widget : item.allWidgets()) {
                if (widget instanceof WidgetBase base) {
                    base.setUiContextInternal(uiContext);
                }
            }
        }
        for (int i = 0, size = regions.size(); i < size; i++) {
            for (Widget widget : List.of(regions.get(i).titleLabel(), regions.get(i).commentLabel())) {
                if (widget instanceof WidgetBase base) {
                    base.setUiContextInternal(uiContext);
                }
            }
        }
    }

    @AiDoc(summary = "Returns the background fill color of the canvas.")
    @WikiMethod(summary = "<key:node.graph.method.backgroundColor>")
    @WikiReturn("<key:node.graph.return.color>")
    public MutableColor backgroundColor() {
        return backgroundColor;
    }

    @AiDoc(summary = "Returns the minor grid lines color.")
    @WikiMethod(summary = "<key:node.graph.method.gridColor>")
    @WikiReturn("<key:node.graph.return.color>")
    public MutableColor gridColor() {
        return gridColor;
    }

    @AiDoc(summary = "Returns the major grid lines color.")
    @WikiMethod(summary = "<key:node.graph.method.majorGridColor>")
    @WikiReturn("<key:node.graph.return.color>")
    public MutableColor majorGridColor() {
        return majorGridColor;
    }

    @AiDoc(summary = "Returns default border color for node items.")
    @WikiMethod(summary = "<key:node.graph.method.itemBorderColor>")
    @WikiReturn("<key:node.graph.return.color>")
    public MutableColor itemBorderColor() {
        return itemBorderColor;
    }

    @AiDoc(summary = "Returns hover border color for node items.")
    @WikiMethod(summary = "<key:node.graph.method.hoveredItemBorderColor>")
    @WikiReturn("<key:node.graph.return.color>")
    public MutableColor hoveredItemBorderColor() {
        return hoveredItemBorderColor;
    }

    @AiDoc(summary = "Returns selected border color for node items.")
    @WikiMethod(summary = "<key:node.graph.method.selectedItemBorderColor>")
    @WikiReturn("<key:node.graph.return.color>")
    public MutableColor selectedItemBorderColor() {
        return selectedItemBorderColor;
    }

    @AiDoc(summary = "Returns default fill color for pin ports.")
    @WikiMethod(summary = "<key:node.graph.method.portColor>")
    @WikiReturn("<key:node.graph.return.color>")
    public MutableColor portColor() {
        return portColor;
    }

    @AiDoc(summary = "Returns hover fill color for pin ports.")
    @WikiMethod(summary = "<key:node.graph.method.hoveredPortColor>")
    @WikiReturn("<key:node.graph.return.color>")
    public MutableColor hoveredPortColor() {
        return hoveredPortColor;
    }

    @AiDoc(summary = "Returns default line color for connections.")
    @WikiMethod(summary = "<key:node.graph.method.connectionColor>")
    @WikiReturn("<key:node.graph.return.color>")
    public MutableColor connectionColor() {
        return connectionColor;
    }

    @AiDoc(summary = "Returns highlight line color for selected connections.")
    @WikiMethod(summary = "<key:node.graph.method.selectedConnectionColor>")
    @WikiReturn("<key:node.graph.return.color>")
    public MutableColor selectedConnectionColor() {
        return selectedConnectionColor;
    }

    @AiDoc(summary = "Returns preview line color for valid drag connections.")
    @WikiMethod(summary = "<key:node.graph.method.connectionPreviewColor>")
    @WikiReturn("<key:node.graph.return.color>")
    public MutableColor connectionPreviewColor() {
        return connectionPreviewColor;
    }

    @AiDoc(summary = "Returns preview line color for invalid drag connections.")
    @WikiMethod(summary = "<key:node.graph.method.invalidConnectionPreviewColor>")
    @WikiReturn("<key:node.graph.return.color>")
    public MutableColor invalidConnectionPreviewColor() {
        return invalidConnectionPreviewColor;
    }

    @AiDoc(summary = "Returns fill color for rubber-band selection rectangles.")
    @WikiMethod(summary = "<key:node.graph.method.selectionBoxFillColor>")
    @WikiReturn("<key:node.graph.return.color>")
    public MutableColor selectionBoxFillColor() {
        return selectionBoxFillColor;
    }

    @AiDoc(summary = "Returns border color for rubber-band selection rectangles.")
    @WikiMethod(summary = "<key:node.graph.method.selectionBoxBorderColor>")
    @WikiReturn("<key:node.graph.return.color>")
    public MutableColor selectionBoxBorderColor() {
        return selectionBoxBorderColor;
    }

    @AiDoc(summary = "Returns color of resize grip handles.")
    @WikiMethod(summary = "<key:node.graph.method.resizeHandleColor>")
    @WikiReturn("<key:node.graph.return.color>")
    public MutableColor resizeHandleColor() {
        return resizeHandleColor;
    }

    @AiDoc(summary = "Returns the preferred width of this graph.")
    @WikiMethod(summary = "<key:node.graph.method.preferredWidthGet>")
    @WikiReturn("<key:node.graph.return.preferredWidth>")
    public float preferredWidth() {
        return preferredWidth;
    }

    @XamlProperty(value = "preferredWidth", category = "Layout", defaultValue = "320", description = "Intrinsic node graph width before layout constraints are applied.")
    @AiDoc(summary = "Sets the preferred width of this graph.")
    @WikiMethod(summary = "<key:node.graph.method.preferredWidthSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph preferredWidth(@WikiParameter(name = "preferredWidth", description = "<key:node.graph.param.preferredWidth>") float preferredWidth) {
        float normalized = positiveOr(preferredWidth, DEFAULT_PREFERRED_WIDTH);
        if (this.preferredWidth == normalized) return this;
        this.preferredWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns the preferred height of this graph.")
    @WikiMethod(summary = "<key:node.graph.method.preferredHeightGet>")
    @WikiReturn("<key:node.graph.return.preferredHeight>")
    public float preferredHeight() {
        return preferredHeight;
    }

    @XamlProperty(value = "preferredHeight", category = "Layout", defaultValue = "200", description = "Intrinsic node graph height before layout constraints are applied.")
    @AiDoc(summary = "Sets the preferred height of this graph.")
    @WikiMethod(summary = "<key:node.graph.method.preferredHeightSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph preferredHeight(@WikiParameter(name = "preferredHeight", description = "<key:node.graph.param.preferredHeight>") float preferredHeight) {
        float normalized = positiveOr(preferredHeight, DEFAULT_PREFERRED_HEIGHT);
        if (this.preferredHeight == normalized) return this;
        this.preferredHeight = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Sets both preferred width and height dimensions.")
    @WikiMethod(summary = "<key:node.graph.method.preferredSize>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph preferredSize(@WikiParameter(name = "width", description = "<key:node.graph.param.preferredWidth>") float width, @WikiParameter(name = "height", description = "<key:node.graph.param.preferredHeight>") float height) {
        return preferredWidth(width).preferredHeight(height);
    }

    @AiDoc(summary = "Returns the active connection validation policy.")
    @WikiMethod(summary = "<key:node.graph.method.connectionPolicyGet>")
    @WikiReturn("<key:node.graph.return.connectionPolicy>")
    public NodeGraphConnectionPolicy connectionPolicy() {
        return connectionPolicy;
    }

    @AiDoc(summary = "Sets the connection validation policy.")
    @WikiMethod(summary = "<key:node.graph.method.connectionPolicySet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph connectionPolicy(@WikiParameter(name = "connectionPolicy", description = "<key:node.graph.param.connectionPolicy>") NodeGraphConnectionPolicy connectionPolicy) {
        this.connectionPolicy = connectionPolicy == null ? NodeGraphConnectionPolicy.DEFAULT : connectionPolicy;
        return this;
    }

    @AiDoc(summary = "Returns current selection mode: NONE, SINGLE, or MULTIPLE.")
    @WikiMethod(summary = "<key:node.graph.method.selectionModeGet>")
    @WikiReturn("<key:node.graph.return.selectionMode>")
    public NodeGraphSelectionMode selectionMode() {
        return selectionMode;
    }

    @XamlProperty(value = "selectionMode", category = "Behavior", defaultValue = "single", description = "Selection mode used by node graph items.")
    @AiDoc(summary = "Sets selection mode.")
    @WikiMethod(summary = "<key:node.graph.method.selectionModeSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph selectionMode(@WikiParameter(name = "selectionMode", description = "<key:node.graph.param.selectionMode>") NodeGraphSelectionMode selectionMode) {
        NodeGraphSelectionMode next = selectionMode == null ? NodeGraphSelectionMode.SINGLE : selectionMode;
        if (this.selectionMode == next) return this;
        List<String> oldSelection = selectedItemIds();
        this.selectionMode = next;
        if (next == NodeGraphSelectionMode.NONE) {
            clearSelectionInternal(false);
        } else if (next == NodeGraphSelectionMode.SINGLE) {
            keepOnlyFirstSelection();
        }
        emitSelectionChanged(oldSelection);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns current viewport pan position and zoom factor.")
    @WikiMethod(summary = "<key:node.graph.method.viewportGet>")
    @WikiReturn("<key:node.graph.return.viewport>")
    public NodeGraphViewport viewport() {
        return new NodeGraphViewport(viewportX(), viewportY(), zoom());
    }

    @AiDoc(summary = "Sets viewport pan position without altering zoom.")
    @WikiMethod(summary = "<key:node.graph.method.viewportPan>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph viewport(@WikiParameter(name = "x", description = "<key:node.graph.param.viewportX>") float x, @WikiParameter(name = "y", description = "<key:node.graph.param.viewportY>") float y) {
        return viewport(x, y, zoom());
    }

    @AiDoc(summary = "Sets viewport pan position and zoom factor.")
    @WikiMethod(summary = "<key:node.graph.method.viewportFull>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph viewport(@WikiParameter(name = "x", description = "<key:node.graph.param.viewportX>") float x, @WikiParameter(name = "y", description = "<key:node.graph.param.viewportY>") float y, @WikiParameter(name = "zoom", description = "<key:node.graph.param.zoom>") float zoom) {
        float oldX = viewportX();
        float oldY = viewportY();
        float oldZoom = zoom();
        if (!viewport.set(x, y, zoom)) return this;
        arrangeItems();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        dispatch(new NodeGraphViewportChangedEvent(this, oldX, oldY, oldZoom, viewportX(), viewportY(), zoom()));
        return this;
    }

    @AiDoc(summary = "Returns the grid cell step size in pixels.")
    @WikiMethod(summary = "<key:node.graph.method.gridSizeGet>")
    @WikiReturn("<key:node.graph.return.gridSize>")
    public float gridSize() {
        return gridSize;
    }

    @XamlProperty(value = "gridSize", category = "Appearance", defaultValue = "24", description = "World-space spacing between rendered grid lines.")
    @AiDoc(summary = "Sets the grid cell step size in pixels.")
    @WikiMethod(summary = "<key:node.graph.method.gridSizeSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph gridSize(@WikiParameter(name = "gridSize", description = "<key:node.graph.param.gridSize>") float gridSize) {
        float next = Float.isFinite(gridSize) ? Math.max(2.0f, gridSize) : 24.0f;
        if (this.gridSize == next) return this;
        this.gridSize = next;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns true if canvas rendering is clipped to widget bounds.")
    @WikiMethod(summary = "<key:node.graph.method.clippingEnabledGet>")
    @WikiReturn("<key:node.graph.return.clippingEnabled>")
    public boolean clippingEnabled() {
        return clippingEnabled;
    }

    @XamlProperty(value = "clippingEnabled", category = "Behavior", defaultValue = "true", description = "Whether graph content is clipped to widget bounds.")
    @AiDoc(summary = "Sets whether canvas rendering is clipped to widget bounds.")
    @WikiMethod(summary = "<key:node.graph.method.clippingEnabledSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph clippingEnabled(@WikiParameter(name = "clippingEnabled", description = "<key:node.graph.param.clippingEnabled>") boolean clippingEnabled) {
        if (this.clippingEnabled == clippingEnabled) return this;
        this.clippingEnabled = clippingEnabled;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns true if item dragging is enabled.")
    @WikiMethod(summary = "<key:node.graph.method.itemDraggingEnabledGet>")
    @WikiReturn("<key:node.graph.return.enabled>")
    public boolean itemDraggingEnabled() {
        return itemDraggingEnabled;
    }

    @XamlProperty(value = "itemDraggingEnabled", category = "Behavior", defaultValue = "true", description = "Whether node items can be moved by pointer dragging.")
    @AiDoc(summary = "Sets whether item dragging is enabled.")
    @WikiMethod(summary = "<key:node.graph.method.itemDraggingEnabledSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph itemDraggingEnabled(@WikiParameter(name = "itemDraggingEnabled", description = "<key:node.graph.param.enabled>") boolean itemDraggingEnabled) {
        this.itemDraggingEnabled = itemDraggingEnabled;
        return this;
    }

    @AiDoc(summary = "Returns true if canvas panning via mouse drag is enabled.")
    @WikiMethod(summary = "<key:node.graph.method.panningEnabledGet>")
    @WikiReturn("<key:node.graph.return.enabled>")
    public boolean panningEnabled() {
        return panningEnabled;
    }

    @XamlProperty(value = "panningEnabled", category = "Behavior", defaultValue = "true", description = "Whether pointer gestures can pan the graph viewport.")
    @AiDoc(summary = "Sets whether canvas panning is enabled.")
    @WikiMethod(summary = "<key:node.graph.method.panningEnabledSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph panningEnabled(@WikiParameter(name = "panningEnabled", description = "<key:node.graph.param.enabled>") boolean panningEnabled) {
        this.panningEnabled = panningEnabled;
        return this;
    }

    @AiDoc(summary = "Returns true if canvas zooming via mouse wheel is enabled.")
    @WikiMethod(summary = "<key:node.graph.method.zoomEnabledGet>")
    @WikiReturn("<key:node.graph.return.enabled>")
    public boolean zoomEnabled() {
        return zoomEnabled;
    }

    @XamlProperty(value = "zoomEnabled", category = "Behavior", defaultValue = "true", description = "Whether wheel gestures can zoom the graph viewport.")
    @AiDoc(summary = "Sets whether canvas zooming is enabled.")
    @WikiMethod(summary = "<key:node.graph.method.zoomEnabledSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph zoomEnabled(@WikiParameter(name = "zoomEnabled", description = "<key:node.graph.param.enabled>") boolean zoomEnabled) {
        this.zoomEnabled = zoomEnabled;
        return this;
    }

    @AiDoc(summary = "Returns true if node item content widgets scale with zoom.")
    @WikiMethod(summary = "<key:node.graph.method.scaleContentWithZoomGet>")
    @WikiReturn("<key:node.graph.return.enabled>")
    public boolean scaleContentWithZoom() {
        return scaleContentWithZoom;
    }

    @XamlProperty(value = "scaleContentWithZoom", category = "Behavior", defaultValue = "true", description = "Whether embedded item widgets scale with viewport zoom.")
    @AiDoc(summary = "Sets whether node item content widgets scale with zoom.")
    @WikiMethod(summary = "<key:node.graph.method.scaleContentWithZoomSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph scaleContentWithZoom(@WikiParameter(name = "scaleContentWithZoom", description = "<key:node.graph.param.enabled>") boolean scaleContentWithZoom) {
        if (this.scaleContentWithZoom == scaleContentWithZoom) return this;
        this.scaleContentWithZoom = scaleContentWithZoom;
        arrangeItems();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Configures minimum and maximum allowed zoom levels.")
    @WikiMethod(summary = "<key:node.graph.method.zoomRange>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph zoomRange(@WikiParameter(name = "minZoom", description = "<key:node.graph.param.minZoom>") float minZoom, @WikiParameter(name = "maxZoom", description = "<key:node.graph.param.maxZoom>") float maxZoom) {
        float oldX = viewportX();
        float oldY = viewportY();
        float oldZoom = zoom();
        viewport.zoomRange(minZoom, maxZoom);
        if (oldX != viewportX() || oldY != viewportY() || oldZoom != zoom()) {
            arrangeItems();
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
            dispatch(new NodeGraphViewportChangedEvent(this, oldX, oldY, oldZoom, viewportX(), viewportY(), zoom()));
        }
        return this;
    }

    @AiDoc(summary = "Returns minimum zoom scale factor.")
    @WikiMethod(summary = "<key:node.graph.method.minZoom>")
    @WikiReturn("<key:node.graph.return.minZoom>")
    public float minZoom() {
        return viewport.minZoom();
    }

    @AiDoc(summary = "Returns maximum zoom scale factor.")
    @WikiMethod(summary = "<key:node.graph.method.maxZoom>")
    @WikiReturn("<key:node.graph.return.maxZoom>")
    public float maxZoom() {
        return viewport.maxZoom();
    }

    @AiDoc(summary = "Returns true if rubber-band lasso box selection is enabled.")
    @WikiMethod(summary = "<key:node.graph.method.lassoSelectionEnabledGet>")
    @WikiReturn("<key:node.graph.return.enabled>")
    public boolean lassoSelectionEnabled() {
        return lassoSelectionEnabled;
    }

    @XamlProperty(value = "lassoSelectionEnabled", category = "Behavior", defaultValue = "true", description = "Whether pointer drag can create lasso item selections.")
    @AiDoc(summary = "Sets whether rubber-band lasso selection is enabled.")
    @WikiMethod(summary = "<key:node.graph.method.lassoSelectionEnabledSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph lassoSelectionEnabled(@WikiParameter(name = "lassoSelectionEnabled", description = "<key:node.graph.param.enabled>") boolean lassoSelectionEnabled) {
        this.lassoSelectionEnabled = lassoSelectionEnabled;
        return this;
    }

    @AiDoc(summary = "Returns true if interactive resizing of resizable items is enabled.")
    @WikiMethod(summary = "<key:node.graph.method.resizeEnabledGet>")
    @WikiReturn("<key:node.graph.return.enabled>")
    public boolean resizeEnabled() {
        return resizeEnabled;
    }

    @XamlProperty(value = "resizeEnabled", category = "Behavior", defaultValue = "true", description = "Whether selected node items can be resized.")
    @AiDoc(summary = "Sets whether interactive item resizing is enabled.")
    @WikiMethod(summary = "<key:node.graph.method.resizeEnabledSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph resizeEnabled(@WikiParameter(name = "resizeEnabled", description = "<key:node.graph.param.enabled>") boolean resizeEnabled) {
        this.resizeEnabled = resizeEnabled;
        return this;
    }

    @AiDoc(summary = "Returns true if keyboard shortcuts (Delete, arrows) are enabled.")
    @WikiMethod(summary = "<key:node.graph.method.keyboardEditingEnabledGet>")
    @WikiReturn("<key:node.graph.return.enabled>")
    public boolean keyboardEditingEnabled() {
        return keyboardEditingEnabled;
    }

    @XamlProperty(value = "keyboardEditingEnabled", category = "Behavior", defaultValue = "true", description = "Whether keyboard shortcuts can edit node graph selections.")
    @AiDoc(summary = "Sets whether keyboard editing shortcuts are enabled.")
    @WikiMethod(summary = "<key:node.graph.method.keyboardEditingEnabledSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph keyboardEditingEnabled(@WikiParameter(name = "keyboardEditingEnabled", description = "<key:node.graph.param.enabled>") boolean keyboardEditingEnabled) {
        this.keyboardEditingEnabled = keyboardEditingEnabled;
        return this;
    }

    @AiDoc(summary = "Returns true if mouse wheel events are consumed while hovering the canvas.")
    @WikiMethod(summary = "<key:node.graph.method.consumeWheelWhileHoveredGet>")
    @WikiReturn("<key:node.graph.return.enabled>")
    public boolean consumeWheelWhileHovered() {
        return consumeWheelWhileHovered;
    }

    @XamlProperty(value = "consumeWheelWhileHovered", category = "Behavior", defaultValue = "true", description = "Whether wheel events are consumed while the graph is hovered.")
    @AiDoc(summary = "Sets whether mouse wheel events are consumed when hovered.")
    @WikiMethod(summary = "<key:node.graph.method.consumeWheelWhileHoveredSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph consumeWheelWhileHovered(@WikiParameter(name = "consumeWheelWhileHovered", description = "<key:node.graph.param.enabled>") boolean consumeWheelWhileHovered) {
        this.consumeWheelWhileHovered = consumeWheelWhileHovered;
        return this;
    }

    @AiDoc(summary = "Returns true if mouse wheel pans instead of zooming.")
    @WikiMethod(summary = "<key:node.graph.method.wheelPanningEnabledGet>")
    @WikiReturn("<key:node.graph.return.enabled>")
    public boolean wheelPanningEnabled() {
        return wheelPanningEnabled;
    }

    @XamlProperty(value = "wheelPanningEnabled", category = "Behavior", defaultValue = "true", description = "Whether non-zoom wheel gestures pan the graph viewport.")
    @AiDoc(summary = "Sets whether mouse wheel pans instead of zooming.")
    @WikiMethod(summary = "<key:node.graph.method.wheelPanningEnabledSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph wheelPanningEnabled(@WikiParameter(name = "wheelPanningEnabled", description = "<key:node.graph.param.enabled>") boolean wheelPanningEnabled) {
        this.wheelPanningEnabled = wheelPanningEnabled;
        return this;
    }

    @AiDoc(summary = "Returns the pixel step distance per wheel tick during wheel panning.")
    @WikiMethod(summary = "<key:node.graph.method.wheelPanStepGet>")
    @WikiReturn("<key:node.graph.return.wheelPanStep>")
    public float wheelPanStep() {
        return wheelPanStep;
    }

    @XamlProperty(value = "wheelPanStep", category = "Behavior", defaultValue = "32", description = "Viewport pan distance applied per wheel delta unit.")
    @AiDoc(summary = "Sets the pixel step distance per wheel tick.")
    @WikiMethod(summary = "<key:node.graph.method.wheelPanStepSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph wheelPanStep(@WikiParameter(name = "wheelPanStep", description = "<key:node.graph.param.wheelPanStep>") float wheelPanStep) {
        this.wheelPanStep = Float.isFinite(wheelPanStep) ? Math.max(1.0f, wheelPanStep) : 32.0f;
        return this;
    }

    @AiDoc(summary = "Returns true if selecting an item moves it to the top of the z-order.")
    @WikiMethod(summary = "<key:node.graph.method.bringToFrontOnSelectGet>")
    @WikiReturn("<key:node.graph.return.enabled>")
    public boolean bringToFrontOnSelect() {
        return bringToFrontOnSelect;
    }

    @XamlProperty(value = "bringToFrontOnSelect", category = "Behavior", defaultValue = "true", description = "Whether selecting an item moves it above sibling nodes.")
    @AiDoc(summary = "Sets whether selecting an item moves it to the front.")
    @WikiMethod(summary = "<key:node.graph.method.bringToFrontOnSelectSet>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph bringToFrontOnSelect(@WikiParameter(name = "bringToFrontOnSelect", description = "<key:node.graph.param.enabled>") boolean bringToFrontOnSelect) {
        this.bringToFrontOnSelect = bringToFrontOnSelect;
        return this;
    }

    @AiDoc(summary = "Returns the ID of the currently hovered node item, or empty string if none.")
    @WikiMethod(summary = "<key:node.graph.method.hoveredItemId>")
    @WikiReturn("<key:node.graph.return.hoveredItemId>")
    public String hoveredItemId() {
        return hoveredItemId;
    }

    @AiDoc(summary = "Returns reference to the currently hovered port, or null if none.")
    @WikiMethod(summary = "<key:node.graph.method.hoveredPort>")
    @WikiReturn("<key:node.graph.return.hoveredPort>")
    public NodeGraphPortRef hoveredPort() {
        return hoveredPort;
    }

    @AiDoc(summary = "Returns the ID of the currently hovered connection, or empty string if none.")
    @WikiMethod(summary = "<key:node.graph.method.hoveredConnectionId>")
    @WikiReturn("<key:node.graph.return.hoveredConnectionId>")
    public String hoveredConnectionId() {
        return hoveredConnectionId;
    }

    @AiDoc(summary = "Returns the ID of the item currently being dragged, or empty string if none.")
    @WikiMethod(summary = "<key:node.graph.method.draggingItemId>")
    @WikiReturn("<key:node.graph.return.draggingItemId>")
    public String draggingItemId() {
        return dragState != null && dragState.kind == DragKind.ITEM ? dragState.item.id() : "";
    }

    @AiDoc(summary = "Returns true if a new connection is currently being dragged from a port.")
    @WikiMethod(summary = "<key:node.graph.method.connectionDragging>")
    @WikiReturn("<key:node.graph.return.enabled>")
    public boolean connectionDragging() {
        return connectionDragState != null;
    }

    @AiDoc(summary = "Returns true if the viewport canvas is currently being panned.")
    @WikiMethod(summary = "<key:node.graph.method.panning>")
    @WikiReturn("<key:node.graph.return.enabled>")
    public boolean panning() {
        return dragState != null && dragState.kind == DragKind.PAN;
    }

    @AiDoc(summary = "Returns true if a rubber-band lasso selection box is actively being dragged.")
    @WikiMethod(summary = "<key:node.graph.method.lassoSelecting>")
    @WikiReturn("<key:node.graph.return.enabled>")
    public boolean lassoSelecting() {
        return lassoState != null;
    }

    @AiDoc(summary = "Returns an unmodifiable list of IDs of currently selected items.")
    @WikiMethod(summary = "<key:node.graph.method.selectedItemIds>")
    @WikiReturn("<key:node.graph.return.selectedItemIds>")
    public List<String> selectedItemIds() {
        List<String> selected = new ObjectArrayList<>();
        Object[] rawLogicalItems = logicalItems.elements();
        for (int logicalItemIndex = 0, logicalItemSize = logicalItems.size(); logicalItemIndex < logicalItemSize; logicalItemIndex++) {
            NodeGraphItem item = (NodeGraphItem) rawLogicalItems[logicalItemIndex];
            if (item.selected()) {
                selected.add(item.id());
            }
        }
        return List.copyOf(selected);
    }

    @AiDoc(summary = "Selects the item with the given ID exclusively.")
    @WikiMethod(summary = "<key:node.graph.method.selectItemId>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph selectItem(@WikiParameter(name = "id", description = "<key:node.graph.param.itemId>") String id) {
        NodeGraphItem item = item(id);
        return item == null ? this : selectItem(item);
    }

    @AiDoc(summary = "Selects the specified item exclusively.")
    @WikiMethod(summary = "<key:node.graph.method.selectItem>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph selectItem(@WikiParameter(name = "item", description = "<key:node.graph.param.item>") NodeGraphItem item) {
        return selectItem(item, false);
    }

    @AiDoc(summary = "Selects the item with optional additive toggle behaviour.")
    @WikiMethod(summary = "<key:node.graph.method.selectItemAdditive>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph selectItem(@WikiParameter(name = "item", description = "<key:node.graph.param.item>") NodeGraphItem item, @WikiParameter(name = "additive", description = "<key:node.graph.param.additive>") boolean additive) {
        if (item == null || !items.contains(item) || selectionMode == NodeGraphSelectionMode.NONE || !item.selectable()) {
            return this;
        }
        List<String> oldSelection = selectedItemIds();
        if (selectionMode == NodeGraphSelectionMode.SINGLE || !additive) {
            clearSelectionInternal(false);
        }
        item.selectedInternal(true);
        if (bringToFrontOnSelect && items.size() > 1 && items.get(items.size() - 1) != item) {
            items.remove(item);
            items.add(item);
            markItemsDirty();
        }
        emitSelectionChanged(oldSelection);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Deselects all currently selected items.")
    @WikiMethod(summary = "<key:node.graph.method.clearSelection>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph clearSelection() {
        List<String> oldSelection = selectedItemIds();
        clearSelectionInternal(false);
        emitSelectionChanged(oldSelection);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns an unmodifiable list of IDs of currently selected connections.")
    @WikiMethod(summary = "<key:node.graph.method.selectedConnectionIds>")
    @WikiReturn("<key:node.graph.return.selectedConnectionIds>")
    public List<String> selectedConnectionIds() {
        List<String> selected = new ObjectArrayList<>();
        Object[] rawConnections = connections.elements();
        for (int connectionIndex = 0, connectionSize = connections.size(); connectionIndex < connectionSize; connectionIndex++) {
            NodeGraphConnection connection = (NodeGraphConnection) rawConnections[connectionIndex];
            if (connection.selected()) {
                selected.add(connection.id());
            }
        }
        return List.copyOf(selected);
    }

    @AiDoc(summary = "Selects the connection with the given ID.")
    @WikiMethod(summary = "<key:node.graph.method.selectConnectionId>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph selectConnection(@WikiParameter(name = "id", description = "<key:node.graph.param.connectionId>") String id) {
        NodeGraphConnection connection = connection(id);
        return connection == null ? this : selectConnection(connection);
    }

    @AiDoc(summary = "Selects the specified connection.")
    @WikiMethod(summary = "<key:node.graph.method.selectConnection>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph selectConnection(@WikiParameter(name = "connection", description = "<key:node.graph.param.connection>") NodeGraphConnection connection) {
        if (connection == null || !connections.contains(connection)) return this;
        List<String> oldSelection = selectedConnectionIds();
        clearConnectionSelectionInternal();
        connection.selectedInternal(true);
        emitConnectionSelectionChanged(oldSelection);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Deselects all currently selected connections.")
    @WikiMethod(summary = "<key:node.graph.method.clearConnectionSelection>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph clearConnectionSelection() {
        List<String> oldSelection = selectedConnectionIds();
        clearConnectionSelectionInternal();
        emitConnectionSelectionChanged(oldSelection);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Registers a listener called when item drag movement begins.")
    @WikiMethod(summary = "<key:node.graph.method.onItemMoveStarted>")
    @WikiReturn("<key:node.graph.return.subscription>")
    public EventSubscription onItemMoveStarted(@WikiParameter(name = "listener", description = "<key:node.graph.param.listener>") EventListener<? super NodeGraphItemMoveStartedEvent> listener) {
        return on(NodeGraphItemMoveStartedEvent.TYPE, listener);
    }

    @AiDoc(summary = "Registers a listener called as an item is dragged across the canvas.")
    @WikiMethod(summary = "<key:node.graph.method.onItemMoved>")
    @WikiReturn("<key:node.graph.return.subscription>")
    public EventSubscription onItemMoved(@WikiParameter(name = "listener", description = "<key:node.graph.param.listener>") EventListener<? super NodeGraphItemMovedEvent> listener) {
        return on(NodeGraphItemMovedEvent.TYPE, listener);
    }

    @AiDoc(summary = "Registers a listener called when item drag movement finishes.")
    @WikiMethod(summary = "<key:node.graph.method.onItemMoveEnded>")
    @WikiReturn("<key:node.graph.return.subscription>")
    public EventSubscription onItemMoveEnded(@WikiParameter(name = "listener", description = "<key:node.graph.param.listener>") EventListener<? super NodeGraphItemMoveEndedEvent> listener) {
        return on(NodeGraphItemMoveEndedEvent.TYPE, listener);
    }

    @AiDoc(summary = "Registers a listener called when a node item is removed.")
    @WikiMethod(summary = "<key:node.graph.method.onItemRemoved>")
    @WikiReturn("<key:node.graph.return.subscription>")
    public EventSubscription onItemRemoved(@WikiParameter(name = "listener", description = "<key:node.graph.param.listener>") EventListener<? super NodeGraphItemRemovedEvent> listener) {
        return on(NodeGraphItemRemovedEvent.TYPE, listener);
    }

    @AiDoc(summary = "Registers a listener called when item resizing begins.")
    @WikiMethod(summary = "<key:node.graph.method.onItemResizeStarted>")
    @WikiReturn("<key:node.graph.return.subscription>")
    public EventSubscription onItemResizeStarted(@WikiParameter(name = "listener", description = "<key:node.graph.param.listener>") EventListener<? super NodeGraphItemResizeStartedEvent> listener) {
        return on(NodeGraphItemResizeStartedEvent.TYPE, listener);
    }

    @AiDoc(summary = "Registers a listener called as an item is being resized.")
    @WikiMethod(summary = "<key:node.graph.method.onItemResized>")
    @WikiReturn("<key:node.graph.return.subscription>")
    public EventSubscription onItemResized(@WikiParameter(name = "listener", description = "<key:node.graph.param.listener>") EventListener<? super NodeGraphItemResizedEvent> listener) {
        return on(NodeGraphItemResizedEvent.TYPE, listener);
    }

    @AiDoc(summary = "Registers a listener called when item resizing ends.")
    @WikiMethod(summary = "<key:node.graph.method.onItemResizeEnded>")
    @WikiReturn("<key:node.graph.return.subscription>")
    public EventSubscription onItemResizeEnded(@WikiParameter(name = "listener", description = "<key:node.graph.param.listener>") EventListener<? super NodeGraphItemResizeEndedEvent> listener) {
        return on(NodeGraphItemResizeEndedEvent.TYPE, listener);
    }

    @AiDoc(summary = "Registers a listener called when item selection changes.")
    @WikiMethod(summary = "<key:node.graph.method.onSelectionChanged>")
    @WikiReturn("<key:node.graph.return.subscription>")
    public EventSubscription onSelectionChanged(@WikiParameter(name = "listener", description = "<key:node.graph.param.listener>") EventListener<? super NodeGraphItemSelectionChangedEvent> listener) {
        return on(NodeGraphItemSelectionChangedEvent.TYPE, listener);
    }

    @AiDoc(summary = "Registers a listener called when viewport pan or zoom changes.")
    @WikiMethod(summary = "<key:node.graph.method.onViewportChanged>")
    @WikiReturn("<key:node.graph.return.subscription>")
    public EventSubscription onViewportChanged(@WikiParameter(name = "listener", description = "<key:node.graph.param.listener>") EventListener<? super NodeGraphViewportChangedEvent> listener) {
        return on(NodeGraphViewportChangedEvent.TYPE, listener);
    }

    @AiDoc(summary = "Registers a listener called when a new connection is created.")
    @WikiMethod(summary = "<key:node.graph.method.onConnectionCreated>")
    @WikiReturn("<key:node.graph.return.subscription>")
    public EventSubscription onConnectionCreated(@WikiParameter(name = "listener", description = "<key:node.graph.param.listener>") EventListener<? super NodeGraphConnectionCreatedEvent> listener) {
        return on(NodeGraphConnectionCreatedEvent.TYPE, listener);
    }

    @AiDoc(summary = "Registers a listener called when a connection is deleted.")
    @WikiMethod(summary = "<key:node.graph.method.onConnectionRemoved>")
    @WikiReturn("<key:node.graph.return.subscription>")
    public EventSubscription onConnectionRemoved(@WikiParameter(name = "listener", description = "<key:node.graph.param.listener>") EventListener<? super NodeGraphConnectionRemovedEvent> listener) {
        return on(NodeGraphConnectionRemovedEvent.TYPE, listener);
    }

    @AiDoc(summary = "Registers a listener called when a connection drag starts from a port.")
    @WikiMethod(summary = "<key:node.graph.method.onConnectionDragStarted>")
    @WikiReturn("<key:node.graph.return.subscription>")
    public EventSubscription onConnectionDragStarted(@WikiParameter(name = "listener", description = "<key:node.graph.param.listener>") EventListener<? super NodeGraphConnectionDragStartedEvent> listener) {
        return on(NodeGraphConnectionDragStartedEvent.TYPE, listener);
    }

    @AiDoc(summary = "Registers a listener called when connection drag interaction completes or cancels.")
    @WikiMethod(summary = "<key:node.graph.method.onConnectionDragEnded>")
    @WikiReturn("<key:node.graph.return.subscription>")
    public EventSubscription onConnectionDragEnded(@WikiParameter(name = "listener", description = "<key:node.graph.param.listener>") EventListener<? super NodeGraphConnectionDragEndedEvent> listener) {
        return on(NodeGraphConnectionDragEndedEvent.TYPE, listener);
    }

    @AiDoc(summary = "Registers a listener called when connection selection state changes.")
    @WikiMethod(summary = "<key:node.graph.method.onConnectionSelectionChanged>")
    @WikiReturn("<key:node.graph.return.subscription>")
    public EventSubscription onConnectionSelectionChanged(@WikiParameter(name = "listener", description = "<key:node.graph.param.listener>") EventListener<? super NodeGraphConnectionSelectionChangedEvent> listener) {
        return on(NodeGraphConnectionSelectionChangedEvent.TYPE, listener);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        // Measure children in world-space (unscaled) so their preferred sizes
        // are stable across zoom levels.
        LayoutContext childContext = new LayoutContext(
                Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        for (NodeGraphItem item : itemSnapshot()) {
            if (!item.visible()) continue;
            if (item.modular()) {
                for (Widget widget : item.hostedWidgets()) {
                    widget.measure(childContext);
                }
                itemLayout.item = item;
                item.measureModules(itemLayout);
                continue;
            }
            if (item.content().visibility() == Visibility.COLLAPSED) continue;
            item.content().measure(childContext);
            float padding = itemContentPadding * 2.0f;
            float width = item.autoWidth() ? StackPanel.preferredWidth(item.content(), 0.0f) + padding : item.width();
            float height = item.autoHeight() ? StackPanel.preferredHeight(item.content(), 0.0f) + padding : item.height();
            item.arrangedSize(Math.max(MIN_ITEM_SIZE, width), Math.max(MIN_ITEM_SIZE, height));
        }
        for (int i = 0, size = regions.size(); i < size; i++) {
            for (Widget widget : regions.get(i).widgets()) {
                widget.measure(childContext);
            }
        }
        setDesiredSize(resolveDesiredSize(context, preferredWidth, preferredHeight));
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        arrangeItems();
    }

    @Override
    public void tick(FrameContext frame) {
        if (visibility() != Visibility.VISIBLE) return;
        super.tick(frame);
        for (NodeGraphItem item : itemSnapshot()) {
            if (!item.visible()) continue;
            for (Widget widget : item.hostedWidgets()) {
                if (widget.visibility() == Visibility.VISIBLE) widget.tick(frame);
            }
        }
        for (int i = 0, size = regions.size(); i < size; i++) {
            for (Widget widget : regions.get(i).widgets()) {
                if (widget.visibility() == Visibility.VISIBLE) widget.tick(frame);
            }
        }
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE || context == null) return;
        if (layoutBounds().width() <= 0.0f || layoutBounds().height() <= 0.0f) return;
        pushOpacity(context);
        boolean pushedClip = false;
        try {
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (clippingEnabled) {
                draw.pushClip(layoutBounds().x(), layoutBounds().y(), layoutBounds().width(), layoutBounds().height());
                pushedClip = true;
            }
            WidgetRender custom = customRender();
            if (custom != null) {
                custom.render(draw, this);
            } else {
                renderBackground(draw);
                renderRegions(context, draw);
                renderConnections(draw);
            }
            for (NodeGraphItem item : itemSnapshot()) {
                if (item.modular()) {
                    if (item.visible()) renderModularItem(context, draw, item);
                    continue;
                }
                Widget content = item.content();
                if (!item.visible() || content.visibility() != Visibility.VISIBLE) continue;
                if (scaleContentWithZoom && zoom() != 1.0f) {
                    final float sx = worldToRootX(item.x());
                    final float sy = worldToRootY(item.y());
                    final float z = zoom();
                    DrawList drawList = context.drawList();
                    int sizeBefore = drawList.size();
                    renderChildWithInheritedTransform(context, content);
                    applyContentZoom(drawList.commands(), sizeBefore, sx, sy, z);
                } else {
                    renderChildWithInheritedTransform(context, content);
                }
            }
            if (custom != null) {
                custom.render(draw, this);
            } else {
                renderForeground(draw);
                renderRegionComment(context, draw);
            }
        } finally {
            if (pushedClip) {
                context.popClip();
            }
            popOpacity(context);
        }
    }

    private static void applyContentZoom(List<DrawCommand> commands, int fromIndex,
                                         float pivotX, float pivotY, float zoom) {
        if (commands == null || zoom == 1.0f) return;
        int start = Math.max(0, fromIndex);
        for (int i = start; i < commands.size(); i++) {
            DrawCommand command = commands.get(i);
            if (command == null) continue;
            if (command.type() != DrawCommandType.POP_CLIP) {
                // Drawn primitives, text, meshes, CUSTOM callbacks and clip rectangles
                // all go through command.transform(), so compose node zoom there.
                composeZoom(command, pivotX, pivotY, zoom);
            }
        }
    }

    private static void composeZoom(DrawCommand command, float pivotX, float pivotY, float zoom) {
        MutableRect bounds = command.bounds();
        Transform transform = command.transform();
        float transformPivotX = bounds.x() + transform.pivot().x();
        float transformPivotY = bounds.y() + transform.pivot().y();
        transform.position().set(
                transform.position().x() * zoom + (1.0f - zoom) * (pivotX - transformPivotX),
                transform.position().y() * zoom + (1.0f - zoom) * (pivotY - transformPivotY));
        transform.scale().set(transform.scale().x() * zoom, transform.scale().y() * zoom);
    }

    @Override
    public void handle(Event event) {
        if (visibility() != Visibility.VISIBLE || !enabled()) return;
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.CAPTURE) return;

        if (event instanceof PointerMovedEvent pointer) {
            handlePointerMoved(pointer);
        } else if (event instanceof PointerPressedEvent pointer) {
            handlePointerPressed(pointer, event);
        } else if (event instanceof PointerReleasedEvent pointer) {
            handlePointerReleased(pointer, event);
        } else if (event instanceof ScrollEvent scroll) {
            handleScroll(scroll, event);
        } else if (event instanceof KeyPressedEvent key) {
            handleKeyPressed(key, event);
        } else if (event instanceof PointerExitedEvent) {
            if (dragState == null && lassoState == null && connectionDragState == null) {
                setHoveredItem(null);
                setHoveredPort(null);
                setHoveredConnection(null);
                setHoveredRegion(null);
            }
        }
    }

    @Override
    public void dispose() {
        if (palette != null) palette.close();
        clearItems();
        clearRegions();
        super.dispose();
    }

    private void attach(NodeGraphItem item) {
        item.owner(this);
        for (Widget widget : item.allWidgets()) {
            adoptModuleWidget(widget);
        }
        for (NodeGraphPort port : item.ports()) {
            updatePortCount(port.ref());
        }
    }

    private void attach(NodeGraphConnection connection) {
        connection.owner(this);
        updatePortCount(connection.from());
        updatePortCount(connection.to());
    }

    private void detach(NodeGraphItem item) {
        for (Widget widget : item.allWidgets()) {
            if (widget instanceof WidgetBase base) {
                base.setParentInternal(null);
                base.setUiContextInternal(null);
            }
        }
        item.owner(null);
        item.selectedInternal(false);
    }

    private void detach(NodeGraphConnection connection) {
        connection.owner(null);
        connection.selectedInternal(false);
        updatePortCount(connection.from());
        updatePortCount(connection.to());
    }

    /**
     * Пересчитывает число соединений порта и уведомляет его модуль.
     */
    private void updatePortCount(NodeGraphPortRef ref) {
        NodeGraphPort port = port(ref);
        if (port == null) return;
        int count = 0;
        Object[] raw = connections.elements();
        for (int i = 0, size = connections.size(); i < size; i++) {
            NodeGraphConnection connection = (NodeGraphConnection) raw[i];
            if (connection.from().equals(ref) || connection.to().equals(ref)) count++;
        }
        if (port.connectionCount() == count) return;
        port.connectionCount(count);
        NodeGraphItem owner = item(ref.itemId());
        if (owner != null) owner.portConnectionsChanged(port);
    }

    /**
     * Освобождает место на порту с ограниченной ёмкостью, удаляя самые старые его соединения.
     */
    private void enforceCapacity(NodeGraphPortRef ref) {
        NodeGraphPort port = port(ref);
        if (port == null || port.capacity() <= 0) return;
        while (port.connectionCount() >= port.capacity()) {
            NodeGraphConnection oldest = null;
            Object[] raw = connections.elements();
            for (int i = 0, size = connections.size(); i < size; i++) {
                NodeGraphConnection connection = (NodeGraphConnection) raw[i];
                if (connection.from().equals(ref) || connection.to().equals(ref)) {
                    oldest = connection;
                    break;
                }
            }
            if (oldest == null) return;
            removeConnection(oldest);
        }
    }

    /**
     * @return нода, которой принадлежит виджет или один из его предков внутри ноды
     */
    private NodeGraphItem itemForContent(Widget content) {
        for (Widget current = content; current != null && current != this; current = current.parent()) {
            Object[] rawItems = items.elements();
            for (int itemIndex = 0, itemSize = items.size(); itemIndex < itemSize; itemIndex++) {
                NodeGraphItem item = (NodeGraphItem) rawItems[itemIndex];
                if (hosts(item, current)) return item;
            }
        }
        return null;
    }

    private static boolean hosts(NodeGraphItem item, Widget widget) {
        List<Widget> hosted = item.hostedWidgets();
        for (int i = 0, size = hosted.size(); i < size; i++) {
            if (hosted.get(i) == widget) return true;
        }
        return false;
    }

    /**
     * @return регион, которому принадлежит виджет заголовка или комментария
     */
    private NodeGraphRegion regionForContent(Widget content) {
        for (int i = 0, size = regions.size(); i < size; i++) {
            NodeGraphRegion region = regions.get(i);
            if (region.titleLabel() == content || region.commentLabel() == content) return region;
        }
        return null;
    }

    void removeConnectionsForPort(NodeGraphPortRef ref) {
        if (ref == null || ref.empty()) return;
        for (NodeGraphConnection connection : connectionSnapshot()) {
            if (connection.from().equals(ref) || connection.to().equals(ref)) {
                removeConnection(connection);
            }
        }
    }

    private void removeConnectionsForItem(String itemId) {
        if (itemId == null) return;
        for (NodeGraphConnection connection : connectionSnapshot()) {
            if (connection.fromItemId().equals(itemId) || connection.toItemId().equals(itemId)) {
                removeConnection(connection);
            }
        }
    }

    private void arrangeItems() {
        if (layoutBounds().width() <= 0.0f && layoutBounds().height() <= 0.0f) return;
        arrangeRegions();
        for (NodeGraphItem item : itemSnapshot()) {
            if (item.modular()) {
                if (!item.visible()) continue;
                itemLayout.item = item;
                item.arrangeModules(itemLayout);
                continue;
            }
            Widget content = item.content();
            if (!item.visible() || content.visibility() == Visibility.COLLAPSED) continue;
            float width = item.arrangedWidth() > 0.0f
                    ? item.arrangedWidth()
                    : Math.max(MIN_ITEM_SIZE, item.autoWidth() ? StackPanel.preferredWidth(content, 0.0f) + itemContentPadding * 2.0f : item.width());
            float height = item.arrangedHeight() > 0.0f
                    ? item.arrangedHeight()
                    : Math.max(MIN_ITEM_SIZE, item.autoHeight() ? StackPanel.preferredHeight(content, 0.0f) + itemContentPadding * 2.0f : item.height());
            item.arrangedSize(width, height);
            float contentPadding = Math.min(itemContentPadding, Math.max(0.0f, Math.min(width, height) * 0.5f));
            float contentWidth = Math.max(0.0f, width - contentPadding * 2.0f);
            float contentHeight = Math.max(0.0f, height - contentPadding * 2.0f);

            if (scaleContentWithZoom) {
                // Arrange in world-space size so child widgets keep stable
                // layout units. The camera zoom is composed into their draw
                // commands after the content is rendered with inherited transforms.
                StackPanel.arrangeChild(content,
                        worldToRootX(item.x()) + contentPadding,
                        worldToRootY(item.y()) + contentPadding,
                        contentWidth,
                        contentHeight);
            } else {
                // Fixed screen size — content ignores zoom completely.
                StackPanel.arrangeChild(content,
                        worldToRootX(item.x()) + contentPadding,
                        worldToRootY(item.y()) + contentPadding,
                        Math.max(0.0f, itemScreenWidth(item) - contentPadding * 2.0f),
                        Math.max(0.0f, itemScreenHeight(item) - contentPadding * 2.0f));
            }
        }
    }

    /**
     * Размещает заголовок и комментарий каждого региона в world-единицах от его угла.
     */
    private void arrangeRegions() {
        for (int i = 0, size = regions.size(); i < size; i++) {
            NodeGraphRegion region = regions.get(i);
            float originX = worldToRootX(region.x());
            float originY = worldToRootY(region.y());
            float iconRoom = region.comment().isEmpty() ? 0.0f : COMMENT_ICON_ROOM;
            float textWidth = Math.max(0.0f, region.width() - NodeGraphRegion.TEXT_INSET * 2.0f - iconRoom);
            StackPanel.arrangeChild(region.titleLabel(), originX + NodeGraphRegion.TEXT_INSET, originY,
                    textWidth, NodeGraphRegion.HEADER_HEIGHT);
        }
    }

    protected void handlePointerPressed(PointerPressedEvent pointer, Event sourceEvent) {
        if (pointer.button() == PointerButton.MIDDLE && panningEnabled) {
            startPan(pointer);
            sourceEvent.cancel();
            return;
        }
        if (pointer.button() == PointerButton.SECONDARY) {
            handleSecondaryPress(pointer, sourceEvent);
            return;
        }
        if (pointer.button() != PointerButton.PRIMARY) return;

        NodeGraphMinimap map = minimapGeometry();
        if (map != null && map.contains(pointer.rootX(), pointer.rootY())) {
            startMinimapDrag(map, pointer);
            sourceEvent.cancel();
            return;
        }

        PortHit portHit = hitPort(pointer.rootX(), pointer.rootY());
        if (portHit != null) {
            if (canStartConnection(portHit.port())) {
                startConnectionDrag(portHit, pointer);
            }
            sourceEvent.cancel();
            return;
        }

        NodeGraphItem resizeItem = hitResizeHandle(pointer.rootX(), pointer.rootY());
        if (resizeItem != null) {
            startResize(resizeItem, pointer);
            sourceEvent.cancel();
            return;
        }

        NodeGraphRegion resizeRegion = hitRegionResizeHandle(pointer.rootX(), pointer.rootY());
        if (resizeRegion != null) {
            startRegionDrag(DragKind.REGION_RESIZE, resizeRegion, pointer);
            sourceEvent.cancel();
            return;
        }

        // Провода рисуются под нодами, поэтому провод, идущий под нодой, не получает клик в области ноды.
        NodeGraphItem item = hitItem(pointer.rootX(), pointer.rootY());
        if (item == null) {
            NodeGraphConnection connection = hitConnection(pointer.rootX(), pointer.rootY());
            if (connection != null) {
                if (registerWireClick(connection, pointer)) {
                    // Двойной клик по проводу вставляет reroute прямо под курсором.
                    NodeGraphItem reroute = NodeGraphEditing.insertReroute(this, connection,
                            viewport.screenToWorldX(pointer.rootX() - layoutBounds().x()),
                            viewport.screenToWorldY(pointer.rootY() - layoutBounds().y()));
                    if (reroute != null) selectOnly(reroute);
                    sourceEvent.cancel();
                    return;
                }
                // Клик выделяет объекты одного вида: иначе Delete удалил бы и ноду, и провод.
                clearSelection();
                selectRegion(null);
                selectConnection(connection);
                sourceEvent.cancel();
                return;
            }
            item = itemForContent(pointer.target());
        }
        if (item == null) {
            NodeGraphRegion region = hitRegionHeader(pointer.rootX(), pointer.rootY());
            if (region != null) {
                clearSelection();
                clearConnectionSelection();
                selectRegion(region);
                if (region.movable()) startRegionDrag(DragKind.REGION, region, pointer);
                sourceEvent.cancel();
                return;
            }
            // Shift оставляет текущее выделение и рамка добавляет к нему новые ноды.
            boolean additive = shiftHeld() && selectionMode == NodeGraphSelectionMode.MULTIPLE;
            if (!additive) clearSelection();
            clearConnectionSelection();
            selectRegion(null);
            if (lassoSelectionEnabled && selectionMode == NodeGraphSelectionMode.MULTIPLE) {
                startLasso(pointer, additive);
                sourceEvent.cancel();
            }
            return;
        }
        selectRegion(null);
        if (item.modular() && pressItemModule(item, pointer)) {
            sourceEvent.cancel();
            return;
        }
        NodeGraphItem collapseTo = null;
        if (item.selectable()) {
            clearConnectionSelection();
            if (shiftHeld() && selectionMode == NodeGraphSelectionMode.MULTIPLE) {
                // Shift+клик добавляет ноду в выделение или убирает её оттуда; убранную нода не тащится.
                if (item.selected()) {
                    deselectItem(item);
                    sourceEvent.cancel();
                    return;
                }
                selectItem(item, true);
            } else if (item.selected() && selectedItemIds().size() > 1) {
                // Нажатие на ноду из выделенной группы не сбрасывает группу, чтобы её можно было тащить целиком;
                // если мышь не сдвинется, выделение схлопнется до этой ноды при отпускании.
                collapseTo = item;
            } else {
                selectItem(item);
            }
        }
        if (itemDraggingEnabled && item.movable()) {
            startItemDrag(item, pointer);
            if (dragState != null) dragState.collapseTo = collapseTo;
            sourceEvent.cancel();
        } else if (collapseTo != null) {
            selectItem(collapseTo);
        }
    }

    /**
     * ПКМ: по ноде открывает меню действий, по пустому месту — палитру нод; по порту, проводу и заголовку региона
     * пока ничего не делает.
     */
    private void handleSecondaryPress(PointerPressedEvent pointer, Event sourceEvent) {
        float rootX = pointer.rootX();
        float rootY = pointer.rootY();
        if (hitPort(rootX, rootY) != null) return;
        NodeGraphItem node = hitItem(rootX, rootY);
        if (node != null) {
            // Как в проводниках: ПКМ по невыделенной ноде выделяет её, а по выделенной сохраняет всю группу.
            if (!node.selected() && node.selectable()) selectOnly(node);
            if (openNodeMenu(rootX, rootY)) sourceEvent.cancel();
            return;
        }
        NodeGraphRegion region = hitRegionHeader(rootX, rootY);
        if (region != null) {
            if (!region.selected()) {
                clearSelection();
                clearConnectionSelection();
                selectRegion(region);
            }
            if (openRegionMenu(rootX, rootY)) sourceEvent.cancel();
            return;
        }
        boolean empty = hitConnection(rootX, rootY) == null;
        if (empty && openNodePalette(rootX, rootY)) {
            sourceEvent.cancel();
        }
    }

    /**
     * Открывает меню действий над выделенным регионом: Undo, Redo, Group (обернуть регионом-родителем), Delete.
     *
     * @param rootX X в root-координатах
     * @param rootY Y в root-координатах
     * @return {@code true}, если меню открыто
     */
    public boolean openRegionMenu(float rootX, float rootY) {
        OverlayLayer layer = overlayHost();
        if (layer == null || !nodeContextMenuEnabled) return false;
        ContextMenu menu = buildRegionMenu(selectedRegion());
        layer.applyQueuedMutations();
        layer.addOverlay(menu);
        menu.openAt(rootX, rootY);
        return true;
    }

    /**
     * Собирает меню региона. Переопределите, чтобы добавить или убрать пункты.
     *
     * @param region выбранный регион; может быть {@code null}
     * @return новое меню
     */
    protected ContextMenu buildRegionMenu(NodeGraphRegion region) {
        ContextMenu menu = new ContextMenu();
        addMenuItem(menu, "Undo", history.canUndo(), this::undo);
        addMenuItem(menu, "Redo", history.canRedo(), this::redo);
        menu.separator();
        addMenuItem(menu, "Rename", region != null,
                () -> openRegionEditor(region, RegionEditor.Mode.TITLE));
        addMenuItem(menu, "Comment...", region != null,
                () -> openRegionEditor(region, RegionEditor.Mode.COMMENT));
        menu.separator();
        addMenuItem(menu, "Group", region != null, () -> groupSelected());
        addMenuItem(menu, "Delete", region != null, () -> history.transact("Delete region", () -> removeRegion(region)));
        return menu;
    }

    /**
     * Открывает редактор названия или комментария региона рядом с его заголовком. Нужен {@code OverlayLayer}.
     *
     * @param region регион графа; {@code null} ничего не открывает
     * @param mode   что редактировать
     * @return {@code true}, если редактор открыт
     */
    public boolean openRegionEditor(NodeGraphRegion region, RegionEditor.Mode mode) {
        OverlayLayer layer = overlayHost();
        if (layer == null || region == null || !regions.contains(region)) return false;
        boolean title = mode != RegionEditor.Mode.COMMENT;
        float rootX = worldToRootX(region.x());
        float rootY = worldToRootY(region.y());
        float zoom = zoom();
        RegionEditor editor = new RegionEditor(title ? RegionEditor.Mode.TITLE : RegionEditor.Mode.COMMENT,
                title ? region.title() : region.comment(), region.width() * zoom,
                text -> {
                    if (title) renameRegion(region, text);
                    else commentRegion(region, text);
                });
        layer.applyQueuedMutations();
        layer.addOverlay(editor);
        // Название открывается поверх заголовка, комментарий под ним.
        editor.openAt(rootX, title ? rootY : rootY + NodeGraphRegion.HEADER_HEIGHT * zoom);
        return true;
    }

    /**
     * Переименовывает регион одним шагом истории.
     *
     * @param region регион графа
     * @param title  новый заголовок; пустой допустим
     * @return {@code true}, если заголовок изменился
     */
    public boolean renameRegion(NodeGraphRegion region, String title) {
        if (region == null || !regions.contains(region)) return false;
        String from = region.title();
        String to = title == null ? "" : title;
        if (from.equals(to)) return false;
        history.transact("Rename region", () -> {
            applyRegionTitle(region, to);
            history.record(NodeGraphHistory.Operation.of(
                    () -> applyRegionTitle(region, from), () -> applyRegionTitle(region, to)));
        });
        return true;
    }

    /**
     * Меняет комментарий региона одним шагом истории.
     *
     * @param region  регион графа
     * @param comment новый комментарий; пустая строка убирает его
     * @return {@code true}, если комментарий изменился
     */
    public boolean commentRegion(NodeGraphRegion region, String comment) {
        if (region == null || !regions.contains(region)) return false;
        String from = region.comment();
        String to = comment == null ? "" : comment;
        if (from.equals(to)) return false;
        history.transact("Edit region comment", () -> {
            applyRegionComment(region, to);
            history.record(NodeGraphHistory.Operation.of(
                    () -> applyRegionComment(region, from), () -> applyRegionComment(region, to)));
        });
        return true;
    }

    private void applyRegionTitle(NodeGraphRegion region, String title) {
        region.title(title);
        arrangeRegions();
        dispatch(new NodeGraphRegionChangedEvent(this, region.id(), NodeGraphRegionChangedEvent.Kind.EDITED,
                region.x(), region.y(), region.width(), region.height()));
    }

    private void applyRegionComment(NodeGraphRegion region, String comment) {
        region.comment(comment);
        arrangeRegions();
        dispatch(new NodeGraphRegionChangedEvent(this, region.id(), NodeGraphRegionChangedEvent.Kind.EDITED,
                region.x(), region.y(), region.width(), region.height()));
    }

    /**
     * @return {@code true}, если ПКМ по ноде открывает меню действий
     */
    public boolean nodeContextMenuEnabled() {
        return nodeContextMenuEnabled;
    }

    /**
     * Включает или выключает меню действий по ПКМ на ноде. Меню работает только внутри {@code OverlayLayer}.
     *
     * @param nodeContextMenuEnabled {@code true}, чтобы включить
     * @return этот граф
     */
    public NodeGraph nodeContextMenuEnabled(boolean nodeContextMenuEnabled) {
        this.nodeContextMenuEnabled = nodeContextMenuEnabled;
        return this;
    }

    /**
     * Открывает меню действий над выделенными нодами: Undo, Redo, Duplicate, Group, подменю Align (Left, Right,
     * Top, Bottom) и Spread (Horizontally, Vertically). Недоступные сейчас пункты отключены.
     *
     * @param rootX X в root-координатах
     * @param rootY Y в root-координатах
     * @return {@code true}, если меню открыто; {@code false}, если оно выключено или граф не лежит в
     * {@code OverlayLayer}
     */
    public boolean openNodeMenu(float rootX, float rootY) {
        OverlayLayer layer = overlayHost();
        if (layer == null || !nodeContextMenuEnabled) return false;
        ContextMenu menu = buildNodeMenu();
        layer.applyQueuedMutations();
        layer.addOverlay(menu);
        menu.openAt(rootX, rootY);
        return true;
    }

    protected ContextMenu buildNodeMenu() {
        List<NodeGraphItem> selected = NodeGraphEditing.selectedItems(this);
        long movable = selected.stream().filter(NodeGraphItem::movable).count();
        ContextMenu menu = new ContextMenu();
        addMenuItem(menu, "Undo", history.canUndo(), this::undo);
        addMenuItem(menu, "Redo", history.canRedo(), this::redo);
        menu.separator();
        addMenuItem(menu, "Duplicate", !NodeGraphEditing.copy(this).isEmpty(), this::duplicateSelection);
        addMenuItem(menu, "Group", !selected.isEmpty() || selectedRegion() != null, () -> groupSelected());

        ContextMenu align = new ContextMenu();
        boolean canAlign = movable >= 2;
        addMenuItem(align, "Left", canAlign, () -> alignSelected(NodeGraphAlign.LEFT));
        addMenuItem(align, "Right", canAlign, () -> alignSelected(NodeGraphAlign.RIGHT));
        addMenuItem(align, "Top", canAlign, () -> alignSelected(NodeGraphAlign.TOP));
        addMenuItem(align, "Bottom", canAlign, () -> alignSelected(NodeGraphAlign.BOTTOM));
        menu.separator();
        menu.submenu("Align", align);
        if (!canAlign) menu.itemButton(menu.itemCount() - 1).enabled(false);

        ContextMenu spread = new ContextMenu();
        boolean canSpread = movable >= 3;
        addMenuItem(spread, "Horizontally", canSpread, () -> distributeSelected(true));
        addMenuItem(spread, "Vertically", canSpread, () -> distributeSelected(false));
        menu.submenu("Spread", spread);
        if (!canSpread) menu.itemButton(menu.itemCount() - 1).enabled(false);
        return menu;
    }

    private static void addMenuItem(ContextMenu menu, String text, boolean enabled, Runnable action) {
        menu.item(text, action);
        if (!enabled) menu.itemButton(menu.itemCount() - 1).enabled(false);
    }

    /**
     * Запоминает нажатие по проводу и сообщает, является ли оно вторым в двойном клике.
     *
     * @return {@code true}, если это двойной клик по тому же проводу рядом с первым нажатием
     */
    private boolean registerWireClick(NodeGraphConnection connection, PointerPressedEvent pointer) {
        long now = clock.getAsLong();
        boolean doubleClick = doubleClickMillis > 0
                && connection.id().equals(lastWireClickId)
                && now - lastWireClickMillis <= doubleClickMillis
                && Math.hypot(pointer.rootX() - lastWireClickX, pointer.rootY() - lastWireClickY) <= DOUBLE_CLICK_DISTANCE;
        lastWireClickId = doubleClick ? "" : connection.id();
        lastWireClickMillis = now;
        lastWireClickX = pointer.rootX();
        lastWireClickY = pointer.rootY();
        return doubleClick;
    }

    private void deselectItem(NodeGraphItem item) {
        List<String> oldSelection = selectedItemIds();
        item.selectedInternal(false);
        emitSelectionChanged(oldSelection);
        invalidate(InvalidationFlags.VISUAL);
    }

    private boolean keyHeld(int keyCode) {
        UIContext context = uiContext();
        return context != null && context.keyboard().isDown(keyCode);
    }

    /**
     * @return {@code true}, пока зажат любой из Ctrl
     */
    private boolean ctrlHeld() {
        return keyHeld(KeyCodes.LEFT_CONTROL) || keyHeld(KeyCodes.RIGHT_CONTROL);
    }

    /**
     * @return {@code true}, пока зажат любой из Shift
     */
    private boolean shiftHeld() {
        return keyHeld(KeyCodes.LEFT_SHIFT) || keyHeld(KeyCodes.RIGHT_SHIFT);
    }

    protected void handlePointerMoved(PointerMovedEvent pointer) {
        lastPointerRootX = pointer.rootX();
        lastPointerRootY = pointer.rootY();
        if (lassoState != null && lassoState.pointerId == pointer.pointerId()) {
            moveLasso(pointer);
            return;
        }
        if (connectionDragState != null && connectionDragState.pointerId == pointer.pointerId()) {
            moveConnectionDrag(pointer);
            return;
        }
        if (dragState != null && dragState.pointerId == pointer.pointerId()) {
            if (dragState.kind == DragKind.ITEM) {
                moveDraggedItem(pointer);
            } else if (dragState.kind == DragKind.PAN) {
                moveViewport(pointer);
            } else if (dragState.kind == DragKind.RESIZE) {
                resizeDraggedItem(pointer);
            } else if (dragState.kind == DragKind.REGION) {
                moveDraggedRegion(pointer);
            } else if (dragState.kind == DragKind.REGION_RESIZE) {
                resizeDraggedRegion(pointer);
            } else if (dragState.kind == DragKind.MINIMAP) {
                centerOnMinimap(dragState.minimap, pointer.rootX(), pointer.rootY());
            }
            return;
        }
        PortHit portHit = hitPort(pointer.rootX(), pointer.rootY());
        setHoveredPort(portHit);
        NodeGraphItem hoveredNode = portHit == null ? hitItem(pointer.rootX(), pointer.rootY()) : null;
        // Под нодой провод не подсвечивается: клик туда достанется ноде.
        setHoveredConnection(portHit == null && hoveredNode == null
                ? hitConnection(pointer.rootX(), pointer.rootY()) : null);
        setHoveredItem(hoveredNode);
        setHoveredRegion(portHit == null && hoveredNode == null ? hitRegionHeader(pointer.rootX(), pointer.rootY()) : null);
    }

    /**
     * @return регион, над заголовком которого сейчас курсор; для него показан комментарий
     */
    NodeGraphRegion hoveredRegion() {
        return hoveredRegion;
    }

    private void setHoveredRegion(NodeGraphRegion region) {
        if (hoveredRegion == region) return;
        hoveredRegion = region;
        invalidate(InvalidationFlags.VISUAL);
    }

    protected void handlePointerReleased(PointerReleasedEvent pointer, Event sourceEvent) {
        if (lassoState != null && lassoState.pointerId == pointer.pointerId()) {
            finishLasso(pointer, sourceEvent);
            return;
        }
        if (connectionDragState != null && connectionDragState.pointerId == pointer.pointerId()) {
            finishConnectionDrag(pointer, sourceEvent);
            return;
        }
        if (dragState == null || dragState.pointerId != pointer.pointerId()) return;
        DragState ended = dragState;
        dragState = null;
        UIContext context = uiContext();
        if (context != null) {
            context.releasePointer(pointer.pointerId(), this);
        }
        if (ended.kind == DragKind.ITEM) {
            dispatch(new NodeGraphItemMoveEndedEvent(this, ended.item.id(), ended.startX, ended.startY,
                    ended.item.x(), ended.item.y(), pointer.pointerId()));
        } else if (ended.kind == DragKind.RESIZE) {
            dispatch(new NodeGraphItemResizeEndedEvent(this, ended.item.id(), ended.startWidth, ended.startHeight,
                    ended.item.arrangedWidth(), ended.item.arrangedHeight(), pointer.pointerId()));
        }
        recordDragHistory(ended);
        if (ended.kind == DragKind.ITEM && ended.collapseTo != null
                && ended.item.x() == ended.startX && ended.item.y() == ended.startY) {
            // Клик без движения по ноде из выделенной группы оставляет выделенной только её.
            selectItem(ended.collapseTo);
        }
        setHoveredItem(hitItem(pointer.rootX(), pointer.rootY()));
        invalidate(InvalidationFlags.VISUAL);
        sourceEvent.cancel();
    }

    /**
     * Записывает в историю завершённое перетаскивание: перенос ноды, растягивание, перенос региона вместе с
     * нодами и растягивание региона. Промежуточные положения не записываются, шаг один на жест.
     */
    private void recordDragHistory(DragState ended) {
        switch (ended.kind) {
            case ITEM -> {
                NodeGraphItem[] members = ended.members;
                float[] from = ended.memberStart;
                float[] to = new float[from.length];
                boolean moved = false;
                for (int i = 0; i < members.length; i++) {
                    to[i * 2] = members[i].x();
                    to[i * 2 + 1] = members[i].y();
                    moved |= to[i * 2] != from[i * 2] || to[i * 2 + 1] != from[i * 2 + 1];
                }
                if (!moved) return;
                history.transact(members.length > 1 ? "Move nodes" : "Move node", () -> {
                    for (int i = 0; i < members.length; i++) {
                        NodeGraphItem member = members[i];
                        float fromX = from[i * 2];
                        float fromY = from[i * 2 + 1];
                        float toX = to[i * 2];
                        float toY = to[i * 2 + 1];
                        history.record(NodeGraphHistory.Operation.of(
                                () -> moveItemTo(member, fromX, fromY), () -> moveItemTo(member, toX, toY)));
                    }
                });
            }
            case RESIZE -> {
                NodeGraphItem item = ended.item;
                float fromW = ended.startWidth;
                float fromH = ended.startHeight;
                float toW = item.arrangedWidth();
                float toH = item.arrangedHeight();
                if (fromW == toW && fromH == toH) return;
                history.transact("Resize node", () -> history.record(NodeGraphHistory.Operation.of(
                        () -> resizeItemTo(item, fromW, fromH), () -> resizeItemTo(item, toW, toH))));
            }
            case REGION -> {
                NodeGraphRegion region = ended.region;
                NodeGraphItem[] members = ended.members;
                float[] fromItems = ended.memberStart;
                float[] toItems = new float[fromItems.length];
                for (int i = 0; i < members.length; i++) {
                    toItems[i * 2] = members[i].x();
                    toItems[i * 2 + 1] = members[i].y();
                }
                NodeGraphRegion[] nested = ended.regionMembers;
                float[] fromNested = ended.regionStart;
                float[] toNested = new float[fromNested.length];
                for (int i = 0; i < nested.length; i++) {
                    toNested[i * 2] = nested[i].x();
                    toNested[i * 2 + 1] = nested[i].y();
                }
                float fromX = ended.startX;
                float fromY = ended.startY;
                float toX = region.x();
                float toY = region.y();
                if (fromX == toX && fromY == toY) return;
                history.transact("Move region", () -> history.record(NodeGraphHistory.Operation.of(
                        () -> moveRegionWithItems(region, fromX, fromY, members, fromItems, nested, fromNested),
                        () -> moveRegionWithItems(region, toX, toY, members, toItems, nested, toNested))));
            }
            case REGION_RESIZE -> {
                NodeGraphRegion region = ended.region;
                float fromW = ended.startWidth;
                float fromH = ended.startHeight;
                float toW = region.width();
                float toH = region.height();
                if (fromW == toW && fromH == toH) return;
                history.transact("Resize region", () -> history.record(NodeGraphHistory.Operation.of(
                        () -> resizeRegionTo(region, fromW, fromH), () -> resizeRegionTo(region, toW, toH))));
            }
            default -> {
                // Панорамирование и миникарта в историю не попадают.
            }
        }
    }

    /**
     * Перемещает ноду и сообщает об этом слушателям; используется отменой и повтором.
     */
    void moveItemTo(NodeGraphItem item, float x, float y) {
        float oldX = item.x();
        float oldY = item.y();
        item.position(x, y);
        arrangeItems();
        if (oldX != item.x() || oldY != item.y()) {
            dispatch(new NodeGraphItemMovedEvent(this, item.id(), oldX, oldY, item.x(), item.y(), -1));
        }
    }

    private void resizeItemTo(NodeGraphItem item, float width, float height) {
        float oldWidth = item.arrangedWidth();
        float oldHeight = item.arrangedHeight();
        item.size(width, height);
        item.arrangedSize(width, height);
        arrangeItems();
        if (oldWidth != width || oldHeight != height) {
            dispatch(new NodeGraphItemResizedEvent(this, item.id(), oldWidth, oldHeight, width, height, -1));
        }
    }

    private void moveRegionWithItems(NodeGraphRegion region, float x, float y,
                                     NodeGraphItem[] members, float[] positions,
                                     NodeGraphRegion[] nested, float[] nestedPositions) {
        region.position(x, y);
        for (int i = 0; i < members.length; i++) {
            moveItemTo(members[i], positions[i * 2], positions[i * 2 + 1]);
        }
        for (int i = 0; i < nested.length; i++) {
            nested[i].position(nestedPositions[i * 2], nestedPositions[i * 2 + 1]);
            dispatch(new NodeGraphRegionChangedEvent(this, nested[i].id(), NodeGraphRegionChangedEvent.Kind.MOVED,
                    nested[i].x(), nested[i].y(), nested[i].width(), nested[i].height()));
        }
        arrangeItems();
        dispatch(new NodeGraphRegionChangedEvent(this, region.id(), NodeGraphRegionChangedEvent.Kind.MOVED,
                region.x(), region.y(), region.width(), region.height()));
    }

    private void resizeRegionTo(NodeGraphRegion region, float width, float height) {
        region.size(width, height);
        arrangeItems();
        dispatch(new NodeGraphRegionChangedEvent(this, region.id(), NodeGraphRegionChangedEvent.Kind.RESIZED,
                region.x(), region.y(), region.width(), region.height()));
    }

    /**
     * Передаёт нажатие модулю под курсором (стрелка сворачивания в заголовке).
     */
    private boolean pressItemModule(NodeGraphItem item, PointerPressedEvent pointer) {
        float localX = (pointer.rootX() - worldToRootX(item.x())) / zoom();
        float localY = (pointer.rootY() - worldToRootY(item.y())) / zoom();
        boolean[] handled = new boolean[1];
        history.transact(item.collapsed() ? "Expand node" : "Collapse node",
                () -> handled[0] = item.pressModule(localX, localY));
        return handled[0];
    }

    private void captureDragPointer(int pointerId) {
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointerId, this);
        }
    }

    private void startMinimapDrag(NodeGraphMinimap map, PointerPressedEvent pointer) {
        dragState = new DragState(DragKind.MINIMAP, null, pointer.pointerId(), pointer.rootX(), pointer.rootY(),
                0.0f, 0.0f, 0.0f, 0.0f, viewportX(), viewportY());
        dragState.minimap = map;
        captureDragPointer(pointer.pointerId());
        centerOnMinimap(map, pointer.rootX(), pointer.rootY());
    }

    /**
     * Центрирует видимое окно на точке миникарты.
     */
    private void centerOnMinimap(NodeGraphMinimap map, float rootX, float rootY) {
        if (map == null) return;
        float worldX = map.toWorldX(rootX);
        float worldY = map.toWorldY(rootY);
        viewport(layoutBounds().width() * 0.5f - worldX * zoom(),
                layoutBounds().height() * 0.5f - worldY * zoom());
    }

    /**
     * Начинает перетаскивание или растягивание региона. При перетаскивании запоминает ноды внутри региона:
     * они едут вместе с ним.
     */
    private void startRegionDrag(DragKind kind, NodeGraphRegion region, PointerPressedEvent pointer) {
        dragState = new DragState(kind, null, pointer.pointerId(), pointer.rootX(), pointer.rootY(),
                region.x(), region.y(), region.width(), region.height(), viewportX(), viewportY());
        dragState.region = region;
        if (kind == DragKind.REGION) {
            List<NodeGraphItem> members = region.containedItems(this);
            dragState.members = members.toArray(new NodeGraphItem[0]);
            dragState.memberStart = new float[members.size() * 2];
            for (int i = 0; i < dragState.members.length; i++) {
                dragState.memberStart[i * 2] = dragState.members[i].x();
                dragState.memberStart[i * 2 + 1] = dragState.members[i].y();
            }
            List<NodeGraphRegion> nested = region.containedRegions(this);
            dragState.regionMembers = nested.toArray(new NodeGraphRegion[0]);
            dragState.regionStart = new float[nested.size() * 2];
            for (int i = 0; i < dragState.regionMembers.length; i++) {
                dragState.regionStart[i * 2] = dragState.regionMembers[i].x();
                dragState.regionStart[i * 2 + 1] = dragState.regionMembers[i].y();
            }
        }
        captureDragPointer(pointer.pointerId());
        invalidate(InvalidationFlags.VISUAL);
    }

    private void moveDraggedRegion(PointerMovedEvent pointer) {
        NodeGraphRegion region = dragState.region;
        region.position(snap(dragState.startX + (pointer.rootX() - dragState.startRootX) / zoom()),
                snap(dragState.startY + (pointer.rootY() - dragState.startRootY) / zoom()));
        // Ноды едут на тот же сдвиг, что и притянутый к сетке регион, чтобы сохранить взаимное положение.
        float dx = region.x() - dragState.startX;
        float dy = region.y() - dragState.startY;
        for (int i = 0; i < dragState.members.length; i++) {
            NodeGraphItem member = dragState.members[i];
            float oldX = member.x();
            float oldY = member.y();
            member.position(dragState.memberStart[i * 2] + dx, dragState.memberStart[i * 2 + 1] + dy);
            if (oldX != member.x() || oldY != member.y()) {
                dispatch(new NodeGraphItemMovedEvent(this, member.id(), oldX, oldY, member.x(), member.y(), pointer.pointerId()));
            }
        }
        for (int i = 0; i < dragState.regionMembers.length; i++) {
            NodeGraphRegion nested = dragState.regionMembers[i];
            nested.position(dragState.regionStart[i * 2] + dx, dragState.regionStart[i * 2 + 1] + dy);
            dispatch(new NodeGraphRegionChangedEvent(this, nested.id(), NodeGraphRegionChangedEvent.Kind.MOVED,
                    nested.x(), nested.y(), nested.width(), nested.height()));
        }
        arrangeItems();
        dispatch(new NodeGraphRegionChangedEvent(this, region.id(), NodeGraphRegionChangedEvent.Kind.MOVED,
                region.x(), region.y(), region.width(), region.height()));
    }

    private void resizeDraggedRegion(PointerMovedEvent pointer) {
        NodeGraphRegion region = dragState.region;
        region.size(snap(dragState.startWidth + (pointer.rootX() - dragState.startRootX) / zoom()),
                snap(dragState.startHeight + (pointer.rootY() - dragState.startRootY) / zoom()));
        arrangeItems();
        dispatch(new NodeGraphRegionChangedEvent(this, region.id(), NodeGraphRegionChangedEvent.Kind.RESIZED,
                region.x(), region.y(), region.width(), region.height()));
    }

    /**
     * @return верхний регион, чья полоса заголовка под курсором
     */
    protected NodeGraphRegion hitRegionHeader(float rootX, float rootY) {
        List<NodeGraphRegion> ordered = regionsByArea();
        for (int i = ordered.size() - 1; i >= 0; i--) {
            NodeGraphRegion region = ordered.get(i);
            float x = worldToRootX(region.x());
            float y = worldToRootY(region.y());
            if (rootX >= x && rootX <= x + region.width() * zoom()
                    && rootY >= y && rootY <= y + NodeGraphRegion.HEADER_HEIGHT * zoom()) {
                return region;
            }
        }
        return null;
    }

    /**
     * @return выделенный регион, у которого курсор над правым нижним углом
     */
    private NodeGraphRegion hitRegionResizeHandle(float rootX, float rootY) {
        if (!resizeEnabled) return null;
        List<NodeGraphRegion> ordered = regionsByArea();
        for (int i = ordered.size() - 1; i >= 0; i--) {
            NodeGraphRegion region = ordered.get(i);
            if (!region.selected() || !region.resizable()) continue;
            float right = worldToRootX(region.x()) + region.width() * zoom();
            float bottom = worldToRootY(region.y()) + region.height() * zoom();
            if (rootX >= right - RESIZE_HANDLE_SIZE && rootX <= right
                    && rootY >= bottom - RESIZE_HANDLE_SIZE && rootY <= bottom) {
                return region;
            }
        }
        return null;
    }

    private void startItemDrag(NodeGraphItem item, PointerPressedEvent pointer) {
        dragState = new DragState(DragKind.ITEM, item, pointer.pointerId(), pointer.rootX(), pointer.rootY(),
                item.x(), item.y(), item.arrangedWidth(), item.arrangedHeight(), viewportX(), viewportY());
        // Выделенная нода тащит за собой всю выделенную группу; невыделенная едет одна.
        List<NodeGraphItem> group = new ArrayList<>();
        if (item.selected()) {
            for (NodeGraphItem other : itemSnapshot()) {
                if (other.selected() && other.movable()) group.add(other);
            }
        }
        if (!group.contains(item)) {
            group.clear();
            group.add(item);
        }
        dragState.members = group.toArray(new NodeGraphItem[0]);
        dragState.memberStart = new float[group.size() * 2];
        for (int i = 0; i < dragState.members.length; i++) {
            dragState.memberStart[i * 2] = dragState.members[i].x();
            dragState.memberStart[i * 2 + 1] = dragState.members[i].y();
        }
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointer.pointerId(), this);
        }
        dispatch(new NodeGraphItemMoveStartedEvent(this, item.id(), item.x(), item.y(), pointer.pointerId()));
        invalidate(InvalidationFlags.VISUAL);
    }

    private void startPan(PointerPressedEvent pointer) {
        dragState = new DragState(DragKind.PAN, null, pointer.pointerId(), pointer.rootX(), pointer.rootY(),
                0.0f, 0.0f, 0.0f, 0.0f, viewportX(), viewportY());
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointer.pointerId(), this);
        }
        invalidate(InvalidationFlags.VISUAL);
    }

    private void startResize(NodeGraphItem item, PointerPressedEvent pointer) {
        dragState = new DragState(DragKind.RESIZE, item, pointer.pointerId(), pointer.rootX(), pointer.rootY(),
                item.x(), item.y(), item.arrangedWidth(), item.arrangedHeight(), viewportX(), viewportY());
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointer.pointerId(), this);
        }
        dispatch(new NodeGraphItemResizeStartedEvent(this, item.id(), item.arrangedWidth(), item.arrangedHeight(), pointer.pointerId()));
        invalidate(InvalidationFlags.VISUAL);
    }

    private void startLasso(PointerPressedEvent pointer, boolean additive) {
        lassoState = new LassoState(pointer.pointerId(), pointer.rootX(), pointer.rootY(), pointer.rootX(), pointer.rootY());
        if (additive) {
            lassoState.base.addAll(selectedItemIds());
        }
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointer.pointerId(), this);
        }
        invalidate(InvalidationFlags.VISUAL);
    }

    private void startConnectionDrag(PortHit from, PointerPressedEvent pointer) {
        connectionDragState = new ConnectionDragState(from.item().id(), from.port().id(), pointer.pointerId(),
                from.x(), from.y(), pointer.rootX(), pointer.rootY(), false, "Missing target");
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointer.pointerId(), this);
        }
        dispatch(new NodeGraphConnectionDragStartedEvent(this, connectionDragState.from(), pointer.pointerId()));
        invalidate(InvalidationFlags.VISUAL);
    }

    private void moveConnectionDrag(PointerMovedEvent pointer) {
        PortHit target = hitPort(pointer.rootX(), pointer.rootY());
        ConnectionTargetValidation validation = validateConnectionDragTarget(target);
        connectionDragState.endRootX = pointer.rootX();
        connectionDragState.endRootY = pointer.rootY();
        connectionDragState.target = target == null ? new NodeGraphPortRef("", "") : target.ref();
        connectionDragState.valid = validation.validation.valid();
        connectionDragState.reason = validation.validation.reason();
        setHoveredPort(target);
        invalidate(InvalidationFlags.VISUAL);
    }

    private void finishConnectionDrag(PointerReleasedEvent pointer, Event sourceEvent) {
        ConnectionDragState ended = connectionDragState;
        PortHit target = hitPort(pointer.rootX(), pointer.rootY());
        ConnectionTargetValidation validation = validateConnectionDragTarget(target);
        NodeGraphConnection created = null;
        if (validation.validation.valid() && target != null) {
            NodeGraphConnection[] holder = new NodeGraphConnection[1];
            NodeGraphPortRef targetRef = target.ref();
            history.transact("Connect", () -> holder[0] = addConnection(normalizeConnectionId(""), ended.from(), targetRef));
            created = holder[0];
        }
        connectionDragState = null;
        UIContext context = uiContext();
        if (context != null) {
            context.releasePointer(pointer.pointerId(), this);
        }
        setHoveredPort(target);
        dispatch(new NodeGraphConnectionDragEndedEvent(this,
                ended.from(),
                target == null ? new NodeGraphPortRef("", "") : target.ref(),
                created == null ? "" : created.id(),
                validation.validation.valid(),
                validation.validation.reason(),
                pointer.pointerId()));
        invalidate(InvalidationFlags.VISUAL);
        sourceEvent.cancel();
        // Провод отпустили в пустом месте: предлагаем создать ноду с совместимым входом и сразу подключить её.
        if (target == null && hitItem(pointer.rootX(), pointer.rootY()) == null) {
            openNodePaletteFromPort(ended.from(), pointer.rootX(), pointer.rootY());
        }
    }

    private void moveDraggedItem(PointerMovedEvent pointer) {
        float nextX = snap(dragState.startX + (pointer.rootX() - dragState.startRootX) / zoom());
        float nextY = snap(dragState.startY + (pointer.rootY() - dragState.startRootY) / zoom());
        // Группа едет на сдвиг перетаскиваемой ноды, уже притянутой к сетке: взаимное положение сохраняется.
        float shiftX = nextX - dragState.startX;
        float shiftY = nextY - dragState.startY;
        for (int i = 0; i < dragState.members.length; i++) {
            NodeGraphItem member = dragState.members[i];
            float oldX = member.x();
            float oldY = member.y();
            member.position(dragState.memberStart[i * 2] + shiftX, dragState.memberStart[i * 2 + 1] + shiftY);
            if (oldX != member.x() || oldY != member.y()) {
                dispatch(new NodeGraphItemMovedEvent(this, member.id(), oldX, oldY, member.x(), member.y(), pointer.pointerId()));
            }
        }
        arrangeItems();
    }

    /**
     * Привязка работает, пока зажат Ctrl; включённый {@link #snapToGrid(boolean)} инвертирует это: тогда привязка
     * действует всегда, а Ctrl временно её отключает.
     */
    protected boolean snapActive() {
        return snapToGrid != ctrlHeld();
    }

    /**
     * Притягивает значение к сетке, если привязка активна.
     */
    protected float snap(float value) {
        if (!snapActive() || gridSize <= 0.0f) return value;
        return Math.round(value / gridSize) * gridSize;
    }

    private void moveViewport(PointerMovedEvent pointer) {
        float oldX = viewportX();
        float oldY = viewportY();
        float nextX = dragState.startViewportX + pointer.rootX() - dragState.startRootX;
        float nextY = dragState.startViewportY + pointer.rootY() - dragState.startRootY;
        if (!viewport.setPosition(nextX, nextY)) return;
        arrangeItems();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        dispatch(new NodeGraphViewportChangedEvent(this, oldX, oldY, zoom(), viewportX(), viewportY(), zoom()));
    }

    private void resizeDraggedItem(PointerMovedEvent pointer) {
        NodeGraphItem item = dragState.item;
        float oldWidth = item.arrangedWidth();
        float oldHeight = item.arrangedHeight();
        float scale = scaleContentWithZoom ? zoom() : 1.0f;
        float nextWidth = Math.max(MIN_RESIZE_SIZE, dragState.startWidth + (pointer.rootX() - dragState.startRootX) / scale);
        float nextHeight = Math.max(MIN_RESIZE_SIZE, dragState.startHeight + (pointer.rootY() - dragState.startRootY) / scale);
        item.size(nextWidth, nextHeight);
        item.arrangedSize(nextWidth, nextHeight);
        arrangeItems();
        if (oldWidth != nextWidth || oldHeight != nextHeight) {
            dispatch(new NodeGraphItemResizedEvent(this, item.id(), oldWidth, oldHeight, nextWidth, nextHeight, pointer.pointerId()));
        }
    }

    private void moveLasso(PointerMovedEvent pointer) {
        lassoState.endRootX = pointer.rootX();
        lassoState.endRootY = pointer.rootY();
        updateLassoSelection();
        invalidate(InvalidationFlags.VISUAL);
    }

    private void finishLasso(PointerReleasedEvent pointer, Event sourceEvent) {
        moveLasso(new PointerMovedEvent(this, pointer.rootX(), pointer.rootY(), pointer.localX(), pointer.localY(), pointer.pointerId()));
        lassoState = null;
        UIContext context = uiContext();
        if (context != null) {
            context.releasePointer(pointer.pointerId(), this);
        }
        invalidate(InvalidationFlags.VISUAL);
        sourceEvent.cancel();
    }

    private void updateLassoSelection() {
        if (lassoState == null || selectionMode == NodeGraphSelectionMode.NONE) return;
        Rect lasso = lassoRect();
        List<String> oldSelection = selectedItemIds();
        clearSelectionInternal(false);
        for (String id : lassoState.base) {
            NodeGraphItem kept = item(id);
            if (kept != null) kept.selectedInternal(true);
        }
        Object[] rawItems = items.elements();
        for (int itemIndex = 0, itemSize = items.size(); itemIndex < itemSize; itemIndex++) {
            NodeGraphItem item = (NodeGraphItem) rawItems[itemIndex];
            if (!item.visible() || !item.selectable()) continue;
            float x = worldToRootX(item.x());
            float y = worldToRootY(item.y());
            if (rectsIntersect(lasso.x(), lasso.y(), lasso.width(), lasso.height(),
                    x, y, itemScreenWidth(item), itemScreenHeight(item))) {
                item.selectedInternal(true);
                if (selectionMode == NodeGraphSelectionMode.SINGLE) break;
            }
        }
        emitSelectionChanged(oldSelection);
    }

    protected void handleScroll(ScrollEvent scroll, Event sourceEvent) {
        if (scroll.phase() == EventPhase.CAPTURE) return;

        boolean handled = false;
        boolean ctrlWheel = KeyModifiers.has(scroll.modifiers(), KeyModifiers.CONTROL);
        if (ctrlWheel) {
            if (zoomEnabled && scroll.deltaY() != 0.0f) {
                float factor = (float) Math.pow(1.1f, scroll.deltaY());
                zoomAt(scroll.rootX(), scroll.rootY(), factor);
            }
            handled = true;
        } else if (wheelPanningEnabled && (scroll.deltaX() != 0.0f || scroll.deltaY() != 0.0f)) {
            float deltaX = scroll.deltaX();
            float deltaY = scroll.deltaY();
            if (KeyModifiers.has(scroll.modifiers(), KeyModifiers.SHIFT) && deltaY != 0.0f) {
                deltaX = deltaY;
                deltaY = 0.0f;
            }

            float oldX = viewportX();
            float oldY = viewportY();
            if (viewport.panBy(deltaX * wheelPanStep, deltaY * wheelPanStep)) {
                arrangeItems();
                invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
                dispatch(new NodeGraphViewportChangedEvent(this, oldX, oldY, zoom(), viewportX(), viewportY(), zoom()));
                handled = true;
            }
        }

        if (handled || consumeWheelWhileHovered) {
            sourceEvent.cancel();
        }
    }

    private void zoomAt(float rootX, float rootY, float factor) {
        float oldX = viewportX();
        float oldY = viewportY();
        float oldZoom = zoom();

        float localX = rootX - layoutBounds().x();
        float localY = rootY - layoutBounds().y();
        if (!viewport.zoomAt(localX, localY, factor)) return;

        arrangeItems();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        dispatch(new NodeGraphViewportChangedEvent(this, oldX, oldY, oldZoom, viewportX(), viewportY(), zoom()));
    }

    protected void handleKeyPressed(KeyPressedEvent key, Event sourceEvent) {
        if (!keyboardEditingEnabled || key.phase() == EventPhase.CAPTURE) return;
        // Нажатия из поля ввода или кнопки внутри ноды графу не принадлежат: TextInput гасит только клавиши
        // редактирования, а буквы и пробел всплывают, и набор текста вызывал бы горячие клавиши графа.
        if (key.target() != this) return;

        boolean handled = false;
        boolean control = KeyModifiers.has(key.modifiers(), KeyModifiers.CONTROL);
        boolean shift = KeyModifiers.has(key.modifiers(), KeyModifiers.SHIFT);
        int code = key.keyCode();
        if (control && code == KeyCodes.A) {
            selectAllItems();
            handled = true;
        } else if (control && code == KeyCodes.Z) {
            handled = shift ? history.redo() : history.undo();
        } else if (control && code == KeyCodes.Y) {
            handled = history.redo();
        } else if (control && code == KeyCodes.C) {
            handled = !copySelection().isEmpty();
        } else if (control && code == KeyCodes.V) {
            handled = !pasteAtPointer().isEmpty();
        } else if (control && code == KeyCodes.D) {
            handled = !duplicateSelection().isEmpty();
        } else if (!control && code == KeyCodes.F) {
            frameSelection();
            handled = true;
        } else if (!control && code == KeyCodes.F2) {
            handled = openRegionEditor(selectedRegion(), RegionEditor.Mode.TITLE);
        } else if (!control && code == KeyCodes.HOME) {
            frameAll();
            handled = true;
        } else if (!control && code == KeyCodes.C) {
            handled = groupSelected() != null;
        } else if (!control && code == KeyCodes.R) {
            NodeGraphConnection selectedWire = firstSelectedConnection();
            handled = selectedWire != null && insertRerouteAtMidpoint(selectedWire) != null;
        } else if (!control && code == KeyCodes.SPACE) {
            handled = openNodePaletteAtPointer();
        } else if (key.keyCode() == KeyCodes.DELETE || key.keyCode() == KeyCodes.BACKSPACE) {
            history.transact("Delete", () -> {
                removeSelectedConnections();
                removeSelectedItems();
                NodeGraphRegion selectedRegion = selectedRegion();
                if (selectedRegion != null) removeRegion(selectedRegion);
            });
            handled = true;
        } else if (key.keyCode() == KeyCodes.LEFT || key.keyCode() == KeyCodes.RIGHT
                || key.keyCode() == KeyCodes.UP || key.keyCode() == KeyCodes.DOWN) {
            float step = KeyModifiers.has(key.modifiers(), KeyModifiers.SHIFT) ? 10.0f : 1.0f;
            float dx = key.keyCode() == KeyCodes.LEFT ? -step : key.keyCode() == KeyCodes.RIGHT ? step : 0.0f;
            float dy = key.keyCode() == KeyCodes.UP ? -step : key.keyCode() == KeyCodes.DOWN ? step : 0.0f;
            moveSelectedItems(dx, dy, -1);
            handled = true;
        }

        if (handled) {
            sourceEvent.cancel();
        }
    }

    private void selectAllItems() {
        List<String> oldSelection = selectedItemIds();
        clearConnectionSelection();
        selectRegion(null);
        clearSelectionInternal(false);
        if (selectionMode == NodeGraphSelectionMode.NONE) {
            emitSelectionChanged(oldSelection);
            return;
        }

        Object[] rawLogicalItems = logicalItems.elements();
        for (int logicalItemIndex = 0, logicalItemSize = logicalItems.size(); logicalItemIndex < logicalItemSize; logicalItemIndex++) {
            NodeGraphItem item = (NodeGraphItem) rawLogicalItems[logicalItemIndex];
            if (!item.selectable() || !item.visible()) continue;
            item.selectedInternal(true);
            if (selectionMode == NodeGraphSelectionMode.SINGLE) break;
        }
        emitSelectionChanged(oldSelection);
        invalidate(InvalidationFlags.VISUAL);
    }

    private void removeSelectedItems() {
        for (NodeGraphItem item : logicalItemSnapshot()) {
            if (item.selected()) {
                removeItem(item);
            }
        }
    }

    private void moveSelectedItems(float dx, float dy, int pointerId) {
        if (dx == 0.0f && dy == 0.0f) return;
        history.transact("Move nodes", () -> {
            for (NodeGraphItem item : itemSnapshot()) {
                if (!item.selected() || !item.movable()) continue;
                float oldX = item.x();
                float oldY = item.y();
                item.position(oldX + dx, oldY + dy);
                dispatch(new NodeGraphItemMovedEvent(this, item.id(), oldX, oldY, item.x(), item.y(), pointerId));
                float newX = item.x();
                float newY = item.y();
                history.record(NodeGraphHistory.Operation.of(
                        () -> moveItemTo(item, oldX, oldY), () -> moveItemTo(item, newX, newY)));
            }
            arrangeItems();
        });
    }

    protected NodeGraphItem hitItem(float rootX, float rootY) {
        for (int i = items.size() - 1; i >= 0; i--) {
            NodeGraphItem item = items.get(i);
            if (!item.visible()) continue;
            float x = worldToRootX(item.x());
            float y = worldToRootY(item.y());
            float width = itemScreenWidth(item);
            float height = itemScreenHeight(item);
            if (rootX >= x && rootX <= x + width && rootY >= y && rootY <= y + height) {
                return item;
            }
        }
        return null;
    }

    private NodeGraphItem hitResizeHandle(float rootX, float rootY) {
        if (!resizeEnabled) return null;
        for (int i = items.size() - 1; i >= 0; i--) {
            NodeGraphItem item = items.get(i);
            if (!item.visible() || !item.selected() || !item.resizable()) continue;
            float right = worldToRootX(item.x()) + itemScreenWidth(item);
            float bottom = worldToRootY(item.y()) + itemScreenHeight(item);
            float size = RESIZE_HANDLE_SIZE;
            if (rootX >= right - size && rootX <= right && rootY >= bottom - size && rootY <= bottom) {
                return item;
            }
        }
        return null;
    }

    private PortHit hitPort(float rootX, float rootY) {
        float hitRadius = screenPortHitRadius();
        float radiusSquared = hitRadius * hitRadius;
        for (int itemIndex = items.size() - 1; itemIndex >= 0; itemIndex--) {
            NodeGraphItem item = items.get(itemIndex);
            if (!item.visible()) continue;
            ObjectArrayList<NodeGraphPort> ports = item.rawPorts();
            Object[] rawPorts = ports.elements();
            for (int portIndex = 0, portSize = ports.size(); portIndex < portSize; portIndex++) {
                NodeGraphPort port = (NodeGraphPort) rawPorts[portIndex];
                if (!item.portShown(port)) continue;
                PortPoint point = portPoint(item, port);
                float dx = rootX - point.x();
                float dy = rootY - point.y();
                if (dx * dx + dy * dy <= radiusSquared) {
                    return new PortHit(item, port, point.x(), point.y());
                }
            }
        }
        return null;
    }

    protected NodeGraphConnection hitConnection(float rootX, float rootY) {
        for (int i = connections.size() - 1; i >= 0; i--) {
            NodeGraphConnection connection = connections.get(i);
            if (!wireCurve(connection)) continue;
            if (NodeGraphWires.distance(rootX, rootY, wirePoints, wireCount) <= CONNECTION_HIT_RADIUS) {
                return connection;
            }
        }
        return null;
    }

    private PortPoint portPoint(NodeGraphItem item, NodeGraphPort port) {
        if (item.modular() && port.anchored()) {
            return new PortPoint(worldToRootX(item.x()) + port.anchorX() * zoom(),
                    worldToRootY(item.y()) + port.anchorY() * zoom());
        }
        float width = itemScreenWidth(item);
        float height = itemScreenHeight(item);
        float x = worldToRootX(item.x());
        float y = worldToRootY(item.y());
        switch (port.side()) {
            case LEFT -> y += height * port.offset();
            case RIGHT -> {
                x += width;
                y += height * port.offset();
            }
            case TOP -> x += width * port.offset();
            case BOTTOM -> {
                x += width * port.offset();
                y += height;
            }
        }
        return new PortPoint(x, y);
    }

    private void setHoveredPort(PortHit portHit) {
        NodeGraphPortRef next = portHit == null ? new NodeGraphPortRef("", "") : portHit.ref();
        if (Objects.equals(hoveredPort, next)) return;
        hoveredPort = next;
        invalidate(InvalidationFlags.VISUAL);
    }

    private void setHoveredConnection(NodeGraphConnection connection) {
        String next = connection == null ? "" : connection.id();
        if (Objects.equals(hoveredConnectionId, next)) return;
        hoveredConnectionId = next;
        invalidate(InvalidationFlags.VISUAL);
    }

    private void setHoveredItem(NodeGraphItem item) {
        String next = item == null ? "" : item.id();
        if (Objects.equals(hoveredItemId, next)) return;
        hoveredItemId = next;
        invalidate(InvalidationFlags.VISUAL);
    }

    private void clearSelectionInternal(boolean includeUnselectable) {
        for (NodeGraphItem item : items) {
            if (includeUnselectable || item.selectable()) {
                item.selectedInternal(false);
            }
        }
    }

    private void keepOnlyFirstSelection() {
        boolean kept = false;
        for (NodeGraphItem item : logicalItems) {
            if (!item.selected()) continue;
            if (!kept && item.selectable()) {
                kept = true;
            } else {
                item.selectedInternal(false);
            }
        }
    }

    private void emitSelectionChanged(List<String> oldSelection) {
        List<String> next = selectedItemIds();
        if (Objects.equals(oldSelection, next)) return;
        dispatch(new NodeGraphItemSelectionChangedEvent(this, oldSelection, next));
    }

    private void clearConnectionSelectionInternal() {
        for (NodeGraphConnection connection : connections) {
            connection.selectedInternal(false);
        }
    }

    private void emitConnectionSelectionChanged(List<String> oldSelection) {
        List<String> next = selectedConnectionIds();
        if (Objects.equals(oldSelection, next)) return;
        dispatch(new NodeGraphConnectionSelectionChangedEvent(this, oldSelection, next));
    }

    protected void renderBackground(DrawScope draw) {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        if (width <= 0.0f || height <= 0.0f) return;
        draw.rect(x, y, width, height, Paint.fill(backgroundColor));
        drawGraphGrid(draw, x, y, width, height);
    }

    /**
     * Рисует регионы-комментарии под проводами и нодами: заливка, рамка, полоса заголовка и тексты.
     */
    protected void renderRegions(RenderContext context, DrawScope draw) {
        float z = zoom();
        List<NodeGraphRegion> ordered = regionsByArea();
        for (int i = 0, size = ordered.size(); i < size; i++) {
            NodeGraphRegion region = ordered.get(i);
            float x = worldToRootX(region.x());
            float y = worldToRootY(region.y());
            float width = region.width() * z;
            float height = region.height() * z;
            float header = NodeGraphRegion.HEADER_HEIGHT * z;
            MutableColor base = region.color();
            draw.rect(x, y, width, height, Paint.fill(new MutableColor(base.r(), base.g(), base.b(), 0.14f)));
            draw.rect(x, y, width, header, Paint.fill(new MutableColor(base.r(), base.g(), base.b(), 0.42f)));
            draw.rect(x, y, width, height, Paint.stroke(
                    region.selected() ? selectedItemBorderColor : new MutableColor(base.r(), base.g(), base.b(), 0.75f),
                    region.selected() ? 2.0f : 1.0f));
            if (region.selected() && region.resizable()) {
                float handle = 8.0f * Math.max(0.75f, z);
                draw.rect(x + width - handle, y + height - handle, handle, handle, Paint.fill(resizeHandleColor));
            }
            if (!region.comment().isEmpty()) drawCommentIcon(draw, x, y, width, header, z, region == hoveredRegion);
            DrawList drawList = context.drawList();
            int sizeBefore = drawList.size();
            for (Widget widget : region.widgets()) {
                if (widget.visibility() == Visibility.VISIBLE) renderChildWithInheritedTransform(context, widget);
            }
            if (scaleContentWithZoom && z != 1.0f) {
                applyContentZoom(drawList.commands(), sizeBefore, x, y, z);
            }
        }
    }

    /**
     * Значок «есть комментарий» в правой части заголовка региона: белый пузырь с тремя точками.
     */
    protected void drawCommentIcon(DrawScope draw, float x, float y, float width, float header, float z, boolean lit) {
        float w = 13.0f * z;
        float h = 9.0f * z;
        float left = x + width - w - 8.0f * z;
        float top = y + (header - h - 2.0f * z) * 0.5f;
        MutableColor fill = new MutableColor(1.0f, 1.0f, 1.0f, lit ? 1.0f : 0.78f);
        draw.roundedRect(left, top, w, h, 2.5f * z, Paint.fill(fill));
        draw.rect(left + 2.5f * z, top + h - 0.5f * z, 3.0f * z, 2.5f * z, Paint.fill(fill));
        MutableColor dot = new MutableColor(0.12f, 0.14f, 0.18f, 0.9f);
        float dotSize = Math.max(1.0f, 1.6f * z);
        for (int i = 0; i < 3; i++) {
            draw.rect(left + (2.5f + i * 3.4f) * z, top + (h - dotSize) * 0.5f, dotSize, dotSize, Paint.fill(dot));
        }
    }

    /**
     * Рисует поверх всего комментарий региона, над заголовком которого сейчас курсор.
     */
    protected void renderRegionComment(RenderContext context, DrawScope draw) {
        NodeGraphRegion region = hoveredRegion;
        if (region == null || region.comment().isEmpty() || dragState != null || lassoState != null
                || connectionDragState != null || !regions.contains(region)) {
            return;
        }
        float z = zoom();
        boolean scaled = scaleContentWithZoom && z != 1.0f;
        float s = scaleContentWithZoom ? z : 1.0f;
        float boxWidth = Math.max(COMMENT_MIN_WIDTH, Math.min(COMMENT_MAX_WIDTH, region.width()));
        Widget label = region.commentLabel();
        label.measure(new LayoutContext(boxWidth - COMMENT_PADDING * 2.0f, Float.POSITIVE_INFINITY));
        float textHeight = Math.max(14.0f, label.desiredSize().height());
        float boxHeight = textHeight + COMMENT_PADDING * 2.0f;
        float x = worldToRootX(region.x());
        float y = worldToRootY(region.y()) + (NodeGraphRegion.HEADER_HEIGHT + 4.0f) * z;
        MutableColor base = region.color();
        draw.roundedRect(x, y, boxWidth * s, boxHeight * s, 4.0f * s,
                Paint.fill(new MutableColor(0.09f, 0.10f, 0.12f, 0.96f)));
        draw.roundedRect(x, y, boxWidth * s, boxHeight * s, 4.0f * s,
                Paint.stroke(new MutableColor(base.r(), base.g(), base.b(), 0.9f), 1.0f));
        StackPanel.arrangeChild(label, x + COMMENT_PADDING, y + COMMENT_PADDING,
                boxWidth - COMMENT_PADDING * 2.0f, textHeight);
        DrawList drawList = context.drawList();
        int sizeBefore = drawList.size();
        if (label.visibility() == Visibility.VISIBLE) renderChildWithInheritedTransform(context, label);
        if (scaled) applyContentZoom(drawList.commands(), sizeBefore, x, y, z);
    }

    /**
     * @return регионы от больших к малым: вложенный регион всегда рисуется и ловит курсор поверх внешнего
     */
    private List<NodeGraphRegion> regionsByArea() {
        List<NodeGraphRegion> sorted = new ArrayList<>(regions);
        sorted.sort((a, b) -> Float.compare(b.width() * b.height(), a.width() * a.height()));
        return sorted;
    }

    /**
     * Рисует все провода кривыми Безье под нодами.
     */
    protected void renderConnections(DrawScope draw) {
        Object[] raw = connections.elements();
        for (int i = 0, size = connections.size(); i < size; i++) {
            NodeGraphConnection connection = (NodeGraphConnection) raw[i];
            NodeGraphPort fromPort = port(connection.from());
            if (fromPort == null || !wireCurve(connection)) continue;
            boolean highlighted = connection.selected() || Objects.equals(hoveredConnectionId, connection.id());
            String effectiveType = NodeGraphEditing.effectiveType(this, fromPort);
            PortType type = portTypes.get(effectiveType);
            ColorView color;
            if (!connection.enabled()) {
                color = new MutableColor(0.5f, 0.5f, 0.5f, 0.35f);
            } else if (connection.selected()) {
                color = selectedConnectionColor;
            } else if (effectiveType.isEmpty()) {
                color = connectionColor;
            } else if (highlighted) {
                color = brighter(type.color());
            } else {
                color = type.color();
            }
            float thickness = (type.flow() ? 2.5f : 2.0f) * Math.max(0.7f, Math.min(1.6f, zoom()));
            if (highlighted) thickness += 1.0f;
            NodeGraphWires.stroke(draw, wirePoints, wireCount, color, thickness);
        }
    }

    private static MutableColor brighter(ColorView color) {
        return new MutableColor(Math.min(1.0f, color.r() + 0.25f), Math.min(1.0f, color.g() + 0.25f),
                Math.min(1.0f, color.b() + 0.25f), color.a());
    }

    /**
     * Строит полилинию провода в {@code wirePoints}; возвращает {@code false}, если порт не найден.
     */
    private boolean wireCurve(NodeGraphConnection connection) {
        NodeGraphItem fromItem = item(connection.fromItemId());
        NodeGraphItem toItem = item(connection.toItemId());
        NodeGraphPort fromPort = port(connection.from());
        NodeGraphPort toPort = port(connection.to());
        if (fromItem == null || toItem == null || fromPort == null || toPort == null) return false;
        PortPoint from = portPoint(fromItem, fromPort);
        PortPoint to = portPoint(toItem, toPort);
        wireCount = NodeGraphWires.build(from.x(), from.y(), fromPort.side(), to.x(), to.y(), toPort.side(),
                zoom(), wirePoints);
        return true;
    }

    /**
     * Рисует модульную ноду целиком: тень, тело, фон модулей, виджеты, рамку и порты.
     */
    protected void renderModularItem(RenderContext context, DrawScope draw, NodeGraphItem item) {
        float z = zoom();
        float x = worldToRootX(item.x());
        float y = worldToRootY(item.y());
        float width = item.arrangedWidth() * z;
        float height = item.arrangedHeight() * z;
        RectView bounds = layoutBounds();
        if (x > bounds.x() + bounds.width() || x + width < bounds.x()
                || y > bounds.y() + bounds.height() || y + height < bounds.y()) {
            return;
        }
        NodeStyle style = item.style();
        float radius = style.cornerRadius() * z;
        boolean hovered = Objects.equals(hoveredItemId, item.id());
        boolean dragging = dragState != null && dragState.kind == DragKind.ITEM && dragState.item == item;
        draw.roundedRect(x + 2.0f * z, y + 3.0f * z, width, height, radius, Paint.fill(shadowColor));
        draw.roundedRect(x, y, width, height, radius, Paint.fill(style.body()));

        itemPaint.bind(draw, item, x, y, hovered || dragging);
        for (int i = 0, count = item.moduleCount(); i < count; i++) {
            if (!item.moduleShown(i)) continue;
            item.moduleAt(i).paintBackground(itemPaint, 0.0f, item.moduleTop(i),
                    item.arrangedWidth(), item.moduleHeight(i));
        }

        DrawList drawList = context.drawList();
        int sizeBefore = drawList.size();
        for (Widget widget : item.hostedWidgets()) {
            if (widget.visibility() == Visibility.VISIBLE) renderChildWithInheritedTransform(context, widget);
        }
        if (z != 1.0f) applyContentZoom(drawList.commands(), sizeBefore, x, y, z);

        ColorView border = item.selected() ? selectedItemBorderColor
                : hovered || dragging ? hoveredItemBorderColor
                : item.issueLevel() == 2 ? errorBorderColor
                : item.issueLevel() == 1 ? warningBorderColor : style.border();
        boolean emphasized = item.selected() || dragging || item.issueLevel() > 0;
        draw.roundedRect(x, y, width, height, radius, Paint.stroke(border, emphasized ? 2.0f : 1.0f));

        NodeGraphPortRef hoveredRef = hoveredPort;
        for (NodeGraphPort port : item.ports()) {
            if (!item.portShown(port)) continue;
            PortPoint point = portPoint(item, port);
            drawPort(draw, item, port, point.x(), point.y(),
                    hoveredRef.itemId().equals(item.id()) && hoveredRef.portId().equals(port.id()));
        }
    }

    /**
     * Рисует маркер порта: форма и цвет по {@link PortType}, заливка у подключённого, контур у свободного.
     */
    protected void drawPort(DrawScope draw, NodeGraphItem item, NodeGraphPort port, float x, float y, boolean hovered) {
        PortType type = portTypes.get(NodeGraphEditing.effectiveType(this, port));
        float radius = screenPortRadius() * (hovered ? 1.25f : 1.0f);
        ColorView color = hovered ? brighter(type.color()) : type.color();
        ColorView hole = item.modular() ? item.style().body() : backgroundColor;
        boolean filled = port.connected();
        switch (type.shape()) {
            case SQUARE -> {
                if (filled) {
                    draw.rect(x - radius, y - radius, radius * 2.0f, radius * 2.0f, Paint.fill(color));
                } else {
                    draw.rect(x - radius, y - radius, radius * 2.0f, radius * 2.0f, Paint.fill(hole));
                    draw.rect(x - radius, y - radius, radius * 2.0f, radius * 2.0f, Paint.stroke(color, 1.5f));
                }
            }
            case DIAMOND -> polygonPort(draw, x, y, radius * 1.25f, color, hole, filled, 4);
            case TRIANGLE -> polygonPort(draw, x, y, radius * 1.2f, color, hole, filled, 3);
            default -> {
                if (filled) {
                    draw.addCircleFilled(x, y, radius, color, 16);
                } else {
                    draw.addCircleFilled(x, y, radius, hole, 16);
                    draw.addCircle(x, y, radius, color, 16, 1.5f);
                }
            }
        }
    }

    private static void polygonPort(DrawScope draw, float x, float y, float radius,
                                    ColorView color, ColorView hole, boolean filled, int corners) {
        polygonPath(draw, x, y, radius, corners);
        draw.pathFillConvex(filled ? color : hole);
        if (!filled) {
            polygonPath(draw, x, y, radius, corners);
            draw.pathStroke(color, true, 1.5f);
        }
    }

    private static void polygonPath(DrawScope draw, float x, float y, float radius, int corners) {
        draw.pathClear();
        // Ромб и треугольник направлены вправо, как стрелка вывода.
        double start = corners == 3 ? 0.0 : Math.PI * 0.5;
        for (int i = 0; i < corners; i++) {
            double angle = start + Math.PI * 2.0 * i / corners;
            draw.pathLineTo(x + (float) Math.cos(angle) * radius, y + (float) Math.sin(angle) * radius);
        }
    }

    /**
     * Рисует миникарту: обзор нод и регионов и рамку видимого окна.
     */
    protected void renderMinimap(DrawScope draw) {
        NodeGraphMinimap map = minimapGeometry();
        if (map == null) return;
        draw.roundedRect(map.x, map.y, map.width, map.height, 4.0f, Paint.fill(minimapBackgroundColor));
        for (int i = 0, size = regions.size(); i < size; i++) {
            NodeGraphRegion region = regions.get(i);
            MutableColor base = region.color();
            draw.rect(map.toScreenX(region.x()), map.toScreenY(region.y()),
                    map.scale(region.width()), map.scale(region.height()),
                    Paint.fill(new MutableColor(base.r(), base.g(), base.b(), 0.35f)));
        }
        for (NodeGraphItem item : items) {
            if (!item.visible()) continue;
            ColorView color = item.modular() ? item.style().header() : itemBorderColor;
            draw.rect(map.toScreenX(item.x()), map.toScreenY(item.y()),
                    Math.max(2.0f, map.scale(item.arrangedWidth())), Math.max(2.0f, map.scale(item.arrangedHeight())),
                    Paint.fill(color));
        }
        float viewX = map.toScreenX(-viewportX() / zoom());
        float viewY = map.toScreenY(-viewportY() / zoom());
        draw.rect(viewX, viewY, map.scale(layoutBounds().width() / zoom()), map.scale(layoutBounds().height() / zoom()),
                Paint.stroke(minimapViewportColor, 1.0f));
        draw.roundedRect(map.x, map.y, map.width, map.height, 4.0f, Paint.stroke(minimapBorderColor, 1.0f));
    }

    /**
     * @return геометрия миникарты на этот кадр или {@code null}, если она выключена или холст пуст
     */
    private NodeGraphMinimap minimapGeometry() {
        if (!minimapEnabled) return null;
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (NodeGraphItem item : items) {
            if (!item.visible()) continue;
            minX = Math.min(minX, item.x());
            minY = Math.min(minY, item.y());
            maxX = Math.max(maxX, item.x() + item.arrangedWidth());
            maxY = Math.max(maxY, item.y() + item.arrangedHeight());
        }
        for (int i = 0, size = regions.size(); i < size; i++) {
            NodeGraphRegion region = regions.get(i);
            minX = Math.min(minX, region.x());
            minY = Math.min(minY, region.y());
            maxX = Math.max(maxX, region.x() + region.width());
            maxY = Math.max(maxY, region.y() + region.height());
        }
        float viewLeft = -viewportX() / zoom();
        float viewTop = -viewportY() / zoom();
        minX = Math.min(Float.isFinite(minX) ? minX : viewLeft, viewLeft);
        minY = Math.min(Float.isFinite(minY) ? minY : viewTop, viewTop);
        maxX = Math.max(Float.isFinite(maxX) ? maxX : viewLeft, viewLeft + layoutBounds().width() / zoom());
        maxY = Math.max(Float.isFinite(maxY) ? maxY : viewTop, viewTop + layoutBounds().height() / zoom());
        float width = Math.min(minimapWidth, layoutBounds().width() - minimapMargin * 2.0f);
        float height = Math.min(minimapHeight, layoutBounds().height() - minimapMargin * 2.0f);
        if (width <= 8.0f || height <= 8.0f) return null;
        float x = layoutBounds().x() + layoutBounds().width() - minimapMargin - width;
        float y = layoutBounds().y() + layoutBounds().height() - minimapMargin - height;
        return new NodeGraphMinimap(x, y, width, height, minX, minY, maxX, maxY);
    }

    protected void renderForeground(DrawScope draw) {
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        if (width <= 0.0f || height <= 0.0f) return;
        for (NodeGraphItem item : items) {
            if (!item.visible() || item.modular()) continue;
            float itemX = worldToRootX(item.x());
            float itemY = worldToRootY(item.y());
            float itemWidth = itemScreenWidth(item);
            float itemHeight = itemScreenHeight(item);
            boolean draggingItem = dragState != null && dragState.kind == DragKind.ITEM && dragState.item == item;
            boolean hoveredItem = Objects.equals(hoveredItemId, item.id());
            ColorView color = item.selected()
                    ? selectedItemBorderColor
                    : hoveredItem || draggingItem
                    ? hoveredItemBorderColor
                    : itemBorderColor;
            draw.roundedRect(itemX, itemY, itemWidth, itemHeight, 4.0f,
                    Paint.stroke(color, item.selected() || draggingItem ? 2.0f : 1.0f));
            if (item.selected() && item.resizable()) {
                float handle = 8.0f;
                draw.rect(itemX + itemWidth - handle, itemY + itemHeight - handle,
                        handle, handle, Paint.fill(resizeHandleColor));
            }
        }
        if (connectionDragState != null) {
            NodeGraphPort dragPort = port(connectionDragState.from());
            NodeGraphPortSide startSide = dragPort == null ? NodeGraphPortSide.RIGHT : dragPort.side();
            wireCount = NodeGraphWires.build(connectionDragState.startRootX, connectionDragState.startRootY, startSide,
                    connectionDragState.endRootX, connectionDragState.endRootY, NodeGraphWires.opposite(startSide),
                    zoom(), wirePoints);
            NodeGraphWires.stroke(draw, wirePoints, wireCount,
                    connectionDragState.valid ? connectionPreviewColor : invalidConnectionPreviewColor,
                    connectionDragState.valid ? 2.5f : 2.0f);
        }
        float portRadius = screenPortRadius();
        for (NodeGraphItem item : items) {
            if (!item.visible() || item.modular()) continue;
            ObjectArrayList<NodeGraphPort> ports = item.rawPorts();
            Object[] rawPorts = ports.elements();
            for (int portIndex = 0, portSize = ports.size(); portIndex < portSize; portIndex++) {
                NodeGraphPort port = (NodeGraphPort) rawPorts[portIndex];
                if (!port.visible()) continue;
                PortPoint point = portPoint(item, port);
                boolean hoveredPort = hoveredPort().equals(new NodeGraphPortRef(item.id(), port.id()));
                ColorView color = hoveredPort ? hoveredPortColor : portColor;
                draw.addCircleFilled(point.x(), point.y(), portRadius, color, 12);
                // Зазор кольца масштабируется вместе с портом, иначе при отдалении порт «толстеет» относительно ноды.
                draw.addCircle(point.x(), point.y(), portRadius + nodeContentScale(),
                        canStartConnection(port) || canEndConnection(port) ? connectionPreviewColor : itemBorderColor,
                        12,
                        hoveredPort ? 1.5f : 1.0f);
            }
        }
        if (lassoState != null) {
            Rect rect = lassoRect();
            draw.rect(rect.x(), rect.y(), rect.width(), rect.height(),
                    Paint.fill(selectionBoxFillColor));
            draw.rect(rect.x(), rect.y(), rect.width(), rect.height(),
                    Paint.stroke(selectionBoxBorderColor, 1.0f));
        }
        renderMinimap(draw);
    }

    protected void drawGraphGrid(DrawScope draw, float x, float y, float width, float height) {
        float step = Math.max(2.0f, gridSize * zoom());
        float majorStep = step * 4.0f;
        float startX = x + positiveModulo(viewportX(), step);
        float startY = y + positiveModulo(viewportY(), step);

        for (float gx = startX; gx <= x + width; gx += step) {
            draw.line(gx, y, gx, y + height, Paint.stroke(gridColor, 1.0f));
        }
        for (float gy = startY; gy <= y + height; gy += step) {
            draw.line(x, gy, x + width, gy, Paint.stroke(gridColor, 1.0f));
        }

        float majorStartX = x + positiveModulo(viewportX(), majorStep);
        float majorStartY = y + positiveModulo(viewportY(), majorStep);
        for (float gx = majorStartX; gx <= x + width; gx += majorStep) {
            draw.line(gx, y, gx, y + height, Paint.stroke(majorGridColor, 1.0f));
        }
        for (float gy = majorStartY; gy <= y + height; gy += majorStep) {
            draw.line(x, gy, x + width, gy, Paint.stroke(majorGridColor, 1.0f));
        }
    }

    private static float positiveModulo(float value, float modulo) {
        if (modulo <= 0.0f) return 0.0f;
        float result = value % modulo;
        return result < 0.0f ? result + modulo : result;
    }

    protected <T extends Event> T dispatch(T event) {
        UIContext context = uiContext();
        if (context == null || !(event instanceof dev.sixik.unigui.api.event.WidgetEvent widgetEvent)) {
            emit(event);
        } else {
            context.routedEvents().dispatch(widgetEvent);
        }
        return event;
    }

    protected float viewportX() {
        return viewport.x();
    }

    protected float viewportY() {
        return viewport.y();
    }

    protected float zoom() {
        return viewport.zoom();
    }

    protected float worldToRootX(float worldX) {
        return layoutBounds().x() + viewport.worldToScreenX(worldX);
    }

    protected float worldToRootY(float worldY) {
        return layoutBounds().y() + viewport.worldToScreenY(worldY);
    }

    private float itemScreenWidth(NodeGraphItem item) {
        if (item == null) return 0.0f;
        return item.arrangedWidth() * (scaleContentWithZoom ? zoom() : 1.0f);
    }

    private float itemScreenHeight(NodeGraphItem item) {
        if (item == null) return 0.0f;
        return item.arrangedHeight() * (scaleContentWithZoom ? zoom() : 1.0f);
    }

    private float screenPortRadius() {
        return PORT_RADIUS * nodeContentScale();
    }

    private float screenPortHitRadius() {
        return PORT_HIT_RADIUS * nodeContentScale();
    }

    private float nodeContentScale() {
        return scaleContentWithZoom ? zoom() : 1.0f;
    }

    protected boolean canStartConnection(NodeGraphPort port) {
        return port != null && port.enabled() && port.visible()
                && (port.kind() == NodeGraphPortKind.OUTPUT || port.kind() == NodeGraphPortKind.BIDIRECTIONAL);
    }

    protected boolean canEndConnection(NodeGraphPort port) {
        return port != null && port.enabled() && port.visible()
                && (port.kind() == NodeGraphPortKind.INPUT || port.kind() == NodeGraphPortKind.BIDIRECTIONAL);
    }

    private ConnectionTargetValidation validateConnectionDragTarget(PortHit target) {
        if (connectionDragState == null) {
            return new ConnectionTargetValidation(NodeGraphConnectionValidation.invalid("No active connection drag"));
        }
        if (target == null) {
            return new ConnectionTargetValidation(NodeGraphConnectionValidation.invalid("Missing target"));
        }
        return new ConnectionTargetValidation(validateConnection(connectionDragState.from(), target.ref()));
    }

    private String normalizeConnectionId(String id) {
        String normalized = id == null ? "" : id.trim();
        if (!normalized.isEmpty()) return normalized;
        String generated;
        do {
            generated = "connection-" + nextConnectionId++;
        } while (connection(generated) != null);
        return generated;
    }

    private static float distanceToSegment(float px, float py, float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        float lengthSquared = dx * dx + dy * dy;
        if (lengthSquared <= 0.0001f) {
            float pointDx = px - ax;
            float pointDy = py - ay;
            return (float) Math.sqrt(pointDx * pointDx + pointDy * pointDy);
        }
        float t = ((px - ax) * dx + (py - ay) * dy) / lengthSquared;
        t = Math.max(0.0f, Math.min(1.0f, t));
        float nearestX = ax + t * dx;
        float nearestY = ay + t * dy;
        float pointDx = px - nearestX;
        float pointDy = py - nearestY;
        return (float) Math.sqrt(pointDx * pointDx + pointDy * pointDy);
    }

    private Rect lassoRect() {
        if (lassoState == null) return new Rect(0.0f, 0.0f, 0.0f, 0.0f);
        float x = Math.min(lassoState.startRootX, lassoState.endRootX);
        float y = Math.min(lassoState.startRootY, lassoState.endRootY);
        return new Rect(x, y, Math.abs(lassoState.endRootX - lassoState.startRootX),
                Math.abs(lassoState.endRootY - lassoState.startRootY));
    }

    private static boolean rectsIntersect(float ax, float ay, float aw, float ah,
                                          float bx, float by, float bw, float bh) {
        return ax <= bx + bw && ax + aw >= bx && ay <= by + bh && ay + ah >= by;
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float clamp(float value, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private enum DragKind {
        ITEM,
        PAN,
        RESIZE,
        REGION,
        REGION_RESIZE,
        MINIMAP
    }

    /**
     * Раскладка модулей ноды: мост между {@link NodeModule} и геометрией графа.
     */
    private final class ItemLayout implements NodeLayoutContext {
        private NodeGraphItem item;

        @Override
        public NodeGraphItem item() {
            return item;
        }

        @Override
        public NodeStyle style() {
            return item.style();
        }

        @Override
        public float preferredWidth(Widget widget) {
            return StackPanel.preferredWidth(widget, 0.0f);
        }

        @Override
        public float preferredHeight(Widget widget) {
            return StackPanel.preferredHeight(widget, 0.0f);
        }

        @Override
        public void place(Widget widget, float x, float y, float width, float height) {
            // Виджеты раскладываются в world-единицах от угла ноды; зум накладывается при рендере.
            StackPanel.arrangeChild(widget, worldToRootX(item.x()) + x, worldToRootY(item.y()) + y, width, height);
        }

        @Override
        public void anchor(NodeGraphPort port, float x, float y) {
            port.anchor(x, y);
        }
    }

    /**
     * Контекст рисования декора модулей: перевод node-local координат в экранные.
     */
    private final class ItemPaint implements NodePaintContext {
        private DrawScope draw;
        private NodeGraphItem item;
        private float originX;
        private float originY;
        private boolean hovered;

        private void bind(DrawScope draw, NodeGraphItem item, float originX, float originY, boolean hovered) {
            this.draw = draw;
            this.item = item;
            this.originX = originX;
            this.originY = originY;
            this.hovered = hovered;
        }

        @Override
        public DrawScope draw() {
            return draw;
        }

        @Override
        public NodeGraphItem item() {
            return item;
        }

        @Override
        public NodeStyle style() {
            return item.style();
        }

        @Override
        public float zoom() {
            return NodeGraph.this.zoom();
        }

        @Override
        public boolean hovered() {
            return hovered;
        }

        @Override
        public boolean selected() {
            return item.selected();
        }

        @Override
        public float x(float localX) {
            return originX + localX * NodeGraph.this.zoom();
        }

        @Override
        public float y(float localY) {
            return originY + localY * NodeGraph.this.zoom();
        }

        @Override
        public float scale(float length) {
            return length * NodeGraph.this.zoom();
        }
    }

    private static final class DragState {
        /**
         * Нода, к которой схлопнется выделение группы, если мышь отпустили без движения.
         */
        private NodeGraphItem collapseTo;
        private NodeGraphRegion region;
        private NodeGraphItem[] members = new NodeGraphItem[0];
        private float[] memberStart = new float[0];
        private NodeGraphRegion[] regionMembers = new NodeGraphRegion[0];
        private float[] regionStart = new float[0];
        private NodeGraphMinimap minimap;
        private final DragKind kind;
        private final NodeGraphItem item;
        private final int pointerId;
        private final float startRootX;
        private final float startRootY;
        private final float startX;
        private final float startY;
        private final float startWidth;
        private final float startHeight;
        private final float startViewportX;
        private final float startViewportY;

        private DragState(DragKind kind, NodeGraphItem item, int pointerId, float startRootX, float startRootY,
                          float startX, float startY, float startWidth, float startHeight,
                          float startViewportX, float startViewportY) {
            this.kind = kind;
            this.item = item;
            this.pointerId = pointerId;
            this.startRootX = startRootX;
            this.startRootY = startRootY;
            this.startX = startX;
            this.startY = startY;
            this.startWidth = startWidth;
            this.startHeight = startHeight;
            this.startViewportX = startViewportX;
            this.startViewportY = startViewportY;
        }
    }

    private static final class LassoState {
        /**
         * Ноды, выделенные до рамки при добавлении через Shift: они остаются выделенными.
         */
        private final java.util.Set<String> base = new java.util.LinkedHashSet<>();
        private final int pointerId;
        private final float startRootX;
        private final float startRootY;
        private float endRootX;
        private float endRootY;

        private LassoState(int pointerId, float startRootX, float startRootY, float endRootX, float endRootY) {
            this.pointerId = pointerId;
            this.startRootX = startRootX;
            this.startRootY = startRootY;
            this.endRootX = endRootX;
            this.endRootY = endRootY;
        }
    }

    private static final class ConnectionDragState {
        private final String fromItemId;
        private final String fromPortId;
        private final int pointerId;
        private final float startRootX;
        private final float startRootY;
        private float endRootX;
        private float endRootY;
        private NodeGraphPortRef target;
        private boolean valid;
        private String reason;

        private ConnectionDragState(String fromItemId, String fromPortId, int pointerId,
                                    float startRootX, float startRootY,
                                    float endRootX, float endRootY,
                                    boolean valid, String reason) {
            this.fromItemId = fromItemId == null ? "" : fromItemId;
            this.fromPortId = fromPortId == null ? "" : fromPortId;
            this.pointerId = pointerId;
            this.startRootX = startRootX;
            this.startRootY = startRootY;
            this.endRootX = endRootX;
            this.endRootY = endRootY;
            this.target = new NodeGraphPortRef("", "");
            this.valid = valid;
            this.reason = reason == null ? "" : reason;
        }

        private NodeGraphPortRef from() {
            return new NodeGraphPortRef(fromItemId, fromPortId);
        }
    }

    private record PortHit(NodeGraphItem item, NodeGraphPort port, float x, float y) {
        private NodeGraphPortRef ref() {
            return new NodeGraphPortRef(item.id(), port.id());
        }
    }

    private record PortPoint(float x, float y) {
    }

    private record ConnectionTargetValidation(NodeGraphConnectionValidation validation) {
    }

    private record Rect(float x, float y, float width, float height) {
    }
}
