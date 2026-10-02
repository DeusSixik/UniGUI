package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.interaction.Checkbox;

/**
 * Стандартные полные визуалы checkbox: поверхность плюс indicator и label.
 */
public final class CheckboxRenderers {
    private static final float LABEL_VISUAL_CENTER_OFFSET = 1.0f;

    /** Полный визуал checkbox: поверхность плюс квадрат indicator и label. */
    public static final WidgetRender DEFAULT = WidgetRender.of(Checkbox.class, (draw, checkbox) -> {
        SurfacePlans.renderWidgetSurface(draw, checkbox);
        renderDefault(draw, checkbox.checkboxSnapshot(draw.context()));
    });

    private CheckboxRenderers() {
    }

    private static void renderDefault(DrawScope draw, CheckboxRenderState state) {
        if (state == null) return;

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
