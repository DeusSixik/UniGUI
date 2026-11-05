package dev.sixik.unigui.cleanroom.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CleanroomDisplayTest {
    @Test
    void usableRatesAreRoundedToWholeHertz() {
        assertEquals(144, CleanroomDisplay.clampHz(143.98f));
        assertEquals(60, CleanroomDisplay.clampHz(59.94f));
        assertEquals(165, CleanroomDisplay.clampHz(165.0f));
    }

    @Test
    void unknownOrBrokenRatesFallBackToSixty() {
        assertEquals(60, CleanroomDisplay.clampHz(0.0f));
        assertEquals(60, CleanroomDisplay.clampHz(-1.0f));
        assertEquals(60, CleanroomDisplay.clampHz(Float.NaN));
        assertEquals(60, CleanroomDisplay.clampHz(Float.POSITIVE_INFINITY));
    }
}