package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.widgets.Orientation;

public final class SeparatorRenderers {
    public static final SeparatorRenderer DEFAULT = (draw, state) -> {
        boolean horizontal = state.orientation() != Orientation.VERTICAL;
        float width = horizontal ? state.width() : state.thickness();
        float height = horizontal ? state.thickness() : state.height();
        draw.rect(state.x(), state.y(), width, height, Paint.fill(state.color()));
    };

    private SeparatorRenderers() {
    }
}
