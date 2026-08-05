package dev.sixik.unigui.api.debug;

public final class UiDebugSnapshot {
    public static final UiDebugSnapshot EMPTY = new UiDebugSnapshot(0L, 0, 0, 0L, 0L, 0L, "NONE");

    private final long frameIndex;
    private final int drawCommandCount;
    private final int batchCount;
    private final long textureCacheHits;
    private final long textureCacheMisses;
    private final long textureRenders;
    private final String lastTextureCacheMissReason;

    public UiDebugSnapshot(long frameIndex, int drawCommandCount, int batchCount,
                           long textureCacheHits, long textureCacheMisses, long textureRenders,
                           String lastTextureCacheMissReason) {
        this.frameIndex = frameIndex;
        this.drawCommandCount = Math.max(0, drawCommandCount);
        this.batchCount = Math.max(0, batchCount);
        this.textureCacheHits = Math.max(0L, textureCacheHits);
        this.textureCacheMisses = Math.max(0L, textureCacheMisses);
        this.textureRenders = Math.max(0L, textureRenders);
        this.lastTextureCacheMissReason = lastTextureCacheMissReason == null || lastTextureCacheMissReason.isBlank() ? "NONE" : lastTextureCacheMissReason;
    }

    public long frameIndex() {
        return frameIndex;
    }

    public int drawCommandCount() {
        return drawCommandCount;
    }

    public int batchCount() {
        return batchCount;
    }

    public long textureCacheHits() {
        return textureCacheHits;
    }

    public long textureCacheMisses() {
        return textureCacheMisses;
    }

    public long textureRenders() {
        return textureRenders;
    }

    public String lastTextureCacheMissReason() {
        return lastTextureCacheMissReason;
    }
}
