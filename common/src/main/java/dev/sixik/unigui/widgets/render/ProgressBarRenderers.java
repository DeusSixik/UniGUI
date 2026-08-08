package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;

public final class ProgressBarRenderers {
    public static final ProgressBarRenderer DEFAULT = (draw, state) -> {
        float width = Math.max(0.0f, state.width());
        float fillWidth = Math.max(0.0f, Math.min(width, width * state.progress()));

        draw.rect(state.x(), state.y(), width, state.height(), Paint.fill(state.trackColor()));
        if (fillWidth > 0.0f) {
            draw.rect(state.x(), state.y(), fillWidth, state.height(), Paint.fill(state.fillColor()));
        }
    };

    private ProgressBarRenderers() {
    }
}
