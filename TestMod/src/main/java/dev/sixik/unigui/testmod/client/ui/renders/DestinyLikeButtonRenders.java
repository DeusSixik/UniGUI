package dev.sixik.unigui.testmod.client.ui.renders;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.render.ButtonRenderer;

public final class DestinyLikeButtonRenders {
    private static final float BORDER_WIDTH = 0.16f;
    private static final float TEXT_HORIZONTAL_PADDING = 3.2f;
    public static final float INTRINSIC_TEXT_PADDING_X = TEXT_HORIZONTAL_PADDING;
    public static final float INTRINSIC_TEXT_PADDING_Y = 4.0f;
    private static final float TEXT_SIZE = 3.2f;
    private static final float MIN_TEXT_SIZE = 1.8f;
    private static final float TEXT_TRACKING = 0.34f;
    private static final float TEXT_VISUAL_CENTER_OFFSET_Y = 0.2f;

    public static final ButtonRenderer DEFAULT = ((draw, state) -> {
        float x = state.x();
        float y = state.y();
        float w = Math.max(0.0f, state.width());
        float h = Math.max(0.0f, state.height());
        if (w <= 0.0f || h <= 0.0f) return;

        draw.rect(x, y, w, h, Paint.fill(state.indicatorColor()));
        drawBorder(draw, x, y, w, h, state.indicatorBorderColor());

        if (!state.hasText()) return;

        float availableWidth = Math.max(0.0f, w - TEXT_HORIZONTAL_PADDING * 2.0f);
        RichText text = destinyText(draw, state.text(), state.textColor(), availableWidth);
        float textWidth = Math.min(availableWidth, TextEngine.measureLineWidth(draw.context(), text));
        float textHeight = Math.min(h, TextEngine.measureTextHeight(text));

        float drawX = x + Math.max(0.0f, w - textWidth) * 0.5f;
        float drawY = y + Math.max(0.0f, h - textHeight) * 0.5f + TEXT_VISUAL_CENTER_OFFSET_Y;

        draw.text(text, drawX, drawY, textWidth, textHeight, Paint.fill(state.textColor()));
    });

    private DestinyLikeButtonRenders() {
    }

    public static RichText dominionButtonText(String value, ColorView color) {
        return buildDestinyText(value, color, TEXT_SIZE);
    }

    private static RichText destinyText(DrawScope draw, String value, ColorView color, float maxWidth) {
        float size = TEXT_SIZE;
        RichText text = buildDestinyText(value, color, size);
        while (size > MIN_TEXT_SIZE && TextEngine.measureLineWidth(draw.context(), text) > maxWidth) {
            size = Math.max(MIN_TEXT_SIZE, size - 0.25f);
            text = buildDestinyText(value, color, size);
        }
        return text;
    }

    private static RichText buildDestinyText(String value, ColorView color, float size) {
        return RichText.builder()
                .size(size)
                .tracking(TEXT_TRACKING)
                .uppercase()
                .color(color)
                .append(value)
                .font(Fonts.defaultFace())
                .build();
    }

    private static void drawBorder(DrawScope draw,
                                   float x,
                                   float y,
                                   float width,
                                   float height,
                                   ColorView color) {
        draw.addLine(x, y, x + width, y, color, BORDER_WIDTH);
        draw.addLine(x, y + height, x + width, y + height, color, BORDER_WIDTH);
        draw.addLine(x, y, x, y + height, color, BORDER_WIDTH);
        draw.addLine(x + width, y, x + width, y + height, color, BORDER_WIDTH);
    }
}
