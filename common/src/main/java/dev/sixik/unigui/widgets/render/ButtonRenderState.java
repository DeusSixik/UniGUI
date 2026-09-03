package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.text.RichText;

/** Typed visual state обычной кнопки без discriminator-полей legacy ButtonState. */
public record ButtonRenderState(
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
        boolean backgroundVisible,
        ColorView backgroundColor,
        float radius,
        boolean borderVisible,
        ColorView borderColor,
        float borderWidth
) {
    public ButtonRenderState {
        text = text == null ? "" : text;
        richText = richText == null ? RichText.plain("") : richText;
        radius = Math.max(0.0f, radius);
        borderWidth = Math.max(0.0f, borderWidth);
    }

    public boolean hasText() {
        return !richText.isEmpty();
    }

    public float textContentX() {
        return x + textPaddingX;
    }

    public float textContentWidth() {
        return Math.max(0.0f, width - textPaddingX * 2.0f);
    }

    public static ButtonRenderState fromLegacyButtonState(ButtonState state) {
        if (state == null) return null;
        return new ButtonRenderState(
                state.x(), state.y(), state.width(), state.height(),
                state.text(), state.richText(), state.textPaddingX(),
                state.textWidth(), state.textHeight(), state.textColor(),
                state.pressed(), state.hovered(), state.enabled(),
                state.backgroundVisible(), state.backgroundColor(), state.radius(),
                state.borderVisible(), state.borderColor(), state.borderWidth());
    }

    /** Переходный adapter для старых ButtonRenderer и RenderPlan. */
    @Deprecated
    public ButtonState toLegacyButtonState() {
        return new ButtonState(
                ButtonRenderType.BUTTON,
                x, y, width, height, text, richText,
                textPaddingX, textWidth, textHeight, textColor,
                pressed, hovered, enabled,
                false, false, 0.0f, 0.0f, 0.0f,
                null, borderColor, 0.0f, false,
                backgroundVisible, backgroundColor, radius,
                borderVisible, borderColor, borderWidth);
    }
}
