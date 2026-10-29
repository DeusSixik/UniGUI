package dev.sixik.unigui.cleanroom.backend;

import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CleanroomKeyCodesTest {
    @Test
    void toCore_knownLetters_mapsLwjglCodesToGlfwCodes() {
        assertEquals('A', CleanroomKeyCodes.toCore(Keyboard.KEY_A));
        assertEquals('B', CleanroomKeyCodes.toCore(Keyboard.KEY_B));
        assertEquals('Z', CleanroomKeyCodes.toCore(Keyboard.KEY_Z));
    }

    @Test
    void toCore_navigationKey_mapsToCoreKey() {
        assertEquals(258, CleanroomKeyCodes.toCore(Keyboard.KEY_TAB));
        assertEquals(262, CleanroomKeyCodes.toCore(Keyboard.KEY_RIGHT));
    }

    @Test
    void toCore_digitAndModifier_preservesGlfwCompatibleCodes() {
        assertEquals('7', CleanroomKeyCodes.toCore(Keyboard.KEY_7));
        assertEquals(340, CleanroomKeyCodes.toCore(Keyboard.KEY_LSHIFT));
        assertEquals(345, CleanroomKeyCodes.toCore(Keyboard.KEY_RCONTROL));
    }

    @Test
    void toCore_unsupportedKey_returnsMinusOne() {
        assertEquals(-1, CleanroomKeyCodes.toCore(Keyboard.KEY_PAUSE));
    }
}
