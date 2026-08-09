package dev.sixik.unigui.widgets.render;

public record NodeGraphSelectionBoxState(
        boolean visible,
        float x,
        float y,
        float width,
        float height
) {
    public static final NodeGraphSelectionBoxState HIDDEN =
            new NodeGraphSelectionBoxState(false, 0.0f, 0.0f, 0.0f, 0.0f);
}

