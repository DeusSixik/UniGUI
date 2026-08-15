package dev.sixik.unigui.widgets.graph;



public record NodeGraphPortSnapshot(
        String id,
        NodeGraphPortKind kind,
        NodeGraphPortSide side,
        float offset,
        String type,
        boolean enabled,
        boolean visible
) {
    public NodeGraphPortSnapshot {
        id = id == null ? "" : id;
        kind = kind == null ? NodeGraphPortKind.BIDIRECTIONAL : kind;
        side = side == null ? NodeGraphPortSide.RIGHT : side;
        type = type == null ? "" : type;
    }
}

