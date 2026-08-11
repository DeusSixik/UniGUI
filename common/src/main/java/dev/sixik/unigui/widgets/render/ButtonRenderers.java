package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.impl.text.TextEngine;

public final class ButtonRenderers {
    private static final float LEADING_LABEL_VISUAL_CENTER_OFFSET = 1.0f;

    public static final ButtonRenderer DEFAULT = (draw, state) -> {
        if (!state.hasText()) return;

        float contentX = state.textContentX();
        float contentWidth = state.textContentWidth();
        float drawWidth = Math.min(Math.max(0.0f, contentWidth), Math.max(0.0f, state.textWidth()));
        float drawHeight = Math.min(Math.max(0.0f, state.height()), Math.max(0.0f, state.textHeight()));
        float drawX = contentX + Math.max(0.0f, contentWidth - drawWidth) * 0.5f;
        float drawY = state.y() + Math.max(0.0f, state.height() - drawHeight) * 0.5f;

        draw.pushClip(contentX, state.y(), contentWidth, state.height());
        try {
            draw.text(state.richText(), drawX, drawY,
                    drawWidth,
                    drawHeight,
                    Paint.fill(state.textColor()));
        } finally {
            draw.popClip();
        }
    };

    public static final ButtonRenderer CHECKBOX = (draw, state) -> {
        float indicatorY = state.y() + Math.max(0.0f, state.height() - state.indicatorSize()) * 0.5f;
        draw.roundedRect(state.x(), indicatorY, state.indicatorSize(), state.indicatorSize(), 2.0f,
                Paint.stroke(state.indicatorBorderColor(), 1.0f));

        if (state.indeterminate()) {
            float dashWidth = Math.max(1.0f, state.indicatorInnerSize());
            float dashHeight = Math.max(1.0f, state.indicatorInnerSize() * 0.28f);
            float offsetX = Math.max(0.0f, (state.indicatorSize() - dashWidth) * 0.5f);
            float offsetY = Math.max(0.0f, (state.indicatorSize() - dashHeight) * 0.5f);
            draw.rect(state.x() + offsetX, indicatorY + offsetY,
                    dashWidth, dashHeight,
                    Paint.fill(state.indicatorColor()));
        } else if (state.checked()) {
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

    public static void drawLeadingLabel(dev.sixik.unigui.api.render.DrawScope draw, ButtonState state) {
        if (!state.hasText()) return;

        float contentX = state.x() + state.indicatorSize() + state.indicatorGap();
        float contentWidth = Math.max(0.0f, state.width() - state.indicatorSize() - state.indicatorGap());
        float drawHeight = Math.min(Math.max(0.0f, state.height()), Math.max(0.0f, state.textHeight()));
        float indicatorY = state.y() + Math.max(0.0f, state.height() - state.indicatorSize()) * 0.5f;
        float indicatorCenterY = indicatorY + state.indicatorSize() * 0.5f;
        float drawY = indicatorCenterY - drawHeight * 0.5f + LEADING_LABEL_VISUAL_CENTER_OFFSET;

        draw.pushClip(contentX, state.y(), contentWidth, state.height());
        try {
            draw.text(state.richText(), contentX, drawY, contentWidth, drawHeight, Paint.fill(state.textColor()));
        } finally {
            draw.popClip();
        }
    }
}
