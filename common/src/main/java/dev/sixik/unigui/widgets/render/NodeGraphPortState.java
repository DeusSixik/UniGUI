package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.widgets.NodeGraphPortKind;
import dev.sixik.unigui.widgets.NodeGraphPortSide;

public record NodeGraphPortState(
        String itemId,
        String portId,
        NodeGraphPortKind kind,
        NodeGraphPortSide side,
        String type,
        float x,
        float y,
        float radius,
        boolean enabled,
        boolean hovered,
        boolean connectable
) {
    public NodeGraphPortState {
        itemId = itemId == null ? "" : itemId;
        portId = portId == null ? "" : portId;
        kind = kind == null ? NodeGraphPortKind.BIDIRECTIONAL : kind;
        side = side == null ? NodeGraphPortSide.RIGHT : side;
        type = type == null ? "" : type;
        radius = Float.isFinite(radius) ? Math.max(0.0f, radius) : 0.0f;
    }
}

