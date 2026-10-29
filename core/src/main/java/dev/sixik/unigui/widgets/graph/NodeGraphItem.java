package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.widget.Widget;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

    public NodeGraphItem(String id, Widget content) {
        this(id, content, 0.0f, 0.0f);
    }

    public NodeGraphItem(String id, Widget content, float x, float y) {
        this.id = normalizeId(id);
        this.content = Objects.requireNonNull(content, "content");
        this.x = sanitize(x);
        this.y = sanitize(y);
    }

    public String id() {
        return id;
    }

    public Widget content() {
        return content;
    }

    public NodeGraphPort addPort(String id, NodeGraphPortKind kind, NodeGraphPortSide side, float offset) {
        NodeGraphPort port = new NodeGraphPort(id, kind, side, offset);
        addPort(port);
        return port;
    }

    public NodeGraphItem addPort(NodeGraphPort port) {
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

    public NodeGraphItem removePort(String id) {
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

    public NodeGraphPort port(String id) {
        if (id == null) return null;
        Object[] rawPorts = ports.elements();
        for (int portIndex = 0, portSize = ports.size(); portIndex < portSize; portIndex++) {
            NodeGraphPort port = (NodeGraphPort) rawPorts[portIndex];
            if (port.id().equals(id)) return port;
        }
        return null;
    }

    public List<NodeGraphPort> ports() {
        return Collections.unmodifiableList(ports);
    }

    ObjectArrayList<NodeGraphPort> rawPorts() {
        return ports;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public NodeGraphItem position(float x, float y) {
        float nextX = sanitize(x);
        float nextY = sanitize(y);
        if (this.x == nextX && this.y == nextY) return this;
        this.x = nextX;
        this.y = nextY;
        invalidateOwner(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public NodeGraphItem size(float width, float height) {
        float nextWidth = sanitizeSize(width);
        float nextHeight = sanitizeSize(height);
        if (this.width == nextWidth && this.height == nextHeight) return this;
        this.width = nextWidth;
        this.height = nextHeight;
        invalidateOwner(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public NodeGraphItem autoSize() {
        return size(0.0f, 0.0f);
    }

    public boolean autoWidth() {
        return width <= 0.0f;
    }

    public boolean autoHeight() {
        return height <= 0.0f;
    }

    public float arrangedWidth() {
        return arrangedWidth;
    }

    public float arrangedHeight() {
        return arrangedHeight;
    }

    public boolean selectable() {
        return selectable;
    }

    public NodeGraphItem selectable(boolean selectable) {
        if (this.selectable == selectable) return this;
        this.selectable = selectable;
        if (!selectable) {
            selectedInternal(false);
        }
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean movable() {
        return movable;
    }

    public NodeGraphItem movable(boolean movable) {
        if (this.movable == movable) return this;
        this.movable = movable;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean resizable() {
        return resizable;
    }

    public NodeGraphItem resizable(boolean resizable) {
        if (this.resizable == resizable) return this;
        this.resizable = resizable;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean selected() {
        return selected;
    }

    public boolean visible() {
        return visible;
    }

    public NodeGraphItem visible(boolean visible) {
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
