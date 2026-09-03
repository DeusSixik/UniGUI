package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.text.RichText;

/** Typed render state semantic role toggle switch. */
public record ToggleSwitchRenderState(
        float x,
        float y,
        float width,
        float height,
        String text,
        RichText richText,
        float trackWidth,
        float trackHeight,
        float thumbSize,
        float labelGap,
        float textWidth,
        float textHeight,
        ColorView textColor,
        boolean pressed,
        boolean hovered,
        boolean enabled,
        boolean checked,
        ColorView trackColor,
        ColorView thumbColor,
        float switchProgress,
        boolean labelLeft
) {
    public ToggleSwitchRenderState {
        text = text == null ? "" : text;
        richText = richText == null ? RichText.plain("") : richText;
        switchProgress = clamp01(switchProgress);
    }

    /** @return {@code true}, если switch имеет label */
    public boolean hasText() {
        return !richText.isEmpty();
    }

    /** Временный переход к старому ButtonState для legacy renderer и RenderPlan. */
    public ButtonState toLegacyButtonState() {
        return new ButtonState(
                ButtonRenderType.TOGGLE_SWITCH,
                x, y, width, height, text, richText,
                trackHeight, textWidth, textHeight, textColor,
                pressed, hovered, enabled, checked, false,
                trackWidth, thumbSize, hasText() ? labelGap : 0.0f,
                trackColor, thumbColor, switchProgress, labelLeft);
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
