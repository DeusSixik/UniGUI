package dev.sixik.unigui.api.core;

/**
 * Unity Canvas Scaler-like provider.
 *
 * <p>Uses a reference resolution and a width/height match value to calculate
 * continuous UI scale from the real viewport/framebuffer size.</p>
 */
public final class UnityLikeUIScaleProvider implements UIScaleProvider {
    public static final float DEFAULT_REFERENCE_WIDTH = 1920.0f;
    public static final float DEFAULT_REFERENCE_HEIGHT = 1080.0f;

    private float referenceWidth = DEFAULT_REFERENCE_WIDTH;
    private float referenceHeight = DEFAULT_REFERENCE_HEIGHT;

    private float viewportWidth = DEFAULT_REFERENCE_WIDTH;
    private float viewportHeight = DEFAULT_REFERENCE_HEIGHT;

    /**
     * 0.0 = match width, 1.0 = match height, 0.5 = balanced.
     */
    private float match = 1.0f;

    private float userScale = 1.0f;
    private float minScale = 0.75f;
    private float maxScale = 2.5f;

    @Override
    public float scale() {
        float scaleX = viewportWidth / referenceWidth;
        float scaleY = viewportHeight / referenceHeight;

        float logX = (float) (Math.log(scaleX) / Math.log(2.0));
        float logY = (float) (Math.log(scaleY) / Math.log(2.0));
        float matchedScale = (float) Math.pow(2.0, lerp(logX, logY, match));

        return clamp(matchedScale * userScale, minScale, maxScale);
    }

    public UnityLikeUIScaleProvider viewport(float width, float height) {
        this.viewportWidth = sanitizeSize(width, referenceWidth);
        this.viewportHeight = sanitizeSize(height, referenceHeight);
        return this;
    }

    public UnityLikeUIScaleProvider referenceResolution(float width, float height) {
        this.referenceWidth = sanitizeSize(width, DEFAULT_REFERENCE_WIDTH);
        this.referenceHeight = sanitizeSize(height, DEFAULT_REFERENCE_HEIGHT);
        return this;
    }

    public UnityLikeUIScaleProvider match(float match) {
        this.match = clamp(match, 0.0f, 1.0f);
        return this;
    }

    public UnityLikeUIScaleProvider matchWidth() {
        return match(0.0f);
    }

    public UnityLikeUIScaleProvider matchBalanced() {
        return match(0.5f);
    }

    public UnityLikeUIScaleProvider matchHeight() {
        return match(1.0f);
    }

    public UnityLikeUIScaleProvider userScale(float userScale) {
        this.userScale = Math.max(0.01f, finiteOr(userScale, 1.0f));
        return this;
    }

    public UnityLikeUIScaleProvider scaleRange(float minScale, float maxScale) {
        float min = Math.max(0.01f, finiteOr(minScale, 0.75f));
        float max = Math.max(min, finiteOr(maxScale, 2.5f));
        this.minScale = min;
        this.maxScale = max;
        return this;
    }

    public float referenceWidth() {
        return referenceWidth;
    }

    public float referenceHeight() {
        return referenceHeight;
    }

    public float viewportWidth() {
        return viewportWidth;
    }

    public float viewportHeight() {
        return viewportHeight;
    }

    public float match() {
        return match;
    }

    public float userScale() {
        return userScale;
    }

    public float minScale() {
        return minScale;
    }

    public float maxScale() {
        return maxScale;
    }

    @Override
    public void viewportSize(float width, float height) {
        viewport(width, height);
    }

    private static float sanitizeSize(float value, float fallback) {
        return Math.max(1.0f, finiteOr(value, fallback));
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}