package dev.sixik.unigui.impl.debug;

import dev.sixik.unigui.api.debug.UiDebugCounters;
import dev.sixik.unigui.api.debug.UiDebugSnapshot;

public final class FrameDebugCounters implements UiDebugCounters {
    private long frameIndex;
    private int drawCommandCount;
    private int batchCount;
    private long textureCacheHits;
    private long textureCacheMisses;
    private long textureRenders;
    private String lastTextureCacheMissReason = "NONE";

    @Override
    public void beginFrame(long frameIndex) {
        this.frameIndex = frameIndex;
        drawCommandCount = 0;
        batchCount = 0;
        textureCacheHits = 0L;
        textureCacheMisses = 0L;
        textureRenders = 0L;
        lastTextureCacheMissReason = "NONE";
    }

    @Override
    public void recordDrawCommands(int count) {
        drawCommandCount = Math.max(0, count);
    }

    @Override
    public void recordBatches(int count) {
        batchCount = Math.max(0, count);
    }

    @Override
    public void recordTextureCacheHit() {
        textureCacheHits++;
    }

    @Override
    public void recordTextureCacheMiss(String reason) {
        textureCacheMisses++;
        lastTextureCacheMissReason = reason == null || reason.isBlank() ? "UNKNOWN" : reason;
    }

    @Override
    public void recordTextureRender() {
        textureRenders++;
    }

    @Override
    public UiDebugSnapshot snapshot() {
        return new UiDebugSnapshot(frameIndex, drawCommandCount, batchCount,
                textureCacheHits, textureCacheMisses, textureRenders, lastTextureCacheMissReason);
    }
}
