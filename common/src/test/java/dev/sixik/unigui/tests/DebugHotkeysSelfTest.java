package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.debug.DebugOverlaySettings;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftDebugHotkeys;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import org.lwjgl.glfw.GLFW;

/**
 * Проверяет отладочные хоткеи без запуска Minecraft: матчинг комбинаций,
 * тоггл профайлера и scale дебаг-оверлея через чистый helper.
 */
public final class DebugHotkeysSelfTest {
    private static final int CTRL_SHIFT = GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT;

    private DebugHotkeysSelfTest() {
    }

    public static void main(String[] args) {
        DefaultUIContext context = new DefaultUIContext();

        // Матчинг: только Ctrl+Shift + P/Equal/Minus/Keypad.
        expect(MinecraftDebugHotkeys.matches(GLFW.GLFW_KEY_P, CTRL_SHIFT), "ctrl+shift+P matches");
        expect(!MinecraftDebugHotkeys.matches(GLFW.GLFW_KEY_P, GLFW.GLFW_MOD_CONTROL), "ctrl+P without shift ignored");
        expect(MinecraftDebugHotkeys.matches(GLFW.GLFW_KEY_EQUAL, CTRL_SHIFT), "ctrl+shift+Equal matches");
        expect(MinecraftDebugHotkeys.matches(GLFW.GLFW_KEY_KP_ADD, CTRL_SHIFT), "ctrl+shift+KP_ADD matches");
        expect(MinecraftDebugHotkeys.matches(GLFW.GLFW_KEY_MINUS, CTRL_SHIFT), "ctrl+shift+Minus matches");
        expect(MinecraftDebugHotkeys.matches(GLFW.GLFW_KEY_KP_SUBTRACT, CTRL_SHIFT), "ctrl+shift+KP_SUBTRACT matches");
        expect(!MinecraftDebugHotkeys.matches(GLFW.GLFW_KEY_P, GLFW.GLFW_MOD_SHIFT), "shift+P without ctrl ignored");
        expect(!MinecraftDebugHotkeys.matches(GLFW.GLFW_KEY_P, 0), "bare P ignored");
        expect(!MinecraftDebugHotkeys.matches(GLFW.GLFW_KEY_Q, CTRL_SHIFT), "ctrl+shift+Q ignored");

        // Тоггл профайлера.
        expect(!DebugFlags.has(context.debugFlags(), DebugFlags.PROFILER_OVERLAY), "profiler off initially");
        expect(MinecraftDebugHotkeys.handle(context, GLFW.GLFW_KEY_P, CTRL_SHIFT, true), "P press consumed");
        expect(DebugFlags.has(context.debugFlags(), DebugFlags.PROFILER_OVERLAY), "profiler on after P");
        expect(MinecraftDebugHotkeys.handle(context, GLFW.GLFW_KEY_P, CTRL_SHIFT, true), "second P press consumed");
        expect(!DebugFlags.has(context.debugFlags(), DebugFlags.PROFILER_OVERLAY), "profiler off after second P");
        expect(!MinecraftDebugHotkeys.handle(context, GLFW.GLFW_KEY_P, CTRL_SHIFT, false), "key release ignored");
        expect(!DebugFlags.has(context.debugFlags(), DebugFlags.PROFILER_OVERLAY), "release does not toggle");
        expect(!MinecraftDebugHotkeys.handle(context, GLFW.GLFW_KEY_Q, CTRL_SHIFT, true), "unknown key ignored");

        // Scale оверлея: +/- шаг 0.25, кламп 0.25..4.0 внутри DebugOverlaySettings.
        DebugOverlaySettings settings = context.debugOverlaySettings();
        float initial = settings.scale();
        expect(MinecraftDebugHotkeys.handle(context, GLFW.GLFW_KEY_EQUAL, CTRL_SHIFT, true), "plus consumed");
        expectNear(initial + MinecraftDebugHotkeys.SCALE_STEP, settings.scale(), "plus grows scale");
        expect(MinecraftDebugHotkeys.handle(context, GLFW.GLFW_KEY_MINUS, CTRL_SHIFT, true), "minus consumed");
        expect(MinecraftDebugHotkeys.handle(context, GLFW.GLFW_KEY_MINUS, CTRL_SHIFT, true), "minus consumed again");
        expectNear(initial - MinecraftDebugHotkeys.SCALE_STEP, settings.scale(), "minus shrinks scale");

        settings.scale(4.0f);
        expect(MinecraftDebugHotkeys.handle(context, GLFW.GLFW_KEY_KP_ADD, CTRL_SHIFT, true), "keypad plus consumed");
        expectNear(4.0f, settings.scale(), "scale clamps at max");
        settings.scale(0.25f);
        expect(MinecraftDebugHotkeys.handle(context, GLFW.GLFW_KEY_KP_SUBTRACT, CTRL_SHIFT, true), "keypad minus consumed");
        expectNear(0.25f, settings.scale(), "scale clamps at min");

        // Null-контекст безопасен.
        expect(!MinecraftDebugHotkeys.handle(null, GLFW.GLFW_KEY_P, CTRL_SHIFT, true), "null context ignored");

        System.out.println("DebugHotkeysSelfTest passed");
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectNear(float expected, float actual, String message) {
        if (Math.abs(expected - actual) > 0.0001f) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
