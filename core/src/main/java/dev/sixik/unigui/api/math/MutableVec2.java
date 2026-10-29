package dev.sixik.unigui.api.math;

import java.util.Objects;

public final class MutableVec2 implements Vec2View {
    private float x;
    private float y;
    private Runnable onChanged;

    public MutableVec2() {
        this(0.0f, 0.0f);
    }

    public MutableVec2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public float x() {
        return x;
    }

    @Override
    public float y() {
        return y;
    }

    public MutableVec2 set(float value) {
        if (this.x == value && this.y == value) return this;
        this.x = value;
        this.y = value;
        changed();
        return this;
    }


    public MutableVec2 set(float x, float y) {
        if (this.x == x && this.y == y) return this;
        this.x = x;
        this.y = y;
        changed();
        return this;
    }

    public MutableVec2 set(Vec2View other) {
        Objects.requireNonNull(other, "other");
        return set(other.x(), other.y());
    }

    public MutableVec2 add(float value) {
        return set(this.x + value, this.y + value);
    }

    public MutableVec2 add(float x, float y) {
        return set(this.x + x, this.y + y);
    }

    public MutableVec2 onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public MutableVec2 copy() {
        return new MutableVec2(x, y);
    }

    private void changed() {
        if (onChanged != null) onChanged.run();
    }
}
