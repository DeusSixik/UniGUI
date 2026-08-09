package dev.sixik.unigui.widgets;

public record NodeGraphViewport(float x, float y, float zoom) {
    public NodeGraphViewport {
        x = sanitize(x);
        y = sanitize(y);
        zoom = sanitizeZoom(zoom);
    }

    public static NodeGraphViewport identity() {
        return new NodeGraphViewport(0.0f, 0.0f, 1.0f);
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float sanitizeZoom(float value) {
        return Float.isFinite(value) && value > 0.0f ? value : 1.0f;
    }
}

