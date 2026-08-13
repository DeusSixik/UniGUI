package dev.sixik.unigui.api.viewport;

public record ViewportChange(
        float oldX,
        float oldY,
        float oldZoom,
        float newX,
        float newY,
        float newZoom
) {
    public boolean changed() {
        return oldX != newX || oldY != newY || oldZoom != newZoom;
    }
}