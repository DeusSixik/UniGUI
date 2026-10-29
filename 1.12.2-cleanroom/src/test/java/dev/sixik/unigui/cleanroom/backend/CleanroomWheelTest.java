package dev.sixik.unigui.cleanroom.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanroomWheelTest {
    private static final long T0 = 1_000_000_000L;

    @Test
    void nothingMarked_neverSwallows() {
        CleanroomWheel wheel = new CleanroomWheel();
        assertFalse(wheel.swallow(120, T0));
        assertFalse(wheel.swallow(-120, T0));
    }

    @Test
    void deliveredNotch_isSwallowedExactlyOnce() {
        CleanroomWheel wheel = new CleanroomWheel();
        wheel.markConsumed(120, T0);
        assertTrue(wheel.swallow(120, T0 + 50_000_000L));
        assertFalse(wheel.swallow(120, T0 + 60_000_000L), "a second event is a new notch and must reach the UI");
    }

    @Test
    void severalNotchesInOneFrame_areSwallowedNotchByNotch() {
        CleanroomWheel wheel = new CleanroomWheel();
        wheel.markConsumed(360, T0);
        assertTrue(wheel.swallow(120, T0));
        assertTrue(wheel.swallow(120, T0));
        assertTrue(wheel.swallow(120, T0));
        assertFalse(wheel.swallow(120, T0));
    }

    @Test
    void marksFromSeveralFramesAccumulate() {
        CleanroomWheel wheel = new CleanroomWheel();
        wheel.markConsumed(120, T0);
        wheel.markConsumed(120, T0 + 16_000_000L);
        assertTrue(wheel.swallow(240, T0 + 50_000_000L), "one tick event carrying two notches");
        assertFalse(wheel.swallow(120, T0 + 51_000_000L));
    }

    @Test
    void oppositeDirection_resetsInsteadOfSwallowing() {
        CleanroomWheel wheel = new CleanroomWheel();
        wheel.markConsumed(120, T0);
        assertFalse(wheel.swallow(-120, T0), "reversed wheel is a different input");
        assertFalse(wheel.swallow(120, T0), "the stale mark was discarded");
    }

    @Test
    void unmatchedMark_expires() {
        CleanroomWheel wheel = new CleanroomWheel();
        wheel.markConsumed(120, T0);
        assertFalse(wheel.swallow(120, T0 + CleanroomWheel.EXPIRY_NANOS + 1L),
                "events of a closed screen must not eat wheel on the next screen");
    }

    @Test
    void zeroUnits_areIgnored() {
        CleanroomWheel wheel = new CleanroomWheel();
        wheel.markConsumed(0, T0);
        assertFalse(wheel.swallow(120, T0));
        assertFalse(wheel.swallow(0, T0));
    }

    @Test
    void eventLargerThanMark_isSwallowedAndClearsTheMark() {
        CleanroomWheel wheel = new CleanroomWheel();
        wheel.markConsumed(120, T0);
        assertTrue(wheel.swallow(240, T0));
        assertFalse(wheel.swallow(120, T0));
    }
}
