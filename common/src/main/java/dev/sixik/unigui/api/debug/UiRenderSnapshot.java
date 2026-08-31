package dev.sixik.unigui.api.debug;

import dev.sixik.unigui.api.render.DrawCommandType;

import java.util.Arrays;

/**
 * Снимок структуры draw list за последний кадр.
 *
 * <p>Счётчики описывают объём работы, который backend должен выполнить: они не
 * вызывают OpenGL и не меняют порядок команд. Это позволяет использовать их
 * для поиска мест, где имеет смысл добавлять batching, кэширование или
 * instancing.</p>
 */
public final class UiRenderSnapshot {
    public static final UiRenderSnapshot EMPTY = new UiRenderSnapshot(
            new int[DrawCommandType.values().length],
            new int[DrawCommandType.values().length],
            new int[DrawCommandType.values().length],
            new int[DrawCommandType.values().length],
            new int[DrawCommandType.values().length],
            new int[DrawCommandType.values().length],
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0);

    private final int[] commandCounts;
    private final int[] batchCounts;
    private final int[] batchCommandTotals;
    private final int[] estimatedDrawCalls;
    private final int[] estimatedVertices;
    private final int[] estimatedIndices;
    private final int textGlyphs;
    private final int textCommands;
    private final int textureCommands;
    private final int textureSwitches;
    private final int shaderUniforms;
    private final int shaderTextures;
    private final long sdfPasses;
    private final long sdfCommands;
    private final long sdfDrawCalls;
    private final long sdfUniformUploads;
    private final long sdfFlushes;
    private final long sdfNanos;
    private final int meshVertices;
    private final int pathElements;
    private final int transformCommands;
    private final int clipCommands;
    private final int barrierBatches;
    private final int singletonBatches;
    private final int maxBatchSize;

    public UiRenderSnapshot(int[] commandCounts, int[] batchCounts, int[] batchCommandTotals,
                            int[] estimatedDrawCalls, int[] estimatedVertices, int[] estimatedIndices,
                            int textGlyphs, int textCommands, int textureCommands, int textureSwitches,
                            int shaderUniforms, int shaderTextures,
                            long sdfPasses, long sdfCommands, long sdfDrawCalls,
                            long sdfUniformUploads, long sdfFlushes, long sdfNanos,
                            int meshVertices, int pathElements, int transformCommands, int clipCommands,
                            int barrierBatches, int singletonBatches, int maxBatchSize) {
        this.commandCounts = copy(commandCounts);
        this.batchCounts = copy(batchCounts);
        this.batchCommandTotals = copy(batchCommandTotals);
        this.estimatedDrawCalls = copy(estimatedDrawCalls);
        this.estimatedVertices = copy(estimatedVertices);
        this.estimatedIndices = copy(estimatedIndices);
        this.textGlyphs = Math.max(0, textGlyphs);
        this.textCommands = Math.max(0, textCommands);
        this.textureCommands = Math.max(0, textureCommands);
        this.textureSwitches = Math.max(0, textureSwitches);
        this.shaderUniforms = Math.max(0, shaderUniforms);
        this.shaderTextures = Math.max(0, shaderTextures);
        this.sdfPasses = Math.max(0L, sdfPasses);
        this.sdfCommands = Math.max(0L, sdfCommands);
        this.sdfDrawCalls = Math.max(0L, sdfDrawCalls);
        this.sdfUniformUploads = Math.max(0L, sdfUniformUploads);
        this.sdfFlushes = Math.max(0L, sdfFlushes);
        this.sdfNanos = Math.max(0L, sdfNanos);
        this.meshVertices = Math.max(0, meshVertices);
        this.pathElements = Math.max(0, pathElements);
        this.transformCommands = Math.max(0, transformCommands);
        this.clipCommands = Math.max(0, clipCommands);
        this.barrierBatches = Math.max(0, barrierBatches);
        this.singletonBatches = Math.max(0, singletonBatches);
        this.maxBatchSize = Math.max(0, maxBatchSize);
    }

    public int commandCount(DrawCommandType type) { return value(commandCounts, type); }
    public int batchCount(DrawCommandType type) { return value(batchCounts, type); }
    public int batchCommandTotal(DrawCommandType type) { return value(batchCommandTotals, type); }
    public int estimatedDrawCalls(DrawCommandType type) { return value(estimatedDrawCalls, type); }
    public int estimatedVertices(DrawCommandType type) { return value(estimatedVertices, type); }
    public int estimatedIndices(DrawCommandType type) { return value(estimatedIndices, type); }
    public int textGlyphs() { return textGlyphs; }
    public int textCommands() { return textCommands; }
    public int textureCommands() { return textureCommands; }
    public int textureSwitches() { return textureSwitches; }
    public int shaderUniforms() { return shaderUniforms; }
    public int shaderTextures() { return shaderTextures; }
    public long sdfPasses() { return sdfPasses; }
    public long sdfCommands() { return sdfCommands; }
    public long sdfDrawCalls() { return sdfDrawCalls; }
    public long sdfUniformUploads() { return sdfUniformUploads; }
    public long sdfFlushes() { return sdfFlushes; }
    public float sdfMillis() { return sdfNanos / 1_000_000.0f; }
    public int meshVertices() { return meshVertices; }
    public int pathElements() { return pathElements; }
    public int transformCommands() { return transformCommands; }
    public int clipCommands() { return clipCommands; }
    public int barrierBatches() { return barrierBatches; }
    public int singletonBatches() { return singletonBatches; }
    public int maxBatchSize() { return maxBatchSize; }

    public int totalEstimatedDrawCalls() { return sum(estimatedDrawCalls); }
    public int totalEstimatedVertices() { return sum(estimatedVertices); }
    public int totalEstimatedIndices() { return sum(estimatedIndices); }

    private static int[] copy(int[] values) {
        return values == null ? new int[DrawCommandType.values().length] : Arrays.copyOf(values, values.length);
    }

    private static int value(int[] values, DrawCommandType type) {
        if (type == null || type.ordinal() >= values.length) return 0;
        return values[type.ordinal()];
    }

    private static int sum(int[] values) {
        int result = 0;
        for (int value : values) result += value;
        return result;
    }
}
