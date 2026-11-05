package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.debug.UiDebugCounters;
import dev.sixik.unigui.api.render.DrawList;

/**
 * Connects the frame loops of screens and layers to the profiler overlay: turns the measurements on while an overlay
 * wants them and feeds the counters.
 */
final class CleanroomProfiling {
    private CleanroomProfiling() {
    }

    /**
     * @param flags debug flags of a context
     * @return {@code true} if any flag needs per-frame draw statistics
     */
    static boolean wantsStats(int flags) {
        return DebugFlags.has(flags, DebugFlags.PROFILER_OVERLAY)
                || DebugFlags.has(flags, DebugFlags.DRAW_COMMANDS)
                || DebugFlags.has(flags, DebugFlags.BATCHES);
    }

    /**
     * Call at the start of a frame, before the backend draws.
     *
     * @param context context of the screen or layer
     * @return {@code true} if statistics are being collected this frame
     */
    static boolean beginFrame(UIContext context) {
        boolean stats = context != null && wantsStats(context.debugFlags());
        boolean profiler = stats && DebugFlags.has(context.debugFlags(), DebugFlags.PROFILER_OVERLAY);
        CleanroomSdfStats.enabled(profiler);
        CleanroomGpuTimer.wanted(profiler);
        if (profiler) {
            // The figures of the previous frame: SDF passes and the GPU query result.
            CleanroomSdfStats.drainInto(context.debugCounters());
            context.debugCounters().recordFrameGpuMillis(CleanroomGpuTimer.lastMillis());
        }
        return stats;
    }

    /**
     * Call once the frame's draw list is complete and before the debug overlay is appended to it.
     *
     * @param context  context of the screen or layer
     * @param drawList the frame's commands
     */
    static void recordDrawList(UIContext context, DrawList drawList) {
        if (context == null) return;
        CleanroomDrawStats.record(context.debugCounters(), drawList);
    }

    /**
     * @return the counters of a context, or {@code null} when nothing is collected
     */
    static UiDebugCounters counters(UIContext context, boolean stats) {
        return stats && context != null ? context.debugCounters() : null;
    }
}