package dev.sixik.unigui.cleanroom.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanroomVanillaDrawTest {
    @Test
    void tallEntityIsLimitedByHeightAndWideOneByWidth() {
        // A player (0.6 x 1.8) in a 54 unit square: height decides.
        assertEquals(54.0f * 0.78f / 1.8f, CleanroomVanillaDraw.entityScale(0.6f, 1.8f, 54.0f), 1.0e-4f);
        // A slime-like wide entity (3 x 2): width decides.
        assertEquals(54.0f * 0.9f / 3.0f, CleanroomVanillaDraw.entityScale(3.0f, 2.0f, 54.0f), 1.0e-4f);
    }

    @Test
    void tinyAndDegenerateEntitiesDoNotExplode() {
        float chicken = CleanroomVanillaDraw.entityScale(0.4f, 0.7f, 54.0f);
        assertTrue(chicken > 0.0f && chicken < 100.0f);
        float point = CleanroomVanillaDraw.entityScale(0.0f, 0.0f, 54.0f);
        assertTrue(point >= 1.0f && point <= 54.0f * 3.0f);
        assertEquals(1.0f, CleanroomVanillaDraw.entityScale(1.0f, 1.0f, 0.0f));
    }
}