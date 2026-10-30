package dev.sixik.unigui.api.viewport;

public record ViewportPoint(float x, float y) {
    public static final ViewportPoint ZERO = new ViewportPoint(0.0f, 0.0f);
}