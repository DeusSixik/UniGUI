package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.backend.shared.TooltipQueue;

/** Cleanroom compatibility facade for the shared tooltip queue. */
public final class CleanroomTooltips {
    private CleanroomTooltips() { }
    public static void begin() { TooltipQueue.begin(); }
    public static void end() { TooltipQueue.end(); }
    public static boolean deferring() { return TooltipQueue.deferring(); }
    public static boolean defer(Runnable draw) { return TooltipQueue.defer(draw); }
    public static void flush() { TooltipQueue.flush(); }
    public static void discard() { TooltipQueue.discard(); }
}
