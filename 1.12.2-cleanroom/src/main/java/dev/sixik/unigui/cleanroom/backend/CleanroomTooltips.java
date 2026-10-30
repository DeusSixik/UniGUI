package dev.sixik.unigui.cleanroom.backend;

import java.util.ArrayList;
import java.util.List;

/**
 * Postpones vanilla tooltips of a screen until the overlay layers are drawn, so tooltips stay on top.
 *
 * <p>Mirrors {@code DeferredMinecraftTooltips} of the 1.20 backend. The screen draws its tooltips from inside
 * {@code drawScreen}; while deferral is active the draw calls are queued as {@link Runnable}s and replayed, in the
 * original order, after the overlay. The replay runs with deferral switched off, so the calls pass straight
 * through.</p>
 *
 * <p>Client-thread only, like all GUI drawing.</p>
 */
public final class CleanroomTooltips {
    private static final List<Runnable> QUEUE = new ArrayList<>();
    private static boolean deferring;

    private CleanroomTooltips() {
    }

    /** Starts queueing tooltip draws; a previous unfinished deferral is discarded. */
    public static void begin() {
        QUEUE.clear();
        deferring = true;
    }

    /** Stops queueing; queued draws stay until {@link #flush()}. */
    public static void end() {
        deferring = false;
    }

    /** @return {@code true} while tooltip draws are being queued */
    public static boolean deferring() {
        return deferring;
    }

    /**
     * Queues a tooltip draw if deferral is active.
     *
     * @param draw the original draw call
     * @return {@code true} if the draw was queued and the caller must skip it
     */
    public static boolean defer(Runnable draw) {
        if (!deferring) return false;
        QUEUE.add(draw);
        return true;
    }

    /** Runs the queued draws in order; deferral must already be off. */
    public static void flush() {
        if (QUEUE.isEmpty()) return;
        Runnable[] pending = QUEUE.toArray(new Runnable[0]);
        QUEUE.clear();
        for (Runnable draw : pending) draw.run();
    }

    /** Drops queued draws without running them (screen failed or closed mid-frame). */
    public static void discard() {
        deferring = false;
        QUEUE.clear();
    }
}
