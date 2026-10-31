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
import dev.sixik.unigui.api.widget.Widget;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@AiDoc(summary = "Node item container placed in a NodeGraph canvas hosting a content widget and pin ports.")
@WikiPage(
        title = "NodeGraphItem",
        slug = "node-graph-item",
        category = "<key:graph.category>",
        summary = "<key:node.graph.item.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.item.section.overview.body>",
        order = 10
)
public final class NodeGraphItem {
    private final String id;
    private final Widget content;
    private final ObjectArrayList<NodeGraphPort> ports = new ObjectArrayList<>();
    private float x;
    private float y;
    private float width;
    private float height;
    private float arrangedWidth;
    private float arrangedHeight;
    private boolean selectable = true;
    private boolean movable = true;
    private boolean resizable = true;
    private boolean selected;
    private boolean visible = true;
    private NodeGraph owner;

    @AiDoc(summary = "Constructs a NodeGraphItem with unique ID and content widget.")
    @WikiMethod(summary = "<key:node.graph.item.method.ctorContent>")
    public NodeGraphItem(@WikiParameter(name = "id", description = "<key:node.graph.item.param.id>") String id, @WikiParameter(name = "content", description = "<key:node.graph.item.param.content>") dev.sixik.unigui.api.widget.Widget content) {
        this(id, content, 0.0f, 0.0f);
    }

    @AiDoc(summary = "Constructs a NodeGraphItem with unique ID, content widget, and initial coordinates.")
    @WikiMethod(summary = "<key:node.graph.item.method.ctorFull>")
    public NodeGraphItem(@WikiParameter(name = "id", description = "<key:node.graph.item.param.id>") String id, @WikiParameter(name = "content", description = "<key:node.graph.item.param.content>") Widget content, @WikiParameter(name = "x", description = "<key:node.graph.item.param.x>") float x, @WikiParameter(name = "y", description = "<key:node.graph.item.param.y>") float y) {
        this.id = normalizeId(id);
        this.content = Objects.requireNonNull(content, "content");
        this.x = sanitize(x);
        this.y = sanitize(y);
    }

    @AiDoc(summary = "Returns the unique identifier of this node item.")
    @WikiMethod(summary = "<key:node.graph.item.method.id>")
    @WikiReturn("<key:node.graph.item.return.id>")
    public String id() {
        return id;
    }

    @AiDoc(summary = "Returns the content widget hosted inside this node.")
    @WikiMethod(summary = "<key:node.graph.item.method.contentGet>")
    @WikiReturn("<key:node.graph.item.return.content>")
    public dev.sixik.unigui.api.widget.Widget content() {
        return content;
    }

    @AiDoc(summary = "Creates and attaches a port with specified ID, direction kind, side, and offset.")
    @WikiMethod(summary = "<key:node.graph.item.method.addPortFull>")
    @WikiReturn("<key:node.graph.item.return.createdPort>")
    public NodeGraphPort addPort(@WikiParameter(name = "id", description = "<key:node.graph.port.param.id>") String id, @WikiParameter(name = "kind", description = "<key:node.graph.port.param.kind>") NodeGraphPortKind kind, @WikiParameter(name = "side", description = "<key:node.graph.port.param.side>") NodeGraphPortSide side, @WikiParameter(name = "offset", description = "<key:node.graph.port.param.offset>") float offset) {
        NodeGraphPort port = new NodeGraphPort(id, kind, side, offset);
        addPort(port);
        return port;
    }

