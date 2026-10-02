package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.interaction.RadioButton;

/** Стандартные полные визуалы radio button: поверхность плюс indicator и label. */
public final class RadioButtonRenderers {
    private static final float LABEL_VISUAL_CENTER_OFFSET = 1.0f;

    /** Полный визуал radio button: поверхность плюс круглый indicator и label. */
    public static final WidgetRender DEFAULT = WidgetRender.of(RadioButton.class, (draw, radio) -> {
        SurfacePlans.renderWidgetSurface(draw, radio);
        renderDefault(draw, radio.radioButtonSnapshot(draw.context()));
    });

    private RadioButtonRenderers() {
    }

    private static void renderDefault(DrawScope draw, RadioButtonRenderState state) {
        if (state == null) return;

        float labelGap = state.hasText() ? Math.max(0.0f, state.textGap()) : 0.0f;
        float labelWidth = state.hasText()
                ? Math.min(Math.max(0.0f, state.textWidth()),
                Math.max(0.0f, state.width() - state.outerSize() - labelGap))
                : 0.0f;
        float indicatorX = state.labelLeft()
                ? state.x() + labelWidth + labelGap
                : state.x();
        float indicatorY = state.y()
                + Math.max(0.0f, state.height() - state.outerSize()) * 0.5f;
        RadioIndicatorPart.render(draw, indicatorX, indicatorY, state.outerSize(),
                state.innerSize(), state.indicatorProgress(), state.indicatorBorderColor(),
                state.indicatorColor());

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
            contentX = state.x() + state.outerSize() + state.textGap();
            contentWidth = Math.max(0.0f,
                    state.width() - state.outerSize() - state.textGap());
            float indicatorCenterY = indicatorY + state.outerSize() * 0.5f;
            drawY = indicatorCenterY - drawHeight * 0.5f + LABEL_VISUAL_CENTER_OFFSET;
        }
        if (contentWidth <= 0.0f || drawHeight <= 0.0f) return;
        LabelPart.render(draw, state.richText(), contentX, state.y(), contentWidth, state.height(),
                contentX, drawY, contentWidth, drawHeight, state.textColor());
    }
}
