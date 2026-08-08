package dev.sixik.unigui.api.layout.v3;

import dev.sixik.unigui.api.math.RectView;

import java.util.Objects;

/** Calculated bounds for one Layout V3 node. */
public record LayoutResult(
        LayoutNodeId id,
        float x,
        float y,
        float width,
        float height,
        float contentWidth,
        float contentHeight,
        float overflowWidth,
        float overflowHeight) implements RectView {
    public LayoutResult(LayoutNodeId id,
                        float x,
                        float y,
                        float width,
                        float height,
                        float contentWidth,
                        float contentHeight) {
        this(id, x, y, width, height, contentWidth, contentHeight, contentWidth, contentHeight);
    }

    public LayoutResult {
        id = Objects.requireNonNull(id, "id");
        x = sanitizePosition(x);
        y = sanitizePosition(y);
        width = sanitizeSize(width);
        height = sanitizeSize(height);
        contentWidth = sanitizeSize(contentWidth);
        contentHeight = sanitizeSize(contentHeight);
        overflowWidth = sanitizeSize(overflowWidth);
        overflowHeight = sanitizeSize(overflowHeight);
    }

    public static LayoutResult of(LayoutNodeId id, float x, float y, float width, float height) {
        return new LayoutResult(id, x, y, width, height, width, height);
    }

    private static float sanitizePosition(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float sanitizeSize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
