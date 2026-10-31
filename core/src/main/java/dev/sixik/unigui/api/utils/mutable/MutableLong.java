package dev.sixik.unigui.api.utils.mutable;

public class MutableLong {

    protected long value;

    public MutableLong(long value) {
        this.value = value;
    }

    public MutableLong set(long value) {
        this.value = value;
        return this;
    }

    public long get() {
        return value;
    }

    public MutableLong increment() {
        this.value++;
        return this;
    }

    public MutableLong increment(int value) {
        this.value += value;
        return this;
    }

    public MutableLong increment(long value) {
        this.value += value;
        return this;
    }

    public MutableLong decrement() {
        this.value--;
        return this;
    }

    public MutableLong subtract(int value) {
        this.value -= value;
        return this;
    }

    public MutableLong subtract(long value) {
        this.value -= value;
        return this;
    }

    public MutableLong multiply(int value) {
        this.value *= value;
        return this;
    }

    public MutableLong multiply(long value) {
        this.value *= value;
        return this;
    }

    public MutableLong divide(int value) {
        this.value /= value;
        return this;
    }

    public MutableLong divide(long value) {
        this.value /= value;
        return this;
    }
}
