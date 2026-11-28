package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.scene.*;
import dev.sixik.unigui.backend.minecraft_impl.GlScenePlatform;
import dev.sixik.unigui.backend.minecraft_impl.GlSceneSprites;
import dev.sixik.unigui.backend.shared.LegacyBlockModels;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * Connects GTNH's compatibility-profile state and model renderers to the indexed scene passes.
 */
final class GtnhGlScenePlatform implements GlScenePlatform, GlScenePlatform.State {
    /**
     * Separate caches for vanilla face shading and scene lighting.
     */
    private final LegacyBlockModels blocks = new LegacyBlockModels(GtnhScene.PLATFORM);
    private final LegacyBlockModels litBlocks = new LegacyBlockModels(new LitBaker());
    /**
     * Captured poses are reused by every shadow map and the color pass of a frame.
     */
    private final IdentityHashMap<SceneNode, List<Surface>> models = new IdentityHashMap<>();
    /**
     * Staging for a native model draw.
     */
    private final MutableMat4 transform = new MutableMat4();
    private final MutableMat4 world = new MutableMat4();
    private final float[] matrix = new float[16];
    /**
     * Texture sampling stack for this renderer.
     */
    private final GtnhSamplerState samplers = new GtnhSamplerState();
    /**
     * Fixed-function alpha state of the enclosing pass.
     */
    private boolean alpha;
    private int alphaFunction;
    private float alphaReference;
    /**
     * Original face selector, independent of the cull enable flag.
     */
    private int cullFace;

