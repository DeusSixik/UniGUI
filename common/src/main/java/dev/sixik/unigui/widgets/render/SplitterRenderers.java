package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.widgets.core.Orientation;

public final class SplitterRenderers {
    public static final SplitterRenderer DEFAULT = (draw, state) -> {
        if (state.orientation() == Orientation.HORIZONTAL) {
            float handleWidth = Math.max(1.0f, Math.min(2.0f, state.width()));
            draw.roundedRect(
                    state.x() + (state.width() - handleWidth) * 0.5f,
                    state.y() + 3.0f,
                    handleWidth,
                    Math.max(1.0f, state.height() - 6.0f),
                    handleWidth * 0.5f,
                    Paint.fill(state.handleColor()));
        } else {
            float handleHeight = Math.max(1.0f, Math.min(2.0f, state.height()));
            draw.roundedRect(
                    state.x() + 3.0f,
                    state.y() + (state.height() - handleHeight) * 0.5f,
                    Math.max(1.0f, state.width() - 6.0f),
                    handleHeight,
                    handleHeight * 0.5f,
                    Paint.fill(state.handleColor()));
        }
    };

    private SplitterRenderers() {
    }
}
