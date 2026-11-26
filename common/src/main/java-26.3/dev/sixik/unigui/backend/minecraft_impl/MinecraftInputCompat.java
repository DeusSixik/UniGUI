package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.sixik.unigui.api.input.MouseCursor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLMouse;

import java.util.Arrays;

/**
 * Ввод и курсор мыши для Minecraft 26.3, где окно и ввод перешли с GLFW на SDL3.
 *
 * <p>Ядро UniGUI работает с кодами GLFW ({@code KeyCodes}, {@code KeyModifiers}), а 26.3 присылает:</p>
 * <ul>
 *     <li>клавиши — скан-кодами SDL ({@link KeyEvent#key()}; это коды USB HID, как и у GLFW привязанные к
 *     положению клавиши, а не к раскладке);</li>
 *     <li>модификаторы — маской {@code SDL_Keymod} (левый и правый Shift — разные биты);</li>
 *     <li>кнопки мыши — номерами SDL (1 — левая, 2 — средняя, 3 — правая).</li>
 * </ul>
 * <p>Класс переводит их в коды GLFW и обратно (для событий, которые UniGUI отдаёт ванильным обработчикам).</p>
 */
public final class MinecraftInputCompat {
    /**
     * Скан-код SDL → код клавиши GLFW; -1 — у клавиши нет кода GLFW.
     */
    private static final int[] SDL_TO_GLFW = new int[512];
    /**
     * Код клавиши GLFW → скан-код SDL; 0 ({@code SDL_SCANCODE_UNKNOWN}) — нет соответствия.
     */
    private static final int[] GLFW_TO_SDL = new int[512];

    // Маска SDL_Keymod.
    private static final int SDL_KMOD_SHIFT = 0x0003;
    private static final int SDL_KMOD_CTRL = 0x00C0;
    private static final int SDL_KMOD_ALT = 0x0300;
    private static final int SDL_KMOD_GUI = 0x0C00;
    private static final int SDL_KMOD_NUM = 0x1000;
    private static final int SDL_KMOD_CAPS = 0x2000;
    // Модификаторы GLFW.
    private static final int GLFW_MOD_SHIFT = 0x0001;
    private static final int GLFW_MOD_CONTROL = 0x0002;
    private static final int GLFW_MOD_ALT = 0x0004;
    private static final int GLFW_MOD_SUPER = 0x0008;
    private static final int GLFW_MOD_CAPS_LOCK = 0x0010;
    private static final int GLFW_MOD_NUM_LOCK = 0x0020;

    /**
     * Курсоры SDL по виду курсора UniGUI; создаются при первом запросе.
     */
    private static final CursorType[] CURSORS = new CursorType[MouseCursor.values().length];
    /**
     * Курсор, который экран UniGUI держит сейчас (его нужно запрашивать каждый кадр).
     */
    private static MouseCursor activeCursor = MouseCursor.DEFAULT;

    static {
        Arrays.fill(SDL_TO_GLFW, -1);
        for (int i = 0; i < 26; i++) map(4 + i, 'A' + i);          // A..Z
        for (int i = 0; i < 9; i++) map(30 + i, '1' + i);          // 1..9
        map(39, '0');
        map(40, 257);   // Enter
        map(41, 256);   // Escape
        map(42, 259);   // Backspace
        map(43, 258);   // Tab
        map(44, 32);    // Space
        map(45, 45);    // Minus
        map(46, 61);    // Equals
        map(47, 91);    // Left bracket
        map(48, 93);    // Right bracket
        map(49, 92);    // Backslash
        map(51, 59);    // Semicolon
        map(52, 39);    // Apostrophe
        map(53, 96);    // Grave accent
        map(54, 44);    // Comma
        map(55, 46);    // Period
        map(56, 47);    // Slash
        map(57, 280);   // Caps lock
        for (int i = 0; i < 12; i++) map(58 + i, 290 + i);         // F1..F12
        map(70, 283);   // Print screen
        map(71, 281);   // Scroll lock
        map(72, 284);   // Pause
        map(73, 260);   // Insert
        map(74, 268);   // Home
        map(75, 266);   // Page up
        map(76, 261);   // Delete
        map(77, 269);   // End
        map(78, 267);   // Page down
        map(79, 262);   // Right
        map(80, 263);   // Left
        map(81, 264);   // Down
        map(82, 265);   // Up
        map(83, 282);   // Num lock
        map(84, 331);   // Keypad divide
        map(85, 332);   // Keypad multiply
        map(86, 333);   // Keypad subtract
        map(87, 334);   // Keypad add
        map(88, 335);   // Keypad enter
        for (int i = 0; i < 9; i++) map(89 + i, 321 + i);          // Keypad 1..9
        map(98, 320);   // Keypad 0
        map(99, 330);   // Keypad decimal
        map(100, 162);  // Non-US backslash (GLFW_KEY_WORLD_2)
        map(101, 348);  // Menu
        map(103, 336);  // Keypad equal
        for (int i = 0; i < 12; i++) map(104 + i, 302 + i);        // F13..F24
        map(224, 341);  // Left control
        map(225, 340);  // Left shift
        map(226, 342);  // Left alt
        map(227, 343);  // Left super
        map(228, 345);  // Right control
        map(229, 344);  // Right shift
        map(230, 346);  // Right alt
        map(231, 347);  // Right super
    }

