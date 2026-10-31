package dev.sixik.unigui.widgets.graph;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.NodeGraphConnectionCreatedEvent;
import dev.sixik.unigui.api.event.NodeGraphConnectionDragEndedEvent;
import dev.sixik.unigui.api.event.NodeGraphConnectionDragStartedEvent;
import dev.sixik.unigui.api.event.NodeGraphConnectionRemovedEvent;
import dev.sixik.unigui.api.event.NodeGraphConnectionSelectionChangedEvent;
import dev.sixik.unigui.api.event.NodeGraphItemMoveEndedEvent;
import dev.sixik.unigui.api.event.NodeGraphItemMoveStartedEvent;
import dev.sixik.unigui.api.event.NodeGraphItemMovedEvent;
import dev.sixik.unigui.api.event.NodeGraphItemRemovedEvent;
import dev.sixik.unigui.api.event.NodeGraphItemResizeEndedEvent;
import dev.sixik.unigui.api.event.NodeGraphItemResizeStartedEvent;
import dev.sixik.unigui.api.event.NodeGraphItemResizedEvent;
import dev.sixik.unigui.api.event.NodeGraphItemSelectionChangedEvent;
import dev.sixik.unigui.api.event.NodeGraphViewportChangedEvent;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.input.HitTestCoordinateMapper;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.widget.RenderedBoundsMapper;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.viewport.Viewport2D;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.api.widget.render.WidgetRender;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import dev.sixik.unigui.widgets.containers.StackPanel;

