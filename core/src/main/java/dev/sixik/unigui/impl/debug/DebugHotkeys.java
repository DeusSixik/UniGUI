package dev.sixik.unigui.impl.debug;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.debug.DebugOverlaySettings;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.impl.core.DefaultUIContext;

/**
 * Shared debug key bindings for game-version screen backends.
 */
public final class DebugHotkeys {
    public static final float SCALE_STEP = 0.25f;

    private DebugHotkeys() {
    }

    public static boolean matches(int keyCode, int modifiers) {
        if (!KeyModifiers.has(modifiers, KeyModifiers.CONTROL)
                || !KeyModifiers.has(modifiers, KeyModifiers.SHIFT)) return false;
        return keyCode == KeyCodes.EQUAL || keyCode == KeyCodes.KEYPAD_ADD
                || keyCode == KeyCodes.MINUS || keyCode == KeyCodes.KEYPAD_SUBTRACT
                || keyCode == KeyCodes.P;
    }

    /**
     * Applies a binding once per physical press while consuming key-repeat callbacks.
     */
    public static boolean handle(UIContext context, int keyCode, int modifiers) {
        if (!matches(keyCode, modifiers) || context == null) return false;
        if (!(context instanceof DefaultUIContext target)) return false;
        boolean firstPress = !context.keyboard().isDown(keyCode);
        context.keyboard().press(keyCode);
        if (!firstPress) return true;

        if (keyCode == KeyCodes.P) {
            int flags = target.debugFlags();
            if (DebugFlags.has(flags, DebugFlags.PROFILER_OVERLAY)) {
                target.disableDebugFlags(DebugFlags.PROFILER_OVERLAY);
            } else {
                target.enableDebugFlags(DebugFlags.PROFILER_OVERLAY);
            }
            return true;
        }

        float delta = keyCode == KeyCodes.EQUAL || keyCode == KeyCodes.KEYPAD_ADD
                ? SCALE_STEP : -SCALE_STEP;
        DebugOverlaySettings settings = target.debugOverlaySettings();
        settings.scale(settings.scale() + delta);
        return true;
    }
}
