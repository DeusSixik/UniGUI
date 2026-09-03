package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.DrawScope;

/**
 * Стандартные renderer'ы checkbox.
 *
 * <p>DEFAULT уже работает с typed {@link CheckboxRenderState}. Legacy adapter оставлен
 * отдельно, чтобы зависимость от {@link ButtonState} была видна и постепенно удалялась.</p>
 */
public final class CheckboxRenderers {
    private static final float LABEL_VISUAL_CENTER_OFFSET = 1.0f;

    /** Стандартный renderer с квадратом indicator и label. */
    public static final CheckboxRenderer DEFAULT = CheckboxRenderers::renderDefault;

    private CheckboxRenderers() {
    }

    /**
     * Создаёт переходник для старого ButtonRenderer.
     *
     * @param renderer legacy renderer
     * @return typed checkbox renderer
     */
    public static CheckboxRenderer legacy(ButtonRenderer renderer) {
        if (renderer == null) return null;
        return (draw, state) -> renderer.render(draw, state.toLegacyButtonState());
    }

    private static void renderDefault(DrawScope draw, CheckboxRenderState state) {
        if (state == null) return;

        ControlChromePart.render(draw, state.x(), state.y(), state.width(), state.height(),
                state.radius(), state.backgroundVisible(), state.backgroundColor(),
                state.borderVisible(), state.borderColor(), state.borderWidth());

        float labelGap = state.hasText() ? Math.max(0.0f, state.indicatorGap()) : 0.0f;
        float labelWidth = state.hasText()
                ? Math.min(Math.max(0.0f, state.textWidth()),
                Math.max(0.0f, state.width() - state.indicatorSize() - labelGap))
                : 0.0f;
        float indicatorX = state.labelLeft()
                ? state.x() + labelWidth + labelGap
                : state.x();
        float indicatorY = state.y()
                + Math.max(0.0f, state.height() - state.indicatorSize()) * 0.5f;

        CheckIndicatorPart.render(draw, indicatorX, indicatorY,
                state.indicatorSize(), state.indicatorInnerSize(), state.checked(),
                state.indeterminate(), state.indicatorBorderColor(), state.indicatorColor());

        if (!state.hasText()) return;
        float contentX;
        float contentWidth;
        float drawY;
        float drawHeight = Math.min(Math.max(0.0f, state.height()),
                Math.max(0.0f, state.textHeight()));
        if (state.labelLeft()) {
            contentX = state.x();
            contentWidth = labelWidth;
            drawY = state.y() + Math.max(0.0f, state.height() - drawHeight) * 0.5f;
        } else {
            contentX = state.x() + state.indicatorSize() + state.indicatorGap();
            contentWidth = Math.max(0.0f,
                    state.width() - state.indicatorSize() - state.indicatorGap());
            float indicatorCenterY = indicatorY + state.indicatorSize() * 0.5f;
            drawY = indicatorCenterY - drawHeight * 0.5f + LABEL_VISUAL_CENTER_OFFSET;
        }
        if (contentWidth <= 0.0f || drawHeight <= 0.0f) return;

        LabelPart.render(draw, state.richText(), contentX, state.y(), contentWidth, state.height(),
                contentX, drawY, contentWidth, drawHeight, state.textColor());
    }
}
