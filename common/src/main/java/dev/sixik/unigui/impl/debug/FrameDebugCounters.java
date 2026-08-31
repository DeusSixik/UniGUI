package dev.sixik.unigui.impl.debug;

import dev.sixik.unigui.api.debug.UiDebugCounters;
import dev.sixik.unigui.api.debug.UiDebugSnapshot;
import dev.sixik.unigui.api.debug.UiRenderSnapshot;
import dev.sixik.unigui.api.render.DrawCommandType;

import java.util.Arrays;

public final class FrameDebugCounters implements UiDebugCounters {
    private final int[] commandCounts = new int[DrawCommandType.values().length];
    private final int[] batchCounts = new int[DrawCommandType.values().length];
    private final int[] batchCommandTotals = new int[DrawCommandType.values().length];
    private final int[] estimatedDrawCalls = new int[DrawCommandType.values().length];
    private final int[] estimatedVertices = new int[DrawCommandType.values().length];
    private final int[] estimatedIndices = new int[DrawCommandType.values().length];

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
    private int textGlyphs;
    private int textCommands;
    private int textureCommands;
    private int textureSwitches;
    private int shaderUniforms;
    private int shaderTextures;
    private int meshVertices;
    private int pathElements;
    private int transformCommands;
    private int clipCommands;
    private int barrierBatches;
    private int singletonBatches;
    private int maxBatchSize;

    @Override
    public void beginFrame(long frameIndex) {
        long now = System.nanoTime();
        if (lastFrameStartNanos > 0L) {
            long deltaNanos = now - lastFrameStartNanos;
            if (deltaNanos > 0L) framesPerSecond = 1_000_000_000.0f / deltaNanos;
        }
        lastFrameStartNanos = now;
        this.frameIndex = frameIndex;
        drawCommandCount = 0;
        batchCount = 0;
        Arrays.fill(commandCounts, 0);
        Arrays.fill(batchCounts, 0);
        Arrays.fill(batchCommandTotals, 0);
        Arrays.fill(estimatedDrawCalls, 0);
        Arrays.fill(estimatedVertices, 0);
        Arrays.fill(estimatedIndices, 0);
        textGlyphs = 0;
        textCommands = 0;
        textureCommands = 0;
        textureSwitches = 0;
        shaderUniforms = 0;
        shaderTextures = 0;
        meshVertices = 0;
        pathElements = 0;
        transformCommands = 0;
        clipCommands = 0;
        barrierBatches = 0;
        singletonBatches = 0;
        maxBatchSize = 0;
        textureCacheHits = 0L;
        textureCacheMisses = 0L;
        textureRenders = 0L;
        lastTextureCacheMissReason = "NONE";
    }

    @Override
    public void recordDrawCommands(int count) { drawCommandCount = Math.max(0, count); }
    @Override
    public void recordBatches(int count) { batchCount = Math.max(0, count); }

    @Override
    public void recordDrawCommand(DrawCommandType type) {
        if (type != null) commandCounts[type.ordinal()]++;
    }

    @Override
    public void recordBatch(DrawCommandType type, int commandCount, boolean barrier,
                            int drawCalls, int vertices, int indices) {
        if (type == null) return;
        int index = type.ordinal();
        batchCounts[index]++;
        int safeCount = Math.max(0, commandCount);
        batchCommandTotals[index] += safeCount;
        estimatedDrawCalls[index] += Math.max(0, drawCalls);
        estimatedVertices[index] += Math.max(0, vertices);
        estimatedIndices[index] += Math.max(0, indices);
        batchCount++;
        if (barrier) barrierBatches++;
        if (safeCount <= 1) singletonBatches++;
        maxBatchSize = Math.max(maxBatchSize, safeCount);
    }

    @Override
    public void recordRenderFeatures(int textGlyphs, int textCommands, int textureCommands,
                                     int textureSwitches, int meshVertices, int pathElements,
                                     int transformCommands, int clipCommands) {
        this.textGlyphs += Math.max(0, textGlyphs);
        this.textCommands += Math.max(0, textCommands);
        this.textureCommands += Math.max(0, textureCommands);
        this.textureSwitches += Math.max(0, textureSwitches);
        this.meshVertices += Math.max(0, meshVertices);
        this.pathElements += Math.max(0, pathElements);
        this.transformCommands += Math.max(0, transformCommands);
        this.clipCommands += Math.max(0, clipCommands);
    }

    @Override
    public void recordShaderFeatures(int uniformCount, int textureCount) {
        shaderUniforms += Math.max(0, uniformCount);
        shaderTextures += Math.max(0, textureCount);
    }

    @Override
    public void recordFrameCpuMillis(float millis) { frameCpuMillis = Float.isFinite(millis) ? Math.max(0.0f, millis) : 0.0f; }
    @Override
    public void recordFrameTotalMillis(float millis) { frameTotalMillis = Float.isFinite(millis) ? Math.max(0.0f, millis) : 0.0f; }
    @Override
    public void recordFrameGpuMillis(float millis) { frameGpuMillis = Float.isFinite(millis) ? millis : -1.0f; }
    @Override
    public void recordTextureCacheHit() { textureCacheHits++; }
    @Override
    public void recordTextureCacheMiss(String reason) {
        textureCacheMisses++;
        lastTextureCacheMissReason = reason == null || reason.isBlank() ? "UNKNOWN" : reason;
    }
    @Override
    public void recordTextureRender() { textureRenders++; }

    @Override
    public UiDebugSnapshot snapshot() {
        return new UiDebugSnapshot(frameIndex, framesPerSecond, frameTotalMillis, frameCpuMillis, frameGpuMillis,
                drawCommandCount, batchCount, textureCacheHits, textureCacheMisses, textureRenders,
                lastTextureCacheMissReason);
    }

    @Override
    public UiRenderSnapshot renderSnapshot() {
        return new UiRenderSnapshot(commandCounts, batchCounts, batchCommandTotals,
                estimatedDrawCalls, estimatedVertices, estimatedIndices,
                textGlyphs, textCommands, textureCommands, textureSwitches,
                shaderUniforms, shaderTextures,
                meshVertices, pathElements, transformCommands, clipCommands,
                barrierBatches, singletonBatches, maxBatchSize);
    }
}
