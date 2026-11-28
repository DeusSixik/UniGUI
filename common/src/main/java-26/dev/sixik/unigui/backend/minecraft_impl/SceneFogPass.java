package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Frustum3D;
import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.FogVolume;

/**
 * Optional volumetric pass behind the Minecraft 26.3 scene renderer.
 */
interface SceneFogPass extends AutoCloseable {
    /**
     * Collects a visible fog node when its volume intersects the camera frustum.
     *
     * @param volume  visible fog node
     * @param frustum active camera frustum
     */
    void collect(FogVolume volume, Frustum3D frustum);

    /**
     * Integrates the collected fog and blends it into the geometry pass's color attachment.
     *
     * @param output         scene color and depth target
     * @param viewProjection camera matrix in canonical clip space
     * @param camera         projection and clipping parameters
     * @param seconds        scene material clock in seconds
     * @param lights         resolved light and shadow bindings for this scene
     * @param width          active viewport width in pixels before rounding
     * @param height         active viewport height in pixels before rounding
     */
    void render(UiGpu.Target output, Mat4View viewProjection, Camera3D camera, float seconds,
                SceneLocalLights lights, float width, float height);

    /**
     * Prepares the per-frame storage for the next scene.
     */
    void reset();

    /**
     * Releases render resources.
     */
    @Override
    void close();
}
