package dev.sixik.unigui.api.utils.mutable;

public class MutableBoolean {

    protected boolean value;

    public MutableBoolean(boolean value) {
        this.value = value;
    }

    public MutableBoolean set(boolean value) {
        this.value = value;
        return this;
    }

    public boolean get() {
        return value;
    }
}
