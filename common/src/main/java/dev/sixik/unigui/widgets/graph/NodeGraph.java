package dev.sixik.unigui.widgets.graph;

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
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.widget.RenderedBoundsMapper;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.viewport.Viewport2D;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.NodeGraphItemState;
import dev.sixik.unigui.widgets.render.NodeGraphConnectionPreviewState;
import dev.sixik.unigui.widgets.render.NodeGraphConnectionState;
import dev.sixik.unigui.widgets.render.NodeGraphPortState;
import dev.sixik.unigui.widgets.render.NodeGraphRenderPhase;
import dev.sixik.unigui.widgets.render.NodeGraphRenderer;
import dev.sixik.unigui.widgets.render.NodeGraphSelectionBoxState;
import dev.sixik.unigui.widgets.render.NodeGraphState;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import dev.sixik.unigui.widgets.containers.StackPanel;

@XmlWidgetName("NodeGraph")
public final class NodeGraph extends WidgetBase implements HitTestCoordinateMapper, RenderedBoundsMapper {
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

    private NodeGraphRenderer renderer;
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

    public NodeGraphItem addItem(String id, Widget content, float x, float y) {
        NodeGraphItem item = new NodeGraphItem(id, content, x, y);
        addItem(item);
        return item;
    }

    public NodeGraph addItem(NodeGraphItem item) {
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

    public NodeGraph removeItem(NodeGraphItem item) {
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

    public NodeGraph removeItem(String id) {
        NodeGraphItem item = item(id);
        return item == null ? this : removeItem(item);
    }

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

    public float itemContentPadding() {
        return itemContentPadding;
    }

    @XmlAttribute(value = "itemContentPadding", category = "Layout", defaultValue = "0", description = "Padding applied around item content during node layout.")
    public NodeGraph itemContentPadding(float padding) {
        float normalized = Float.isFinite(padding) ? Math.max(0.0f, padding) : DEFAULT_ITEM_CONTENT_PADDING;
        if (itemContentPadding == normalized) return this;
        itemContentPadding = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public List<NodeGraphItem> items() {
        return itemsView;
    }

    public NodeGraphItem item(String id) {
        if (id == null) return null;
        Object[] rawItems = items.elements();
        for (int itemIndex = 0, itemSize = items.size(); itemIndex < itemSize; itemIndex++) {
            NodeGraphItem item = (NodeGraphItem) rawItems[itemIndex];
            if (item.id().equals(id)) return item;
        }
        return null;
    }

    public NodeGraphPort port(NodeGraphPortRef ref) {
        if (ref == null || ref.empty()) return null;
        NodeGraphItem item = item(ref.itemId());
        return item == null ? null : item.port(ref.portId());
    }

    public NodeGraphConnection addConnection(String id,
                                             String fromItemId, String fromPortId,
                                             String toItemId, String toPortId) {
        return addConnection(new NodeGraphConnection(
                normalizeConnectionId(id),
                new NodeGraphPortRef(fromItemId, fromPortId),
                new NodeGraphPortRef(toItemId, toPortId)));
    }

    public NodeGraphConnection addConnection(String id, NodeGraphPortRef from, NodeGraphPortRef to) {
        return addConnection(new NodeGraphConnection(normalizeConnectionId(id), from, to));
    }

    public NodeGraphConnection addConnection(NodeGraphConnection connection) {
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

    public NodeGraph removeConnection(String id) {
        NodeGraphConnection connection = connection(id);
        return connection == null ? this : removeConnection(connection);
    }

    public NodeGraph removeConnection(NodeGraphConnection connection) {
        if (connection == null || !connections.remove(connection)) return this;
        markConnectionsDirty();
        detach(connection);
        invalidate(InvalidationFlags.VISUAL);
        dispatch(new NodeGraphConnectionRemovedEvent(this, connection.id(), connection.from(), connection.to()));
        return this;
    }

    public NodeGraph removeSelectedConnections() {
        for (NodeGraphConnection connection : connectionSnapshot()) {
            if (connection.selected()) {
                removeConnection(connection);
            }
        }
        return this;
    }

    public NodeGraphConnection connection(String id) {
        if (id == null) return null;
        Object[] rawConnections = connections.elements();
        for (int connectionIndex = 0, connectionSize = connections.size(); connectionIndex < connectionSize; connectionIndex++) {
            NodeGraphConnection connection = (NodeGraphConnection) rawConnections[connectionIndex];
            if (connection.id().equals(id)) return connection;
        }
        return null;
    }

    public List<NodeGraphConnection> connections() {
        return connectionsView;
    }

    public boolean hasConnection(NodeGraphPortRef from, NodeGraphPortRef to) {
        Object[] rawConnections = connections.elements();
        for (int connectionIndex = 0, connectionSize = connections.size(); connectionIndex < connectionSize; connectionIndex++) {
            NodeGraphConnection connection = (NodeGraphConnection) rawConnections[connectionIndex];
            if (connection.from().equals(from) && connection.to().equals(to)) {
                return true;
            }
        }
        return false;
    }

    public NodeGraphConnectionValidation validateConnection(NodeGraphPortRef from, NodeGraphPortRef to) {
        NodeGraphConnectionPolicy policy = connectionPolicy == null ? NodeGraphConnectionPolicy.DEFAULT : connectionPolicy;
        NodeGraphConnectionValidation validation = policy.validate(this, from, to);
        return validation == null ? NodeGraphConnectionValidation.invalid("Connection rejected") : validation;
    }

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

    public NodeGraph restoreSnapshot(NodeGraphSnapshot snapshot, NodeGraphWidgetResolver resolver) {
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

    public MutableColor backgroundColor() {
        return backgroundColor;
    }

    public MutableColor gridColor() {
        return gridColor;
    }

    public MutableColor majorGridColor() {
        return majorGridColor;
    }

    public MutableColor itemBorderColor() {
        return itemBorderColor;
    }

    public MutableColor hoveredItemBorderColor() {
        return hoveredItemBorderColor;
    }

    public MutableColor selectedItemBorderColor() {
        return selectedItemBorderColor;
    }

    public MutableColor portColor() {
        return portColor;
    }

    public MutableColor hoveredPortColor() {
        return hoveredPortColor;
    }

    public MutableColor connectionColor() {
        return connectionColor;
    }

    public MutableColor selectedConnectionColor() {
        return selectedConnectionColor;
    }

    public MutableColor connectionPreviewColor() {
        return connectionPreviewColor;
    }

    public MutableColor invalidConnectionPreviewColor() {
        return invalidConnectionPreviewColor;
    }

    public MutableColor selectionBoxFillColor() {
        return selectionBoxFillColor;
    }

    public MutableColor selectionBoxBorderColor() {
        return selectionBoxBorderColor;
    }

    public MutableColor resizeHandleColor() {
        return resizeHandleColor;
    }

    public NodeGraphRenderer renderer() {
        return renderer;
    }

    public NodeGraph renderer(NodeGraphRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public NodeGraph useDefaultRenderer() {
        return renderer(null);
    }

    public float preferredWidth() {
        return preferredWidth;
    }

    @XmlAttribute(value = "preferredWidth", category = "Layout", defaultValue = "320", description = "Intrinsic node graph width before layout constraints are applied.")
    public NodeGraph preferredWidth(float preferredWidth) {
        float normalized = positiveOr(preferredWidth, DEFAULT_PREFERRED_WIDTH);
        if (this.preferredWidth == normalized) return this;
        this.preferredWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float preferredHeight() {
        return preferredHeight;
    }

    @XmlAttribute(value = "preferredHeight", category = "Layout", defaultValue = "200", description = "Intrinsic node graph height before layout constraints are applied.")
    public NodeGraph preferredHeight(float preferredHeight) {
        float normalized = positiveOr(preferredHeight, DEFAULT_PREFERRED_HEIGHT);
        if (this.preferredHeight == normalized) return this;
        this.preferredHeight = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public NodeGraph preferredSize(float width, float height) {
        return preferredWidth(width).preferredHeight(height);
    }

    public NodeGraphConnectionPolicy connectionPolicy() {
        return connectionPolicy;
    }

    public NodeGraph connectionPolicy(NodeGraphConnectionPolicy connectionPolicy) {
        this.connectionPolicy = connectionPolicy == null ? NodeGraphConnectionPolicy.DEFAULT : connectionPolicy;
        return this;
    }

    public NodeGraphSelectionMode selectionMode() {
        return selectionMode;
    }

    @XmlAttribute(value = "selectionMode", category = "Behavior", defaultValue = "single", description = "Selection mode used by node graph items.")
    public NodeGraph selectionMode(NodeGraphSelectionMode selectionMode) {
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

    public NodeGraphViewport viewport() {
        return new NodeGraphViewport(viewportX(), viewportY(), zoom());
    }

    public NodeGraph viewport(float x, float y) {
        return viewport(x, y, zoom());
    }

    public NodeGraph viewport(float x, float y, float zoom) {
        float oldX = viewportX();
        float oldY = viewportY();
        float oldZoom = zoom();
        if (!viewport.set(x, y, zoom)) return this;
        arrangeItems();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        dispatch(new NodeGraphViewportChangedEvent(this, oldX, oldY, oldZoom, viewportX(), viewportY(), zoom()));
        return this;
    }

    public float gridSize() {
        return gridSize;
    }

    @XmlAttribute(value = "gridSize", category = "Appearance", defaultValue = "24", description = "World-space spacing between rendered grid lines.")
    public NodeGraph gridSize(float gridSize) {
        float next = Float.isFinite(gridSize) ? Math.max(2.0f, gridSize) : 24.0f;
        if (this.gridSize == next) return this;
        this.gridSize = next;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean clippingEnabled() {
        return clippingEnabled;
    }

    @XmlAttribute(value = "clippingEnabled", category = "Behavior", defaultValue = "true", description = "Whether graph content is clipped to widget bounds.")
    public NodeGraph clippingEnabled(boolean clippingEnabled) {
        if (this.clippingEnabled == clippingEnabled) return this;
        this.clippingEnabled = clippingEnabled;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean itemDraggingEnabled() {
        return itemDraggingEnabled;
    }

    @XmlAttribute(value = "itemDraggingEnabled", category = "Behavior", defaultValue = "true", description = "Whether node items can be moved by pointer dragging.")
    public NodeGraph itemDraggingEnabled(boolean itemDraggingEnabled) {
        this.itemDraggingEnabled = itemDraggingEnabled;
        return this;
    }

    public boolean panningEnabled() {
        return panningEnabled;
    }

    @XmlAttribute(value = "panningEnabled", category = "Behavior", defaultValue = "true", description = "Whether pointer gestures can pan the graph viewport.")
    public NodeGraph panningEnabled(boolean panningEnabled) {
        this.panningEnabled = panningEnabled;
        return this;
    }

    public boolean zoomEnabled() {
        return zoomEnabled;
    }

    @XmlAttribute(value = "zoomEnabled", category = "Behavior", defaultValue = "true", description = "Whether wheel gestures can zoom the graph viewport.")
    public NodeGraph zoomEnabled(boolean zoomEnabled) {
        this.zoomEnabled = zoomEnabled;
        return this;
    }

    public boolean scaleContentWithZoom() {
        return scaleContentWithZoom;
    }

    @XmlAttribute(value = "scaleContentWithZoom", category = "Behavior", defaultValue = "true", description = "Whether embedded item widgets scale with viewport zoom.")
    public NodeGraph scaleContentWithZoom(boolean scaleContentWithZoom) {
        if (this.scaleContentWithZoom == scaleContentWithZoom) return this;
        this.scaleContentWithZoom = scaleContentWithZoom;
        arrangeItems();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public NodeGraph zoomRange(float minZoom, float maxZoom) {
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

    public float minZoom() {
        return viewport.minZoom();
    }

    public float maxZoom() {
        return viewport.maxZoom();
    }

    public boolean lassoSelectionEnabled() {
        return lassoSelectionEnabled;
    }

    @XmlAttribute(value = "lassoSelectionEnabled", category = "Behavior", defaultValue = "true", description = "Whether pointer drag can create lasso item selections.")
    public NodeGraph lassoSelectionEnabled(boolean lassoSelectionEnabled) {
        this.lassoSelectionEnabled = lassoSelectionEnabled;
        return this;
    }

    public boolean resizeEnabled() {
        return resizeEnabled;
    }

    @XmlAttribute(value = "resizeEnabled", category = "Behavior", defaultValue = "true", description = "Whether selected node items can be resized.")
    public NodeGraph resizeEnabled(boolean resizeEnabled) {
        this.resizeEnabled = resizeEnabled;
        return this;
    }

    public boolean keyboardEditingEnabled() {
        return keyboardEditingEnabled;
    }

    @XmlAttribute(value = "keyboardEditingEnabled", category = "Behavior", defaultValue = "true", description = "Whether keyboard shortcuts can edit node graph selections.")
    public NodeGraph keyboardEditingEnabled(boolean keyboardEditingEnabled) {
        this.keyboardEditingEnabled = keyboardEditingEnabled;
        return this;
    }

    public boolean consumeWheelWhileHovered() {
        return consumeWheelWhileHovered;
    }

    @XmlAttribute(value = "consumeWheelWhileHovered", category = "Behavior", defaultValue = "true", description = "Whether wheel events are consumed while the graph is hovered.")
    public NodeGraph consumeWheelWhileHovered(boolean consumeWheelWhileHovered) {
        this.consumeWheelWhileHovered = consumeWheelWhileHovered;
        return this;
    }

    public boolean wheelPanningEnabled() {
        return wheelPanningEnabled;
    }

    @XmlAttribute(value = "wheelPanningEnabled", category = "Behavior", defaultValue = "true", description = "Whether non-zoom wheel gestures pan the graph viewport.")
    public NodeGraph wheelPanningEnabled(boolean wheelPanningEnabled) {
        this.wheelPanningEnabled = wheelPanningEnabled;
        return this;
    }

    public float wheelPanStep() {
        return wheelPanStep;
    }

    @XmlAttribute(value = "wheelPanStep", category = "Behavior", defaultValue = "32", description = "Viewport pan distance applied per wheel delta unit.")
    public NodeGraph wheelPanStep(float wheelPanStep) {
        this.wheelPanStep = Float.isFinite(wheelPanStep) ? Math.max(1.0f, wheelPanStep) : 32.0f;
        return this;
    }

    public boolean bringToFrontOnSelect() {
        return bringToFrontOnSelect;
    }

    @XmlAttribute(value = "bringToFrontOnSelect", category = "Behavior", defaultValue = "true", description = "Whether selecting an item moves it above sibling nodes.")
    public NodeGraph bringToFrontOnSelect(boolean bringToFrontOnSelect) {
        this.bringToFrontOnSelect = bringToFrontOnSelect;
        return this;
    }

    public String hoveredItemId() {
        return hoveredItemId;
    }

    public NodeGraphPortRef hoveredPort() {
        return hoveredPort;
    }

    public String hoveredConnectionId() {
        return hoveredConnectionId;
    }

    public String draggingItemId() {
        return dragState != null && dragState.kind == DragKind.ITEM ? dragState.item.id() : "";
    }

    public boolean connectionDragging() {
        return connectionDragState != null;
    }

    public boolean panning() {
        return dragState != null && dragState.kind == DragKind.PAN;
    }

    public boolean lassoSelecting() {
        return lassoState != null;
    }

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

    public NodeGraph selectItem(String id) {
        NodeGraphItem item = item(id);
        return item == null ? this : selectItem(item);
    }

    public NodeGraph selectItem(NodeGraphItem item) {
        return selectItem(item, false);
    }

    public NodeGraph selectItem(NodeGraphItem item, boolean additive) {
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

    public NodeGraph clearSelection() {
        List<String> oldSelection = selectedItemIds();
        clearSelectionInternal(false);
        emitSelectionChanged(oldSelection);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

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

    public NodeGraph selectConnection(String id) {
        NodeGraphConnection connection = connection(id);
        return connection == null ? this : selectConnection(connection);
    }

    public NodeGraph selectConnection(NodeGraphConnection connection) {
        if (connection == null || !connections.contains(connection)) return this;
        List<String> oldSelection = selectedConnectionIds();
        clearConnectionSelectionInternal();
        connection.selectedInternal(true);
        emitConnectionSelectionChanged(oldSelection);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public NodeGraph clearConnectionSelection() {
        List<String> oldSelection = selectedConnectionIds();
        clearConnectionSelectionInternal();
        emitConnectionSelectionChanged(oldSelection);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public EventSubscription onItemMoveStarted(EventListener<? super NodeGraphItemMoveStartedEvent> listener) {
        return on(NodeGraphItemMoveStartedEvent.TYPE, listener);
    }

    public EventSubscription onItemMoved(EventListener<? super NodeGraphItemMovedEvent> listener) {
        return on(NodeGraphItemMovedEvent.TYPE, listener);
    }

    public EventSubscription onItemMoveEnded(EventListener<? super NodeGraphItemMoveEndedEvent> listener) {
        return on(NodeGraphItemMoveEndedEvent.TYPE, listener);
    }

    public EventSubscription onItemRemoved(EventListener<? super NodeGraphItemRemovedEvent> listener) {
        return on(NodeGraphItemRemovedEvent.TYPE, listener);
    }

    public EventSubscription onItemResizeStarted(EventListener<? super NodeGraphItemResizeStartedEvent> listener) {
        return on(NodeGraphItemResizeStartedEvent.TYPE, listener);
    }

    public EventSubscription onItemResized(EventListener<? super NodeGraphItemResizedEvent> listener) {
        return on(NodeGraphItemResizedEvent.TYPE, listener);
    }

    public EventSubscription onItemResizeEnded(EventListener<? super NodeGraphItemResizeEndedEvent> listener) {
        return on(NodeGraphItemResizeEndedEvent.TYPE, listener);
    }

    public EventSubscription onSelectionChanged(EventListener<? super NodeGraphItemSelectionChangedEvent> listener) {
        return on(NodeGraphItemSelectionChangedEvent.TYPE, listener);
    }

    public EventSubscription onViewportChanged(EventListener<? super NodeGraphViewportChangedEvent> listener) {
        return on(NodeGraphViewportChangedEvent.TYPE, listener);
    }

    public EventSubscription onConnectionCreated(EventListener<? super NodeGraphConnectionCreatedEvent> listener) {
        return on(NodeGraphConnectionCreatedEvent.TYPE, listener);
    }

    public EventSubscription onConnectionRemoved(EventListener<? super NodeGraphConnectionRemovedEvent> listener) {
        return on(NodeGraphConnectionRemovedEvent.TYPE, listener);
    }

    public EventSubscription onConnectionDragStarted(EventListener<? super NodeGraphConnectionDragStartedEvent> listener) {
        return on(NodeGraphConnectionDragStartedEvent.TYPE, listener);
    }

    public EventSubscription onConnectionDragEnded(EventListener<? super NodeGraphConnectionDragEndedEvent> listener) {
        return on(NodeGraphConnectionDragEndedEvent.TYPE, listener);
    }

    public EventSubscription onConnectionSelectionChanged(EventListener<? super NodeGraphConnectionSelectionChangedEvent> listener) {
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
            NodeGraphRenderer activeRenderer = effectiveRenderer();
            activeRenderer.render(draw, snapshot(NodeGraphRenderPhase.BACKGROUND));
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
            activeRenderer.render(draw, snapshot(NodeGraphRenderPhase.FOREGROUND));
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
            if (command.type() == DrawCommandType.PUSH_CLIP) {
                // Scissor rectangles are not drawn through the PoseStack/matrix path,
                // so their bounds must be converted to screen-space directly.
                scaleBounds(command.bounds(), pivotX, pivotY, zoom);
            } else if (command.type() != DrawCommandType.POP_CLIP) {
                // Drawn primitives, SDF/Minecraft text, meshes and CUSTOM callbacks
                // all go through command.transform(), so compose node zoom there.
                composeZoom(command, pivotX, pivotY, zoom);
            }
        }
    }

    private static void scaleBounds(MutableRect bounds, float pivotX, float pivotY, float zoom) {
        bounds.set(
                pivotX + (bounds.x() - pivotX) * zoom,
                pivotY + (bounds.y() - pivotY) * zoom,
                bounds.width() * zoom,
                bounds.height() * zoom);
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

    private NodeGraphRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(NodeGraphRenderer.class, WidgetsRender.nodeGraph()) : renderer;
    }

    private NodeGraphState snapshot(NodeGraphRenderPhase phase) {
        List<NodeGraphItemState> itemStates = new ObjectArrayList<>(items.size());
        List<NodeGraphPortState> portStates = new ObjectArrayList<>();
        for (NodeGraphItem item : items) {
            if (!item.visible()) continue;
            itemStates.add(new NodeGraphItemState(
                    item.id(),
                    item.x(),
                    item.y(),
                    worldToRootX(item.x()),
                    worldToRootY(item.y()),
                    itemScreenWidth(item),
                    itemScreenHeight(item),
                    item.selected(),
                    Objects.equals(hoveredItemId, item.id()),
                    dragState != null && dragState.kind == DragKind.ITEM && dragState.item == item,
                    item.movable(),
                    item.resizable()));
            ObjectArrayList<NodeGraphPort> ports = item.rawPorts();
            Object[] rawPorts = ports.elements();
            for (int portIndex = 0, portSize = ports.size(); portIndex < portSize; portIndex++) {
                NodeGraphPort port = (NodeGraphPort) rawPorts[portIndex];
                if (!port.visible()) continue;
                PortPoint point = portPoint(item, port);
                NodeGraphPortRef ref = new NodeGraphPortRef(item.id(), port.id());
                portStates.add(new NodeGraphPortState(
                        item.id(),
                        port.id(),
                        port.kind(),
                        port.side(),
                        port.type(),
                        point.x(),
                        point.y(),
                        screenPortRadius(),
                        port.enabled(),
                        hoveredPort.equals(ref),
                        canStartConnection(port) || canEndConnection(port)));
            }
        }
        List<NodeGraphConnectionState> connectionStates = new ObjectArrayList<>(connections.size());
        for (NodeGraphConnection connection : connections) {
            ConnectionPoints points = connectionPoints(connection);
            if (points == null) continue;
            connectionStates.add(new NodeGraphConnectionState(
                    connection.id(),
                    connection.fromItemId(),
                    connection.fromPortId(),
                    connection.toItemId(),
                    connection.toPortId(),
                    points.startX(),
                    points.startY(),
                    points.endX(),
                    points.endY(),
                    connection.selected(),
                    Objects.equals(hoveredConnectionId, connection.id()),
                    connection.enabled(),
                    connection.type()));
        }
        return new NodeGraphState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                viewportX(),
                viewportY(),
                zoom(),
                gridSize,
                phase,
                itemStates,
                portStates,
                connectionStates,
                previewState(),
                selectionBoxState(),
                backgroundColor.copy(),
                gridColor.copy(),
                majorGridColor.copy(),
                itemBorderColor.copy(),
                hoveredItemBorderColor.copy(),
                selectedItemBorderColor.copy(),
                portColor.copy(),
                hoveredPortColor.copy(),
                connectionColor.copy(),
                selectedConnectionColor.copy(),
                connectionPreviewColor.copy(),
                invalidConnectionPreviewColor.copy(),
                selectionBoxFillColor.copy(),
                selectionBoxBorderColor.copy(),
                resizeHandleColor.copy());
    }

    private NodeGraphConnectionPreviewState previewState() {
        if (connectionDragState == null) {
            return NodeGraphConnectionPreviewState.HIDDEN;
        }
        return new NodeGraphConnectionPreviewState(
                true,
                connectionDragState.fromItemId,
                connectionDragState.fromPortId,
                connectionDragState.target.itemId(),
                connectionDragState.target.portId(),
                connectionDragState.startRootX,
                connectionDragState.startRootY,
                connectionDragState.endRootX,
                connectionDragState.endRootY,
                connectionDragState.valid,
                connectionDragState.reason);
    }

    private NodeGraphSelectionBoxState selectionBoxState() {
        if (lassoState == null) {
            return NodeGraphSelectionBoxState.HIDDEN;
        }
        Rect rect = lassoRect();
        return new NodeGraphSelectionBoxState(true, rect.x(), rect.y(), rect.width(), rect.height());
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
