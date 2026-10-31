package dev.sixik.unigui.gtnh.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtnhItemStacksTest {
    @Test
    void nullIsTheEmptyStack() {
        assertNull(GtnhItemStacks.EMPTY);
        assertTrue(GtnhItemStacks.isEmpty(null));
        assertNull(GtnhItemStacks.copyOrEmpty(null));
    }
}