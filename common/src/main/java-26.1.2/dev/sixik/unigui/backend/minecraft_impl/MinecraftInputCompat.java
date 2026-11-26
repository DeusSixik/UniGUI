package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.input.MouseCursor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.Map;

/**
 * Ввод и курсор мыши, которые отличаются между версиями (26.1–26.2: окно на GLFW).
 *
 * <p>Ядро UniGUI работает с кодами клавиш, кнопок и модификаторов GLFW ({@code KeyCodes}, {@code KeyModifiers}).
 * Здесь Minecraft сам отдаёт коды GLFW, поэтому перевод тождественный. В 26.3 окно перешло на SDL3, и та же
 * реализация переводит коды SDL в GLFW.</p>
 */
public final class MinecraftInputCompat {
    /** Значение {@code GLFW_RESIZE_NWSE_CURSOR} из GLFW 3.4. */
    private static final int GLFW_RESIZE_NWSE_CURSOR = 0x00036007;
    /** Значение {@code GLFW_RESIZE_NESW_CURSOR} из GLFW 3.4. */
    private static final int GLFW_RESIZE_NESW_CURSOR = 0x00036008;

    /** Созданные нативные курсоры GLFW; освобождаются в {@link #releaseCursors}. */
    private static final Map<MouseCursor, Long> NATIVE_CURSORS = new EnumMap<>(MouseCursor.class);

    private MinecraftInputCompat() {
    }

    /**
     * @return код клавиши GLFW
     */
    public static int key(KeyEvent event) {
        return event.key();
    }

    /**
     * @return платформенный скан-код клавиши
     */
    public static int scancode(KeyEvent event) {
        return event.scancode();
    }

    /**
     * @param nativeModifiers модификаторы из события Minecraft
     * @return модификаторы GLFW ({@code KeyModifiers})
     */
    public static int modifiers(int nativeModifiers) {
        return nativeModifiers;
    }

    /**
     * @param nativeButton кнопка из события Minecraft
     * @return кнопка GLFW (0 — левая, 1 — правая, 2 — средняя)
     */
    public static int mouseButton(int nativeButton) {
        return nativeButton;
    }

    /**
     * Обратный перевод для событий, которые UniGUI отдаёт ванильным обработчикам.
     *
     * @param button кнопка GLFW
     * @return кнопка в кодах Minecraft
     */
    public static int nativeMouseButton(int button) {
        return button;
    }

    /**
     * Собирает событие клавиши Minecraft из кодов GLFW (для ванильных обработчиков).
     */
    public static KeyEvent keyEvent(int key, int scancode, int modifiers) {
        return new KeyEvent(key, scancode, modifiers);
    }

    /**
     * @param modifiers модификаторы GLFW
     * @return модификаторы в кодах Minecraft
     */
    public static int nativeModifiers(int modifiers) {
        return modifiers;
    }

    /**
     * Включает или выключает ввод текста окна. В GLFW символы приходят всегда, поэтому ничего делать не нужно.
     */
    static void setTextInput(Object owner, boolean active) {
    }

    /**
     * @return включён ли ввод текста окна; в GLFW символы приходят всегда
     */
    static boolean textInputActive() {
        return true;
    }

    /**
     * @return частота обновления монитора окна (Гц) или 0, если неизвестна
     */
    static int refreshRate(Minecraft minecraft) {
        return minecraft.getWindow() == null ? 0 : minecraft.getWindow().getRefreshRate();
    }

    /**
     * Ставит курсор мыши сразу. {@link MouseCursor#DEFAULT} возвращает стандартную стрелку.
     */
    static void setCursor(Minecraft minecraft, MouseCursor cursor) {
        long window = minecraft == null || minecraft.getWindow() == null ? 0L : minecraft.getWindow().handle();
        if (window == 0L) return;
        long nativeCursor = cursor == MouseCursor.DEFAULT
                ? 0L
                : NATIVE_CURSORS.computeIfAbsent(cursor, MinecraftInputCompat::createNativeCursor);
        GLFW.glfwSetCursor(window, nativeCursor);
    }

    /**
     * Запрашивает курсор на текущий кадр. Курсор GLFW держится сам, поэтому здесь ничего делать не нужно.
     */
    static void requestCursor(GuiGraphicsExtractor graphics, MouseCursor cursor) {
    }

    /**
     * Возвращает стандартный курсор и освобождает созданные нативные курсоры.
     */
    static void releaseCursors(Minecraft minecraft) {
        long window = minecraft == null || minecraft.getWindow() == null ? 0L : minecraft.getWindow().handle();
        if (window != 0L) GLFW.glfwSetCursor(window, 0L);
        for (long cursor : NATIVE_CURSORS.values()) {
            if (cursor != 0L) GLFW.glfwDestroyCursor(cursor);
        }
        NATIVE_CURSORS.clear();
    }

    private static long createNativeCursor(MouseCursor cursor) {
        int shape = switch (cursor) {
            case POINTER -> GLFW.GLFW_HAND_CURSOR;
            case TEXT -> GLFW.GLFW_IBEAM_CURSOR;
            case CROSSHAIR -> GLFW.GLFW_CROSSHAIR_CURSOR;
            case RESIZE_HORIZONTAL -> GLFW.GLFW_HRESIZE_CURSOR;
            case RESIZE_VERTICAL -> GLFW.GLFW_VRESIZE_CURSOR;
            case RESIZE_NWSE -> diagonalCursorShape(GLFW_RESIZE_NWSE_CURSOR);
            case RESIZE_NESW -> diagonalCursorShape(GLFW_RESIZE_NESW_CURSOR);
            case DEFAULT -> GLFW.GLFW_ARROW_CURSOR;
        };
        return GLFW.glfwCreateStandardCursor(shape);
    }

    /**
     * Диагональный курсор есть только в GLFW 3.4+; на старых версиях остаётся горизонтальная стрелка, чтобы не
     * вызвать ошибку {@code invalid enum}.
     */
    private static int diagonalCursorShape(int shape) {
        int[] major = new int[1];
        int[] minor = new int[1];
        int[] revision = new int[1];
        GLFW.glfwGetVersion(major, minor, revision);
        return major[0] > 3 || (major[0] == 3 && minor[0] >= 4) ? shape : GLFW.GLFW_HRESIZE_CURSOR;
    }
}