    private MinecraftInputCompat() {
    }

    private static void map(int sdlScancode, int glfwKey) {
        SDL_TO_GLFW[sdlScancode] = glfwKey;
        GLFW_TO_SDL[glfwKey] = sdlScancode;
    }

    /**
     * @return код клавиши GLFW или -1 ({@code GLFW_KEY_UNKNOWN}), если у клавиши его нет
     */
    public static int key(KeyEvent event) {
        int scancode = event.key();
        return scancode >= 0 && scancode < SDL_TO_GLFW.length ? SDL_TO_GLFW[scancode] : -1;
    }

    /**
     * @return скан-код клавиши (скан-код SDL)
     */
    public static int scancode(KeyEvent event) {
        return event.key();
    }

    /**
     * @param nativeModifiers маска {@code SDL_Keymod} из события Minecraft
     * @return модификаторы GLFW ({@code KeyModifiers})
     */
    public static int modifiers(int nativeModifiers) {
        int modifiers = 0;
        if ((nativeModifiers & SDL_KMOD_SHIFT) != 0) modifiers |= GLFW_MOD_SHIFT;
        if ((nativeModifiers & SDL_KMOD_CTRL) != 0) modifiers |= GLFW_MOD_CONTROL;
        if ((nativeModifiers & SDL_KMOD_ALT) != 0) modifiers |= GLFW_MOD_ALT;
        if ((nativeModifiers & SDL_KMOD_GUI) != 0) modifiers |= GLFW_MOD_SUPER;
        if ((nativeModifiers & SDL_KMOD_CAPS) != 0) modifiers |= GLFW_MOD_CAPS_LOCK;
        if ((nativeModifiers & SDL_KMOD_NUM) != 0) modifiers |= GLFW_MOD_NUM_LOCK;
        return modifiers;
    }

    /**
     * @param modifiers модификаторы GLFW
     * @return маска {@code SDL_Keymod} (зажатыми считаются левые клавиши)
     */
    public static int nativeModifiers(int modifiers) {
        int sdl = 0;
        if ((modifiers & GLFW_MOD_SHIFT) != 0) sdl |= 0x0001;
        if ((modifiers & GLFW_MOD_CONTROL) != 0) sdl |= 0x0040;
        if ((modifiers & GLFW_MOD_ALT) != 0) sdl |= 0x0100;
        if ((modifiers & GLFW_MOD_SUPER) != 0) sdl |= 0x0400;
        if ((modifiers & GLFW_MOD_CAPS_LOCK) != 0) sdl |= SDL_KMOD_CAPS;
        if ((modifiers & GLFW_MOD_NUM_LOCK) != 0) sdl |= SDL_KMOD_NUM;
        return sdl;
    }

    /**
     * @param nativeButton кнопка SDL (1 — левая, 2 — средняя, 3 — правая, 4+ — дополнительные)
     * @return кнопка GLFW (0 — левая, 1 — правая, 2 — средняя, 3+ — дополнительные)
     */
    public static int mouseButton(int nativeButton) {
        return switch (nativeButton) {
            case 1 -> 0;
            case 2 -> 2;
            case 3 -> 1;
            default -> nativeButton - 1;
        };
    }

    /**
     * @param button кнопка GLFW
     * @return кнопка SDL
     */
    public static int nativeMouseButton(int button) {
        return switch (button) {
            case 0 -> 1;
            case 1 -> 3;
            case 2 -> 2;
            default -> button + 1;
        };
    }

