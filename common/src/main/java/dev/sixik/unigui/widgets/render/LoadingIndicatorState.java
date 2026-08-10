package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.widgets.Spinner;

public record LoadingIndicatorState(
        float x,
        float y,
        float width,
        float height,
        float phase,
        float elapsedSeconds,
        float speed,
        int segments,
        int dots,
        int activeDots,
        int arcs,
        float thickness,
        float radius,
        float angle,
        ColorView accentColor,
        ColorView secondaryColor,
        ColorView trackColor,
        Spinner.Style spinnerStyle
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

    public float timeRadians() {
        return elapsedSeconds * speed * ((float) Math.PI * 2.0f);
    }
}
