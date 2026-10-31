package dev.sixik.unigui.api.utils.mutable;

public class MutableByte {

    protected byte value;

    public MutableByte(byte value) {
        this.value = value;
    }

    public MutableByte set(byte value) {
        this.value = value;
        return this;
    }

    public byte get() {
        return value;
    }
}
