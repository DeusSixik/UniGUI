package dev.sixik.unigui.api.editor;

import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;

import java.util.Objects;

/** Keyboard shortcut binding for an {@link EditorCommand}. */
public final class KeyBinding {
    private static final int KNOWN_MODIFIERS = KeyModifiers.SHIFT
            | KeyModifiers.CONTROL
            | KeyModifiers.ALT
            | KeyModifiers.SUPER;

    private final int keyCode;
    private final int modifiers;
    private final String shortcutText;

    public KeyBinding(int keyCode, int modifiers) {
        this(keyCode, modifiers, null);
    }

    public KeyBinding(int keyCode, int modifiers, String shortcutText) {
        this.keyCode = normalizeKeyCode(keyCode);
        this.modifiers = normalizeModifiers(modifiers);
        String normalizedText = shortcutText == null ? "" : shortcutText.trim();
        this.shortcutText = normalizedText.isEmpty()
                ? buildShortcutText(this.keyCode, this.modifiers)
                : normalizedText;
    }

    public static KeyBinding of(int keyCode, int modifiers) {
        return new KeyBinding(keyCode, modifiers);
    }

    public static KeyBinding of(int keyCode, int modifiers, String shortcutText) {
        return new KeyBinding(keyCode, modifiers, shortcutText);
    }

    public static KeyBinding ctrl(int keyCode) {
        return of(keyCode, KeyModifiers.CONTROL);
    }

    public int keyCode() {
        return keyCode;
    }

    public int modifiers() {
        return modifiers;
    }

    public String shortcutText() {
        return shortcutText;
    }

    public boolean matches(int keyCode, int modifiers) {
        return this.keyCode == normalizeKeyCode(keyCode)
                && this.modifiers == normalizeModifiers(modifiers);
    }

    private static int normalizeKeyCode(int keyCode) {
        if (keyCode >= 'a' && keyCode <= 'z') return Character.toUpperCase(keyCode);
        return keyCode;
    }

    private static int normalizeModifiers(int modifiers) {
        return modifiers & KNOWN_MODIFIERS;
    }

    private static String buildShortcutText(int keyCode, int modifiers) {
        StringBuilder builder = new StringBuilder();
        appendModifier(builder, modifiers, KeyModifiers.CONTROL, "Ctrl");
        appendModifier(builder, modifiers, KeyModifiers.SHIFT, "Shift");
        appendModifier(builder, modifiers, KeyModifiers.ALT, "Alt");
        appendModifier(builder, modifiers, KeyModifiers.SUPER, "Super");
        if (!builder.isEmpty()) builder.append('+');
        builder.append(keyText(keyCode));
        return builder.toString();
    }

    private static void appendModifier(StringBuilder builder, int modifiers, int mask, String label) {
        if (!KeyModifiers.has(modifiers, mask)) return;
        if (!builder.isEmpty()) builder.append('+');
        builder.append(label);
    }

    private static String keyText(int keyCode) {
        return switch (keyCode) {
            case KeyCodes.ENTER, KeyCodes.KEYPAD_ENTER -> "Enter";
            case KeyCodes.TAB -> "Tab";
            case KeyCodes.ESCAPE -> "Esc";
            case KeyCodes.SPACE -> "Space";
            case KeyCodes.BACKSPACE -> "Backspace";
            case KeyCodes.DELETE -> "Delete";
            case KeyCodes.LEFT -> "Left";
            case KeyCodes.RIGHT -> "Right";
            case KeyCodes.UP -> "Up";
            case KeyCodes.DOWN -> "Down";
            case KeyCodes.PAGE_UP -> "Page Up";
            case KeyCodes.PAGE_DOWN -> "Page Down";
            case KeyCodes.HOME -> "Home";
            case KeyCodes.END -> "End";
            case KeyCodes.F2 -> "F2";
            default -> printableKeyText(keyCode);
        };
    }

    private static String printableKeyText(int keyCode) {
        if (keyCode >= 'A' && keyCode <= 'Z') return Character.toString((char) keyCode);
        if (keyCode >= '0' && keyCode <= '9') return Character.toString((char) keyCode);
        return "Key " + keyCode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KeyBinding binding)) return false;
        return keyCode == binding.keyCode && modifiers == binding.modifiers;
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyCode, modifiers);
    }

    @Override
    public String toString() {
        return shortcutText;
    }
}
