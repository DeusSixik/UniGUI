package dev.sixik.unigui.editor;

import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EditorShortcutTest {
    @Test
    void chordsMatchOnlyTheExactModifiers() {
        EditorShortcut save = EditorShortcut.parse("Ctrl+S");
        assertTrue(save.matches(KeyCodes.S, KeyModifiers.CONTROL));
        assertFalse(save.matches(KeyCodes.S, 0));
        assertFalse(save.matches(KeyCodes.S, KeyModifiers.CONTROL | KeyModifiers.SHIFT), "extra modifier");
        assertFalse(save.matches(KeyCodes.A, KeyModifiers.CONTROL));

        EditorShortcut redo = EditorShortcut.parse("ctrl + shift + z");
        assertTrue(redo.matches(KeyCodes.Z, KeyModifiers.CONTROL | KeyModifiers.SHIFT));
        assertFalse(redo.matches(KeyCodes.Z, KeyModifiers.CONTROL));
    }

    @Test
    void namedAndFunctionKeysAreRecognised() {
        assertTrue(EditorShortcut.parse("Delete").matches(KeyCodes.DELETE, 0));
        assertTrue(EditorShortcut.parse("Del").matches(KeyCodes.DELETE, 0));
        assertTrue(EditorShortcut.parse("Alt+Enter").matches(KeyCodes.ENTER, KeyModifiers.ALT));
        assertTrue(EditorShortcut.parse("F5").matches(294, 0), "GLFW_KEY_F5");
        assertTrue(EditorShortcut.parse("Ctrl+1").matches('1', KeyModifiers.CONTROL));
        assertTrue(EditorShortcut.parse("Cmd+Up").matches(KeyCodes.UP, KeyModifiers.SUPER));
    }

    @Test
    void mistakesAreRejectedWithTheOffendingText() {
        assertThrows(IllegalArgumentException.class, () -> EditorShortcut.parse(""));
        assertThrows(IllegalArgumentException.class, () -> EditorShortcut.parse("Ctrl"));
        assertThrows(IllegalArgumentException.class, () -> EditorShortcut.parse("Ctrl+S+D"));
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> EditorShortcut.parse("Ctrl+Banana"));
        assertTrue(unknown.getMessage().contains("Banana".toUpperCase()), unknown.getMessage());
        assertThrows(IllegalArgumentException.class, () -> EditorShortcut.parse("F13"));
        assertThrows(IllegalArgumentException.class, () -> EditorShortcut.parse("Ctrl++S"));
    }

    @Test
    void aBadShortcutOnACommandFailsWhenTheCommandIsDescribed() {
        assertThrows(IllegalArgumentException.class, () -> EditorCommand.builder("x").shortcut("Ctrl+Nope"));
        assertEquals("Ctrl+S", EditorCommand.builder("x").shortcut(" Ctrl+S ").build().shortcut());
        assertEquals(null, EditorCommand.builder("y").build().binding());
    }

    @Test
    void textFieldsKeepTheirOwnEditingKeys() {
        assertFalse(EditorShortcut.parse("Delete").worksInTextFields(), "Delete deletes a character there");
        assertFalse(EditorShortcut.parse("Ctrl+Z").worksInTextFields(), "the field has its own undo");
        assertFalse(EditorShortcut.parse("Ctrl+C").worksInTextFields());
        assertTrue(EditorShortcut.parse("Ctrl+S").worksInTextFields(), "saving works while typing");
        assertTrue(EditorShortcut.parse("Ctrl+D").worksInTextFields());
        assertFalse(EditorShortcut.parse("F2").worksInTextFields(), "no modifier means typing");
    }
}