    /**
     * Собирает событие клавиши Minecraft из кодов GLFW (для ванильных обработчиков).
     *
     * @param key       код клавиши GLFW
     * @param scancode  скан-код SDL из {@link #scancode}; если неизвестен, берётся по коду GLFW
     * @param modifiers модификаторы GLFW
     */
    public static KeyEvent keyEvent(int key, int scancode, int modifiers) {
        int sdlScancode = scancode > 0 ? scancode : (key >= 0 && key < GLFW_TO_SDL.length ? GLFW_TO_SDL[key] : 0);
        int sdlModifiers = nativeModifiers(modifiers);
        int keycode = sdlScancode == 0 ? 0 : SDLKeyboard.SDL_GetKeyFromScancode(sdlScancode, (short) sdlModifiers, false);
        return new KeyEvent(sdlScancode, keycode, sdlModifiers);
    }

    /**
     * Включает или выключает ввод текста окна для владельца {@code owner}.
     *
     * <p>В SDL3 символы ({@code charTyped}) приходят, только пока у окна включён ввод текста; ванильные поля
     * включают его сами через {@code TextInputManager}. Выключение срабатывает, только если владелец тот же, поэтому
     * экраны UniGUI и ванильные поля не выключают ввод друг другу.</p>
     */
    static void setTextInput(Object owner, boolean active) {
        Minecraft.getInstance().textInputManager().onTextInputFocusChange(owner, active);
    }

    /**
     * @return включён ли сейчас ввод текста окна (SDL)
     */
    static boolean textInputActive() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getWindow() != null && SDLKeyboard.SDL_TextInputActive(minecraft.getWindow().handle());
    }

    /**
     * @return частота обновления монитора окна (Гц) или 0, если неизвестна
     */
    static int refreshRate(Minecraft minecraft) {
        if (minecraft.getWindow() == null) return 0;
        VideoMode mode = minecraft.getWindow().getActiveVideoMode();
        return mode == null ? 0 : Math.round(mode.getRefreshRate());
    }

    /**
     * Ставит курсор мыши сразу. {@link MouseCursor#DEFAULT} возвращает стандартную стрелку.
     *
     * <p>Minecraft 26.3 в конце каждого кадра ставит курсор, запрошенный через
     * {@link GuiGraphicsExtractor#requestCursor}, и без запроса возвращает стрелку. Поэтому курсор запоминается и
     * запрашивается заново каждый кадр в {@link #requestCursor}.</p>
     */
    static void setCursor(Minecraft minecraft, MouseCursor cursor) {
        activeCursor = cursor == null ? MouseCursor.DEFAULT : cursor;
        if (minecraft != null && minecraft.getWindow() != null) {
            minecraft.getWindow().selectCursor(cursorType(activeCursor));
        }
    }

    /**
     * Запрашивает текущий курсор UniGUI на этот кадр.
     */
    static void requestCursor(GuiGraphicsExtractor graphics, MouseCursor cursor) {
        MouseCursor requested = cursor == null ? activeCursor : cursor;
        if (requested != MouseCursor.DEFAULT) graphics.requestCursor(cursorType(requested));
    }

    /**
     * Возвращает стандартный курсор. Курсоры SDL общие для окна и остаются в кэше.
     */
    static void releaseCursors(Minecraft minecraft) {
        setCursor(minecraft, MouseCursor.DEFAULT);
    }

    private static CursorType cursorType(MouseCursor cursor) {
        CursorType type = CURSORS[cursor.ordinal()];
        if (type == null) {
            type = switch (cursor) {
                case POINTER -> CursorTypes.POINTING_HAND;
                case TEXT -> CursorTypes.IBEAM;
                case CROSSHAIR -> CursorTypes.CROSSHAIR;
                case RESIZE_HORIZONTAL -> CursorTypes.RESIZE_EW;
                case RESIZE_VERTICAL -> CursorTypes.RESIZE_NS;
                case RESIZE_NWSE -> CursorType.createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE,
                        "unigui_resize_nwse", CursorTypes.RESIZE_EW);
                case RESIZE_NESW -> CursorType.createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_NESW_RESIZE,
                        "unigui_resize_nesw", CursorTypes.RESIZE_EW);
                case DEFAULT -> CursorType.DEFAULT;
            };
            CURSORS[cursor.ordinal()] = type;
        }
        return type;
    }
}
