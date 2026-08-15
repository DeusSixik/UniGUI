package dev.sixik.unigui.testmod.client.ui.renders;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.widgets.render.TooltipRenderer;
import dev.sixik.unigui.widgets.render.TooltipState;

public final class DestinyLikeTooltipRenders {
    public static final float TOP_LINE_HEIGHT = 0.6f;
    public static final float TEXT_PADDING_X = 9.0f;
    public static final float TEXT_PADDING_TOP = 5.5f;
    public static final float BODY_INDENT = 13.0f;
    public static final float BODY_GAP = 1.4f;
    public static final float BOTTOM_PADDING = 3.0f;
    private static final float BULLET_SIZE = 4.2f;

    private static final ColorView BACKGROUND = MutableColor.rgba255(8, 9, 8, 238);
    private static final ColorView TOP_LINE = MutableColor.rgba255(210, 201, 172, 235);
    private static final ColorView BODY_TEXT = MutableColor.rgba255(218, 220, 225, 255);
    private static final ColorView BULLET = MutableColor.rgba255(245, 247, 255, 255);

    public static final TooltipRenderer DEFAULT = (draw, state) -> {
        float x = state.x();
        float y = state.y();
        float width = Math.max(0.0f, state.width());
        float height = Math.max(0.0f, state.height());
        if (width <= 0.0f || height <= 0.0f) return;

        draw.rect(x, y, width, height, Paint.fill(BACKGROUND));
        draw.rect(x, y, width, TOP_LINE_HEIGHT, Paint.fill(TOP_LINE));

        draw.pushClip(x, y, width, height);
        try {
            drawText(draw, state, x, y, width, height);
        } finally {
            draw.popClip();
        }
    };

    private DestinyLikeTooltipRenders() {
    }

    private static void drawText(DrawScope draw,
                                 TooltipState state,
                                 float x,
                                 float y,
                                 float width,
                                 float height) {
        float lineY = y + TEXT_PADDING_TOP;
        float limitY = y + height - BOTTOM_PADDING;
        for (int i = 0; i < state.lines().size(); i++) {
            float lineHeight = state.lineHeight(i);
            if (lineY >= limitY) break;

            boolean title = i == 0;
            float textX = x + TEXT_PADDING_X + (title ? 0.0f : BODY_INDENT);
            float availableWidth = Math.max(0.0f, width - (textX - x) - TEXT_PADDING_X);
            draw.text(state.lines().get(i), textX, lineY, availableWidth, lineHeight,
                    Paint.fill(title ? state.textColor() : BODY_TEXT));

            if (i == 1) {
                drawBullet(draw, x + TEXT_PADDING_X + BULLET_SIZE * 0.5f, lineY + lineHeight * 0.52f);
            }

            lineY += lineHeight + (title ? BODY_GAP : 0.0f);
        }
    }

    private static void drawBullet(DrawScope draw, float centerX, float centerY) {
        float half = BULLET_SIZE * 0.5f;
        draw.addQuadFilled(
                new DrawPoint(centerX, centerY - half),
                new DrawPoint(centerX + half, centerY),
                new DrawPoint(centerX, centerY + half),
                new DrawPoint(centerX - half, centerY),
                BULLET);
    }
}
