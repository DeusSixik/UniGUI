package dev.sixik.unigui.cleanroom.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanroomDemoLauncherTest {
    @Test
    void supportedDemoNames_includePortableScreens() {
        assertTrue(CleanroomDemoLauncher.supported("widgets"));
        assertTrue(CleanroomDemoLauncher.supported("UNITY"));
        assertTrue(CleanroomDemoLauncher.supported("postprocess"));
    }

    @Test
    void unsupportedDemoName_isRejectedWithoutClientAccess() {
        assertFalse(CleanroomDemoLauncher.supported("minecraft-only"));
        assertFalse(CleanroomDemoLauncher.supported(null));
    }
}
