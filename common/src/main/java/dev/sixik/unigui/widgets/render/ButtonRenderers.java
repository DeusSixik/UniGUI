package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;

public final class ButtonRenderers {
    public static final ButtonRenderer DEFAULT = (draw, state) -> {
        if (!state.hasText()) return;

        float contentX = state.textContentX();
        float contentWidth = state.textContentWidth();
        float drawWidth = Math.min(Math.max(0.0f, contentWidth), Math.max(0.0f, state.textWidth()));
        float drawHeight = Math.min(Math.max(0.0f, state.height()), Math.max(0.0f, state.textHeight()));
        float drawX = contentX + Math.max(0.0f, contentWidth - drawWidth) * 0.5f;
        float drawY = state.y() + Math.max(0.0f, state.height() - drawHeight) * 0.5f;

        draw.text(state.richText(), drawX, drawY,
                Math.max(0.0f, contentWidth - (drawX - contentX)),
                drawHeight,
                Paint.fill(state.textColor()));
    };

    public static final ButtonRenderer CHECKBOX = (draw, state) -> {
        float indicatorY = state.y() + Math.max(0.0f, state.height() - state.indicatorSize()) * 0.5f;
        draw.roundedRect(state.x(), indicatorY, state.indicatorSize(), state.indicatorSize(), 2.0f,
                Paint.stroke(state.indicatorBorderColor(), 1.0f));

        if (state.checked()) {
            float offset = Math.max(0.0f, (state.indicatorSize() - state.indicatorInnerSize()) * 0.5f);
            draw.rect(state.x() + offset, indicatorY + offset,
                    state.indicatorInnerSize(), state.indicatorInnerSize(),
                    Paint.fill(state.indicatorColor()));
        }

        drawLeadingLabel(draw, state);
    };

    public static final ButtonRenderer RADIO_BUTTON = (draw, state) -> {
        float indicatorY = state.y() + Math.max(0.0f, state.height() - state.indicatorSize()) * 0.5f;
        draw.circle(state.x(), indicatorY, state.indicatorSize(), state.indicatorSize(),
                Paint.stroke(state.indicatorBorderColor(), 1.0f));

        if (state.checked()) {
            float offset = Math.max(0.0f, (state.indicatorSize() - state.indicatorInnerSize()) * 0.5f);
            draw.circle(state.x() + offset, indicatorY + offset,
                    state.indicatorInnerSize(), state.indicatorInnerSize(),
                    Paint.fill(state.indicatorColor()));
        }

        drawLeadingLabel(draw, state);
    };

    private ButtonRenderers() {
    }

    private static void drawLeadingLabel(dev.sixik.unigui.api.render.DrawScope draw, ButtonState state) {
        if (!state.hasText()) return;

        float contentX = state.x() + state.indicatorSize() + state.indicatorGap();
        float contentWidth = Math.max(0.0f, state.width() - state.indicatorSize() - state.indicatorGap());
        float drawHeight = Math.min(Math.max(0.0f, state.height()), Math.max(0.0f, state.textHeight()));
        float drawY = state.y() + Math.max(0.0f, state.height() - drawHeight) * 0.5f;

        draw.text(state.richText(), contentX, drawY, contentWidth, drawHeight, Paint.fill(state.textColor()));
    }
}
