package dev.sixik.unigui.api.utils.mutable;

public class MutableInteger {

    protected int value;

    public MutableInteger(int value) {
        this.value = value;
    }

    public MutableInteger set(int value) {
        this.value = value;
        return this;
    }

    public int get() {
        return value;
    }

    public MutableInteger increment() {
        this.value++;
        return this;
    }

    public MutableInteger increment(int value) {
        this.value += value;
        return this;
    }

    public MutableInteger decrement() {
        this.value--;
        return this;
    }

    public MutableInteger subtract(int value) {
        this.value -= value;
        return this;
    }

    public MutableInteger multiply(int value) {
        this.value *= value;
        return this;
    }

    public MutableInteger divide(int value) {
        this.value /= value;
        return this;
    }
}
