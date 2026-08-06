package dev.sixik.unigui.api.layout;

public final class LayoutConstraints {
    public static final float AUTO = Float.NaN;
    public static final LayoutConstraints DEFAULT = new LayoutConstraints(
            AUTO, AUTO,
            0.0f, 0.0f,
            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            EdgeInsets.ZERO,
            Alignment.STRETCH, Alignment.STRETCH,
            1.0f);

    private final float preferredWidth;
    private final float preferredHeight;
    private final float minWidth;
    private final float minHeight;
    private final float maxWidth;
    private final float maxHeight;
    private final EdgeInsets margin;
    private final Alignment horizontalAlignment;
    private final Alignment verticalAlignment;
    private final float grow;

    public LayoutConstraints(float preferredWidth, float preferredHeight,
                             float minWidth, float minHeight,
                             float maxWidth, float maxHeight,
                             EdgeInsets margin,
                             Alignment horizontalAlignment, Alignment verticalAlignment,
                             float grow) {
        this.preferredWidth = sanitizeAuto(preferredWidth);
        this.preferredHeight = sanitizeAuto(preferredHeight);
        this.minWidth = sanitizeMin(minWidth);
        this.minHeight = sanitizeMin(minHeight);
        this.maxWidth = sanitizeMax(maxWidth, this.minWidth);
        this.maxHeight = sanitizeMax(maxHeight, this.minHeight);
        this.margin = margin == null ? EdgeInsets.ZERO : margin;
        this.horizontalAlignment = horizontalAlignment == null ? Alignment.STRETCH : horizontalAlignment;
        this.verticalAlignment = verticalAlignment == null ? Alignment.STRETCH : verticalAlignment;
        this.grow = Float.isFinite(grow) ? Math.max(0.0f, grow) : 0.0f;
    }

    public float preferredWidth() {
        return preferredWidth;
    }

    public float preferredHeight() {
        return preferredHeight;
    }

    public float minWidth() {
        return minWidth;
    }

    public float minHeight() {
        return minHeight;
    }

    public float maxWidth() {
        return maxWidth;
    }

    public float maxHeight() {
        return maxHeight;
    }

    public EdgeInsets margin() {
        return margin;
    }

    public Alignment horizontalAlignment() {
        return horizontalAlignment;
    }

    public Alignment verticalAlignment() {
        return verticalAlignment;
    }

    public float grow() {
        return grow;
    }

    public LayoutConstraints preferredSize(float width, float height) {
        return new LayoutConstraints(width, height, minWidth, minHeight, maxWidth, maxHeight, margin, horizontalAlignment, verticalAlignment, grow);
    }

    public LayoutConstraints minSize(float width, float height) {
        return new LayoutConstraints(preferredWidth, preferredHeight, width, height, maxWidth, maxHeight, margin, horizontalAlignment, verticalAlignment, grow);
    }

    public LayoutConstraints maxSize(float width, float height) {
        return new LayoutConstraints(preferredWidth, preferredHeight, minWidth, minHeight, width, height, margin, horizontalAlignment, verticalAlignment, grow);
    }

    public LayoutConstraints margin(EdgeInsets margin) {
        return new LayoutConstraints(preferredWidth, preferredHeight, minWidth, minHeight, maxWidth, maxHeight, margin, horizontalAlignment, verticalAlignment, grow);
    }

    public LayoutConstraints align(Alignment horizontal, Alignment vertical) {
        return new LayoutConstraints(preferredWidth, preferredHeight, minWidth, minHeight, maxWidth, maxHeight, margin, horizontal, vertical, grow);
    }

    public LayoutConstraints grow(float grow) {
        return new LayoutConstraints(preferredWidth, preferredHeight, minWidth, minHeight, maxWidth, maxHeight, margin, horizontalAlignment, verticalAlignment, grow);
    }

    public static boolean isAuto(float value) {
        return Float.isNaN(value);
    }

    private static float sanitizeAuto(float value) {
        if (Float.isNaN(value)) return AUTO;
        return Float.isFinite(value) ? Math.max(0.0f, value) : AUTO;
    }

    private static float sanitizeMin(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }

    private static float sanitizeMax(float value, float min) {
        if (!Float.isFinite(value)) return Float.POSITIVE_INFINITY;
        return Math.max(min, value);
    }
}
