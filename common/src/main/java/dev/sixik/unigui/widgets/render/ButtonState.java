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
        boolean indeterminate,
        float indicatorSize,
        float indicatorInnerSize,
        float indicatorGap,
        ColorView indicatorColor,
        ColorView indicatorBorderColor,
        float indicatorProgress,
        boolean labelLeft
) {
    public ButtonState {
        indicatorProgress = clamp01(indicatorProgress);
    }

    public ButtonState(ButtonRenderType type,
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
                       boolean indeterminate,
                       float indicatorSize,
                       float indicatorInnerSize,
                       float indicatorGap,
                       ColorView indicatorColor,
                       ColorView indicatorBorderColor,
                       float indicatorProgress) {
        this(type,
                x,
                y,
                width,
                height,
                text,
                richText,
                textPaddingX,
                textWidth,
                textHeight,
                textColor,
                pressed,
                hovered,
                enabled,
                checked,
                indeterminate,
                indicatorSize,
                indicatorInnerSize,
                indicatorGap,
                indicatorColor,
                indicatorBorderColor,
                indicatorProgress,
                false);
    }

    public ButtonState(ButtonRenderType type,
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
                       boolean indeterminate,
                       float indicatorSize,
                       float indicatorInnerSize,
                       float indicatorGap,
                       ColorView indicatorColor,
                       ColorView indicatorBorderColor) {
        this(type,
                x,
                y,
                width,
                height,
                text,
                richText,
                textPaddingX,
                textWidth,
                textHeight,
                textColor,
                pressed,
                hovered,
                enabled,
                checked,
                indeterminate,
                indicatorSize,
                indicatorInnerSize,
                indicatorGap,
                indicatorColor,
                indicatorBorderColor,
                checked ? 1.0f : 0.0f,
                false);
    }

    public boolean hasText() {
        return richText != null && !richText.isEmpty();
    }

    public float textContentX() {
        return x + textPaddingX;
    }

    public float textContentWidth() {
        return Math.max(0.0f, width - textPaddingX * 2.0f);
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
