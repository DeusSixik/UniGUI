package dev.sixik.unigui.gtnh.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtnhWheelTest {
    private static final long T0 = 1_000_000_000L;

    @Test
    void nothingMarked_neverSwallows() {
        GtnhWheel wheel = new GtnhWheel();
        assertFalse(wheel.swallow(120, T0));
        assertFalse(wheel.swallow(-120, T0));
    }

    @Test
    void deliveredNotch_isSwallowedExactlyOnce() {
        GtnhWheel wheel = new GtnhWheel();
        wheel.markConsumed(120, T0);
        assertTrue(wheel.swallow(120, T0 + 50_000_000L));
        assertFalse(wheel.swallow(120, T0 + 60_000_000L), "a second event is a new notch and must reach the UI");
    }

    @Test
    void severalNotchesInOneFrame_areSwallowedNotchByNotch() {
        GtnhWheel wheel = new GtnhWheel();
        wheel.markConsumed(360, T0);
        assertTrue(wheel.swallow(120, T0));
        assertTrue(wheel.swallow(120, T0));
        assertTrue(wheel.swallow(120, T0));
        assertFalse(wheel.swallow(120, T0));
    }

    @Test
    void marksFromSeveralFramesAccumulate() {
        GtnhWheel wheel = new GtnhWheel();
        wheel.markConsumed(120, T0);
        wheel.markConsumed(120, T0 + 16_000_000L);
        assertTrue(wheel.swallow(240, T0 + 50_000_000L), "one tick event carrying two notches");
        assertFalse(wheel.swallow(120, T0 + 51_000_000L));
    }

    @Test
    void oppositeDirection_resetsInsteadOfSwallowing() {
        GtnhWheel wheel = new GtnhWheel();
        wheel.markConsumed(120, T0);
        assertFalse(wheel.swallow(-120, T0), "reversed wheel is a different input");
        assertFalse(wheel.swallow(120, T0), "the stale mark was discarded");
    }

    @Test
    void unmatchedMark_expires() {
        GtnhWheel wheel = new GtnhWheel();
        wheel.markConsumed(120, T0);
        assertFalse(wheel.swallow(120, T0 + GtnhWheel.EXPIRY_NANOS + 1L),
                "events of a closed screen must not eat wheel on the next screen");
    }

    @Test
    void zeroUnits_areIgnored() {
        GtnhWheel wheel = new GtnhWheel();
        wheel.markConsumed(0, T0);
        assertFalse(wheel.swallow(120, T0));
        assertFalse(wheel.swallow(0, T0));
    }

    @Test
    void eventLargerThanMark_isSwallowedAndClearsTheMark() {
        GtnhWheel wheel = new GtnhWheel();
        wheel.markConsumed(120, T0);
        assertTrue(wheel.swallow(240, T0));
        assertFalse(wheel.swallow(120, T0));
    }

    @Test
    void notches_countsWholeNotchesAsReportedByLwjgl3ify() {
        org.junit.jupiter.api.Assertions.assertEquals(1.0f, GtnhWheel.notches(1));
        org.junit.jupiter.api.Assertions.assertEquals(-3.0f, GtnhWheel.notches(-3));
        org.junit.jupiter.api.Assertions.assertEquals(0.0f, GtnhWheel.notches(0));
    }

    @Test
    void notches_stillUnderstandsTheLwjgl2Convention() {
        org.junit.jupiter.api.Assertions.assertEquals(1.0f, GtnhWheel.notches(120));
        org.junit.jupiter.api.Assertions.assertEquals(-2.0f, GtnhWheel.notches(-240));
    }
}