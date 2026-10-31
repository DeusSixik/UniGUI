package dev.sixik.unigui.api.utils.mutable;

public class MutableChar {

    protected char value;

    public MutableChar(char value) {
        this.value = value;
    }

    public MutableChar set(char value) {
        this.value = value;
        return this;
    }

    public char get() {
        return value;
    }
}
