package dev.sixik.unigui.backend.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Ванильное затенение граней, запекаемое в цвет вершин блоков.
 */
class LegacySceneQuadsTest {
    @Test
    void shade_matchesTheGameOnEachAxis() {
        int white = 0xFFFFFFFF;
        assertEquals(0xFFFFFFFF, LegacySceneQuads.shade(white, 0, 1, 0), "top");
        assertEquals(0xFF808080, LegacySceneQuads.shade(white, 0, -1, 0), "bottom 0.5");
        assertEquals(0xFFCCCCCC, LegacySceneQuads.shade(white, 0, 0, 1), "north/south 0.8");
        assertEquals(0xFF999999, LegacySceneQuads.shade(white, -1, 0, 0), "east/west 0.6");
    }

    @Test
    void shade_keepsAlphaAndTint() {
        assertEquals(0x80406000, LegacySceneQuads.shade(0x8080C000, 0, -1, 0));
    }
}
