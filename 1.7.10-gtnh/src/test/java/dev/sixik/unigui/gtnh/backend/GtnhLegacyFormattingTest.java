package dev.sixik.unigui.gtnh.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtnhLegacyFormattingTest {
    private static String prefix(GtnhLegacyFormatting.State state) {
        StringBuilder out = new StringBuilder();
        state.appendPrefix(out);
        return out.toString();
    }

    @Test
    void signNeedsACharacterAfterIt() {
        assertTrue(GtnhLegacyFormatting.isCode("\u00a7a", 0));
        assertFalse(GtnhLegacyFormatting.isCode("ab\u00a7", 2), "a trailing sign is an ordinary character");
        assertFalse(GtnhLegacyFormatting.isCode("abc", 1));
    }

    @Test
    void stripRemovesCodesButKeepsATrailingSign() {
        assertEquals("red text", GtnhLegacyFormatting.strip("\u00a7cred \u00a7ltext"));
        assertEquals("a\u00a7", GtnhLegacyFormatting.strip("a\u00a7"));
        assertEquals("", GtnhLegacyFormatting.strip("\u00a7a\u00a7b"));
        String plain = "no codes";
        assertSame(plain, GtnhLegacyFormatting.strip(plain));
    }

    @Test
    void paletteMatchesVanilla() {
        assertEquals(0x000000, GtnhLegacyFormatting.rgb(0));
        assertEquals(0xFFAA00, GtnhLegacyFormatting.rgb(6));
        assertEquals(0xFF5555, GtnhLegacyFormatting.rgb(12));
        assertEquals(0xFFFFFF, GtnhLegacyFormatting.rgb(15));
    }

    @Test
    void colourClearsStylesAndStylesAccumulate() {
        GtnhLegacyFormatting.State state = new GtnhLegacyFormatting.State();
        state.apply('l');
        state.apply('N');
        assertEquals("\u00a7l\u00a7n", prefix(state));
        state.apply('c');
        assertEquals("\u00a7c", prefix(state));
        state.apply('o');
        assertEquals("\u00a7c\u00a7o", prefix(state));
        assertEquals(12, state.color());
    }

    @Test
    void resetClearsEverythingAndUnknownCodeIsWhite() {
        GtnhLegacyFormatting.State state = new GtnhLegacyFormatting.State();
        state.apply('a');
        state.apply('k');
        state.apply('R');
        assertTrue(state.isDefault());
        assertEquals("", prefix(state));
        state.apply('z');
        assertEquals(15, state.color());
        assertEquals("\u00a7f", prefix(state));
    }
}