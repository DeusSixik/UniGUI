package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.text.RichText;

/** Typed render state semantic role toggle button. */
public record ToggleButtonRenderState(
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
        ColorView checkedBackground,
        ColorView uncheckedBackground,
        boolean backgroundVisible,
        ColorView backgroundColor,
        float radius,
        boolean borderVisible,
        ColorView borderColor,
        float borderWidth
) {
    public ToggleButtonRenderState {
        text = text == null ? "" : text;
        richText = richText == null ? RichText.plain("") : richText;
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
                ButtonRenderType.TOGGLE_BUTTON,
                x, y, width, height, text, richText,
                textPaddingX, textWidth, textHeight, textColor,
                pressed, hovered, enabled, checked, false,
                0.0f, 0.0f, 0.0f,
                checkedBackground, uncheckedBackground, checked ? 1.0f : 0.0f, false,
                backgroundVisible, backgroundColor, radius,
                borderVisible, borderColor, borderWidth);
    }
}
