package dev.sixik.unigui.gtnh.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GtnhDisplayTest {
    @Test
    void usableRatesAreRoundedToWholeHertz() {
        assertEquals(144, GtnhDisplay.clampHz(143.98f));
        assertEquals(60, GtnhDisplay.clampHz(59.94f));
        assertEquals(165, GtnhDisplay.clampHz(165.0f));
    }

    @Test
    void unknownOrBrokenRatesFallBackToSixty() {
        assertEquals(60, GtnhDisplay.clampHz(0.0f));
        assertEquals(60, GtnhDisplay.clampHz(-1.0f));
        assertEquals(60, GtnhDisplay.clampHz(Float.NaN));
        assertEquals(60, GtnhDisplay.clampHz(Float.POSITIVE_INFINITY));
    }
}