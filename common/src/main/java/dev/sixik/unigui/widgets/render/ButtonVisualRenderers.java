package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.DrawScope;

/** Стандартные typed renderer-ы обычной кнопки. */
public final class ButtonVisualRenderers {
    public static final ButtonVisualRenderer DEFAULT = ButtonVisualRenderers::renderDefault;

    private ButtonVisualRenderers() {
    }

    public static ButtonVisualRenderer fromLegacy(ButtonRenderer renderer) {
        if (renderer == null) return DEFAULT;
        return (draw, state) -> {
            if (draw != null && state != null) {
                renderer.render(draw, state.toLegacyButtonState());
            }
        };
    }

    private static void renderDefault(DrawScope draw, ButtonRenderState state) {
        if (draw == null || state == null) return;
        ControlChromePart.render(draw, state.x(), state.y(), state.width(), state.height(),
                state.radius(), state.backgroundVisible(), state.backgroundColor(),
                state.borderVisible(), state.borderColor(), state.borderWidth());
        if (!state.hasText()) return;

        float contentWidth = state.textContentWidth();
        float drawWidth = Math.min(contentWidth, Math.max(0.0f, state.textWidth()));
        float drawHeight = Math.min(Math.max(0.0f, state.height()), Math.max(0.0f, state.textHeight()));
        if (contentWidth <= 0.0f || drawWidth <= 0.0f || drawHeight <= 0.0f) return;

        float drawX = state.textContentX() + Math.max(0.0f, contentWidth - drawWidth) * 0.5f;
        float drawY = LabelPart.centeredY(state.y(), state.height(), drawHeight);
        LabelPart.render(draw, state.richText(), state.textContentX(), state.y(),
                contentWidth, state.height(), drawX, drawY, drawWidth, drawHeight, state.textColor());
    }
}
