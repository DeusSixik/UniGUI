package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;

public final class ColorPickerRenderers {
    public static final ColorPickerRenderer DEFAULT = (draw, state) -> {
        if (state.width() <= 0.0f || state.height() <= 0.0f) return;

        int steps = 32;
        float[] hueRgb = hsvToRgb(state.hue(), 1.0f, 1.0f);
        MutableColor hueColor = MutableColor.rgba(hueRgb[0], hueRgb[1], hueRgb[2], 1.0f);
        MutableColor white = MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f);
        MutableColor black = MutableColor.rgba(0.0f, 0.0f, 0.0f, 1.0f);
        for (int i = 0; i < steps; i++) {
            float leftT = i / (float) steps;
            float rightT = (i + 1) / (float) steps;
            MutableColor topLeft = mix(white, hueColor, leftT);
            MutableColor topRight = mix(white, hueColor, rightT);
            float x1 = state.x() + state.width() * leftT;
            float x2 = state.x() + state.width() * rightT;
            draw.addRectFilledMultiColor(x1, state.y(), x2 - x1, state.height(), topLeft, topRight, black, black);
        }
        draw.rect(state.x(), state.y(), state.width(), state.height(),
                Paint.stroke(MutableColor.rgba(0.85f, 0.92f, 1.0f, state.enabled() ? 0.75f : 0.35f), 1.0f));
        float cx = state.x() + state.saturation() * state.width();
        float cy = state.y() + (1.0f - state.value()) * state.height();
        float radius = state.dragging() || state.hovered() ? 4.5f : 4.0f;
        draw.addCircle(cx, cy, radius, MutableColor.rgba(0.0f, 0.0f, 0.0f, state.enabled() ? 0.90f : 0.45f), 16, 2.0f);
        draw.addCircle(cx, cy, Math.max(1.0f, radius - 1.0f), MutableColor.rgba(1.0f, 1.0f, 1.0f, state.enabled() ? 0.95f : 0.45f), 16, 1.0f);
    };

    private ColorPickerRenderers() {
    }

    private static float[] hsvToRgb(float hue, float saturation, float value) {
        float h = ((hue % 360.0f) + 360.0f) % 360.0f;
        float c = value * saturation;
        float x = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m = value - c;
        float r;
        float g;
        float b;
        if (h < 60.0f) {
            r = c;
            g = x;
            b = 0.0f;
        } else if (h < 120.0f) {
            r = x;
            g = c;
            b = 0.0f;
        } else if (h < 180.0f) {
            r = 0.0f;
            g = c;
            b = x;
        } else if (h < 240.0f) {
            r = 0.0f;
            g = x;
            b = c;
        } else if (h < 300.0f) {
            r = x;
            g = 0.0f;
            b = c;
        } else {
            r = c;
            g = 0.0f;
            b = x;
        }
        return new float[]{r + m, g + m, b + m};
    }

    private static MutableColor mix(MutableColor left, MutableColor right, float t) {
        float inverse = 1.0f - t;
        return MutableColor.rgba(
                left.r() * inverse + right.r() * t,
                left.g() * inverse + right.g() * t,
                left.b() * inverse + right.b() * t,
                left.a() * inverse + right.a() * t);
    }
}

