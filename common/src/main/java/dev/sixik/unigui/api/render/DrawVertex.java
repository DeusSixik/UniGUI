package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

public record DrawVertex(float x, float y, float u, float v, ColorView color) {
    public DrawVertex {
        color = color == null ? new MutableColor() : new MutableColor(color.r(), color.g(), color.b(), color.a());
    }

    public DrawVertex(float x, float y, ColorView color) {
        this(x, y, 0.0f, 0.0f, color);
    }

    public DrawVertex copy() {
        return new DrawVertex(x, y, u, v, color);
    }
}
