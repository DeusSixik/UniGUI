package dev.sixik.unigui.api.utils.mutable;

public class MutableDouble {

    protected double value;

    public MutableDouble(double value) {
        this.value = value;
    }

    public MutableDouble set(double value) {
        this.value = value;
        return this;
    }

    public double get() {
        return value;
    }

    public MutableDouble increment() {
        this.value++;
        return this;
    }

    public MutableDouble increment(int value) {
        this.value += value;
        return this;
    }

    public MutableDouble increment(long value) {
        this.value += value;
        return this;
    }

    public MutableDouble increment(double value) {
        this.value += value;
        return this;
    }

    public MutableDouble decrement() {
        this.value--;
        return this;
    }

    public MutableDouble subtract(int value) {
        this.value -= value;
        return this;
    }

    public MutableDouble subtract(long value) {
        this.value -= value;
        return this;
    }

    public MutableDouble subtract(double value) {
        this.value -= value;
        return this;
    }

    public MutableDouble multiply(int value) {
        this.value *= value;
        return this;
    }

    public MutableDouble multiply(long value) {
        this.value *= value;
        return this;
    }

    public MutableDouble multiply(double value) {
        this.value *= value;
        return this;
    }

    public MutableDouble divide(int value) {
        this.value /= value;
        return this;
    }

    public MutableDouble divide(long value) {
        this.value /= value;
        return this;
    }

    public MutableDouble divide(double value) {
        this.value /= value;
        return this;
    }
}
