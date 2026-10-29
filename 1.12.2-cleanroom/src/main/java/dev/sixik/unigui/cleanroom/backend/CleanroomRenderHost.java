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
}
