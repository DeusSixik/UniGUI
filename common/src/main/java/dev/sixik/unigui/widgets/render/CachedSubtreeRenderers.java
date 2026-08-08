package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;

public final class CachedSubtreeRenderers {
    public static final CachedSubtreeRenderer DEFAULT = (draw, state) -> {
        if (state.texture() != null) {
            draw.texture(state.texture(), state.x(), state.y(), state.width(), state.height(), Paint.fill(state.tint()));
        }

        if (!state.debugVisible()) return;

        var debugColor = state.cacheHit() ? state.debugHitColor() : state.debugMissColor();
        draw.rect(state.x(), state.y(), state.overlayWidth(), 22.0f, Paint.fill(state.debugBackgroundColor()));
        draw.rect(state.x(), state.y(), state.width(), state.height(), Paint.stroke(debugColor, 1.0f));
        draw.text("cache " + state.stateText(), state.x() + 3.0f, state.y() + 3.0f,
                state.overlayWidth() - 6.0f, 9.0f, Paint.fill(debugColor));
        draw.text(state.statsText(), state.x() + 3.0f, state.y() + 13.0f,
                state.overlayWidth() - 6.0f, 9.0f, Paint.fill(state.debugTextColor()));
    };

    private CachedSubtreeRenderers() {
    }
}
