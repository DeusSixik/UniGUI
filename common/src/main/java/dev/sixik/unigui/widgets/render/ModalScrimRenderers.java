package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;

public final class ModalScrimRenderers {
    public static final WidgetRender DEFAULT = WidgetRender.of(OverlayLayer.class, (draw, layer) -> {
        ModalScrimState state = layer.modalScrimState();
        if (!state.visible() || state.width() <= 0.0f || state.height() <= 0.0f) return;
        draw.rect(state.x(), state.y(), state.width(), state.height(), Paint.fill(state.color()));
    });

    private ModalScrimRenderers() {
    }
}
