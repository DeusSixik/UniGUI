package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.scene.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * Adapts scene content and the game client's tracked OpenGL state to the shared passes.
 */
public interface GlScenePlatform {
    /**
     * @param output destination supplied by Scene3D @return its live GL attachments
     */
    Target output(RenderTarget output);

    /**
     * @param handle scene texture @return resolved GL texture binding or null
     */
    Texture texture(TextureHandle handle);

    /**
     * @return game client's GL state operations
     */
    State state();

    /**
     * Flushes game GUI data before entering a scene.
     */
    void flush();

    /**
     * Invalidates game-owned cached vertex bindings before changing VAOs.
     */
    void invalidate();

    /**
     * @param state block state resolved by the active Minecraft platform
     * @param lit   whether face shading should be supplied by the scene
     * @return surfaces baked in block coordinates
     */
    List<Surface> block(Object state, boolean lit);

    /**
     * @param structure owning structure
     * @param section   visible section in structure coordinates
     * @param lit       whether face shading should be supplied by the scene
     * @return baked surfaces in the structure's coordinates
     */
    List<Surface> section(StructureNode structure, StructureNode.Section section, boolean lit);

    /**
     * @return the native scene platform used to resolve content
     */
    ScenePlatform scenePlatform();

    /**
     * @param node scene node @return whether it has a vanilla model that needs a separate capture
     */
    boolean hasModel(SceneNode node);

    /**
     * @param node        owner of the local geometry
     * @param partialTick fractional game tick
     * @return captured surfaces, or null to use native rendering
     */
    List<Surface> capture(SceneNode node, float partialTick);

    /**
     * @param nodes       visible model slots
     * @param count       number of live slots
     * @param target      scene color and depth
     * @param camera      scene camera
     * @param view        world-to-view transform
     * @param scaleX      viewport width divided by target width
     * @param scaleY      viewport height divided by target height
     * @param partialTick fractional game tick
     */
    void drawNative(SceneNode[] nodes, int count, Target target, Camera3D camera, MutableMat4 view,
                    float scaleX, float scaleY, float partialTick);

    /**
     * @param retire releases temporary meshes uploaded by the renderer
     */
    void endFrame(Consumer<Mesh3D> retire);

    /**
     * @return version-specific source of native sprite paths
     */
    GlSceneSprites particles();

    /**
     * Releases resources owned by this platform adapter.
     */
    default void close() {
    }

    /**
     * A texture and its sampling options.
     */
    record Texture(int id, boolean flipY, boolean premultipliedAlpha, TextureOptions options) {
    }

    /**
     * An attachment pair owned by one game render target.
     */
    interface Target {
        /**
         * @return target width in pixels
         */
        int width();

        /**
         * @return target height in pixels
         */
        int height();

        /**
         * @return color texture name, or zero if absent
         */
        int color();

        /**
         * @return sampleable depth texture name, or zero if absent
         */
        int depth();

        /**
         * Binds the framebuffer and sets a full-size viewport.
         */
        void bind();
    }

    /**
     * @param mesh                  node-local indexed mesh
     * @param material              texture, alpha mode and shading
     * @param glint                 texture transformation for foil, or null for a base surface
     * @param translucentDepthWrite whether the blended layer writes depth
     * @param translucentCutout     whether the blended layer uses its alpha cutoff
     */
    record Surface(Mesh3D mesh, Material material, Glint glint,
                   boolean translucentDepthWrite, boolean translucentCutout) {
    }

    /**
     * Two rows of the native texture matrix, for {@code (u',v') = rows * (u,v,1)}.
     *
     * @param ux U weight of the new U
     * @param uy V weight of the new U
     * @param uz U offset
     * @param vx U weight of the new V
     * @param vy V weight of the new V
     * @param vz V offset
     */
    record Glint(float ux, float uy, float uz, float vx, float vy, float vz) {
    }

    /**
     * Operations that must keep the game's cached OpenGL state consistent.
     */
    interface State {
        void activeTexture(int unit);

        void bindTexture(int texture);

        void depthTest(boolean enabled);

        void depthMask(boolean write);

        void depthFunc(int function);

        void cull(boolean enabled);

        void blend(boolean enabled);

        void blendFuncSeparate(int src, int dst, int srcAlpha, int dstAlpha);

        void colorMask(boolean r, boolean g, boolean b, boolean a);

        void viewport(int x, int y, int w, int h);

        void scissor(int x, int y, int w, int h);

        void disableScissor();

        int samplerDepth();

        void applySampler(int unit, TextureOptions options);

        void restoreSamplers(int depth);
    }
}
