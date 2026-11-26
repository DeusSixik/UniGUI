package dev.sixik.unigui.cleanroom.commands;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    /** Checks partial names for both 3D screens via the client command's tab completion. */
    @Test
    void demoTabCompletion_partialNames_findBothSceneDemos() {
        var command = new CommandUniGUI();
        assertEquals(List.of("scene"), command.getTabCompletions(null, null, new String[]{"demo", "sce"}, null));
        assertEquals(List.of("furnace"), command.getTabCompletions(null, null, new String[]{"demo", "fur"}, null));
    }
}
