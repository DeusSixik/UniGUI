package dev.sixik.unigui.gtnh.backend;

import org.junit.jupiter.api.Test;
import org.lwjglx.input.Keyboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GtnhKeyCodesTest {
    @Test
    void toCore_knownLetters_mapsLwjglCodesToGlfwCodes() {
        assertEquals('A', GtnhKeyCodes.toCore(Keyboard.KEY_A));
        assertEquals('B', GtnhKeyCodes.toCore(Keyboard.KEY_B));
        assertEquals('P', GtnhKeyCodes.toCore(Keyboard.KEY_P));
        assertEquals('Z', GtnhKeyCodes.toCore(Keyboard.KEY_Z));
    }

    @Test
    void toCore_navigationKey_mapsToCoreKey() {
        assertEquals(258, GtnhKeyCodes.toCore(Keyboard.KEY_TAB));
        assertEquals(262, GtnhKeyCodes.toCore(Keyboard.KEY_RIGHT));
    }

    @Test
    void toCore_digitAndModifier_preservesGlfwCompatibleCodes() {
        assertEquals('7', GtnhKeyCodes.toCore(Keyboard.KEY_7));
        assertEquals(340, GtnhKeyCodes.toCore(Keyboard.KEY_LSHIFT));
        assertEquals(345, GtnhKeyCodes.toCore(Keyboard.KEY_RCONTROL));
    }

    @Test
    void toCore_unsupportedKey_returnsMinusOne() {
        assertEquals(-1, GtnhKeyCodes.toCore(Keyboard.KEY_PAUSE));
    }
}
