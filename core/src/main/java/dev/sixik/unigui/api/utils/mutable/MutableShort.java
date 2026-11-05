package dev.sixik.unigui.api.utils.mutable;

public class MutableShort {

    protected short value;

    public MutableShort(short value) {
        this.value = value;
    }

    public MutableShort set(short value) {
        this.value = value;
        return this;
    }

    public short get() {
        return value;
    }

    public MutableShort increment() {
        this.value++;
        return this;
    }

    public MutableShort increment(short value) {
        this.value += value;
        return this;
    }

    public MutableShort decrement() {
        this.value--;
        return this;
    }

    public MutableShort subtract(short value) {
        this.value -= value;
        return this;
    }

    public MutableShort multiply(short value) {
        this.value *= value;
        return this;
    }

    public MutableShort divide(short value) {
        this.value /= value;
        return this;
    }
}
