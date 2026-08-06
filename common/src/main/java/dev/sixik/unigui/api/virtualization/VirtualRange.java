package dev.sixik.unigui.api.virtualization;

public record VirtualRange(int firstIndex, int lastIndexExclusive) {
    public static final VirtualRange EMPTY = new VirtualRange(0, 0);

    public VirtualRange {
        firstIndex = Math.max(0, firstIndex);
        lastIndexExclusive = Math.max(firstIndex, lastIndexExclusive);
    }

    public int count() {
        return Math.max(0, lastIndexExclusive - firstIndex);
    }

    public boolean contains(int index) {
        return index >= firstIndex && index < lastIndexExclusive;
    }

    public boolean isEmpty() {
        return count() == 0;
    }
}
