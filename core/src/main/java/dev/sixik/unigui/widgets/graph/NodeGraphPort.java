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

/**
 * Pin/port descriptor for a {@link NodeGraphItem}.
 *
 * <p>Terminology: {@link #kind()} is connection direction, {@link #side()} is
 * visual edge placement, and {@link #type()} is an optional compatibility or
 * data label used by connection policies.</p>
 */
@AiDoc(summary = "Pin/port descriptor on a NodeGraphItem connecting lines to other nodes.")
@WikiPage(
        title = "NodeGraphPort",
        slug = "node-graph-port",
        category = "<key:graph.category>",
        summary = "<key:node.graph.port.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.port.section.overview.body>",
        order = 10
)
public final class NodeGraphPort {
    private final String id;
    private NodeGraphPortKind kind;
    private NodeGraphPortSide side;
    private float offset;
    private String type = "";
    private boolean enabled = true;
    private boolean visible = true;
    private NodeGraphItem owner;

    @AiDoc(summary = "Constructs a NodeGraphPort with ID, direction kind, visual edge side, and pixel offset.")
    @WikiMethod(summary = "<key:node.graph.port.method.ctor>")
    public NodeGraphPort(@WikiParameter(name = "id", description = "<key:node.graph.port.param.id>") String id, @WikiParameter(name = "kind", description = "<key:node.graph.port.param.kind>") NodeGraphPortKind kind, @WikiParameter(name = "side", description = "<key:node.graph.port.param.side>") NodeGraphPortSide side, @WikiParameter(name = "offset", description = "<key:node.graph.port.param.offset>") float offset) {
        this.id = normalizeId(id);
        this.kind = kind == null ? NodeGraphPortKind.BIDIRECTIONAL : kind;
        this.side = side == null ? NodeGraphPortSide.RIGHT : side;
        this.offset = sanitizeOffset(offset);
    }

    @AiDoc(summary = "Returns the port identifier unique within its owning item.")
    @WikiMethod(summary = "<key:node.graph.port.method.id>")
    @WikiReturn("<key:node.graph.port.return.id>")
    public String id() {
        return id;
    }

    @AiDoc(summary = "Returns the ID of the item owning this port.")
    @WikiMethod(summary = "<key:node.graph.port.method.itemId>")
    @WikiReturn("<key:node.graph.port.return.itemId>")
    public String itemId() {
        return owner == null ? "" : owner.id();
    }

    @AiDoc(summary = "Returns the compound NodeGraphPortRef pointing to this port.")
    @WikiMethod(summary = "<key:node.graph.port.method.ref>")
    @WikiReturn("<key:node.graph.port.return.ref>")
    public NodeGraphPortRef ref() {
        return new NodeGraphPortRef(itemId(), id);
    }

    @AiDoc(summary = "Returns the connection direction kind.")
    @WikiMethod(summary = "<key:node.graph.port.method.kindGet>")
    @WikiReturn("<key:node.graph.port.return.kind>")
    public NodeGraphPortKind kind() {
        return kind;
    }

    @AiDoc(summary = "Sets the connection direction kind.")
    @WikiMethod(summary = "<key:node.graph.port.method.kindSet>")
    @WikiReturn("<key:node.graph.port.return.self>")
    public NodeGraphPort kind(@WikiParameter(name = "kind", description = "<key:node.graph.port.param.kind>") NodeGraphPortKind kind) {
        NodeGraphPortKind next = kind == null ? NodeGraphPortKind.BIDIRECTIONAL : kind;
        if (this.kind == next) return this;
        this.kind = next;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns the visual edge side where the port is rendered.")
    @WikiMethod(summary = "<key:node.graph.port.method.sideGet>")
    @WikiReturn("<key:node.graph.port.return.side>")
    public NodeGraphPortSide side() {
        return side;
    }

    @AiDoc(summary = "Sets the visual edge side where the port is rendered.")
    @WikiMethod(summary = "<key:node.graph.port.method.sideSet>")
    @WikiReturn("<key:node.graph.port.return.self>")
    public NodeGraphPort side(@WikiParameter(name = "side", description = "<key:node.graph.port.param.side>") NodeGraphPortSide side) {
        NodeGraphPortSide next = side == null ? NodeGraphPortSide.RIGHT : side;
        if (this.side == next) return this;
        this.side = next;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns the offset along the node edge in pixels.")
    @WikiMethod(summary = "<key:node.graph.port.method.offsetGet>")
    @WikiReturn("<key:node.graph.port.return.offset>")
    public float offset() {
        return offset;
    }

    @AiDoc(summary = "Sets the offset along the node edge in pixels.")
    @WikiMethod(summary = "<key:node.graph.port.method.offsetSet>")
    @WikiReturn("<key:node.graph.port.return.self>")
    public NodeGraphPort offset(@WikiParameter(name = "offset", description = "<key:node.graph.port.param.offset>") float offset) {
        float next = sanitizeOffset(offset);
        if (this.offset == next) return this;
        this.offset = next;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns the data type or compatibility label for connection validation.")
    @WikiMethod(summary = "<key:node.graph.port.method.typeGet>")
    @WikiReturn("<key:node.graph.port.return.type>")
    public String type() {
        return type;
    }

    @AiDoc(summary = "Sets the data type or compatibility label.")
    @WikiMethod(summary = "<key:node.graph.port.method.typeSet>")
    @WikiReturn("<key:node.graph.port.return.self>")
    public NodeGraphPort type(@WikiParameter(name = "type", description = "<key:node.graph.port.param.type>") String type) {
        String next = type == null ? "" : type;
        if (this.type.equals(next)) return this;
        this.type = next;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns true if this port is interactive and can accept or initiate links.")
    @WikiMethod(summary = "<key:node.graph.port.method.enabledGet>")
    @WikiReturn("<key:node.graph.port.return.enabled>")
    public boolean enabled() {
        return enabled;
    }

    @AiDoc(summary = "Sets whether this port is interactive.")
    @WikiMethod(summary = "<key:node.graph.port.method.enabledSet>")
    @WikiReturn("<key:node.graph.port.return.self>")
    public NodeGraphPort enabled(@WikiParameter(name = "enabled", description = "<key:node.graph.port.param.enabled>") boolean enabled) {
        if (this.enabled == enabled) return this;
        this.enabled = enabled;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns true if this port is visible.")
    @WikiMethod(summary = "<key:node.graph.port.method.visibleGet>")
    @WikiReturn("<key:node.graph.port.return.visible>")
    public boolean visible() {
        return visible;
    }

    @AiDoc(summary = "Sets whether this port is visible.")
    @WikiMethod(summary = "<key:node.graph.port.method.visibleSet>")
    @WikiReturn("<key:node.graph.port.return.self>")
    public NodeGraphPort visible(@WikiParameter(name = "visible", description = "<key:node.graph.port.param.visible>") boolean visible) {
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

