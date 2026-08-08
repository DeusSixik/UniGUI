package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;

public final class PathRenderers {
    public static final PathRenderer DEFAULT = (draw, state) -> {
        if (state.path() == null || state.path().isEmpty()) return;
        Paint paint = state.stroke()
                ? Paint.stroke(state.color(), state.strokeWidth())
                : Paint.fill(state.color());
        draw.path(state.path(), state.x(), state.y(), state.width(), state.height(), paint);
    };

    private PathRenderers() {
    }
}
