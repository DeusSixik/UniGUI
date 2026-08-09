package dev.sixik.unigui.widgets.render;

public record NodeGraphItemState(
        String id,
        float worldX,
        float worldY,
        float x,
        float y,
        float width,
        float height,
        boolean selected,
        boolean hovered,
        boolean dragging,
        boolean movable,
        boolean resizable
) {
    public NodeGraphItemState {
        id = id == null ? "" : id;
    }
}
