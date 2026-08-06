package dev.sixik.unigui.api.layout;

public record EdgeInsets(float left, float top, float right, float bottom) {
    public static final EdgeInsets ZERO = new EdgeInsets(0.0f, 0.0f, 0.0f, 0.0f);

    public EdgeInsets {
        left = sanitize(left);
        top = sanitize(top);
        right = sanitize(right);
        bottom = sanitize(bottom);
    }

    public static EdgeInsets all(float value) {
        return new EdgeInsets(value, value, value, value);
    }

    public static EdgeInsets symmetric(float horizontal, float vertical) {
        return new EdgeInsets(horizontal, vertical, horizontal, vertical);
    }

    public float horizontal() {
        return left + right;
    }

    public float vertical() {
        return top + bottom;
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
