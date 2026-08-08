package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;

public record LoadingIndicatorState(
        float x,
        float y,
        float width,
        float height,
        float phase,
        float speed,
        int segments,
        float thickness,
        ColorView accentColor,
        ColorView trackColor
) {
    public float centerX() {
        return x + width * 0.5f;
    }

    public float centerY() {
        return y + height * 0.5f;
    }

    public float size() {
        return Math.min(width, height);
    }

    public float phaseRadians() {
        return phase * ((float) Math.PI * 2.0f);
    }
}
