package dev.sixik.unigui.gtnh.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtnhDemoLauncherTest {
    @Test
    void supportedDemoNames_includePortableScreens() {
        assertTrue(GtnhDemoLauncher.supported("widgets"));
        assertTrue(GtnhDemoLauncher.supported("UNITY"));
    }

    @Test
    void layerDemos_areSupported() {
        assertTrue(GtnhDemoLauncher.supported("postprocess"));
        assertTrue(GtnhDemoLauncher.supported("overlay"));
    }

    @Test
    void unsupportedDemoName_isRejectedWithoutClientAccess() {
        assertFalse(GtnhDemoLauncher.supported("minecraft-only"));
        assertFalse(GtnhDemoLauncher.supported(null));
    }
}