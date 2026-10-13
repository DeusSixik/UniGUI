package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.debug.DebugOverlaySettings;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import org.lwjgl.glfw.GLFW;

/**
 * Отладочные хоткеи для {@link MinecraftWidgetScreen} и {@link MinecraftWidgetRenderLayer}.
 *
 * <ul>
 *     <li>{@code Ctrl+Shift+P} — вкл/выкл {@link DebugFlags#PROFILER_OVERLAY};</li>
 *     <li>{@code Ctrl+Shift+Plus} / {@code Ctrl+Shift+Equal} — увеличить
 *     {@link DebugOverlaySettings#scale() scale} дебаг-оверлея;</li>
 *     <li>{@code Ctrl+Shift+Minus} / {@code Ctrl+Shift+Numpad -} — уменьшить scale.</li>
 * </ul>
 *
 * <p>GLFW не отдаёт '+'/ '-' как отдельные key codes: на основном ряду это
 * {@code EQUAL} ({@code Shift+=} даёт '+') и {@code MINUS}. Поэтому обе клавиши
 * принимаются в обоих вариантах плюс keypad-эквиваленты.</p>
 *
 * <p>Helper намеренно не зависит от Minecraft-классов, кроме GLFW key codes:
 * логика матчинга покрыта {@code DebugHotkeysSelfTest} без запуска игры.</p>
 */
public final class MinecraftDebugHotkeys {
    /** Шаг изменения scale дебаг-оверлея за одно нажатие. */
    public static final float SCALE_STEP = 0.25f;

    private MinecraftDebugHotkeys() {
    }

    /**
     * Применяет отладочный хоткей, если нажатие ему соответствует.
     *
     * @param context контекст экрана; хоткеи применяются только к {@link DefaultUIContext}
     * @param keyCode GLFW key code
     * @param modifiers GLFW modifier bitmask ({@code GLFW_MOD_CONTROL} и т.п.)
     * @param pressed {@code true} для keyPressed, {@code false} для keyReleased;
     *                действие выполняется один раз на нажатие, без auto-repeat
     * @return {@code true}, если нажатие поглощено хоткеем
     */
    public static boolean handle(dev.sixik.unigui.api.core.UIContext context,
                                 int keyCode, int modifiers, boolean pressed) {
        if (!pressed || context == null) return false;
        if (!(context instanceof DefaultUIContext target)) return false;
        if (!matches(keyCode, modifiers)) return false;

        if (keyCode == GLFW.GLFW_KEY_P) {
            toggleProfiler(target);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_EQUAL
                || keyCode == GLFW.GLFW_KEY_KP_ADD) {
            bumpScale(target, SCALE_STEP);
            return true;
        }
        bumpScale(target, -SCALE_STEP);
        return true;
    }

    /**
     * Проверяет, соответствует ли нажатие одному из отладочных хоткеев, без выполнения действия.
     *
     * <p>Нужно экрану, чтобы поглощать auto-repeat нажатия, не дёргая тоггл по второму разу.</p>
     *
     * @param keyCode GLFW key code
     * @param modifiers GLFW modifier bitmask
     * @return {@code true}, если это {@code Ctrl+Shift+P/Plus/Minus}
     */
    public static boolean matches(int keyCode, int modifiers) {
        if (!hasControl(modifiers) || !hasShift(modifiers)) return false;
        return keyCode == GLFW.GLFW_KEY_P
                || keyCode == GLFW.GLFW_KEY_EQUAL
                || keyCode == GLFW.GLFW_KEY_KP_ADD
                || keyCode == GLFW.GLFW_KEY_MINUS
                || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT;
    }

    private static void toggleProfiler(DefaultUIContext context) {
        int flags = context.debugFlags();
        if (DebugFlags.has(flags, DebugFlags.PROFILER_OVERLAY)) {
            context.disableDebugFlags(DebugFlags.PROFILER_OVERLAY);
        } else {
            context.enableDebugFlags(DebugFlags.PROFILER_OVERLAY);
        }
    }

    private static void bumpScale(DefaultUIContext context, float delta) {
        DebugOverlaySettings settings = context.debugOverlaySettings();
        // Диапазон 0.25..4.0 зажимает сам DebugOverlaySettings.scale().
        settings.scale(settings.scale() + delta);
    }

    private static boolean hasControl(int glfwModifiers) {
        return (glfwModifiers & GLFW.GLFW_MOD_CONTROL) != 0;
    }

    private static boolean hasShift(int glfwModifiers) {
        return (glfwModifiers & GLFW.GLFW_MOD_SHIFT) != 0;
    }
}
