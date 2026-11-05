package dev.sixik.unigui.impl.debug;

import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugHotkeysTest {
    @Test
    void handle_ctrlShiftP_togglesProfilerAndConsumesRepeat() {
        DefaultUIContext context = new DefaultUIContext();
        int modifiers = KeyModifiers.CONTROL | KeyModifiers.SHIFT;

        assertFalse(DebugHotkeys.handle(context, KeyCodes.P, KeyModifiers.CONTROL));
        assertFalse(DebugFlags.has(context.debugFlags(), DebugFlags.PROFILER_OVERLAY));
        assertTrue(DebugHotkeys.handle(context, KeyCodes.P, modifiers));
        assertTrue(DebugFlags.has(context.debugFlags(), DebugFlags.PROFILER_OVERLAY));
        assertTrue(DebugHotkeys.handle(context, KeyCodes.P, modifiers));
        assertTrue(DebugFlags.has(context.debugFlags(), DebugFlags.PROFILER_OVERLAY));

        context.keyboard().release(KeyCodes.P);
        assertTrue(DebugHotkeys.handle(context, KeyCodes.P, modifiers));
        assertFalse(DebugFlags.has(context.debugFlags(), DebugFlags.PROFILER_OVERLAY));
    }

    @Test
    void handle_ctrlShiftPlusAndMinus_adjustsOverlayScale() {
        DefaultUIContext context = new DefaultUIContext();
        float initial = context.debugOverlaySettings().scale();
        int modifiers = KeyModifiers.CONTROL | KeyModifiers.SHIFT;

        assertTrue(DebugHotkeys.handle(context, KeyCodes.EQUAL, modifiers));
        assertEquals(initial + DebugHotkeys.SCALE_STEP, context.debugOverlaySettings().scale(), 0.00001f);
        assertTrue(DebugHotkeys.handle(context, KeyCodes.MINUS, modifiers));
        assertEquals(initial, context.debugOverlaySettings().scale(), 0.00001f);
    }

    @Test
    void handle_nonBinding_doesNotChangeDebugState() {
        DefaultUIContext context = new DefaultUIContext();

        assertFalse(DebugHotkeys.handle(context, KeyCodes.P, 0));
        assertEquals(DebugFlags.NONE, context.debugFlags());
    }
}
