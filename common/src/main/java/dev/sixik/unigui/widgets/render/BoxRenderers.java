package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;

public final class BoxRenderers {
    public static final BoxRenderer DEFAULT = (draw, state) -> {
        if (state.backgroundVisible()) {
            draw.roundedRect(state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.fill(state.background()));
        }

        if (state.backgroundTexture() != null && state.backgroundTexturePlacement() != null) {
            draw.texture(state.backgroundTexture(), state.backgroundTexturePlacement(), state.radius(),
                    Paint.fill(state.backgroundTextureTint()));
        }

        if (state.borderVisible()) {
            draw.roundedRect(state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.stroke(state.borderColor(), state.borderWidth()));
        }
    };

    private BoxRenderers() {
    }
}
