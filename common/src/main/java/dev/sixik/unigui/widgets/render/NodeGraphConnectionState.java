package dev.sixik.unigui.widgets.render;

public record NodeGraphConnectionState(
        String id,
        String fromItemId,
        String fromPortId,
        String toItemId,
        String toPortId,
        float startX,
        float startY,
        float endX,
        float endY,
        boolean selected,
        boolean hovered,
        boolean enabled,
        String type
) {
    public NodeGraphConnectionState {
        id = id == null ? "" : id;
        fromItemId = fromItemId == null ? "" : fromItemId;
        fromPortId = fromPortId == null ? "" : fromPortId;
        toItemId = toItemId == null ? "" : toItemId;
        toPortId = toPortId == null ? "" : toPortId;
        type = type == null ? "" : type;
    }
}

