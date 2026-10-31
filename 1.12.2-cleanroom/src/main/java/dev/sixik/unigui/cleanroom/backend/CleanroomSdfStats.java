package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.debug.UiDebugCounters;

/**
 * What the SDF renderers (shapes and text) actually submitted, for the profiler overlay.
 *
 * <p>Collection is off unless the overlay is open ({@link #enabled(boolean)}): when off every method is a single
 * branch and no clock is read. The renderers are process-wide singletons used by every screen and layer, so the
 * counters are static and are drained by whichever screen draws its frame next. Client thread only.</p>
 */
final class CleanroomSdfStats {
    private static boolean enabled;
    private static long passes;
    private static long commands;
    private static long drawCalls;
    private static long uniformUploads;
    private static long flushes;
    private static long nanos;

    private CleanroomSdfStats() {
    }

    /**
     * @param on {@code true} while a profiler overlay wants the numbers
     */
    static void enabled(boolean on) {
        enabled = on;
    }

    static boolean enabled() {
        return enabled;
    }

    /**
     * @return a timestamp for {@link #record}, or {@code 0} when collection is off
     */
    static long start() {
        return enabled ? System.nanoTime() : 0L;
    }

    /**
     * Adds one SDF pass.
     *
     * @param startNanos   value from {@link #start()}
     * @param commandCount shapes or glyphs submitted
     * @param draws        GL draw calls issued
     * @param uniforms     uniform / matrix uploads issued
     * @param flushed      {@code true} if the pass ended with a buffer upload
     */
    static void record(long startNanos, int commandCount, int draws, int uniforms, boolean flushed) {
        if (!enabled) return;
        passes++;
        commands += commandCount;
        drawCalls += draws;
        uniformUploads += uniforms;
        if (flushed) flushes++;
        if (startNanos != 0L) nanos += System.nanoTime() - startNanos;
    }

    /**
     * Hands the numbers since the previous call to the counters and starts again from zero.
     */
    static void drainInto(UiDebugCounters counters) {
        if (counters != null) counters.recordSdfRuntime(passes, commands, drawCalls, uniformUploads, flushes, nanos);
        passes = 0L;
        commands = 0L;
        drawCalls = 0L;
        uniformUploads = 0L;
        flushes = 0L;
        nanos = 0L;
    }
}