package dev.sixik.unigui.api.viewport;

public record ViewportBounds(float x, float y, float width, float height) {
    public static final ViewportBounds EMPTY = new ViewportBounds(0.0f, 0.0f, 0.0f, 0.0f);

    public ViewportBounds {
        x = sanitize(x);
        y = sanitize(y);
        width = sanitizeNonNegative(width);
        height = sanitizeNonNegative(height);
    }

    public float right() {
        return x + width;
    }

    public float bottom() {
        return y + height;
    }

    public boolean empty() {
        return width <= 0.0f || height <= 0.0f;
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float sanitizeNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}