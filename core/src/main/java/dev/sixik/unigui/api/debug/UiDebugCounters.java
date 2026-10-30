package dev.sixik.unigui.api.debug;

import dev.sixik.unigui.api.render.DrawCommandType;

public interface UiDebugCounters {
    UiDebugCounters NOOP = new UiDebugCounters() {
    };

    default void beginFrame(long frameIndex) {
    }

    default void recordDrawCommands(int count) {
    }

    default void recordBatches(int count) {
    }

    /** Записывает одну команду для анализа распределения draw list. */
    default void recordDrawCommand(DrawCommandType type) {
    }

    /** Записывает batch и оценку работы, которую backend выполнит для него. */
    default void recordBatch(DrawCommandType type, int commandCount, boolean barrier,
                             int estimatedDrawCalls, int estimatedVertices, int estimatedIndices) {
    }

    /** Записывает дополнительные свойства команд, полезные для будущих оптимизаций. */
    default void recordRenderFeatures(int textGlyphs, int textCommands, int textureCommands,
                                      int textureSwitches, int meshVertices, int pathElements,
                                      int transformCommands, int clipCommands) {
    }

    /** Записывает объём данных, которые shader-команды передают в backend. */
    default void recordShaderFeatures(int uniformCount, int textureCount) {
    }

    /** Записывает фактическую работу SDF-рендера за завершённый backend-кадр. */
    default void recordSdfRuntime(long passes, long commands, long drawCalls,
                                  long uniformUploads, long flushes, long nanos) {
    }

    default void recordFrameCpuMillis(float millis) {
    }

    default void recordFrameTotalMillis(float millis) {
    }

    default void recordFrameGpuMillis(float millis) {
    }

    default void recordTextureCacheHit() {
    }

    default void recordTextureCacheMiss(String reason) {
    }

    default void recordTextureRender() {
    }

    default UiDebugSnapshot snapshot() {
        return UiDebugSnapshot.EMPTY;
    }

    default UiRenderSnapshot renderSnapshot() {
        return UiRenderSnapshot.EMPTY;
    }
}
