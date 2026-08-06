package dev.sixik.unigui.api.virtualization;

public final class FixedRowVirtualizer {
    private int itemCount;
    private float itemExtent = 18.0f;
    private int overscan = 1;
    private float scrollOffset;
    private float viewportExtent;
    private VirtualRange visibleRange = VirtualRange.EMPTY;

    public int itemCount() {
        return itemCount;
    }

    public FixedRowVirtualizer itemCount(int itemCount) {
        int normalized = Math.max(0, itemCount);
        if (this.itemCount == normalized) return this;
        this.itemCount = normalized;
        scrollOffset = clamp(scrollOffset, 0.0f, maxScrollOffset());
        updateVisibleRange();
        return this;
    }

    public float itemExtent() {
        return itemExtent;
    }

    public FixedRowVirtualizer itemExtent(float itemExtent) {
        float normalized = Float.isFinite(itemExtent) ? Math.max(1.0f, itemExtent) : 18.0f;
        if (this.itemExtent == normalized) return this;
        this.itemExtent = normalized;
        scrollOffset = clamp(scrollOffset, 0.0f, maxScrollOffset());
        updateVisibleRange();
        return this;
    }

    public int overscan() {
        return overscan;
    }

    public FixedRowVirtualizer overscan(int overscan) {
        int normalized = Math.max(0, overscan);
        if (this.overscan == normalized) return this;
        this.overscan = normalized;
        updateVisibleRange();
        return this;
    }

    public float scrollOffset() {
        return scrollOffset;
    }

    public FixedRowVirtualizer scrollOffset(float scrollOffset) {
        float normalized = clamp(scrollOffset, 0.0f, maxScrollOffset());
        if (this.scrollOffset == normalized) return this;
        this.scrollOffset = normalized;
        updateVisibleRange();
        return this;
    }

    public float viewportExtent() {
        return viewportExtent;
    }

    public FixedRowVirtualizer viewportExtent(float viewportExtent) {
        float normalized = Float.isFinite(viewportExtent) ? Math.max(0.0f, viewportExtent) : 0.0f;
        if (this.viewportExtent == normalized) return this;
        this.viewportExtent = normalized;
        scrollOffset = clamp(scrollOffset, 0.0f, maxScrollOffset());
        updateVisibleRange();
        return this;
    }

    public float contentExtent() {
        return itemCount * itemExtent;
    }

    public float maxScrollOffset() {
        return Math.max(0.0f, contentExtent() - viewportExtent);
    }

    public VirtualRange visibleRange() {
        return visibleRange;
    }

    public int firstVisibleIndex() {
        return visibleRange.firstIndex();
    }

    public int lastVisibleIndexExclusive() {
        return visibleRange.lastIndexExclusive();
    }

    public int visibleCount() {
        return visibleRange.count();
    }

    public float itemOffset(int index) {
        return index * itemExtent - scrollOffset;
    }

    private void updateVisibleRange() {
        if (itemCount == 0) {
            visibleRange = VirtualRange.EMPTY;
            return;
        }

        int first = clampIndex((int) Math.floor(scrollOffset / itemExtent) - overscan);
        int visibleItems = (int) Math.ceil(viewportExtent / itemExtent) + overscan * 2 + 1;
        int last = Math.min(itemCount, first + Math.max(0, visibleItems));
        visibleRange = new VirtualRange(first, last);
    }

    private int clampIndex(int index) {
        return Math.max(0, Math.min(itemCount, index));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
