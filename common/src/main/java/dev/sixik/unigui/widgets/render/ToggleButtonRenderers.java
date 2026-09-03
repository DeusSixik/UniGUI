package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.DrawScope;

/** Стандартные renderer'ы toggle button. */
public final class ToggleButtonRenderers {
    /** Стандартный renderer с состоянием фона и центрированным label. */
    public static final ToggleButtonRenderer DEFAULT = ToggleButtonRenderers::renderDefault;

    private ToggleButtonRenderers() {
    }

    /** Создаёт переходник для старого ButtonRenderer. */
    public static ToggleButtonRenderer legacy(ButtonRenderer renderer) {
        if (renderer == null) return null;
        return (draw, state) -> renderer.render(draw, state.toLegacyButtonState());
    }

    private static void renderDefault(DrawScope draw, ToggleButtonRenderState state) {
        if (state == null) return;
        ControlChromePart.render(draw, state.x(), state.y(), state.width(), state.height(),
                state.radius(), state.backgroundVisible(), state.backgroundColor(),
                state.borderVisible(), state.borderColor(), state.borderWidth());
        if (!state.hasText()) return;

        float contentWidth = Math.max(0.0f, state.width() - state.textPaddingX() * 2.0f);
        float drawWidth = Math.min(contentWidth, Math.max(0.0f, state.textWidth()));
        float drawHeight = Math.min(Math.max(0.0f, state.height()),
                Math.max(0.0f, state.textHeight()));
        if (contentWidth <= 0.0f || drawHeight <= 0.0f) return;
        float contentX = state.x() + state.textPaddingX();
        float drawX = contentX + Math.max(0.0f, contentWidth - drawWidth) * 0.5f;
        float drawY = state.y() + Math.max(0.0f, state.height() - drawHeight) * 0.5f;
        LabelPart.render(draw, state.richText(), contentX, state.y(), contentWidth, state.height(),
                drawX, drawY, drawWidth, drawHeight, state.textColor());
    }
}
