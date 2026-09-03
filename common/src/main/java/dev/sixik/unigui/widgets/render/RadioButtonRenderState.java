package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.text.RichText;

/** Typed render state semantic role radio button. */
public record RadioButtonRenderState(
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
        float outerSize,
        float innerSize,
        float textGap,
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
    public RadioButtonRenderState {
        text = text == null ? "" : text;
        richText = richText == null ? RichText.plain("") : richText;
        indicatorProgress = clamp01(indicatorProgress);
        radius = Math.max(0.0f, radius);
        borderWidth = Math.max(0.0f, borderWidth);
    }

    /** @return {@code true}, если label содержит текст для отрисовки */
    public boolean hasText() {
        return !richText.isEmpty();
    }

    /** Временный переход к старому ButtonState для legacy renderer и RenderPlan. */
    public ButtonState toLegacyButtonState() {
        return new ButtonState(
                ButtonRenderType.RADIO_BUTTON,
                x, y, width, height, text, richText,
                textPaddingX, textWidth, textHeight, textColor,
                pressed, hovered, enabled, checked, false,
                outerSize, innerSize, textGap,
                indicatorColor, indicatorBorderColor, indicatorProgress, labelLeft,
                backgroundVisible, backgroundColor, radius,
                borderVisible, borderColor, borderWidth);
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
