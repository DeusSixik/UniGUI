package dev.sixik.unigui.api.debug;

public final class DebugFlags {
    public static final int NONE = 0;
    public static final int WIDGET_BOUNDS = 1;
    public static final int DIRTY_FLAGS = 1 << 1;
    public static final int DRAW_COMMANDS = 1 << 2;
    public static final int BATCHES = 1 << 3;
    public static final int OVERDRAW = 1 << 4;
    public static final int FOCUS_AND_HOVER = 1 << 5;
    public static final int PROFILER_OVERLAY = 1 << 6;
    public static final int CACHED_SUBTREE = 1 << 7;
    public static final int ALL = WIDGET_BOUNDS | DIRTY_FLAGS | DRAW_COMMANDS | BATCHES | OVERDRAW | FOCUS_AND_HOVER | PROFILER_OVERLAY | CACHED_SUBTREE;

    private DebugFlags() {
    }

    public static boolean has(int flags, int mask) {
        return (flags & mask) == mask;
    }
}
