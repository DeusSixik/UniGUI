package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.widgets.DockSplitOrientation;

public final class DockSplitHandleRenderers {
    private static final MutableColor TRACK = new MutableColor(0.055f, 0.062f, 0.080f, 0.98f);
    private static final MutableColor LINE = new MutableColor(0.25f, 0.78f, 1.0f, 0.42f);

    public static final DockSplitHandleRenderer DEFAULT = (draw, state) -> {
        if (state.width() <= 0.0f || state.height() <= 0.0f) return;
        draw.rect(state.x(), state.y(), state.width(), state.height(), Paint.fill(TRACK));
        if (state.orientation() == DockSplitOrientation.HORIZONTAL) {
            float x = state.x() + state.width() * 0.5f;
            draw.line(x, state.y() + 4.0f, x, state.y() + Math.max(4.0f, state.height() - 4.0f),
                    Paint.stroke(LINE, 1.0f));
        } else {
            float y = state.y() + state.height() * 0.5f;
            draw.line(state.x() + 4.0f, y, state.x() + Math.max(4.0f, state.width() - 4.0f), y,
                    Paint.stroke(LINE, 1.0f));
        }
    };

    private DockSplitHandleRenderers() {
    }
}
