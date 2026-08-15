package dev.sixik.unigui.widgets.graph;



public record NodeGraphConnectionSnapshot(
        String id,
        NodeGraphPortRef from,
        NodeGraphPortRef to,
        boolean enabled,
        String type
) {
    public NodeGraphConnectionSnapshot {
        id = id == null ? "" : id;
        from = from == null ? new NodeGraphPortRef("", "") : from;
        to = to == null ? new NodeGraphPortRef("", "") : to;
        type = type == null ? "" : type;
    }
}

