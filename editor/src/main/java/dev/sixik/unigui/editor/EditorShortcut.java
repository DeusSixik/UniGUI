package dev.sixik.unigui.editor;

import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Сочетание клавиш команды: {@code Ctrl+S}, {@code Ctrl+Shift+Z}, {@code Delete}, {@code F5}.
 *
 * <p>Коды клавиш совпадают с теми, что приходят в {@code KeyPressedEvent} (коды GLFW). Допустимы буквы, цифры,
 * {@code F1}–{@code F12}, {@code Enter}, {@code Escape}, {@code Tab}, {@code Space}, {@code Backspace},
 * {@code Delete} и стрелки. Модификаторы: {@code Ctrl}, {@code Shift}, {@code Alt}, {@code Cmd}.</p>
 */
public final class EditorShortcut {
    private static final int MASK = KeyModifiers.SHIFT | KeyModifiers.CONTROL | KeyModifiers.ALT | KeyModifiers.SUPER;
    /** Клавиши с Ctrl, которыми сами владеют поля ввода: пока в поле печатают, это его отмена и буфер обмена. */
    private static final Set<Integer> OWNED_BY_TEXT = Set.of(KeyCodes.Z, KeyCodes.Y, KeyCodes.C, KeyCodes.V,
            KeyCodes.X, KeyCodes.A);
    private static final Map<String, Integer> NAMED = Map.ofEntries(
            Map.entry("ENTER", KeyCodes.ENTER), Map.entry("RETURN", KeyCodes.ENTER),
            Map.entry("ESC", KeyCodes.ESCAPE), Map.entry("ESCAPE", KeyCodes.ESCAPE),
            Map.entry("TAB", KeyCodes.TAB), Map.entry("SPACE", KeyCodes.SPACE),
            Map.entry("BACKSPACE", KeyCodes.BACKSPACE),
            Map.entry("DELETE", KeyCodes.DELETE), Map.entry("DEL", KeyCodes.DELETE),
            Map.entry("UP", KeyCodes.UP), Map.entry("DOWN", KeyCodes.DOWN),
            Map.entry("LEFT", KeyCodes.LEFT), Map.entry("RIGHT", KeyCodes.RIGHT),
            Map.entry("HOME", KeyCodes.HOME), Map.entry("END", KeyCodes.END),
            Map.entry("PAGEUP", KeyCodes.PAGE_UP), Map.entry("PAGEDOWN", KeyCodes.PAGE_DOWN));

    private final int keyCode;
    private final int modifiers;
    private final String text;

    private EditorShortcut(int keyCode, int modifiers, String text) {
        this.keyCode = keyCode;
        this.modifiers = modifiers;
        this.text = text;
    }

    /**
     * Разбирает запись сочетания.
     *
     * @param text запись вроде {@code Ctrl+Shift+Z}; регистр не важен
     * @return сочетание
     * @throws IllegalArgumentException если запись пуста или клавиша неизвестна
     */
    public static EditorShortcut parse(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("shortcut must not be blank");
        int mods = 0;
        Integer key = null;
        for (String raw : text.split("\\+")) {
            String token = raw.trim().toUpperCase(Locale.ROOT);
            if (token.isEmpty()) throw new IllegalArgumentException("Bad shortcut '" + text + "'");
            switch (token) {
                case "CTRL", "CONTROL" -> mods |= KeyModifiers.CONTROL;
                case "SHIFT" -> mods |= KeyModifiers.SHIFT;
                case "ALT" -> mods |= KeyModifiers.ALT;
                case "CMD", "SUPER", "META" -> mods |= KeyModifiers.SUPER;
                default -> {
                    if (key != null) throw new IllegalArgumentException("Shortcut '" + text + "' has two keys");
                    key = keyOf(token, text);
                }
            }
        }
        if (key == null) throw new IllegalArgumentException("Shortcut '" + text + "' has no key");
        return new EditorShortcut(key, mods, text.trim());
    }

    private static int keyOf(String token, String whole) {
        if (token.length() == 1) {
            char c = token.charAt(0);
            if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) return c; // коды GLFW букв и цифр равны ASCII
        }
        Integer named = NAMED.get(token);
        if (named != null) return named;
        if (token.length() >= 2 && token.charAt(0) == 'F') {
            try {
                int number = Integer.parseInt(token.substring(1));
                if (number >= 1 && number <= 12) return 290 + number - 1; // GLFW_KEY_F1 = 290
            } catch (NumberFormatException ignored) {
                // не номер функциональной клавиши
            }
        }
        throw new IllegalArgumentException("Unknown key '" + token + "' in shortcut '" + whole + "'");
    }

    /**
     * @param code      код нажатой клавиши
     * @param modifiers состояние модификаторов
     * @return {@code true}, если нажато именно это сочетание
     */
    public boolean matches(int code, int modifiers) {
        return code == keyCode && (modifiers & MASK) == this.modifiers;
    }

    /**
     * Сочетание, которое срабатывает, даже когда фокус в поле ввода: с Ctrl, Alt или Cmd и не из клавиш, которыми
     * поле владеет само. Одиночные клавиши вроде Delete в поле ввода печатают, а не вызывают команду.
     *
     * @return {@code true}, если команду можно вызвать из поля ввода
     */
    public boolean worksInTextFields() {
        boolean chord = (modifiers & (KeyModifiers.CONTROL | KeyModifiers.ALT | KeyModifiers.SUPER)) != 0;
        return chord && !((modifiers & KeyModifiers.CONTROL) != 0 && OWNED_BY_TEXT.contains(keyCode));
    }

    @Override
    public String toString() {
        return text;
    }
}