    /**
     * {@inheritDoc}
     */
    @Override
    public Target output(RenderTarget output) {
        if (output instanceof GtnhRenderTarget target && target.depth() != 0) return target;
        throw new IllegalArgumentException("UniGUI scene requires a GTNH target with sampleable depth");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Texture texture(TextureHandle handle) {
        if (handle.nativeHandle() instanceof Integer id) return new Texture(id, false, false, handle.options());
        var binding = GtnhTextures.sceneTexture(handle);
        return binding == null ? null : new Texture(binding.id(), binding.flipY(),
            handle.options().premultipliedAlpha(), handle.options());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public State state() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() {
        alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        alphaFunction = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        alphaReference = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        cullFace = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidate() {
        // Compatibility-profile vanilla draws do not cache vertex array objects.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Surface> block(Object state, boolean lit) {
        var surface = (lit ? litBlocks : blocks).surface(state);
        return surface == null ? List.of() : List.of(convert(surface));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Surface> section(StructureNode structure, StructureNode.Section section, boolean lit) {
        return (lit ? litBlocks : blocks).section(structure, section).stream()
            .map(GtnhGlScenePlatform::convert).toList();
    }

    /**
     * @param surface baked atlas geometry @return surface for the indexed renderer
     */
    private static Surface convert(LegacyBlockModels.Surface surface) {
        return new Surface(surface.mesh(), surface.material(), null, false, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScenePlatform scenePlatform() {
        return GtnhScene.PLATFORM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasModel(SceneNode node) {
        if (node instanceof BlockNode block) {
            return GtnhSceneContent.hasBlockEntity(block.nativeState(GtnhScene.PLATFORM));
        }
        return node instanceof ItemNode || node instanceof EntityNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Surface> capture(SceneNode node, float partialTick) {
        if (models.containsKey(node)) return models.get(node);
        List<Surface> surfaces = GtnhSceneCapture.capture(node, partialTick);
        models.put(node, surfaces);
        return surfaces;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drawNative(SceneNode[] nodes, int count, Target target, Camera3D camera, MutableMat4 view,
                           float scaleX, float scaleY, float partialTick) {
        int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL20.glUseProgram(0);
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        camera.projection(transform).copyColumnMajor(matrix, 0);
        // Map clip coordinates to the upper-left subrectangle, preserving the camera's depth.
        for (int i = 0; i < 16; i += 4) {
            float w = matrix[i + 3];
            matrix[i] = scaleX * matrix[i] + (scaleX - 1) * w;
            matrix[i + 1] = scaleY * matrix[i + 1] + (1 - scaleY) * w;
        }
        GL11.glLoadMatrixf(matrix);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            view.copyColumnMajor(matrix, 0);
            GL11.glLoadMatrixf(matrix);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GtnhSceneContent.beginVanilla();
            for (int i = 0; i < count; i++) {
                SceneNode node = nodes[i];
                transform.setProduct(view, node.worldMatrix(world)).copyColumnMajor(matrix, 0);
                GL11.glLoadMatrixf(matrix);
                draw(node, partialTick);
            }
        } finally {
            GtnhSceneContent.endVanilla();
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            GL11.glMatrixMode(mode);
        }
    }

    /**
     * @param node        vanilla model owner
     * @param partialTick fraction of the current game tick
     * @return whether the model was available and rendered
     */
    static boolean draw(SceneNode node, float partialTick) {
        if (node instanceof ItemNode item) {
            Object stack = item.nativeItem(GtnhScene.PLATFORM);
            return stack != null && GtnhSceneContent.drawItem(stack, item.display());
        }
        if (node instanceof EntityNode entity) {
            Object handle = entity.nativeEntity(GtnhScene.PLATFORM);
            return handle != null && GtnhSceneContent.drawEntity(handle, entity, partialTick);
        }
        if (node instanceof BlockNode block) {
            Object state = block.nativeState(GtnhScene.PLATFORM);
            return state != null && GtnhSceneContent.drawBlockEntity(state, partialTick);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endFrame(Consumer<Mesh3D> retire) {
        try {
            for (var surfaces : models.values()) {
                if (surfaces != null) for (Surface surface : surfaces) retire.accept(surface.mesh());
            }
        } finally {
            models.clear();
            GL11.glCullFace(cullFace);
            GL11.glAlphaFunc(alphaFunction, alphaReference);
            enable(GL11.GL_ALPHA_TEST, alpha);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GlSceneSprites particles() {
        return new GtnhSceneSprites(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        samplers.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activeTexture(int unit) {
        GL13.glActiveTexture(unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bindTexture(int texture) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void depthTest(boolean enabled) {
        enable(GL11.GL_DEPTH_TEST, enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void depthMask(boolean write) {
        GL11.glDepthMask(write);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void depthFunc(int function) {
        GL11.glDepthFunc(function);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cull(boolean enabled) {
        enable(GL11.GL_CULL_FACE, enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void blend(boolean enabled) {
        enable(GL11.GL_BLEND, enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void blendFuncSeparate(int src, int dst, int srcAlpha, int dstAlpha) {
        GL14.glBlendFuncSeparate(src, dst, srcAlpha, dstAlpha);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void colorMask(boolean r, boolean g, boolean b, boolean a) {
        GL11.glColorMask(r, g, b, a);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void viewport(int x, int y, int w, int h) {
        GL11.glViewport(x, y, w, h);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void scissor(int x, int y, int w, int h) {
        GL11.glScissor(x, y, w, h);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int samplerDepth() {
        return samplers.depth();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applySampler(int unit, TextureOptions options) {
        samplers.apply(unit, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreSamplers(int depth) {
        samplers.restoreTo(depth);
    }

    /**
     * @param capability fixed-function GL capability @param enabled requested enable state
     */
    private static void enable(int capability, boolean enabled) {
        if (enabled) GL11.glEnable(capability);
        else GL11.glDisable(capability);
    }

    /**
     * The same block atlas with directional face shading supplied by the scene.
     */
    private static final class LitBaker implements LegacyBlockModels.Baker {
        /**
         * {@inheritDoc}
         */
        @Override
        public ScenePlatform platform() {
            return GtnhScene.PLATFORM;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int resourceGeneration() {
            return GtnhScene.PLATFORM.resourceGeneration();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LegacyBlockModels.Surface bake(Object state) {
            return GtnhScene.PLATFORM.bake(state, true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isOpaqueCube(Object state) {
            return GtnhScene.PLATFORM.isOpaqueCube(state);
        }
    }
}
