package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
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
        boolean labelLeft,
        boolean backgroundVisible,
        ColorView backgroundColor,
        float radius,
        boolean borderVisible,
        ColorView borderColor,
        float borderWidth
) {
    private static final MutableColor TRANSPARENT = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);

    public ButtonState {
        type = type == null ? ButtonRenderType.BUTTON : type;
        indicatorProgress = clamp01(indicatorProgress);
        radius = Math.max(0.0f, radius);
        borderWidth = Math.max(0.0f, borderWidth);
        backgroundColor = backgroundColor == null ? TRANSPARENT : backgroundColor;
        borderColor = borderColor == null ? indicatorBorderColor : borderColor;
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
                       float indicatorProgress,
                       boolean labelLeft) {
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
                labelLeft,
                defaultBackgroundVisible(type),
                indicatorColor,
                3.0f,
                defaultBackgroundVisible(type),
                indicatorBorderColor,
                1.0f);
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

    private static boolean defaultBackgroundVisible(ButtonRenderType type) {
        return type == ButtonRenderType.BUTTON || type == ButtonRenderType.TOGGLE_BUTTON;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}