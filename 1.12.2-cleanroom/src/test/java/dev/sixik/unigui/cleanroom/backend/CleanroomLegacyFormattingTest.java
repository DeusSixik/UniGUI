package dev.sixik.unigui.cleanroom.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanroomLegacyFormattingTest {
    private static String prefix(CleanroomLegacyFormatting.State state) {
        StringBuilder out = new StringBuilder();
        state.appendPrefix(out);
        return out.toString();
    }

    @Test
    void signNeedsACharacterAfterIt() {
        assertTrue(CleanroomLegacyFormatting.isCode("\u00a7a", 0));
        assertFalse(CleanroomLegacyFormatting.isCode("ab\u00a7", 2), "a trailing sign is an ordinary character");
        assertFalse(CleanroomLegacyFormatting.isCode("abc", 1));
    }

    @Test
    void stripRemovesCodesButKeepsATrailingSign() {
        assertEquals("red text", CleanroomLegacyFormatting.strip("\u00a7cred \u00a7ltext"));
        assertEquals("a\u00a7", CleanroomLegacyFormatting.strip("a\u00a7"));
        assertEquals("", CleanroomLegacyFormatting.strip("\u00a7a\u00a7b"));
        String plain = "no codes";
        assertSame(plain, CleanroomLegacyFormatting.strip(plain));
    }

    @Test
    void paletteMatchesVanilla() {
        assertEquals(0x000000, CleanroomLegacyFormatting.rgb(0));
        assertEquals(0xFFAA00, CleanroomLegacyFormatting.rgb(6));
        assertEquals(0xFF5555, CleanroomLegacyFormatting.rgb(12));
        assertEquals(0xFFFFFF, CleanroomLegacyFormatting.rgb(15));
    }

    @Test
    void colourClearsStylesAndStylesAccumulate() {
        CleanroomLegacyFormatting.State state = new CleanroomLegacyFormatting.State();
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
        CleanroomLegacyFormatting.State state = new CleanroomLegacyFormatting.State();
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