package dev.sixik.unigui.cleanroom.backend;

/**
 * Geometry of the surface a {@link CleanroomGuiRenderBackend} draws to.
 */
interface CleanroomRenderHost {
    /**
     * @return surface width in Minecraft scaled GUI pixels
     */
    int logicalWidth();

    /**
     * @return surface height in Minecraft scaled GUI pixels
     */
    int logicalHeight();

    /**
     * @return multiplier applied to the draw list through {@code glScalef} (see {@code CleanroomRenderMath.effectiveUiScale})
     */
    float uiScale();

    /**
     * @return extra alpha multiplier for the whole surface
     */
    float opacity();

    /**
     * @return width in physical pixels of the framebuffer being drawn to (the window, or a render target)
     */
    default int framebufferWidth() {
        return net.minecraft.client.Minecraft.getMinecraft().displayWidth;
    }

    /**
     * @return height in physical pixels of the framebuffer being drawn to
     */
    default int framebufferHeight() {
        return net.minecraft.client.Minecraft.getMinecraft().displayHeight;
    }

    /**
     * @return physical pixels per draw-list unit along x (sizes anti-aliasing fringes, glyph atlas levels and clips)
     */
    default float pixelsPerUnitX() {
        return CleanroomRenderMath.windowScale(uiScale(), framebufferWidth(), logicalWidth());
    }

    /**
     * @return physical pixels per draw-list unit along y
     */
    default float pixelsPerUnitY() {
        return CleanroomRenderMath.windowScale(uiScale(), framebufferHeight(), logicalHeight());
    }
}
