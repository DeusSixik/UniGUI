package dev.sixik.unigui.cleanroom.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CleanroomItemIconsTest {
    @Test
    void iconPixelsRoundUpToWholeSixteens() {
        assertEquals(16, CleanroomItemIcons.iconPixels(16.0f, false));
        assertEquals(32, CleanroomItemIcons.iconPixels(17.0f, false));
        assertEquals(48, CleanroomItemIcons.iconPixels(33.0f, false));
    }

    @Test
    void blockLikeModelsGetAtLeastThirtyTwo() {
        assertEquals(16, CleanroomItemIcons.iconPixels(10.0f, false));
        assertEquals(32, CleanroomItemIcons.iconPixels(10.0f, true));
        assertEquals(64, CleanroomItemIcons.iconPixels(64.0f, true));
    }

    @Test
    void iconPixelsAreBoundedAndTolerateBadInput() {
        assertEquals(256, CleanroomItemIcons.iconPixels(5000.0f, false));
        assertEquals(16, CleanroomItemIcons.iconPixels(0.0f, false));
        assertEquals(16, CleanroomItemIcons.iconPixels(-4.0f, false));
        assertEquals(16, CleanroomItemIcons.iconPixels(Float.NaN, false));
        assertEquals(32, CleanroomItemIcons.iconPixels(Float.POSITIVE_INFINITY, true));
    }
}