    @AiDoc(summary = "Attaches an existing port instance to this node.")
    @WikiMethod(summary = "<key:node.graph.item.method.addPortInstance>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem addPort(@WikiParameter(name = "port", description = "<key:node.graph.item.param.port>") NodeGraphPort port) {
        if (port == null || ports.contains(port)) return this;
        if (port.owner() != null && port.owner() != this) {
            throw new IllegalArgumentException("NodeGraphPort already belongs to another NodeGraphItem");
        }
        if (port(port.id()) != null) {
            throw new IllegalArgumentException("Duplicate NodeGraph port id: " + port.id());
        }
        ports.add(port);
        port.owner(this);
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Removes the port with the specified ID.")
    @WikiMethod(summary = "<key:node.graph.item.method.removePort>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem removePort(@WikiParameter(name = "id", description = "<key:node.graph.port.param.id>") String id) {
        NodeGraphPort port = port(id);
        if (port == null) return this;
        ports.remove(port);
        port.owner(null);
        if (owner != null) {
            owner.removeConnectionsForPort(new NodeGraphPortRef(this.id, port.id()));
        }
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns the port with the specified ID, or null if not found.")
    @WikiMethod(summary = "<key:node.graph.item.method.port>")
    @WikiReturn("<key:node.graph.item.return.port>")
    public NodeGraphPort port(@WikiParameter(name = "id", description = "<key:node.graph.port.param.id>") String id) {
        if (id == null) return null;
        Object[] rawPorts = ports.elements();
        for (int portIndex = 0, portSize = ports.size(); portIndex < portSize; portIndex++) {
            NodeGraphPort port = (NodeGraphPort) rawPorts[portIndex];
            if (port.id().equals(id)) return port;
        }
        return null;
    }

    @AiDoc(summary = "Returns an unmodifiable list of all ports attached to this node.")
    @WikiMethod(summary = "<key:node.graph.item.method.ports>")
    @WikiReturn("<key:node.graph.item.return.ports>")
    public List<NodeGraphPort> ports() {
        return Collections.unmodifiableList(ports);
    }

    ObjectArrayList<NodeGraphPort> rawPorts() {
        return ports;
    }

    @AiDoc(summary = "Returns the X position of this node in canvas world coordinates.")
    @WikiMethod(summary = "<key:node.graph.item.method.x>")
    @WikiReturn("<key:node.graph.item.return.x>")
    public float x() {
        return x;
    }

    @AiDoc(summary = "Returns the Y position of this node in canvas world coordinates.")
    @WikiMethod(summary = "<key:node.graph.item.method.y>")
    @WikiReturn("<key:node.graph.item.return.y>")
    public float y() {
        return y;
    }

    @AiDoc(summary = "Sets the (X, Y) world position of this node on the canvas.")
    @WikiMethod(summary = "<key:node.graph.item.method.position>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem position(@WikiParameter(name = "x", description = "<key:node.graph.item.param.x>") float x, @WikiParameter(name = "y", description = "<key:node.graph.item.param.y>") float y) {
        float nextX = sanitize(x);
        float nextY = sanitize(y);
        if (this.x == nextX && this.y == nextY) return this;
        this.x = nextX;
        this.y = nextY;
        invalidateOwner(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns the explicit configured width of this node, or <= 0 for auto-sizing.")
    @WikiMethod(summary = "<key:node.graph.item.method.width>")
    @WikiReturn("<key:node.graph.item.return.width>")
    public float width() {
        return width;
    }

    @AiDoc(summary = "Returns the explicit configured height of this node, or <= 0 for auto-sizing.")
    @WikiMethod(summary = "<key:node.graph.item.method.height>")
    @WikiReturn("<key:node.graph.item.return.height>")
    public float height() {
        return height;
    }

    @AiDoc(summary = "Sets explicit width and height dimensions for this node.")
    @WikiMethod(summary = "<key:node.graph.item.method.size>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem size(@WikiParameter(name = "width", description = "<key:node.graph.item.param.width>") float width, @WikiParameter(name = "height", description = "<key:node.graph.item.param.height>") float height) {
        float nextWidth = sanitizeSize(width);
        float nextHeight = sanitizeSize(height);
        if (this.width == nextWidth && this.height == nextHeight) return this;
        this.width = nextWidth;
        this.height = nextHeight;
        invalidateOwner(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Resets width and height to automatic sizing based on content.")
    @WikiMethod(summary = "<key:node.graph.item.method.autoSize>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem autoSize() {
        return size(0.0f, 0.0f);
    }

    @AiDoc(summary = "Returns true if width is determined automatically by content layout.")
    @WikiMethod(summary = "<key:node.graph.item.method.autoWidth>")
    @WikiReturn("<key:node.graph.item.return.autoWidth>")
    public boolean autoWidth() {
        return width <= 0.0f;
    }

    @AiDoc(summary = "Returns true if height is determined automatically by content layout.")
    @WikiMethod(summary = "<key:node.graph.item.method.autoHeight>")
    @WikiReturn("<key:node.graph.item.return.autoHeight>")
    public boolean autoHeight() {
        return height <= 0.0f;
    }

    @AiDoc(summary = "Returns the actual arranged layout width.")
    @WikiMethod(summary = "<key:node.graph.item.method.arrangedWidth>")
    @WikiReturn("<key:node.graph.item.return.arrangedWidth>")
    public float arrangedWidth() {
        return arrangedWidth;
    }

    @AiDoc(summary = "Returns the actual arranged layout height.")
    @WikiMethod(summary = "<key:node.graph.item.method.arrangedHeight>")
    @WikiReturn("<key:node.graph.item.return.arrangedHeight>")
    public float arrangedHeight() {
        return arrangedHeight;
    }

    @AiDoc(summary = "Returns true if this node can be selected by clicks or lasso selection.")
    @WikiMethod(summary = "<key:node.graph.item.method.selectableGet>")
    @WikiReturn("<key:node.graph.item.return.selectable>")
    public boolean selectable() {
        return selectable;
    }

    @AiDoc(summary = "Sets whether this node can be selected.")
    @WikiMethod(summary = "<key:node.graph.item.method.selectableSet>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem selectable(@WikiParameter(name = "selectable", description = "<key:node.graph.item.param.selectable>") boolean selectable) {
        if (this.selectable == selectable) return this;
        this.selectable = selectable;
        if (!selectable) {
            selectedInternal(false);
        }
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns true if this node can be dragged to move across the canvas.")
    @WikiMethod(summary = "<key:node.graph.item.method.movableGet>")
    @WikiReturn("<key:node.graph.item.return.movable>")
    public boolean movable() {
        return movable;
    }

    @AiDoc(summary = "Sets whether this node can be dragged to move.")
    @WikiMethod(summary = "<key:node.graph.item.method.movableSet>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem movable(@WikiParameter(name = "movable", description = "<key:node.graph.item.param.movable>") boolean movable) {
        if (this.movable == movable) return this;
        this.movable = movable;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns true if this node displays resize handles and can be interactively resized.")
    @WikiMethod(summary = "<key:node.graph.item.method.resizableGet>")
    @WikiReturn("<key:node.graph.item.return.resizable>")
    public boolean resizable() {
        return resizable;
    }

    @AiDoc(summary = "Sets whether this node can be resized by dragging corner handles.")
    @WikiMethod(summary = "<key:node.graph.item.method.resizableSet>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem resizable(@WikiParameter(name = "resizable", description = "<key:node.graph.item.param.resizable>") boolean resizable) {
        if (this.resizable == resizable) return this;
        this.resizable = resizable;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns true if this node is currently selected.")
    @WikiMethod(summary = "<key:node.graph.item.method.selected>")
    @WikiReturn("<key:node.graph.item.return.selected>")
    public boolean selected() {
        return selected;
    }

    @AiDoc(summary = "Returns true if this node is visible.")
    @WikiMethod(summary = "<key:node.graph.item.method.visibleGet>")
    @WikiReturn("<key:node.graph.item.return.visible>")
    public boolean visible() {
        return visible;
    }

    @AiDoc(summary = "Sets whether this node is visible.")
    @WikiMethod(summary = "<key:node.graph.item.method.visibleSet>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem visible(@WikiParameter(name = "visible", description = "<key:node.graph.item.param.visible>") boolean visible) {
        if (this.visible == visible) return this;
        this.visible = visible;
        invalidateOwner(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    NodeGraph owner() {
        return owner;
    }

    void owner(NodeGraph owner) {
        this.owner = owner;
    }

    void arrangedSize(float width, float height) {
        this.arrangedWidth = sanitizeSize(width);
        this.arrangedHeight = sanitizeSize(height);
    }

    void selectedInternal(boolean selected) {
        this.selected = selected && selectable;
    }

    private void invalidateOwner(int flags) {
        if (owner != null) {
            owner.invalidate(flags);
        }
    }

    private static String normalizeId(String id) {
        String normalized = id == null ? "" : id.trim();
        return normalized.isEmpty() ? "item" : normalized;
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float sanitizeSize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
