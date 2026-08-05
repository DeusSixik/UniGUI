package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

public final class Paint {
    private final MutableColor color = new MutableColor();
    private float strokeWidth;
    private boolean stroke;

    public static Paint fill(ColorView color) {
        return new Paint().color(color).stroke(false);
    }

    public static Paint stroke(ColorView color, float width) {
        return new Paint().color(color).stroke(true).strokeWidth(width);
    }

    public MutableColor color() {
        return color;
    }

    public Paint color(ColorView color) {
        this.color.set(color);
        return this;
    }

    public float strokeWidth() {
        return strokeWidth;
    }

    public Paint strokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        return this;
    }

    public boolean isStroke() {
        return stroke;
    }

    public Paint stroke(boolean stroke) {
        this.stroke = stroke;
        return this;
    }

    public Paint copy() {
        Paint copy = new Paint();
        copy.color.set(color);
        copy.strokeWidth = strokeWidth;
        copy.stroke = stroke;
        return copy;
    }
}
