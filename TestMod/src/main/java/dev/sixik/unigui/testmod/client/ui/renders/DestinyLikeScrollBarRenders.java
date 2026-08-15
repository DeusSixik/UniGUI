package dev.sixik.unigui.testmod.client.ui.renders;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.render.ScrollBarRenderer;

public final class DestinyLikeScrollBarRenders {
    private static final float BORDER_WIDTH = 0.16f;
    private static final float THUMB_INSET = 1.35f;
    private static final ColorView TRACK = MutableColor.rgba255(7, 9, 13, 160);
    private static final ColorView TRACK_BORDER = MutableColor.rgba255(105, 109, 112, 120);
    private static final ColorView THUMB = MutableColor.rgba255(126, 129, 138, 220);
    private static final ColorView THUMB_ACTIVE = MutableColor.rgba255(238, 241, 247, 250);
    private static final ColorView THUMB_ACCENT = MutableColor.rgba255(214, 207, 145, 210);

    public static final ScrollBarRenderer DEFAULT = (draw, state) -> {
        float x = state.x();
        float y = state.y();
        float width = Math.max(0.0f, state.width());
        float height = Math.max(0.0f, state.height());
        if (width <= 0.0f || height <= 0.0f) return;

        draw.rect(x, y, width, height, Paint.fill(TRACK));
        DestinyLikeRenderPrimitives.rectBorder(draw, x, y, width, height, TRACK_BORDER, BORDER_WIDTH);

        if (state.orientation() == Orientation.VERTICAL) {
            float thumbHeight = Math.max(8.0f, Math.min(height, state.thumbLength(height)));
            float travel = Math.max(0.0f, height - thumbHeight);
            float thumbY = y + travel * clamp01(state.normalizedValue());
            drawVerticalThumb(draw, state.dragging(), x, thumbY, width, thumbHeight);
        } else {
            float thumbWidth = Math.max(8.0f, Math.min(width, state.thumbLength(width)));
            float travel = Math.max(0.0f, width - thumbWidth);
            float thumbX = x + travel * clamp01(state.normalizedValue());
            drawHorizontalThumb(draw, state.dragging(), thumbX, y, thumbWidth, height);
        }
    };

    private DestinyLikeScrollBarRenders() {
    }

    private static void drawVerticalThumb(dev.sixik.unigui.api.render.DrawScope draw,
                                          boolean active,
                                          float x,
                                          float y,
                                          float width,
                                          float height) {
        float thumbX = x + Math.min(THUMB_INSET, width * 0.25f);
        float thumbWidth = Math.max(1.0f, width - Math.min(THUMB_INSET, width * 0.25f) * 2.0f);
        draw.rect(thumbX, y, thumbWidth, height, Paint.fill(active ? THUMB_ACTIVE : THUMB));
        DestinyLikeRenderPrimitives.rectBorder(draw, thumbX, y, thumbWidth, height,
                active ? THUMB_ACTIVE : THUMB_ACCENT, BORDER_WIDTH);
        draw.rect(thumbX, y, thumbWidth, Math.min(1.0f, height), Paint.fill(THUMB_ACCENT));
    }

    private static void drawHorizontalThumb(dev.sixik.unigui.api.render.DrawScope draw,
                                            boolean active,
                                            float x,
                                            float y,
                                            float width,
                                            float height) {
        float thumbY = y + Math.min(THUMB_INSET, height * 0.25f);
        float thumbHeight = Math.max(1.0f, height - Math.min(THUMB_INSET, height * 0.25f) * 2.0f);
        draw.rect(x, thumbY, width, thumbHeight, Paint.fill(active ? THUMB_ACTIVE : THUMB));
        DestinyLikeRenderPrimitives.rectBorder(draw, x, thumbY, width, thumbHeight,
                active ? THUMB_ACTIVE : THUMB_ACCENT, BORDER_WIDTH);
        draw.rect(x, thumbY, Math.min(1.0f, width), thumbHeight, Paint.fill(THUMB_ACCENT));
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
