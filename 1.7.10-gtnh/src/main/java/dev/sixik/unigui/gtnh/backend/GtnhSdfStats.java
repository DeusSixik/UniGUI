package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.debug.UiDebugCounters;
import dev.sixik.unigui.backend.shared.SdfStats;

/** GTNH compatibility facade for shared SDF counters. */
final class GtnhSdfStats {
    private GtnhSdfStats() { }
    static void enabled(boolean on) { SdfStats.enabled(on); }
    static boolean enabled() { return SdfStats.enabled(); }
    static long start() { return SdfStats.start(); }
    static void record(long start, int commands, int draws, int uniforms, boolean flushed) {
        SdfStats.record(start, commands, draws, uniforms, flushed);
    }
    static void drainInto(UiDebugCounters counters) { SdfStats.drainInto(counters); }
}
