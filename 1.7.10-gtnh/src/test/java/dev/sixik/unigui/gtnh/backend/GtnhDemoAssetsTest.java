package dev.sixik.unigui.gtnh.backend;

import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GtnhDemoAssetsTest {
    @Test
    void resourceLocation_demoAsset_usesGtnhModDomain() {
        assertEquals(new ResourceLocation("unigui", "textures/gui/fork_in_socket/wilka.png"),
                GtnhDemoAssets.resourceLocation(
                        "assets/unigui_testmod/textures/gui/fork_in_socket/wilka.png"));
    }

    @Test
    void resourceLocation_invalidClasspathPath_returnsNull() {
        assertNull(GtnhDemoAssets.resourceLocation("unigui_testmod:textures/gui/icon.png"));
        assertNull(GtnhDemoAssets.resourceLocation("assets/unigui_testmod"));
    }
}
