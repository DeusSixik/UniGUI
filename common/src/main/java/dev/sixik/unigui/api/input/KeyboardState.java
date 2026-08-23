package dev.sixik.unigui.api.input;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Состояние клавиатуры текущего UI context'а.
 *
 * <p>Routed {@code KeyPressedEvent}/{@code KeyReleasedEvent} удобны для точечных действий, но для
 * игрового управления внутри UI нужен polling: виджет на каждом {@code tick(...)} спрашивает,
 * удерживается ли клавиша. Этот класс хранит оба типа состояния:</p>
 *
 * <ul>
 *     <li>{@link #isDown(int)} — клавиша удерживается прямо сейчас;</li>
 *     <li>{@link #wasPressed(int)} — клавиша была нажата с прошлого UI frame;</li>
 *     <li>{@link #wasReleased(int)} — клавиша была отпущена с прошлого UI frame.</li>
 * </ul>
 *
 * <p>Backend обновляет состояние из native input callbacks, а в конце кадра вызывает
 * {@link #endFrame()}, чтобы одноразовые edge-флаги не повторялись бесконечно. Если клавишу быстро
 * нажали и отпустили между двумя кадрами, следующий tick увидит и {@code wasPressed}, и
 * {@code wasReleased}, но {@code isDown} уже будет {@code false}.</p>
 */
public class KeyboardState {
    /** No-op состояние для минимальных runtime'ов без клавиатурного ввода. */
    public static final KeyboardState NONE = new KeyboardState(false);

    private final Set<Integer> downKeys = new HashSet<>();
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Set<Integer> releasedKeys = new HashSet<>();
    private final boolean mutable;

    /** Создаёт обычное mutable состояние клавиатуры. */
    public KeyboardState() {
        this(true);
    }

    private KeyboardState(boolean mutable) {
        this.mutable = mutable;
    }

    /**
     * Помечает клавишу как нажатую.
     *
     * <p>Повторные native key-repeat события не создают новый {@link #wasPressed(int)}, пока клавиша
     * уже находится в down-состоянии.</p>
     *
     * @param keyCode backend key code, обычно GLFW/Minecraft key code
     */
    public void press(int keyCode) {
        if (!mutable || !validKey(keyCode)) return;
        if (downKeys.add(keyCode)) {
            pressedKeys.add(keyCode);
        }
    }

    /**
     * Помечает клавишу как отпущенную.
     *
     * @param keyCode backend key code, обычно GLFW/Minecraft key code
     */
    public void release(int keyCode) {
        if (!mutable || !validKey(keyCode)) return;
        if (downKeys.remove(keyCode)) {
            releasedKeys.add(keyCode);
        }
    }

    /**
     * Проверяет, удерживается ли клавиша сейчас.
     *
     * @param keyCode key code
     * @return {@code true}, если клавиша находится в down-состоянии
     */
    public boolean isDown(int keyCode) {
        return validKey(keyCode) && downKeys.contains(keyCode);
    }

    /**
     * Проверяет, была ли клавиша нажата с прошлого кадра.
     *
     * @param keyCode key code
     * @return {@code true} только до ближайшего {@link #endFrame()}
     */
    public boolean wasPressed(int keyCode) {
        return validKey(keyCode) && pressedKeys.contains(keyCode);
    }

    /**
     * Проверяет, была ли клавиша отпущена с прошлого кадра.
     *
     * @param keyCode key code
     * @return {@code true} только до ближайшего {@link #endFrame()}
     */
    public boolean wasReleased(int keyCode) {
        return validKey(keyCode) && releasedKeys.contains(keyCode);
    }

    /**
     * Проверяет, удерживается ли хотя бы одна из переданных клавиш.
     *
     * @param keyCodes список key code'ов
     * @return {@code true}, если хотя бы одна клавиша down
     */
    public boolean isAnyDown(int... keyCodes) {
        if (keyCodes == null || keyCodes.length == 0) return false;
        for (int keyCode : keyCodes) {
            if (isDown(keyCode)) return true;
        }
        return false;
    }

    /**
     * Проверяет, удерживаются ли все переданные клавиши.
     *
     * @param keyCodes список key code'ов
     * @return {@code true}, если все клавиши down
     */
    public boolean areAllDown(int... keyCodes) {
        if (keyCodes == null || keyCodes.length == 0) return false;
        for (int keyCode : keyCodes) {
            if (!isDown(keyCode)) return false;
        }
        return true;
    }

    /** @return количество удерживаемых клавиш */
    public int downCount() {
        return downKeys.size();
    }

    /** @return immutable snapshot удерживаемых клавиш */
    public Set<Integer> downKeys() {
        return downKeys.isEmpty() ? Set.of() : Collections.unmodifiableSet(new HashSet<>(downKeys));
    }

    /** Очищает одноразовые pressed/released флаги в конце UI кадра. */
    public void endFrame() {
        if (!mutable) return;
        pressedKeys.clear();
        releasedKeys.clear();
    }

    /** Полностью сбрасывает состояние, например при закрытии screen или потере input focus. */
    public void clear() {
        if (!mutable) return;
        downKeys.clear();
        pressedKeys.clear();
        releasedKeys.clear();
    }

    private static boolean validKey(int keyCode) {
        return keyCode >= 0;
    }
}