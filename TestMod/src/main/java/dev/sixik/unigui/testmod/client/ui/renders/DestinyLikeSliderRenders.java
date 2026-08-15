package dev.sixik.unigui.testmod.client.ui.renders;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.widgets.render.SliderRenderer;

public final class DestinyLikeSliderRenders {
    private static final float TRACK_HEIGHT = 3.2f;
    private static final float BORDER_WIDTH = 0.16f;
    private static final float FILL_EDGE_WIDTH = 0.9f;
    private static final ColorView TRACK = MutableColor.rgba255(16, 19, 25, 235);
    private static final ColorView TRACK_BORDER = MutableColor.rgba255(105, 109, 112, 220);
    private static final ColorView TRACK_BORDER_ACTIVE = MutableColor.rgba255(238, 241, 247, 255);
    private static final ColorView FILL = MutableColor.rgba255(214, 207, 145, 245);
    private static final ColorView FILL_EDGE = MutableColor.rgba255(255, 249, 184, 150);
    private static final ColorView KNOB = MutableColor.rgba255(238, 241, 247, 255);
    private static final ColorView KNOB_ACTIVE = MutableColor.rgba255(255, 255, 255, 255);
    private static final ColorView KNOB_SHADOW = MutableColor.rgba255(0, 0, 0, 130);

    public static final SliderRenderer DEFAULT = (draw, state) -> {
        float x = state.x();
        float y = state.y();
        float width = Math.max(0.0f, state.width());
        float height = Math.max(1.0f, state.height());
        if (width <= 0.0f) return;

        float trackHeight = Math.min(height, TRACK_HEIGHT);
        float trackY = y + (height - trackHeight) * 0.5f;
        float normalized = clamp01(state.normalizedValue());
        float fillWidth = Math.max(0.0f, Math.min(width, width * normalized));

        draw.rect(x, trackY, width, trackHeight, Paint.fill(TRACK));
        DestinyLikeRenderPrimitives.rectBorder(draw, x, trackY, width, trackHeight,
                state.dragging() ? TRACK_BORDER_ACTIVE : TRACK_BORDER, BORDER_WIDTH);

        if (fillWidth > 0.0f) {
            draw.rect(x, trackY, fillWidth, trackHeight, Paint.fill(FILL));
            if (fillWidth < width) {
                draw.rect(x + fillWidth - FILL_EDGE_WIDTH * 0.5f, trackY - 0.7f,
                        FILL_EDGE_WIDTH, trackHeight + 1.4f, Paint.fill(FILL_EDGE));
            }
        }

        float knobWidth = Math.max(2.4f, state.knobWidth() * 0.72f);
        float knobHeight = Math.max(trackHeight + 3.0f, height - 2.0f);
        float knobX = x + fillWidth - knobWidth * 0.5f;
        knobX = Math.max(x, Math.min(x + width - knobWidth, knobX));
        float knobY = y + (height - knobHeight) * 0.5f;

        draw.rect(knobX + 0.45f, knobY + 0.45f, knobWidth, knobHeight, Paint.fill(KNOB_SHADOW));
        draw.rect(knobX, knobY, knobWidth, knobHeight, Paint.fill(state.dragging() ? KNOB_ACTIVE : KNOB));
        DestinyLikeRenderPrimitives.rectBorder(draw, knobX, knobY, knobWidth, knobHeight,
                state.dragging() ? TRACK_BORDER_ACTIVE : TRACK_BORDER, BORDER_WIDTH);
    };

    private DestinyLikeSliderRenders() {
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
