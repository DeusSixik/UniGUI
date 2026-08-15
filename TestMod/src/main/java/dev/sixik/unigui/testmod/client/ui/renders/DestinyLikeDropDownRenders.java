package dev.sixik.unigui.testmod.client.ui.renders;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.render.BoxRenderer;
import dev.sixik.unigui.widgets.render.BoxState;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ButtonState;

public final class DestinyLikeDropDownRenders {
    public static final float HEADER_HEIGHT = 20.0f;
    public static final float OPTION_HEIGHT = 18.0f;
    public static final float TEXT_PADDING_X = 7.0f;

    private static final float BORDER_WIDTH = 0.18f;
    private static final float TEXT_SIZE = 4.2f;
    private static final float TEXT_TRACKING = 0.32f;
    private static final float CHEVRON_SIZE = 3.1f;
    private static final float CHEVRON_RIGHT_PADDING = 7.0f;

    private static final ColorView BACKGROUND = MutableColor.rgba255(22, 25, 31, 246);
    private static final ColorView OPTION_HOVER_BACKGROUND = MutableColor.rgba255(39, 43, 52, 246);
    private static final ColorView BORDER = MutableColor.rgba255(105, 109, 112, 225);
    private static final ColorView BORDER_HOVER = MutableColor.rgba255(238, 241, 247, 255);
    private static final ColorView TEXT = MutableColor.rgba255(255, 255, 255, 255);
    private static final ColorView TEXT_HOVER = MutableColor.rgba255(105, 105, 105, 255);

    public static final ButtonRenderer HEADER = (draw, state) -> {
        float x = state.x();
        float y = state.y();
        float width = Math.max(0.0f, state.width());
        float height = Math.max(0.0f, state.height());
        if (width <= 0.0f || height <= 0.0f) return;

        draw.rect(x, y, width, height, Paint.fill(BACKGROUND));
        draw.rect(x, y, width, height, Paint.stroke(state.hovered() && state.enabled() ? BORDER_HOVER : BORDER, BORDER_WIDTH));
        drawLabel(draw, state, displayText(state.text()), x + TEXT_PADDING_X,
                Math.max(0.0f, width - TEXT_PADDING_X - CHEVRON_RIGHT_PADDING - CHEVRON_SIZE - 5.0f),
                state.enabled() ? TEXT : TEXT_HOVER);
        drawChevron(draw, x + width - CHEVRON_RIGHT_PADDING - CHEVRON_SIZE, y + height * 0.5f - CHEVRON_SIZE * 0.35f);
    };

    public static final BoxRenderer OPTIONS_HOST = (draw, state) -> {
        float x = state.x();
        float y = state.y();
        float width = Math.max(0.0f, state.width());
        float height = Math.max(0.0f, state.height());
        if (width <= 0.0f || height <= 0.0f) return;

        if (state.backgroundVisible()) {
            draw.rect(x, y, width, height, Paint.fill(state.background()));
        }
        if (state.borderVisible()) {
            drawFullBorder(draw, state, x, y, width, height);
        }
    };

    public static final ButtonRenderer OPTION = (draw, state) -> {
        float x = state.x();
        float y = state.y();
        float width = Math.max(0.0f, state.width());
        float height = Math.max(0.0f, state.height());
        if (width <= 0.0f || height <= 0.0f) return;

        draw.rect(x, y, width, height, Paint.fill(state.hovered() || state.checked() ? OPTION_HOVER_BACKGROUND : BACKGROUND));
        if (state.hovered() && state.enabled()) {
            draw.rect(x, y, width, height, Paint.stroke(BORDER_HOVER, BORDER_WIDTH));
        }
        drawLabel(draw, state, displayText(state.text()), x + TEXT_PADDING_X,
                Math.max(0.0f, width - TEXT_PADDING_X * 2.0f), TEXT);
    };

    private DestinyLikeDropDownRenders() {
    }

    public static RichText destinyText(String text) {
        return destinyText(text, TEXT);
    }

    private static RichText destinyText(String text, ColorView color) {
        return RichText.builder()
                .size(TEXT_SIZE)
                .tracking(TEXT_TRACKING)
                .uppercase()
                .color(color)
                .append(text == null ? "" : text)
                .font(Fonts.defaultFace())
                .build();
    }

    private static void drawLabel(dev.sixik.unigui.api.render.DrawScope draw,
                                  ButtonState state,
                                  String text,
                                  float x,
                                  float width,
                                  ColorView color) {
        if (text == null || text.isEmpty() || width <= 0.0f) return;

        RichText richText = destinyText(text, color);
        float textWidth = Math.min(width, TextEngine.measureLineWidth(draw.context(), richText));
        float textHeight = Math.min(Math.max(0.0f, state.height()), TextEngine.measureTextHeight(richText));
        float drawY = state.y() + Math.max(0.0f, state.height() - textHeight) * 0.5f + 0.2f;

        draw.pushClip(x, state.y(), width, state.height());
        try {
            draw.text(richText, x, drawY, textWidth, textHeight, Paint.fill(color));
        } finally {
            draw.popClip();
        }
    }

    private static void drawFullBorder(dev.sixik.unigui.api.render.DrawScope draw,
                                       BoxState state,
                                       float x,
                                       float y,
                                       float width,
                                       float height) {
        float stroke = Math.max(0.01f, state.borderWidth());
        float inset = stroke * 0.5f;
        float left = x + inset;
        float top = y + inset;
        float right = x + width - inset;
        float bottom = y + height - inset;

        draw.addLine(left, top, right, top, state.borderColor(), stroke);
        draw.addLine(right, top, right, bottom, state.borderColor(), stroke);
        draw.addLine(right, bottom, left, bottom, state.borderColor(), stroke);
        draw.addLine(left, bottom, left, top, state.borderColor(), stroke);
    }

    private static void drawChevron(dev.sixik.unigui.api.render.DrawScope draw, float x, float y) {
        float half = CHEVRON_SIZE * 0.5f;
        draw.addLine(x, y, x + half, y + half, TEXT, BORDER_WIDTH * 2.2f);
        draw.addLine(x + half, y + half, x + CHEVRON_SIZE, y, TEXT, BORDER_WIDTH * 2.2f);
    }

    private static String displayText(String text) {
        if (text == null) return "";
        String normalized = text.trim();
        if (normalized.endsWith("?")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }
}
