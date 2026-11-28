package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.scene.*;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Binds the shared OpenGL scene passes to Minecraft 1.20.1 and 1.21.1. */
final class MinecraftGlScenePlatform implements GlScenePlatform, GlScenePlatform.State {
    /** Resolves texture handles using the UI backend's cache. */
    private final Function<TextureHandle, MinecraftGuiRenderBackend.TextureBinding> textures;
    /** Submits accumulated GUI geometry. */
    private final Runnable flushGui;
    /** Vanilla and scene-lit block caches. */
    private final LegacySceneBlockMeshes blocks = new LegacySceneBlockMeshes(false);
    /** Separate bake cache without vanilla directional shading. */
    private LegacySceneBlockMeshes litBlocks;
    /** Native model capture and compatibility drawers. */
    private LegacySceneFeatures features;

    /**
     * @param textures GUI texture resolver
     * @param flushGui flush of queued GUI geometry
     */
    MinecraftGlScenePlatform(Function<TextureHandle, MinecraftGuiRenderBackend.TextureBinding> textures, Runnable flushGui) {
        this.textures = textures;
        this.flushGui = flushGui;
    }

    /** {@inheritDoc} */
    @Override
    public Target output(RenderTarget output) {
        if (!(output instanceof MinecraftRenderTarget target) || !target.options().useDepth()
                || target.options().clearBeforeRender()) {
            throw new IllegalArgumentException("UniGUI scene requires a Minecraft depth target with no 2D auto-clear");
        }
        return target;
    }

    /** {@inheritDoc} */
    @Override
    public Texture texture(TextureHandle handle) {
        var texture = textures.apply(handle);
        if (texture == null) return null;
        int id;
        if (texture.textureId() != null) {
            id = texture.textureId();
        } else {
            id = net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(texture.location()).getId();
        }
        return new Texture(id, texture.flipY(), texture.premultipliedAlpha(), texture.options());
    }

    /** {@inheritDoc} */
    @Override
    public State state() { return this; }

    /** {@inheritDoc} */
    @Override
    public void flush() { flushGui.run(); }

    /** {@inheritDoc} */
    @Override
    public void invalidate() { BufferUploader.invalidate(); }

    /** {@inheritDoc} */
    @Override
    public List<Surface> block(Object state, boolean lit) {
        return blockCache(lit).single((net.minecraft.world.level.block.state.BlockState) state);
    }

    /** {@inheritDoc} */
    @Override
    public List<Surface> section(StructureNode node, StructureNode.Section section, boolean lit) {
        return blockCache(lit).section(node, section);
    }

    /**
     * @param lit whether scene lights will shade the blocks
     * @return selected bake cache
     */
    private LegacySceneBlockMeshes blockCache(boolean lit) {
        if (!lit) return blocks;
        if (litBlocks == null) litBlocks = new LegacySceneBlockMeshes(true);
        return litBlocks;
    }

    /** {@inheritDoc} */
    @Override
    public ScenePlatform scenePlatform() { return MinecraftScene.PLATFORM; }

    /** {@inheritDoc} */
    @Override
    public boolean hasModel(SceneNode node) {
        return !(node instanceof BlockNode block) || MinecraftScene.blockEntity(block) != null;
    }

    /** {@inheritDoc} */
    @Override
    public List<Surface> capture(SceneNode node, float partialTick) {
        return features().capture(node, partialTick);
    }

    /** {@inheritDoc} */
    @Override
    public void drawNative(SceneNode[] nodes, int count, Target target, Camera3D camera, MutableMat4 view,
                           float scaleX, float scaleY, float partialTick) {
        features().render(nodes, count, (MinecraftRenderTarget) target, camera, view, scaleX, scaleY, partialTick);
    }

    /** @return reusable native drawer */
    private LegacySceneFeatures features() {
        if (features == null) features = new LegacySceneFeatures();
        return features;
    }

    /** {@inheritDoc} */
    @Override
    public void endFrame(Consumer<Mesh3D> retire) {
        if (features != null) features.endFrame(retire);
    }

    /** {@inheritDoc} */
    @Override
    public GlSceneSprites particles() { return new LegacySceneParticles(this); }

    /** {@inheritDoc} */
    @Override
    public void activeTexture(int unit) { RenderSystem.activeTexture(unit); }

    /** {@inheritDoc} */
    @Override
    public void bindTexture(int texture) { RenderSystem.bindTexture(texture); }

    /** {@inheritDoc} */
    @Override
    public void depthTest(boolean enabled) {
        if (enabled) RenderSystem.enableDepthTest();
        else RenderSystem.disableDepthTest();
    }

    /** {@inheritDoc} */
    @Override
    public void depthMask(boolean write) { RenderSystem.depthMask(write); }

    /** {@inheritDoc} */
    @Override
    public void depthFunc(int fn) { RenderSystem.depthFunc(fn); }

    /** {@inheritDoc} */
    @Override
    public void cull(boolean enabled) {
        if (enabled) RenderSystem.enableCull();
        else RenderSystem.disableCull();
    }

    /** {@inheritDoc} */
    @Override
    public void blend(boolean enabled) {
        if (enabled) RenderSystem.enableBlend();
        else RenderSystem.disableBlend();
    }

    /** {@inheritDoc} */
    @Override
    public void blendFuncSeparate(int src, int dst, int srcAlpha, int dstAlpha) {
        RenderSystem.blendFuncSeparate(src, dst, srcAlpha, dstAlpha);
    }

    /** {@inheritDoc} */
    @Override
    public void colorMask(boolean r, boolean g, boolean b, boolean a) { RenderSystem.colorMask(r, g, b, a); }

    /** {@inheritDoc} */
    @Override
    public void viewport(int x, int y, int w, int h) { RenderSystem.viewport(x, y, w, h); }

    /** {@inheritDoc} */
    @Override
    public void scissor(int x, int y, int w, int h) { RenderSystem.enableScissor(x, y, w, h); }

    /** {@inheritDoc} */
    @Override
    public void disableScissor() { RenderSystem.disableScissor(); }

    /** {@inheritDoc} */
    @Override
    public int samplerDepth() { return MinecraftTextureSamplerState.depth(); }

    /** {@inheritDoc} */
    @Override
    public void applySampler(int unit, TextureOptions options) { MinecraftTextureSamplerState.apply(unit, options); }

    /** {@inheritDoc} */
    @Override
    public void restoreSamplers(int depth) { MinecraftTextureSamplerState.restoreTo(depth); }
}
