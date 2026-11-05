package dev.sixik.unigui.gtnh.backend;

import org.lwjglx.input.Mouse;

/**
 * Delivers the mouse wheel once per frame and drops the same wheel events when they arrive again on the game tick.
 *
 * <p>Like pointer motion (see {@link GtnhPointer}), wheel events reach {@code handleMouseInput()} only 20 times per
 * second, so a UI that scrolls from them moves in bursts. {@link Mouse#getDWheel()} accumulates the wheel since its
 * last call independently of the event queue, so the frame can take it right away. The event queue still holds the
 * same notches, which would scroll a second time; {@link #markConsumed} remembers what the frame delivered and
 * {@link #swallow} cancels exactly that amount when the events show up.</p>
 *
 * <p>Wheel the UI did not accept is not marked, so vanilla and other mods still receive it through the event as
 * before. If {@code getDWheel} returns nothing, nothing is ever marked and the tick path keeps working. Render
 * thread only.</p>
 */
final class GtnhWheel {
    /**
     * Wheel units of one notch in LWJGL 2 (the original library reports 120 per notch).
     */
    static final int LEGACY_UNITS_PER_NOTCH = 120;

    /**
     * A marked amount older than this is dropped: its events never arrived (screen closed, tick stall).
     */
    static final long EXPIRY_NANOS = 2_000_000_000L;

    private int pending;
    private long markedAt;

    /**
     * @return wheel units accumulated since the previous call, positive is up; see {@link #notches(int)}
     */
    int poll() {
        return Mouse.getDWheel();
    }

    /**
     * Remembers wheel the UI handled in the frame.
     *
     * @param units    signed wheel units that were delivered to the UI
     * @param nowNanos current {@code System.nanoTime()}
     */
    void markConsumed(int units, long nowNanos) {
        if (units == 0) return;
        if (isStale(nowNanos) || (pending != 0 && Integer.signum(pending) != Integer.signum(units))) pending = 0;
        pending += units;
        markedAt = nowNanos;
    }

    /**
     * Decides whether a queued wheel event was already delivered by the frame.
     *
     * @param eventUnits signed {@code Mouse.getEventDWheel()}
     * @param nowNanos   current {@code System.nanoTime()}
     * @return {@code true} if the event must be ignored
     */
    boolean swallow(int eventUnits, long nowNanos) {
        if (eventUnits == 0 || pending == 0) return false;
        if (isStale(nowNanos) || Integer.signum(pending) != Integer.signum(eventUnits)) {
            pending = 0;
            return false;
        }
        pending = Math.abs(eventUnits) >= Math.abs(pending) ? 0 : pending - eventUnits;
        return true;
    }

    /**
     * Converts raw wheel units into notches, the unit scrolling widgets expect.
     *
     * <p>LWJGL 2 reported 120 units per notch, but the compatibility layer of lwjgl3ify (which supplies
     * {@code Mouse.getDWheel()} here) counts whole notches: one notch is {@code 1} (scaled by its scroll speed
     * setting). Dividing that by 120 made lists move a hundredth of the intended distance. A value of at least 120 in
     * magnitude can only come from the old convention and is still divided.</p>
     *
     * @param units signed units from {@code Mouse.getDWheel()} / {@code Mouse.getEventDWheel()}
     * @return signed notches, positive is up
     */
    static float notches(int units) {
        return Math.abs(units) >= LEGACY_UNITS_PER_NOTCH ? units / (float) LEGACY_UNITS_PER_NOTCH : units;
    }

    private boolean isStale(long nowNanos) {
        return pending != 0 && nowNanos - markedAt > EXPIRY_NANOS;
    }
}
