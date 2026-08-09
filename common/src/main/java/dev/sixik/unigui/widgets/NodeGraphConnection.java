package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;

public final class NodeGraphConnection {
    private final String id;
    private final NodeGraphPortRef from;
    private final NodeGraphPortRef to;
    private boolean selected;
    private boolean enabled = true;
    private String type = "";
    private NodeGraph owner;

    public NodeGraphConnection(String id, String fromItemId, String fromPortId, String toItemId, String toPortId) {
        this(id, new NodeGraphPortRef(fromItemId, fromPortId), new NodeGraphPortRef(toItemId, toPortId));
    }

    public NodeGraphConnection(String id, NodeGraphPortRef from, NodeGraphPortRef to) {
        this.id = normalizeId(id);
        this.from = from == null ? new NodeGraphPortRef("", "") : from;
        this.to = to == null ? new NodeGraphPortRef("", "") : to;
    }

    public String id() {
        return id;
    }

    public NodeGraphPortRef from() {
        return from;
    }

    public String fromItemId() {
        return from.itemId();
    }

    public String fromPortId() {
        return from.portId();
    }

    public NodeGraphPortRef to() {
        return to;
    }

    public String toItemId() {
        return to.itemId();
    }

    public String toPortId() {
        return to.portId();
    }

    public boolean selected() {
        return selected;
    }

    public boolean enabled() {
        return enabled;
    }

    public NodeGraphConnection enabled(boolean enabled) {
        if (this.enabled == enabled) return this;
        this.enabled = enabled;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    public String type() {
        return type;
    }

    public NodeGraphConnection type(String type) {
        String next = type == null ? "" : type;
        if (this.type.equals(next)) return this;
        this.type = next;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    NodeGraph owner() {
        return owner;
    }

    void owner(NodeGraph owner) {
        this.owner = owner;
    }

    void selectedInternal(boolean selected) {
        this.selected = selected;
    }

    private void invalidateOwner(int flags) {
        if (owner != null) {
            owner.invalidate(flags);
        }
    }

    private static String normalizeId(String id) {
        String normalized = id == null ? "" : id.trim();
        return normalized.isEmpty() ? "connection" : normalized;
    }
}