@AiDoc(summary = "Interactive 2D node graph canvas supporting movable nodes, pin connections, pan, zoom, grid snapping, and selection.")
@WikiPage(
        title = "NodeGraph",
        slug = "node-graph",
        category = "<key:graph.category>",
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
@XmlWidgetName("NodeGraph")
public final class NodeGraph extends WidgetBase implements HitTestCoordinateMapper, RenderedBoundsMapper {
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
        for (NodeGraphItem item : itemSnapshot()) {
            detach(item);
            item.content().dispose();
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

    @XmlAttribute(value = "itemContentPadding", category = "Layout", defaultValue = "0", description = "Padding applied around item content during node layout.")
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
        connections.add(connection);
        markConnectionsDirty();
        attach(connection);
        invalidate(InvalidationFlags.VISUAL);
        dispatch(new NodeGraphConnectionCreatedEvent(this, connection.id(), connection.from(), connection.to(), connection.type()));
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
                    item.content().getClass().getName(),
                    item.x(),
                    item.y(),
                    item.width(),
                    item.height(),
                    item.selectable(),
                    item.movable(),
                    item.resizable(),
                    item.visible(),
                    portSnapshots));
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
                selectedConnectionIds());
    }

    @AiDoc(summary = "Restores graph items, connections, and viewport from a serialized snapshot using a widget resolver.")
    @WikiMethod(summary = "<key:node.graph.method.restoreSnapshot>")
    @WikiReturn("<key:node.graph.return.self>")
    public NodeGraph restoreSnapshot(@WikiParameter(name = "snapshot", description = "<key:node.graph.param.snapshot>") NodeGraphSnapshot snapshot, @WikiParameter(name = "resolver", description = "<key:node.graph.param.resolver>") NodeGraphWidgetResolver resolver) {
        if (snapshot == null) return this;
        NodeGraphWidgetResolver safeResolver = resolver == null ? (itemId, contentType) -> null : resolver;
        clearItems();

        for (NodeGraphItemSnapshot itemSnapshot : snapshot.items()) {
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
            if (snapshot.length == 0) {
                childrenView = Collections.emptyList();
            } else {
                List<Widget> children = new ObjectArrayList<>(snapshot.length);
                for (NodeGraphItem item : snapshot) {
                    children.add(item.content());
                }
                childrenView = Collections.unmodifiableList(children);
            }
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
        NodeGraphItem item = itemForContent(child);
        if (item == null) return null;
        float itemX = worldToRootX(item.x());
        float itemY = worldToRootY(item.y());
        return new HitTestPoint(
                itemX + (x - itemX) / zoom(),
                itemY + (y - itemY) / zoom());
    }

    @Override
    public RectView renderedBoundsForChild(Widget child, RectView bounds) {
        if (!scaleContentWithZoom || zoom() == 1.0f || child == null || bounds == null) return null;
        NodeGraphItem item = itemForContent(child);
        if (item == null) return null;
        float itemX = worldToRootX(item.x());
        float itemY = worldToRootY(item.y());
        return new MutableRect(
                itemX + (bounds.x() - itemX) * zoom(),
                itemY + (bounds.y() - itemY) * zoom(),
                bounds.width() * zoom(),
                bounds.height() * zoom());
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        for (NodeGraphItem item : itemSnapshot()) {
            if (item.content() instanceof WidgetBase base) {
                base.setUiContextInternal(uiContext);
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

    @XmlAttribute(value = "preferredWidth", category = "Layout", defaultValue = "320", description = "Intrinsic node graph width before layout constraints are applied.")
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

    @XmlAttribute(value = "preferredHeight", category = "Layout", defaultValue = "200", description = "Intrinsic node graph height before layout constraints are applied.")
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

    @XmlAttribute(value = "selectionMode", category = "Behavior", defaultValue = "single", description = "Selection mode used by node graph items.")
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

    @XmlAttribute(value = "gridSize", category = "Appearance", defaultValue = "24", description = "World-space spacing between rendered grid lines.")
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

    @XmlAttribute(value = "clippingEnabled", category = "Behavior", defaultValue = "true", description = "Whether graph content is clipped to widget bounds.")
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

    @XmlAttribute(value = "itemDraggingEnabled", category = "Behavior", defaultValue = "true", description = "Whether node items can be moved by pointer dragging.")
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

    @XmlAttribute(value = "panningEnabled", category = "Behavior", defaultValue = "true", description = "Whether pointer gestures can pan the graph viewport.")
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

    @XmlAttribute(value = "zoomEnabled", category = "Behavior", defaultValue = "true", description = "Whether wheel gestures can zoom the graph viewport.")
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

    @XmlAttribute(value = "scaleContentWithZoom", category = "Behavior", defaultValue = "true", description = "Whether embedded item widgets scale with viewport zoom.")
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

    @XmlAttribute(value = "lassoSelectionEnabled", category = "Behavior", defaultValue = "true", description = "Whether pointer drag can create lasso item selections.")
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

    @XmlAttribute(value = "resizeEnabled", category = "Behavior", defaultValue = "true", description = "Whether selected node items can be resized.")
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

    @XmlAttribute(value = "keyboardEditingEnabled", category = "Behavior", defaultValue = "true", description = "Whether keyboard shortcuts can edit node graph selections.")
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

    @XmlAttribute(value = "consumeWheelWhileHovered", category = "Behavior", defaultValue = "true", description = "Whether wheel events are consumed while the graph is hovered.")
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

    @XmlAttribute(value = "wheelPanningEnabled", category = "Behavior", defaultValue = "true", description = "Whether non-zoom wheel gestures pan the graph viewport.")
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

    @XmlAttribute(value = "wheelPanStep", category = "Behavior", defaultValue = "32", description = "Viewport pan distance applied per wheel delta unit.")
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

    @XmlAttribute(value = "bringToFrontOnSelect", category = "Behavior", defaultValue = "true", description = "Whether selecting an item moves it above sibling nodes.")
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
            if (!item.visible() || item.content().visibility() == Visibility.COLLAPSED) continue;
            item.content().measure(childContext);
            float padding = itemContentPadding * 2.0f;
            float width = item.autoWidth() ? StackPanel.preferredWidth(item.content(), 0.0f) + padding : item.width();
            float height = item.autoHeight() ? StackPanel.preferredHeight(item.content(), 0.0f) + padding : item.height();
            item.arrangedSize(Math.max(MIN_ITEM_SIZE, width), Math.max(MIN_ITEM_SIZE, height));
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
            if (item.visible() && item.content().visibility() == Visibility.VISIBLE) {
                item.content().tick(frame);
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
            }
            for (NodeGraphItem item : itemSnapshot()) {
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
            }
        }
    }

    @Override
    public void dispose() {
        clearItems();
        super.dispose();
    }

    private void attach(NodeGraphItem item) {
        item.owner(this);
        if (item.content() instanceof WidgetBase base) {
            base.setParentInternal(this);
            base.setUiContextInternal(uiContext());
        }
    }

    private void attach(NodeGraphConnection connection) {
        connection.owner(this);
    }

    private void detach(NodeGraphItem item) {
        if (item.content() instanceof WidgetBase base) {
            base.setParentInternal(null);
            base.setUiContextInternal(null);
        }
        item.owner(null);
        item.selectedInternal(false);
    }

    private void detach(NodeGraphConnection connection) {
        connection.owner(null);
        connection.selectedInternal(false);
    }

    private NodeGraphItem itemForContent(Widget content) {
        Object[] rawItems = items.elements();
        for (int itemIndex = 0, itemSize = items.size(); itemIndex < itemSize; itemIndex++) {
            NodeGraphItem item = (NodeGraphItem) rawItems[itemIndex];
            if (item.content() == content) return item;
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
        for (NodeGraphItem item : itemSnapshot()) {
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

    private void handlePointerPressed(PointerPressedEvent pointer, Event sourceEvent) {
        if (pointer.button() == PointerButton.MIDDLE && panningEnabled) {
            startPan(pointer);
            sourceEvent.cancel();
            return;
        }
        if (pointer.button() != PointerButton.PRIMARY) return;

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

        NodeGraphConnection connection = hitConnection(pointer.rootX(), pointer.rootY());
        if (connection != null) {
            selectConnection(connection);
            sourceEvent.cancel();
            return;
        }

        NodeGraphItem item = hitItem(pointer.rootX(), pointer.rootY());
        if (item == null) {
            item = itemForContent(pointer.target());
        }
        if (item == null) {
            clearSelection();
            clearConnectionSelection();
            if (lassoSelectionEnabled && selectionMode == NodeGraphSelectionMode.MULTIPLE) {
                startLasso(pointer);
                sourceEvent.cancel();
            }
            return;
        }
        if (item.selectable()) {
            selectItem(item);
            clearConnectionSelection();
        }
        if (itemDraggingEnabled && item.movable()) {
            startItemDrag(item, pointer);
            sourceEvent.cancel();
        }
    }

    private void handlePointerMoved(PointerMovedEvent pointer) {
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
            }
            return;
        }
        PortHit portHit = hitPort(pointer.rootX(), pointer.rootY());
        setHoveredPort(portHit);
        setHoveredConnection(portHit == null ? hitConnection(pointer.rootX(), pointer.rootY()) : null);
        setHoveredItem(portHit == null ? hitItem(pointer.rootX(), pointer.rootY()) : null);
    }

    private void handlePointerReleased(PointerReleasedEvent pointer, Event sourceEvent) {
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
        setHoveredItem(hitItem(pointer.rootX(), pointer.rootY()));
        invalidate(InvalidationFlags.VISUAL);
        sourceEvent.cancel();
    }

    private void startItemDrag(NodeGraphItem item, PointerPressedEvent pointer) {
        dragState = new DragState(DragKind.ITEM, item, pointer.pointerId(), pointer.rootX(), pointer.rootY(),
                item.x(), item.y(), item.arrangedWidth(), item.arrangedHeight(), viewportX(), viewportY());
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

    private void startLasso(PointerPressedEvent pointer) {
        lassoState = new LassoState(pointer.pointerId(), pointer.rootX(), pointer.rootY(), pointer.rootX(), pointer.rootY());
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
            created = addConnection(normalizeConnectionId(""), ended.from(), target.ref());
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
    }

    private void moveDraggedItem(PointerMovedEvent pointer) {
        NodeGraphItem item = dragState.item;
        float oldX = item.x();
        float oldY = item.y();
        float nextX = dragState.startX + (pointer.rootX() - dragState.startRootX) / zoom();
        float nextY = dragState.startY + (pointer.rootY() - dragState.startRootY) / zoom();
        item.position(nextX, nextY);
        arrangeItems();
        if (oldX != item.x() || oldY != item.y()) {
            dispatch(new NodeGraphItemMovedEvent(this, item.id(), oldX, oldY, item.x(), item.y(), pointer.pointerId()));
        }
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

    private void handleScroll(ScrollEvent scroll, Event sourceEvent) {
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

    private void handleKeyPressed(KeyPressedEvent key, Event sourceEvent) {
        if (!keyboardEditingEnabled || key.phase() == EventPhase.CAPTURE) return;

        boolean handled = false;
        if (KeyModifiers.has(key.modifiers(), KeyModifiers.CONTROL) && key.keyCode() == KeyCodes.A) {
            selectAllItems();
            handled = true;
        } else if (key.keyCode() == KeyCodes.DELETE || key.keyCode() == KeyCodes.BACKSPACE) {
            removeSelectedConnections();
            removeSelectedItems();
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
        for (NodeGraphItem item : itemSnapshot()) {
            if (!item.selected() || !item.movable()) continue;
            float oldX = item.x();
            float oldY = item.y();
            item.position(oldX + dx, oldY + dy);
            dispatch(new NodeGraphItemMovedEvent(this, item.id(), oldX, oldY, item.x(), item.y(), pointerId));
        }
        arrangeItems();
    }

    private NodeGraphItem hitItem(float rootX, float rootY) {
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
                if (!port.visible()) continue;
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

    private NodeGraphConnection hitConnection(float rootX, float rootY) {
        for (int i = connections.size() - 1; i >= 0; i--) {
            NodeGraphConnection connection = connections.get(i);
            ConnectionPoints points = connectionPoints(connection);
            if (points == null) continue;
            if (distanceToSegment(rootX, rootY, points.startX(), points.startY(), points.endX(), points.endY())
                    <= CONNECTION_HIT_RADIUS) {
                return connection;
            }
        }
        return null;
    }

    private PortPoint portPoint(NodeGraphItem item, NodeGraphPort port) {
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

    private ConnectionPoints connectionPoints(NodeGraphConnection connection) {
        NodeGraphItem fromItem = item(connection.fromItemId());
        NodeGraphItem toItem = item(connection.toItemId());
        NodeGraphPort fromPort = port(connection.from());
        NodeGraphPort toPort = port(connection.to());
        if (fromItem == null || toItem == null || fromPort == null || toPort == null) return null;
        PortPoint from = portPoint(fromItem, fromPort);
        PortPoint to = portPoint(toItem, toPort);
        return new ConnectionPoints(from.x(), from.y(), to.x(), to.y());
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

    private void renderBackground(DrawScope draw) {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        if (width <= 0.0f || height <= 0.0f) return;
        draw.rect(x, y, width, height, Paint.fill(backgroundColor));
        drawGraphGrid(draw, x, y, width, height);
        for (NodeGraphConnection connection : connections) {
            if (!connection.enabled()) continue;
            ConnectionPoints points = connectionPoints(connection);
            if (points == null) continue;
            boolean highlighted = connection.selected() || Objects.equals(hoveredConnectionId, connection.id());
            ColorView color = highlighted ? selectedConnectionColor : connectionColor;
            draw.addLine(points.startX(), points.startY(), points.endX(), points.endY(),
                    color, highlighted ? 2.0f : 1.0f);
        }
    }

    private void renderForeground(DrawScope draw) {
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        if (width <= 0.0f || height <= 0.0f) return;
        for (NodeGraphItem item : items) {
            if (!item.visible()) continue;
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
            draw.addLine(connectionDragState.startRootX, connectionDragState.startRootY,
                    connectionDragState.endRootX, connectionDragState.endRootY,
                    connectionDragState.valid ? connectionPreviewColor : invalidConnectionPreviewColor,
                    connectionDragState.valid ? 2.0f : 1.5f);
        }
        float portRadius = screenPortRadius();
        for (NodeGraphItem item : items) {
            if (!item.visible()) continue;
            ObjectArrayList<NodeGraphPort> ports = item.rawPorts();
            Object[] rawPorts = ports.elements();
            for (int portIndex = 0, portSize = ports.size(); portIndex < portSize; portIndex++) {
                NodeGraphPort port = (NodeGraphPort) rawPorts[portIndex];
                if (!port.visible()) continue;
                PortPoint point = portPoint(item, port);
                boolean hoveredPort = hoveredPort().equals(new NodeGraphPortRef(item.id(), port.id()));
                ColorView color = hoveredPort ? hoveredPortColor : portColor;
                draw.addCircleFilled(point.x(), point.y(), portRadius, color, 12);
                draw.addCircle(point.x(), point.y(), portRadius + 1.0f,
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
    }

    private void drawGraphGrid(DrawScope draw, float x, float y, float width, float height) {
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

    private <T extends Event> T dispatch(T event) {
        UIContext context = uiContext();
        if (context == null || !(event instanceof dev.sixik.unigui.api.event.WidgetEvent widgetEvent)) {
            emit(event);
        } else {
            context.routedEvents().dispatch(widgetEvent);
        }
        return event;
    }

    private float viewportX() {
        return viewport.x();
    }

    private float viewportY() {
        return viewport.y();
    }

    private float zoom() {
        return viewport.zoom();
    }

    private float worldToRootX(float worldX) {
        return layoutBounds().x() + viewport.worldToScreenX(worldX);
    }

    private float worldToRootY(float worldY) {
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

    private boolean canStartConnection(NodeGraphPort port) {
        return port != null && port.enabled() && port.visible()
                && (port.kind() == NodeGraphPortKind.OUTPUT || port.kind() == NodeGraphPortKind.BIDIRECTIONAL);
    }

    private boolean canEndConnection(NodeGraphPort port) {
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
        RESIZE
    }

    private static final class DragState {
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

    private record ConnectionPoints(float startX, float startY, float endX, float endY) {
    }

    private record ConnectionTargetValidation(NodeGraphConnectionValidation validation) {
    }

    private record Rect(float x, float y, float width, float height) {
    }
}
