package dev.sixik.unigui.gtnh.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtnhPointerBlockersTest {
    @Test
    void blocksOnlyInsideAnyRegisteredBlocker() throws Exception {
        GtnhPointerBlockers<String> blockers = new GtnhPointerBlockers<>();
        assertFalse(blockers.blocks("screen", 10, 10));
        AutoCloseable handle = blockers.add((screen, x, y) -> x >= 5 && x < 20);
        assertTrue(blockers.blocks("screen", 10, 10));
        assertFalse(blockers.blocks("screen", 30, 10));
        handle.close();
        assertFalse(blockers.blocks("screen", 10, 10));
    }

    @Test
    void nullScreenIsNeverBlocked() {
        GtnhPointerBlockers<String> blockers = new GtnhPointerBlockers<>();
        blockers.add((screen, x, y) -> true);
        assertFalse(blockers.blocks(null, 0, 0));
    }

    @Test
    void clearRemovesAll() {
        GtnhPointerBlockers<String> blockers = new GtnhPointerBlockers<>();
        blockers.add((screen, x, y) -> true);
        blockers.clear();
        assertFalse(blockers.blocks("screen", 0, 0));
    }
}