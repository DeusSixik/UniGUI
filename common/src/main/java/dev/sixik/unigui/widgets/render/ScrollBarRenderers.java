package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.widgets.core.Orientation;

public final class ScrollBarRenderers {
    public static final ScrollBarRenderer DEFAULT = (draw, state) -> {
        float width = state.width();
        float height = state.height();

        draw.roundedRect(state.x(), state.y(), width, height, Math.min(width, height) * 0.5f,
                Paint.fill(state.trackColor()));

        if (state.orientation() == Orientation.VERTICAL) {
            float thumbHeight = state.thumbLength(height);
            float thumbY = state.y() + (height - thumbHeight) * state.normalizedValue();
            draw.roundedRect(state.x(), thumbY, width, thumbHeight, width * 0.5f,
                    Paint.fill(state.thumbColor()));
        } else {
            float thumbWidth = state.thumbLength(width);
            float thumbX = state.x() + (width - thumbWidth) * state.normalizedValue();
            draw.roundedRect(thumbX, state.y(), thumbWidth, height, height * 0.5f,
                    Paint.fill(state.thumbColor()));
        }
    };

    private ScrollBarRenderers() {
    }
}
