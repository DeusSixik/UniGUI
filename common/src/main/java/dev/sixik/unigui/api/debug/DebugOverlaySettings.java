package dev.sixik.unigui.api.debug;

public final class DebugOverlaySettings {
    public static final int DEFAULT_SAMPLE_WINDOW = 100;
    public static final int MAX_SAMPLE_WINDOW = 10_000;
    public static final float DEFAULT_SCALE = 0.75f;

    private DebugOverlayAnchor anchor = DebugOverlayAnchor.TOP_LEFT;
    private float scale = DEFAULT_SCALE;
    private int sampleWindow = DEFAULT_SAMPLE_WINDOW;
    private float margin = 4.0f;

    public DebugOverlayAnchor anchor() {
        return anchor;
    }

    public DebugOverlaySettings anchor(DebugOverlayAnchor anchor) {
        this.anchor = anchor == null ? DebugOverlayAnchor.TOP_LEFT : anchor;
        return this;
    }

    public float scale() {
        return scale;
    }

    public DebugOverlaySettings scale(float scale) {
        this.scale = Float.isFinite(scale) ? clamp(scale, 0.25f, 4.0f) : DEFAULT_SCALE;
        return this;
    }

    public int sampleWindow() {
        return sampleWindow;
    }

    public DebugOverlaySettings sampleWindow(int sampleWindow) {
        this.sampleWindow = Math.max(1, Math.min(MAX_SAMPLE_WINDOW, sampleWindow));
        return this;
    }

    public float margin() {
        return margin;
    }

    public DebugOverlaySettings margin(float margin) {
        this.margin = Float.isFinite(margin) ? Math.max(0.0f, margin) : 4.0f;
        return this;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
