package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.text.RichText;

public record ButtonState(
        ButtonRenderType type,
        float x,
        float y,
        float width,
        float height,
        String text,
        RichText richText,
        float textPaddingX,
        float textWidth,
        float textHeight,
        ColorView textColor,
        boolean pressed,
        boolean hovered,
        boolean enabled,
        boolean checked,
        float indicatorSize,
        float indicatorInnerSize,
        float indicatorGap,
        ColorView indicatorColor,
        ColorView indicatorBorderColor
) {
    public boolean hasText() {
        return richText != null && !richText.isEmpty();
    }

    public float textContentX() {
        return x + textPaddingX;
    }

    public float textContentWidth() {
        return Math.max(0.0f, width - textPaddingX * 2.0f);
    }
}
