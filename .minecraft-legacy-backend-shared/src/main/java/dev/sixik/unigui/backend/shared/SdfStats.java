package dev.sixik.unigui.backend.shared;

import dev.sixik.unigui.api.debug.UiDebugCounters;

/** Process-wide counters for submitted SDF passes. */
public final class SdfStats {
    private static boolean enabled;
    private static long passes;
    private static long commands;
    private static long drawCalls;
    private static long uniformUploads;
    private static long flushes;
    private static long nanos;

    private SdfStats() { }
    public static void enabled(boolean on) { enabled = on; }
    public static boolean enabled() { return enabled; }
    public static long start() { return enabled ? System.nanoTime() : 0L; }

    public static void record(long startNanos, int commandCount, int draws, int uniforms, boolean flushed) {
        if (!enabled) return;
        passes++;
        commands += commandCount;
        drawCalls += draws;
        uniformUploads += uniforms;
        if (flushed) flushes++;
        if (startNanos != 0L) nanos += System.nanoTime() - startNanos;
    }

    public static void drainInto(UiDebugCounters counters) {
        if (counters != null) counters.recordSdfRuntime(passes, commands, drawCalls, uniformUploads, flushes, nanos);
        passes = 0L;
        commands = 0L;
        drawCalls = 0L;
        uniformUploads = 0L;
        flushes = 0L;
        nanos = 0L;
    }
}
