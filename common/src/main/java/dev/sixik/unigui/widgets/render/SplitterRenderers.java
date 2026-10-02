package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.containers.Splitter;
import dev.sixik.unigui.widgets.core.Orientation;

public final class SplitterRenderers {
    public static final WidgetRender DEFAULT = WidgetRender.of(Splitter.class, (draw, w) -> {
        SplitterState state = w.snapshot();
        if (state.backgroundVisible()) {
            draw.roundedRect(state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.fill(state.backgroundColor()));
        }
        if (state.borderVisible() && state.borderWidth() > 0.0f) {
            draw.roundedRect(state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.stroke(state.borderColor(), state.borderWidth()));
        }
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
    });

    private SplitterRenderers() {
    }
}
