package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;

public final class ProgressBarRenderers {
    public static final ProgressBarRenderer DEFAULT = (draw, state) -> {
        float width = Math.max(0.0f, state.width());
        float fillWidth = Math.max(0.0f, Math.min(width, width * state.progress()));

        draw.rect(state.x(), state.y(), width, state.height(), Paint.fill(state.trackColor()));
        if (state.indeterminate()) {
            float segmentWidth = Math.max(8.0f, width * 0.32f);
            float travel = width + segmentWidth;
            float offset = state.indeterminateOffset() - (float) Math.floor(state.indeterminateOffset());
            float segmentX = state.x() + offset * travel - segmentWidth;
            draw.pushClip(state.x(), state.y(), width, state.height());
            try {
                draw.rect(segmentX, state.y(), segmentWidth, state.height(), Paint.fill(state.fillColor()));
            } finally {
                draw.popClip();
            }
            return;
        }
        if (fillWidth > 0.0f) {
            draw.rect(state.x(), state.y(), fillWidth, state.height(), Paint.fill(state.fillColor()));
        }
    };

    private ProgressBarRenderers() {
    }
}
