package dev.sixik.unigui.api.utils.mutable;

public class MutableString {

    protected String value;

    public MutableString(String value) {
        this.value = value;
    }

    public MutableString set(String value) {
        this.value = value;
        return this;
    }

    public String get() {
        return value;
    }
}
