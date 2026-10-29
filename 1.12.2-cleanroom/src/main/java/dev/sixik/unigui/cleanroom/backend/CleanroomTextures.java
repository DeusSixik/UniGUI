package dev.sixik.unigui.cleanroom.backend;

import net.minecraft.util.ResourceLocation;

/**
 * Maps UniGUI texture ids to Minecraft resource locations.
 */
final class CleanroomTextures {
    private static final ResourceLocation MISSING = new ResourceLocation("minecraft", "missingno");

    private CleanroomTextures() {
    }

    /**
     * @param id texture id such as {@code modid:textures/gui/icon.png}; {@code null} or blank gives the missing texture
     * @return resource location to bind
     */
    static ResourceLocation location(String id) {
        if (id == null || id.isBlank()) return MISSING;
        return new ResourceLocation(id);
    }
}
