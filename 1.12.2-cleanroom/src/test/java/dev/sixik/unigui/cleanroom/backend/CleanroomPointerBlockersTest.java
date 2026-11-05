package dev.sixik.unigui.cleanroom.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanroomPointerBlockersTest {
    @Test
    void blocksOnlyInsideAnyRegisteredBlocker() throws Exception {
        CleanroomPointerBlockers<String> blockers = new CleanroomPointerBlockers<>();
        assertFalse(blockers.blocks("screen", 10, 10));
        AutoCloseable handle = blockers.add((screen, x, y) -> x >= 5 && x < 20);
        assertTrue(blockers.blocks("screen", 10, 10));
        assertFalse(blockers.blocks("screen", 30, 10));
        handle.close();
        assertFalse(blockers.blocks("screen", 10, 10));
    }

    @Test
    void nullScreenIsNeverBlocked() {
        CleanroomPointerBlockers<String> blockers = new CleanroomPointerBlockers<>();
        blockers.add((screen, x, y) -> true);
        assertFalse(blockers.blocks(null, 0, 0));
    }

    @Test
    void clearRemovesAll() {
        CleanroomPointerBlockers<String> blockers = new CleanroomPointerBlockers<>();
        blockers.add((screen, x, y) -> true);
        blockers.clear();
        assertFalse(blockers.blocks("screen", 0, 0));
    }
}