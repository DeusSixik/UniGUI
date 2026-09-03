package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.text.RichText;

/** Самостоятельное typed-состояние визуального hold-контрола. */
public record HoldButtonState(
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
        float borderWidth,
        float holdProgress,
        float holdElapsedSeconds,
        float holdDurationSeconds,
        boolean holding,
        boolean completed,
        ColorView holdColor
) {
    /**
     * Переходный конструктор для кода, который создавал состояние из legacy ButtonState.
     *
     * @deprecated создавайте состояние с typed визуальными полями
     */
    @Deprecated
    public HoldButtonState(ButtonState button,
                           float holdProgress,
                           float holdElapsedSeconds,
                           float holdDurationSeconds,
                           boolean holding,
                           boolean completed,
                           ColorView holdColor) {
        this(button == null ? 0.0f : button.x(),
                button == null ? 0.0f : button.y(),
                button == null ? 0.0f : button.width(),
                button == null ? 0.0f : button.height(),
                button == null ? "" : button.text(),
                button == null ? RichText.plain("") : button.richText(),
                button == null ? 0.0f : button.textPaddingX(),
                button == null ? 0.0f : button.textWidth(),
                button == null ? 0.0f : button.textHeight(),
                button == null ? null : button.textColor(),
                button != null && button.pressed(),
                button != null && button.hovered(),
                button != null && button.enabled(),
                button != null && button.backgroundVisible(),
                button == null ? null : button.backgroundColor(),
                button == null ? 0.0f : button.radius(),
                button != null && button.borderVisible(),
                button == null ? null : button.borderColor(),
                button == null ? 0.0f : button.borderWidth(),
                holdProgress,
                holdElapsedSeconds,
                holdDurationSeconds,
                holding,
                completed,
                holdColor);
    }

    public HoldButtonState {
        text = text == null ? "" : text;
        richText = richText == null ? RichText.plain("") : richText;
        holdProgress = clamp01(holdProgress);
        radius = Math.max(0.0f, radius);
        borderWidth = Math.max(0.0f, borderWidth);
    }

    /** Переходный adapter к старому состоянию ButtonRenderer/RenderPlan. */
    @Deprecated
    public ButtonState button() {
        return new ButtonState(
                ButtonRenderType.BUTTON,
                x, y, width, height, text, richText,
                textPaddingX, textWidth, textHeight, textColor,
                pressed, hovered, enabled,
                false, false, 0.0f, 0.0f, 0.0f,
                null, null, 0.0f, false,
                backgroundVisible, backgroundColor, radius,
                borderVisible, borderColor, borderWidth);
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

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
