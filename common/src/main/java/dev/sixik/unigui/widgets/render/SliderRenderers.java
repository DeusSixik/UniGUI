package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;

public final class SliderRenderers {
    public static final SliderRenderer DEFAULT = (draw, state) -> {
        float height = Math.max(1.0f, state.height());
        float trackHeight = Math.max(2.0f, Math.min(4.0f, height * 0.25f));
        float trackY = state.y() + (height - trackHeight) * 0.5f;
        float fillWidth = state.width() * state.normalizedValue();
        float knobX = state.x() + fillWidth - state.knobWidth() * 0.5f;

        draw.roundedRect(state.x(), trackY, state.width(), trackHeight, trackHeight * 0.5f,
                Paint.fill(state.trackColor()));
        draw.roundedRect(state.x(), trackY, fillWidth, trackHeight, trackHeight * 0.5f,
                Paint.fill(state.fillColor()));
        draw.roundedRect(knobX, state.y() + 2.0f, state.knobWidth(), Math.max(1.0f, height - 4.0f), 2.0f,
                Paint.fill(state.knobColor()));
    };

    private SliderRenderers() {
    }
}
