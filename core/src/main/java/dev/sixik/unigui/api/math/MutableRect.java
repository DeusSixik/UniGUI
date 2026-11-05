package dev.sixik.unigui.api.math;

import java.util.Objects;

public final class MutableRect implements RectView {
    private float x;
    private float y;
    private float width;
    private float height;
    private Runnable onChanged;

    public MutableRect() {
        this(0.0f, 0.0f, 0.0f, 0.0f);
    }

    public MutableRect(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public float x() {
        return x;
    }

    @Override
    public float y() {
        return y;
    }

    @Override
    public float width() {
        return width;
    }

    @Override
    public float height() {
        return height;
    }

    public MutableRect set(float x, float y, float width, float height) {
        if (this.x == x && this.y == y && this.width == width && this.height == height) return this;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        changed();
        return this;
    }

    public MutableRect set(RectView other) {
        Objects.requireNonNull(other, "other");
        return set(other.x(), other.y(), other.width(), other.height());
    }

    public MutableRect onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public MutableRect copy() {
        return new MutableRect(x, y, width, height);
    }

    private void changed() {
        if (onChanged != null) onChanged.run();
    }
}
