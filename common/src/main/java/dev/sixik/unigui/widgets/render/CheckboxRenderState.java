package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.text.RichText;

/**
 * Typed render state checkbox-контрола.
 *
 * <p>Состояние намеренно не использует {@link ButtonState}: checkbox имеет собственную
 * semantic role и indicator-specific данные. Метод {@link #toLegacyButtonState()} нужен
 * только переходному compatibility adapter'у.</p>
 */
public record CheckboxRenderState(
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
    public CheckboxRenderState {
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

    /**
     * Временно преобразует typed state в старый state кнопки.
     * Используется только для legacy renderer'ов и старых RenderPlan.
     */
    public ButtonState toLegacyButtonState() {
        return new ButtonState(
                ButtonRenderType.CHECKBOX,
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
                backgroundVisible,
                backgroundColor,
                radius,
                borderVisible,
                borderColor,
                borderWidth);
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
