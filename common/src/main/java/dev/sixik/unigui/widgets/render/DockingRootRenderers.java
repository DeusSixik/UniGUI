package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;

public final class DockingRootRenderers {
    private static final MutableColor EMPTY_TEXT = new MutableColor(0.62f, 0.68f, 0.78f, 0.8f);

    public static final DockingRootRenderer DEFAULT = (draw, state) -> {
        if (state.width() <= 0.0f || state.height() <= 0.0f) return;
        if (state.backgroundVisible()) {
            draw.roundedRect(state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.fill(state.backgroundColor()));
        }
        if (state.borderVisible() && state.borderWidth() > 0.0f) {
            draw.roundedRect(state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.stroke(state.borderColor(), state.borderWidth()));
        }
        if (state.dockDragging() || state.dropPreviewVisible()) {
            draw.roundedRect(state.x() + 1.0f, state.y() + 1.0f,
                    Math.max(0.0f, state.width() - 2.0f), Math.max(0.0f, state.height() - 2.0f),
                    Math.max(0.0f, state.radius() - 1.0f), Paint.stroke(EMPTY_TEXT, 1.0f));
        }
        if (state.empty()) {
            draw.text("Drop or add a DockPane", state.x() + 8.0f, state.y() + 8.0f,
                    Math.max(0.0f, state.width() - 16.0f), 14.0f, Paint.fill(EMPTY_TEXT));
        }
    };

    private DockingRootRenderers() {
    }
}
