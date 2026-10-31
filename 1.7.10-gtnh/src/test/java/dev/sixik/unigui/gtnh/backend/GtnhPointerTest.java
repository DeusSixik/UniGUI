package dev.sixik.unigui.gtnh.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtnhPointerTest {
    private static final float EPS = 1.0e-4f;

    @Test
    void firstUpdate_isAlwaysAChange() {
        assertTrue(new GtnhPointer().update("screen", 0.0f, 0.0f));
    }

    @Test
    void samePositionAndOwner_isNotReportedAgain() {
        GtnhPointer pointer = new GtnhPointer();
        Object screen = new Object();
        assertTrue(pointer.update(screen, 10.5f, 20.25f));
        assertFalse(pointer.update(screen, 10.5f, 20.25f));
        assertFalse(pointer.update(screen, 10.5f, 20.25f));
    }

    @Test
    void movement_isReportedWithSubPixelPrecision() {
        GtnhPointer pointer = new GtnhPointer();
        Object screen = new Object();
        pointer.update(screen, 10.0f, 10.0f);
        assertTrue(pointer.update(screen, 10.5f, 10.0f));
        assertEquals(10.5f, pointer.x(), EPS);
        assertTrue(pointer.update(screen, 10.5f, 10.125f));
        assertEquals(10.125f, pointer.y(), EPS);
    }

    @Test
    void newOwnerAtTheSamePosition_stillGetsItsInitialHover() {
        GtnhPointer pointer = new GtnhPointer();
        pointer.update("first", 5.0f, 5.0f);
        assertTrue(pointer.update("second", 5.0f, 5.0f));
        assertFalse(pointer.update("second", 5.0f, 5.0f));
    }

    @Test
    void guiCoordinates_matchTheVanillaDrawScreenFormula() {
        // 1920x1080 window at GUI scale 2: 960x540 GUI units.
        assertEquals(0.0f, GtnhPointer.guiX(0, 960, 1920), EPS);
        assertEquals(480.0f, GtnhPointer.guiX(960, 960, 1920), EPS);
        assertEquals(0.5f, GtnhPointer.guiX(1, 960, 1920), EPS);
        // LWJGL y grows upwards: the bottom row is the last GUI row.
        assertEquals(539.0f, GtnhPointer.guiY(0, 540, 1080), EPS);
        assertEquals(269.0f, GtnhPointer.guiY(540, 540, 1080), EPS);
        assertEquals(-1.0f, GtnhPointer.guiY(1080, 540, 1080), EPS);
    }

    @Test
    void guiCoordinates_survivesAZeroSizedDisplay() {
        assertEquals(0.0f, GtnhPointer.guiX(0, 100, 0), EPS);
        assertTrue(Float.isFinite(GtnhPointer.guiY(5, 100, 0)));
    }
}
