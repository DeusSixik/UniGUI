package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

public final class Paint {
    private final MutableColor color = new MutableColor();
    private float strokeWidth;
    private boolean stroke;
    private BlendMode blendMode = BlendMode.NORMAL;
    private float dashLength;
    private float dashGap;
    private float dashOffset;

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

    public BlendMode blendMode() {
        return blendMode;
    }

    public Paint blend(BlendMode blendMode) {
        this.blendMode = blendMode == null ? BlendMode.NORMAL : blendMode;
        return this;
    }

    public boolean dashed() {
        return dashLength > 0.0f && dashGap > 0.0f;
    }

    public float dashLength() {
        return dashLength;
    }

    public float dashGap() {
        return dashGap;
    }

    public float dashOffset() {
        return dashOffset;
    }

    public Paint dash(float length, float gap) {
        dashLength = sanitizeDash(length);
        dashGap = sanitizeDash(gap);
        return this;
    }

    public Paint dashOffset(float dashOffset) {
        this.dashOffset = Float.isFinite(dashOffset) ? dashOffset : 0.0f;
        return this;
    }

    public Paint clearDash() {
        dashLength = 0.0f;
        dashGap = 0.0f;
        dashOffset = 0.0f;
        return this;
    }

    public Paint copy() {
        Paint copy = new Paint();
        copy.color.set(color);
        copy.strokeWidth = strokeWidth;
        copy.stroke = stroke;
        copy.blendMode = blendMode;
        copy.dashLength = dashLength;
        copy.dashGap = dashGap;
        copy.dashOffset = dashOffset;
        return copy;
    }

    private static float sanitizeDash(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
