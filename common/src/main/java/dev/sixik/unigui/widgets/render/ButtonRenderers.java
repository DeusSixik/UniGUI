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
            TextEngine.drawInline(draw, state.richText(), drawX, drawY,
                    drawWidth,
                    drawHeight,
                    Paint.fill(state.textColor()));
        } finally {
            draw.popClip();
        }
    };

    public static final ButtonRenderer CHECKBOX = (draw, state) -> {
        float labelGap = state.hasText() ? Math.max(0.0f, state.indicatorGap()) : 0.0f;
        float labelWidth = state.hasText()
                ? Math.min(Math.max(0.0f, state.textWidth()), Math.max(0.0f, state.width() - state.indicatorSize() - labelGap))
                : 0.0f;
        float indicatorX = state.labelLeft() ? state.x() + labelWidth + labelGap : state.x();
        float indicatorY = state.y() + Math.max(0.0f, state.height() - state.indicatorSize()) * 0.5f;
        draw.roundedRect(indicatorX, indicatorY, state.indicatorSize(), state.indicatorSize(), 2.0f,
                Paint.stroke(state.indicatorBorderColor(), 1.0f));

        if (state.indeterminate()) {
            float dashWidth = Math.max(1.0f, state.indicatorInnerSize());
            float dashHeight = Math.max(1.0f, state.indicatorInnerSize() * 0.28f);
            float offsetX = Math.max(0.0f, (state.indicatorSize() - dashWidth) * 0.5f);
            float offsetY = Math.max(0.0f, (state.indicatorSize() - dashHeight) * 0.5f);
            draw.rect(indicatorX + offsetX, indicatorY + offsetY,
                    dashWidth, dashHeight,
                    Paint.fill(state.indicatorColor()));
        } else if (state.checked()) {
            float offset = Math.max(0.0f, (state.indicatorSize() - state.indicatorInnerSize()) * 0.5f);
            draw.rect(indicatorX + offset, indicatorY + offset,
                    state.indicatorInnerSize(), state.indicatorInnerSize(),
                    Paint.fill(state.indicatorColor()));
        }

        if (state.labelLeft()) {
            drawLabel(draw, state, state.x(), labelWidth);
        } else {
            drawLeadingLabel(draw, state);
        }
    };

    public static final ButtonRenderer RADIO_BUTTON = (draw, state) -> {
        float labelGap = state.hasText() ? Math.max(0.0f, state.indicatorGap()) : 0.0f;
        float labelWidth = state.hasText()
                ? Math.min(Math.max(0.0f, state.textWidth()), Math.max(0.0f, state.width() - state.indicatorSize() - labelGap))
                : 0.0f;
        float indicatorX = state.labelLeft() ? state.x() + labelWidth + labelGap : state.x();
        float indicatorY = state.y() + Math.max(0.0f, state.height() - state.indicatorSize()) * 0.5f;
        draw.circle(indicatorX, indicatorY, state.indicatorSize(), state.indicatorSize(),
                Paint.stroke(state.indicatorBorderColor(), 1.0f));

        float progress = state.indicatorProgress();
        if (progress > 0.0f) {
            float innerSize = state.indicatorInnerSize() * progress;
            float offset = Math.max(0.0f, (state.indicatorSize() - innerSize) * 0.5f);
            draw.circle(indicatorX + offset, indicatorY + offset,
                    innerSize, innerSize,
                    Paint.fill(state.indicatorColor()));
        }

        if (state.labelLeft()) {
            drawLabel(draw, state, state.x(), labelWidth);
        } else {
            drawLeadingLabel(draw, state);
        }
    };

    public static final ButtonRenderer TOGGLE_SWITCH = (draw, state) -> {
        float trackWidth = Math.max(0.0f, state.indicatorSize());
        float trackHeight = Math.max(0.0f, state.textPaddingX());
        float thumbSize = Math.max(0.0f, state.indicatorInnerSize());
        if (trackWidth <= 0.0f || trackHeight <= 0.0f || thumbSize <= 0.0f) return;

        float labelGap = state.hasText() ? Math.max(0.0f, state.indicatorGap()) : 0.0f;
        float labelWidth = state.hasText()
                ? Math.min(Math.max(0.0f, state.textWidth()), Math.max(0.0f, state.width() - trackWidth - labelGap))
                : 0.0f;
        float trackX = state.labelLeft() ? state.x() + labelWidth + labelGap : state.x();
        float trackY = state.y() + Math.max(0.0f, state.height() - trackHeight) * 0.5f;
        float radius = trackHeight * 0.5f;
        draw.roundedRect(trackX, trackY, trackWidth, trackHeight, radius, Paint.fill(state.indicatorColor()));

        float thumbPadding = Math.max(1.0f, (trackHeight - thumbSize) * 0.5f);
        float thumbTravel = Math.max(0.0f, trackWidth - thumbSize - thumbPadding * 2.0f);
        float thumbX = trackX + thumbPadding + (state.checked() ? thumbTravel : 0.0f);
        float thumbY = trackY + Math.max(0.0f, trackHeight - thumbSize) * 0.5f;
        draw.circle(thumbX, thumbY, thumbSize, thumbSize, Paint.fill(state.indicatorBorderColor()));

        if (state.labelLeft()) {
            drawLabel(draw, state, state.x(), labelWidth);
        } else {
            drawTrailingLabel(draw, state, trackX + trackWidth + labelGap);
        }
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
            TextEngine.drawInline(draw, state.richText(), contentX, drawY, contentWidth, drawHeight, Paint.fill(state.textColor()));
        } finally {
            draw.popClip();
        }
    }

    private static void drawTrailingLabel(dev.sixik.unigui.api.render.DrawScope draw, ButtonState state, float contentX) {
        if (!state.hasText()) return;

        float contentWidth = Math.max(0.0f, state.width() - (contentX - state.x()));
        drawLabel(draw, state, contentX, contentWidth);
    }

    private static void drawLabel(dev.sixik.unigui.api.render.DrawScope draw, ButtonState state, float contentX, float contentWidth) {
        if (!state.hasText() || contentWidth <= 0.0f) return;

        float drawHeight = Math.min(Math.max(0.0f, state.height()), Math.max(0.0f, state.textHeight()));
        float drawY = state.y() + Math.max(0.0f, state.height() - drawHeight) * 0.5f;

        draw.pushClip(contentX, state.y(), contentWidth, state.height());
        try {
            TextEngine.drawInline(draw, state.richText(), contentX, drawY, contentWidth, drawHeight, Paint.fill(state.textColor()));
        } finally {
            draw.popClip();
        }
    }
}
