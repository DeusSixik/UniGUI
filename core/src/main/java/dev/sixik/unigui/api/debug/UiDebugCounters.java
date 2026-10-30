package dev.sixik.unigui.api.debug;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.render.DrawCommandType;

@AiDoc(
        summary = "Optional sink used by render and cache code to collect debug metrics.",
        purpose = "Lets custom backends report comparable frame, batching, geometry, shader, and texture-cache metrics to the built-in debug tooling.",
        useWhen = "Integrate a custom render backend or cache implementation with `UIContext.debugCounters()`.",
        criticalRules = {
                "Call `beginFrame` once before recording metrics for a new frame.",
                "Record non-negative counts for work actually submitted; snapshots sanitize invalid values but cannot repair double-counting.",
                "Use `NOOP` when collection is disabled instead of adding null checks to hot paths."
        },
        related = {"UiDebugSnapshot", "UiRenderSnapshot", "DrawCommandType"})
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

    @AiDoc(section = "Snapshots", sectionOrder = 20, order = 10,
            summary = "Returns frame-level timing, command, batch, and texture-cache metrics.")
    default UiDebugSnapshot snapshot() {
        return UiDebugSnapshot.EMPTY;
    }

    @AiDoc(section = "Snapshots", sectionOrder = 20, order = 20,
            summary = "Returns detailed draw-list and backend-work metrics for the latest frame.")
    default UiRenderSnapshot renderSnapshot() {
        return UiRenderSnapshot.EMPTY;
    }
}
