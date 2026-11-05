package dev.sixik.unigui.cleanroom.backend;

import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CleanroomDemoAssetsTest {
    @Test
    void resourceLocation_demoAsset_usesCleanroomModDomain() {
        assertEquals(new ResourceLocation("unigui", "textures/gui/fork_in_socket/wilka.png"),
                CleanroomDemoAssets.resourceLocation(
                        "assets/unigui_testmod/textures/gui/fork_in_socket/wilka.png"));
    }

    @Test
    void resourceLocation_invalidClasspathPath_returnsNull() {
        assertNull(CleanroomDemoAssets.resourceLocation("unigui_testmod:textures/gui/icon.png"));
        assertNull(CleanroomDemoAssets.resourceLocation("assets/unigui_testmod"));
    }
}
