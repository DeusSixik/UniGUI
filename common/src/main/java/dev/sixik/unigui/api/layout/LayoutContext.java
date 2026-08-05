package dev.sixik.unigui.api.layout;

public final class LayoutContext {
    private final float availableWidth;
    private final float availableHeight;

    public LayoutContext(float availableWidth, float availableHeight) {
        this.availableWidth = availableWidth;
        this.availableHeight = availableHeight;
    }

    public float availableWidth() {
        return availableWidth;
    }

    public float availableHeight() {
        return availableHeight;
    }
}
