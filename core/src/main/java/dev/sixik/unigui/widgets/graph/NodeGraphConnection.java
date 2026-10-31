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

@AiDoc(summary = "Represents a directional or bidirectional connection between two ports in a NodeGraph.")
@WikiPage(
        title = "NodeGraphConnection",
        slug = "node-graph-connection",
        category = "<key:graph.category>",
        summary = "<key:node.graph.connection.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.connection.section.overview.body>",
        order = 10
)
public final class NodeGraphConnection {
    private final String id;
    private final NodeGraphPortRef from;
    private final NodeGraphPortRef to;
    private boolean selected;
    private boolean enabled = true;
    private String type = "";
    private NodeGraph owner;

    @AiDoc(summary = "Constructs a connection between ports by item and port IDs.")
    @WikiMethod(summary = "<key:node.graph.connection.method.ctorIds>")
    public NodeGraphConnection(@WikiParameter(name = "id", description = "<key:node.graph.connection.param.id>") String id, @WikiParameter(name = "fromItemId", description = "<key:node.graph.connection.param.fromItemId>") String fromItemId, @WikiParameter(name = "fromPortId", description = "<key:node.graph.connection.param.fromPortId>") String fromPortId, @WikiParameter(name = "toItemId", description = "<key:node.graph.connection.param.toItemId>") String toItemId, @WikiParameter(name = "toPortId", description = "<key:node.graph.connection.param.toPortId>") String toPortId) {
        this(id, new NodeGraphPortRef(fromItemId, fromPortId), new NodeGraphPortRef(toItemId, toPortId));
    }

    @AiDoc(summary = "Constructs a connection between ports specified by port references.")
    @WikiMethod(summary = "<key:node.graph.connection.method.ctorRefs>")
    public NodeGraphConnection(@WikiParameter(name = "id", description = "<key:node.graph.connection.param.id>") String id, @WikiParameter(name = "from", description = "<key:node.graph.connection.param.from>") NodeGraphPortRef from, @WikiParameter(name = "to", description = "<key:node.graph.connection.param.to>") NodeGraphPortRef to) {
        this.id = normalizeId(id);
        this.from = from == null ? new NodeGraphPortRef("", "") : from;
        this.to = to == null ? new NodeGraphPortRef("", "") : to;
    }

    @AiDoc(summary = "Returns the unique identifier of this connection.")
    @WikiMethod(summary = "<key:node.graph.connection.method.id>")
    @WikiReturn("<key:node.graph.connection.return.id>")
    public String id() {
        return id;
    }

    @AiDoc(summary = "Returns the source port reference.")
    @WikiMethod(summary = "<key:node.graph.connection.method.from>")
    @WikiReturn("<key:node.graph.connection.return.from>")
    public NodeGraphPortRef from() {
        return from;
    }

    @AiDoc(summary = "Returns the source node item ID.")
    @WikiMethod(summary = "<key:node.graph.connection.method.fromItemId>")
    @WikiReturn("<key:node.graph.connection.return.itemId>")
    public String fromItemId() {
        return from.itemId();
    }

    @AiDoc(summary = "Returns the source port ID.")
    @WikiMethod(summary = "<key:node.graph.connection.method.fromPortId>")
    @WikiReturn("<key:node.graph.connection.return.portId>")
    public String fromPortId() {
        return from.portId();
    }

    @AiDoc(summary = "Returns the destination port reference.")
    @WikiMethod(summary = "<key:node.graph.connection.method.to>")
    @WikiReturn("<key:node.graph.connection.return.to>")
    public NodeGraphPortRef to() {
        return to;
    }

    @AiDoc(summary = "Returns the destination node item ID.")
    @WikiMethod(summary = "<key:node.graph.connection.method.toItemId>")
    @WikiReturn("<key:node.graph.connection.return.itemId>")
    public String toItemId() {
        return to.itemId();
    }

    @AiDoc(summary = "Returns the destination port ID.")
    @WikiMethod(summary = "<key:node.graph.connection.method.toPortId>")
    @WikiReturn("<key:node.graph.connection.return.portId>")
    public String toPortId() {
        return to.portId();
    }

    @AiDoc(summary = "Returns true if this connection is currently selected.")
    @WikiMethod(summary = "<key:node.graph.connection.method.selected>")
    @WikiReturn("<key:node.graph.connection.return.selected>")
    public boolean selected() {
        return selected;
    }

    @AiDoc(summary = "Returns true if this connection is active and enabled.")
    @WikiMethod(summary = "<key:node.graph.connection.method.enabledGet>")
    @WikiReturn("<key:node.graph.connection.return.enabled>")
    public boolean enabled() {
        return enabled;
    }

    @AiDoc(summary = "Sets whether this connection is active and enabled.")
    @WikiMethod(summary = "<key:node.graph.connection.method.enabledSet>")
    @WikiReturn("<key:node.graph.connection.return.self>")
    public NodeGraphConnection enabled(@WikiParameter(name = "enabled", description = "<key:node.graph.connection.param.enabled>") boolean enabled) {
        if (this.enabled == enabled) return this;
        this.enabled = enabled;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns the connection data type or compatibility category string.")
    @WikiMethod(summary = "<key:node.graph.connection.method.typeGet>")
    @WikiReturn("<key:node.graph.connection.return.type>")
    public String type() {
        return type;
    }

    @AiDoc(summary = "Sets the connection data type or compatibility category string.")
    @WikiMethod(summary = "<key:node.graph.connection.method.typeSet>")
    @WikiReturn("<key:node.graph.connection.return.self>")
    public NodeGraphConnection type(@WikiParameter(name = "type", description = "<key:node.graph.connection.param.type>") String type) {
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

