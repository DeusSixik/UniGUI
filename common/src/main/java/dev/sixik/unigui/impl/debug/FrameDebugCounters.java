package dev.sixik.unigui.impl.debug;

import dev.sixik.unigui.api.debug.UiDebugCounters;
import dev.sixik.unigui.api.debug.UiDebugSnapshot;

public final class FrameDebugCounters implements UiDebugCounters {
    private long frameIndex;
    private long lastFrameStartNanos;
    private float framesPerSecond;
    private float frameTotalMillis;
    private float frameCpuMillis;
    private float frameGpuMillis = -1.0f;
    private int drawCommandCount;
    private int batchCount;
    private long textureCacheHits;
    private long textureCacheMisses;
    private long textureRenders;
    private String lastTextureCacheMissReason = "NONE";

    @Override
    public void beginFrame(long frameIndex) {
        long now = System.nanoTime();
        if (lastFrameStartNanos > 0L) {
            long deltaNanos = now - lastFrameStartNanos;
            if (deltaNanos > 0L) {
                framesPerSecond = 1_000_000_000.0f / deltaNanos;
            }
        }
        lastFrameStartNanos = now;

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
    public void recordFrameCpuMillis(float millis) {
        frameCpuMillis = Float.isFinite(millis) ? Math.max(0.0f, millis) : 0.0f;
    }

    @Override
    public void recordFrameTotalMillis(float millis) {
        frameTotalMillis = Float.isFinite(millis) ? Math.max(0.0f, millis) : 0.0f;
    }

    @Override
    public void recordFrameGpuMillis(float millis) {
        frameGpuMillis = Float.isFinite(millis) ? millis : -1.0f;
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
        return new UiDebugSnapshot(frameIndex, framesPerSecond, frameTotalMillis, frameCpuMillis, frameGpuMillis,
                drawCommandCount, batchCount,
                textureCacheHits, textureCacheMisses, textureRenders, lastTextureCacheMissReason);
    }
}
