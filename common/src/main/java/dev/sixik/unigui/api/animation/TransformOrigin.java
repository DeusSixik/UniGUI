package dev.sixik.unigui.api.animation;

/**
 * Named pivot positions used by widget transform animations.
 *
 * <p>The origin is resolved against the widget layout bounds. {@link #CUSTOM}
 * leaves the current transform pivot untouched and is used when code sets or
 * animates a custom pivot in pixels.</p>
 */
public enum TransformOrigin {
    CUSTOM(Float.NaN, Float.NaN),
    LEFT_TOP(0.0f, 0.0f),
    TOP(0.5f, 0.0f),
    RIGHT_TOP(1.0f, 0.0f),
    LEFT_CENTER(0.0f, 0.5f),
    CENTER(0.5f, 0.5f),
    RIGHT_CENTER(1.0f, 0.5f),
    LEFT_BOTTOM(0.0f, 1.0f),
    BOTTOM(0.5f, 1.0f),
    RIGHT_BOTTOM(1.0f, 1.0f);

    private final float relativeX;
    private final float relativeY;

    TransformOrigin(float relativeX, float relativeY) {
        this.relativeX = relativeX;
        this.relativeY = relativeY;
    }

    public float relativeX() {
        return relativeX;
    }

    public float relativeY() {
        return relativeY;
    }

    public boolean custom() {
        return this == CUSTOM;
    }
}