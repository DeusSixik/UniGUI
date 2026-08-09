package dev.sixik.unigui.widgets.render;

public record NodeGraphConnectionPreviewState(
        boolean visible,
        String fromItemId,
        String fromPortId,
        String toItemId,
        String toPortId,
        float startX,
        float startY,
        float endX,
        float endY,
        boolean valid,
        String reason
) {
    public static final NodeGraphConnectionPreviewState HIDDEN =
            new NodeGraphConnectionPreviewState(false, "", "", "", "", 0.0f, 0.0f, 0.0f, 0.0f, false, "");

    public NodeGraphConnectionPreviewState {
        fromItemId = fromItemId == null ? "" : fromItemId;
        fromPortId = fromPortId == null ? "" : fromPortId;
        toItemId = toItemId == null ? "" : toItemId;
        toPortId = toPortId == null ? "" : toPortId;
        reason = reason == null ? "" : reason;
    }
}

