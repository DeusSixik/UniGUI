package dev.sixik.unigui.api.utils.mutable;

public class MutableFloat {

    protected float value;

    public MutableFloat(float value) {
        this.value = value;
    }

    public MutableFloat set(float value) {
        this.value = value;
        return this;
    }

    public float get() {
        return value;
    }

    public MutableFloat increment() {
        this.value++;
        return this;
    }

    public MutableFloat increment(int value) {
        this.value += value;
        return this;
    }

    public MutableFloat increment(long value) {
        this.value += value;
        return this;
    }

    public MutableFloat increment(float value) {
        this.value += value;
        return this;
    }

    public MutableFloat decrement() {
        this.value--;
        return this;
    }

    public MutableFloat subtract(int value) {
        this.value -= value;
        return this;
    }

    public MutableFloat subtract(long value) {
        this.value -= value;
        return this;
    }

    public MutableFloat subtract(float value) {
        this.value -= value;
        return this;
    }

    public MutableFloat multiply(int value) {
        this.value *= value;
        return this;
    }

    public MutableFloat multiply(long value) {
        this.value *= value;
        return this;
    }

    public MutableFloat multiply(float value) {
        this.value *= value;
        return this;
    }

    public MutableFloat divide(int value) {
        this.value /= value;
        return this;
    }

    public MutableFloat divide(long value) {
        this.value /= value;
        return this;
    }

    public MutableFloat divide(float value) {
        this.value /= value;
        return this;
    }
}
