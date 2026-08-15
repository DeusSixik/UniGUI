package dev.sixik.unigui.widgets.caching;



public final class CachedSubtreeStats {
    public static final CachedSubtreeStats EMPTY = new CachedSubtreeStats(0L, 0L, 0L, 0L, 0, 0, CachedSubtreeMissReason.NONE);

    private final long renderCalls;
    private final long cacheHits;
    private final long cacheMisses;
    private final long textureRenders;
    private final int cachedWidth;
    private final int cachedHeight;
    private final CachedSubtreeMissReason lastMissReason;

    public CachedSubtreeStats(long renderCalls, long cacheHits, long cacheMisses, long textureRenders,
                              int cachedWidth, int cachedHeight, CachedSubtreeMissReason lastMissReason) {
        this.renderCalls = renderCalls;
        this.cacheHits = cacheHits;
        this.cacheMisses = cacheMisses;
        this.textureRenders = textureRenders;
        this.cachedWidth = cachedWidth;
        this.cachedHeight = cachedHeight;
        this.lastMissReason = lastMissReason == null ? CachedSubtreeMissReason.NONE : lastMissReason;
    }

    public long renderCalls() {
        return renderCalls;
    }

    public long cacheHits() {
        return cacheHits;
    }

    public long cacheMisses() {
        return cacheMisses;
    }

    public long textureRenders() {
        return textureRenders;
    }

    public int cachedWidth() {
        return cachedWidth;
    }

    public int cachedHeight() {
        return cachedHeight;
    }

    public CachedSubtreeMissReason lastMissReason() {
        return lastMissReason;
    }

    public float hitRate() {
        long total = cacheHits + cacheMisses;
        return total == 0L ? 0.0f : cacheHits / (float) total;
    }
}
