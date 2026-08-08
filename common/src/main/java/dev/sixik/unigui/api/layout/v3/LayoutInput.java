package dev.sixik.unigui.api.layout.v3;

import dev.sixik.unigui.api.layout.LayoutContext;

/** Root constraints supplied to a Layout V3 compute pass. */
public record LayoutInput(float availableWidth, float availableHeight, float scale) {
    public LayoutInput {
        availableWidth = sanitizeAvailable(availableWidth);
        availableHeight = sanitizeAvailable(availableHeight);
        scale = Float.isFinite(scale) && scale > 0.0f ? scale : 1.0f;
    }

    public static LayoutInput of(float availableWidth, float availableHeight) {
        return new LayoutInput(availableWidth, availableHeight, 1.0f);
    }

    public LayoutContext toContext() {
        return new LayoutContext(availableWidth, availableHeight);
    }

    private static float sanitizeAvailable(float value) {
        if (Float.isNaN(value)) return 0.0f;
        return Float.isFinite(value) ? Math.max(0.0f, value) : Float.POSITIVE_INFINITY;
    }
}
