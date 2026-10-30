package dev.sixik.unigui.api.layout;

public record LayoutSize(float width, float height) {
    public static final LayoutSize ZERO = new LayoutSize(0.0f, 0.0f);

    public LayoutSize {
        width = sanitize(width);
        height = sanitize(height);
    }

    public static LayoutSize of(float width, float height) {
        return new LayoutSize(width, height);
    }

    public LayoutSize withMargin(EdgeInsets margin) {
        EdgeInsets safeMargin = margin == null ? EdgeInsets.ZERO : margin;
        return new LayoutSize(width + safeMargin.horizontal(), height + safeMargin.vertical());
    }

    public LayoutSize resolve(LayoutConstraints constraints, LayoutContext context) {
        LayoutConstraints safeConstraints = constraints == null ? LayoutConstraints.DEFAULT : constraints;
        return new LayoutSize(
                resolveAxis(width, safeConstraints.preferredWidth(), safeConstraints.minWidth(), safeConstraints.maxWidth(),
                        context == null ? Float.POSITIVE_INFINITY : context.availableWidth()),
                resolveAxis(height, safeConstraints.preferredHeight(), safeConstraints.minHeight(), safeConstraints.maxHeight(),
                        context == null ? Float.POSITIVE_INFINITY : context.availableHeight()));
    }

    private static float resolveAxis(float content, float preferred, float min, float max, float available) {
        float desired = LayoutConstraints.isAuto(preferred) ? sanitize(content) : sanitize(preferred);
        desired = clamp(desired, min, max);
        if (Float.isFinite(available)) {
            desired = Math.min(desired, Math.max(0.0f, available));
        }
        return clamp(desired, min, max);
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, Math.max(0.0f, value)));
    }
}
