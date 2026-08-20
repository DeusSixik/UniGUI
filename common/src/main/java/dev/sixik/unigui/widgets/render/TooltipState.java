package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.text.RichText;

import java.util.List;

public record TooltipState(
        float x,
        float y,
        float width,
        float height,
        boolean backgroundVisible,
        ColorView backgroundColor,
        float radius,
        boolean borderVisible,
        ColorView borderColor,
        float borderWidth,
        float textX,
        float textY,
        float textWidth,
        float textHeight,
        List<RichText> lines,
        float[] lineHeights,
        ColorView textColor
) {
    private static final MutableColor TRANSPARENT = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);

    public TooltipState {
        backgroundColor = backgroundColor == null ? TRANSPARENT : backgroundColor;
        borderColor = borderColor == null ? TRANSPARENT : borderColor;
        radius = Math.max(0.0f, radius);
        borderWidth = Math.max(0.0f, borderWidth);
        lines = lines == null ? List.of() : List.copyOf(lines);
        lineHeights = lineHeights == null ? new float[0] : java.util.Arrays.copyOf(lineHeights, lineHeights.length);
    }

    public float lineHeight(int index) {
        return index < 0 || index >= lineHeights.length ? 0.0f : lineHeights[index];
    }
}