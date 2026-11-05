package dev.sixik.unigui.gtnh.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GtnhItemIconsTest {
    @Test
    void iconPixelsRoundUpToWholeSixteens() {
        assertEquals(16, GtnhItemIcons.iconPixels(16.0f, false));
        assertEquals(32, GtnhItemIcons.iconPixels(17.0f, false));
        assertEquals(48, GtnhItemIcons.iconPixels(33.0f, false));
    }

    @Test
    void blockLikeModelsGetAtLeastThirtyTwo() {
        assertEquals(16, GtnhItemIcons.iconPixels(10.0f, false));
        assertEquals(32, GtnhItemIcons.iconPixels(10.0f, true));
        assertEquals(64, GtnhItemIcons.iconPixels(64.0f, true));
    }

    @Test
    void iconPixelsAreBoundedAndTolerateBadInput() {
        assertEquals(256, GtnhItemIcons.iconPixels(5000.0f, false));
        assertEquals(16, GtnhItemIcons.iconPixels(0.0f, false));
        assertEquals(16, GtnhItemIcons.iconPixels(-4.0f, false));
        assertEquals(16, GtnhItemIcons.iconPixels(Float.NaN, false));
        assertEquals(32, GtnhItemIcons.iconPixels(Float.POSITIVE_INFINITY, true));
    }
}