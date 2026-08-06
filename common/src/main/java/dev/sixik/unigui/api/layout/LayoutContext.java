package dev.sixik.unigui.api.layout;

public final class LayoutContext {
    private final float availableWidth;
    private final float availableHeight;

    public LayoutContext(float availableWidth, float availableHeight) {
        this.availableWidth = sanitizeAvailable(availableWidth);
        this.availableHeight = sanitizeAvailable(availableHeight);
    }

    public float availableWidth() {
        return availableWidth;
    }

    public float availableHeight() {
        return availableHeight;
    }

    private static float sanitizeAvailable(float value) {
        if (Float.isNaN(value)) return 0.0f;
        return Float.isFinite(value) ? Math.max(0.0f, value) : Float.POSITIVE_INFINITY;
    }
}
