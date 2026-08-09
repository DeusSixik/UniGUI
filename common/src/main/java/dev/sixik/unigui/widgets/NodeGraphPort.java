package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;

public final class NodeGraphPort {
    private final String id;
    private NodeGraphPortKind kind;
    private NodeGraphPortSide side;
    private float offset;
    private String type = "";
    private boolean enabled = true;
    private boolean visible = true;
    private NodeGraphItem owner;

    public NodeGraphPort(String id, NodeGraphPortKind kind, NodeGraphPortSide side, float offset) {
        this.id = normalizeId(id);
        this.kind = kind == null ? NodeGraphPortKind.BIDIRECTIONAL : kind;
        this.side = side == null ? NodeGraphPortSide.RIGHT : side;
        this.offset = sanitizeOffset(offset);
    }

    public String id() {
        return id;
    }

    public String itemId() {
        return owner == null ? "" : owner.id();
    }

    public NodeGraphPortRef ref() {
        return new NodeGraphPortRef(itemId(), id);
    }

    public NodeGraphPortKind kind() {
        return kind;
    }

    public NodeGraphPort kind(NodeGraphPortKind kind) {
        NodeGraphPortKind next = kind == null ? NodeGraphPortKind.BIDIRECTIONAL : kind;
        if (this.kind == next) return this;
        this.kind = next;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    public NodeGraphPortSide side() {
        return side;
    }

    public NodeGraphPort side(NodeGraphPortSide side) {
        NodeGraphPortSide next = side == null ? NodeGraphPortSide.RIGHT : side;
        if (this.side == next) return this;
        this.side = next;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    public float offset() {
        return offset;
    }

    public NodeGraphPort offset(float offset) {
        float next = sanitizeOffset(offset);
        if (this.offset == next) return this;
        this.offset = next;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    public String type() {
        return type;
    }

    public NodeGraphPort type(String type) {
        String next = type == null ? "" : type;
        if (this.type.equals(next)) return this;
        this.type = next;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean enabled() {
        return enabled;
    }

    public NodeGraphPort enabled(boolean enabled) {
        if (this.enabled == enabled) return this;
        this.enabled = enabled;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean visible() {
        return visible;
    }

    public NodeGraphPort visible(boolean visible) {
        if (this.visible == visible) return this;
        this.visible = visible;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    NodeGraphItem owner() {
        return owner;
    }

    void owner(NodeGraphItem owner) {
        this.owner = owner;
    }

    private void invalidateOwner(int flags) {
        if (owner != null && owner.owner() != null) {
            owner.owner().invalidate(flags);
        }
    }

    private static String normalizeId(String id) {
        String normalized = id == null ? "" : id.trim();
        return normalized.isEmpty() ? "port" : normalized;
    }

    private static float sanitizeOffset(float value) {
        if (!Float.isFinite(value)) return 0.5f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}

