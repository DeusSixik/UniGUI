package dev.sixik.unigui.api.math;

import java.util.Objects;

public final class MutableColor implements ColorView {
    private float r;
    private float g;
    private float b;
    private float a;
    private Runnable onChanged;

    public MutableColor() {
        this(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public MutableColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public static MutableColor rgba(float r, float g, float b, float a) {
        return new MutableColor(r, g, b, a);
    }

    @Override
    public float r() {
        return r;
    }

    @Override
    public float g() {
        return g;
    }

    @Override
    public float b() {
        return b;
    }

    @Override
    public float a() {
        return a;
    }

    public MutableColor set(float r, float g, float b, float a) {
        if (this.r == r && this.g == g && this.b == b && this.a == a) return this;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        changed();
        return this;
    }

    public MutableColor set(ColorView other) {
        Objects.requireNonNull(other, "other");
        return set(other.r(), other.g(), other.b(), other.a());
    }

    public MutableColor onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public MutableColor copy() {
        return new MutableColor(r, g, b, a);
    }

    private void changed() {
        if (onChanged != null) onChanged.run();
    }
}
