package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;

public final class ModalScrimRenderers {
    public static final ModalScrimRenderer DEFAULT = (draw, state) -> {
        if (!state.visible() || state.width() <= 0.0f || state.height() <= 0.0f) return;
        draw.rect(state.x(), state.y(), state.width(), state.height(), Paint.fill(state.color()));
    };

    private ModalScrimRenderers() {
    }
}

