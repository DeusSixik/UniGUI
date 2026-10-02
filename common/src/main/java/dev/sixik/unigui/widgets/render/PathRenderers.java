package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.display.Path;

public final class PathRenderers {
    public static final WidgetRender DEFAULT = WidgetRender.of(Path.class, (draw, w) -> {
        PathState state = w.snapshot();
        if (state.path() == null || state.path().isEmpty()) return;
        Paint paint = state.stroke()
                ? Paint.stroke(state.color(), state.strokeWidth())
                : Paint.fill(state.color());
        draw.path(state.path(), state.x(), state.y(), state.width(), state.height(), paint);
    });

    private PathRenderers() {
    }
}
