package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.render.BlendMode;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Captures the OpenGL state UniGUI touches and restores it after a frame.
 *
 * <p>Minecraft 1.12.2 keeps a shadow copy of most fixed-function state inside {@link GlStateManager} and skips GL
 * calls when the requested value equals the cached one. Raw {@code GL11} calls (or {@code glPushAttrib}/
 * {@code glPopAttrib}) change the real state without updating that cache, which later shows up as vanilla text,
 * tiles or item models rendering with the wrong state. All state changes here therefore go through
 * {@link GlStateManager}; only state that {@code GlStateManager} does not track (scissor) is touched directly.</p>
 *
 * <p>An instance owns reusable buffers and is not thread safe: use it from the render thread only. Capture
 * reads the real GL state with a handful of {@code glGet*} calls once per frame per layer.</p>
 */
final class CleanroomGlState {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    /**
     * Enable with {@code -Dunigui.glDebug=true} to log {@code glGetError} after each UI frame.
     */
    private static final boolean DEBUG = Boolean.getBoolean("unigui.glDebug");

    private final IntBuffer ints = BufferUtils.createIntBuffer(16);
    private final FloatBuffer floats = BufferUtils.createFloatBuffer(16);
    private final int[] scissorBox = new int[4];
    private final float[] color = new float[4];
    private boolean blend;
    private boolean texture2d;
    private boolean alphaTest;
    private boolean depthTest;
    private boolean lighting;
    private boolean cull;
    private boolean scissor;
    private int blendSrcRgb;
    private int blendDstRgb;
    private int blendSrcAlpha;
    private int blendDstAlpha;
    private int shadeModel;
    private boolean captured;

    /**
     * Reads the current GL state so that {@link #restore()} can put it back.
     */
    void capture() {
        blend = GL11.glIsEnabled(GL11.GL_BLEND);
        texture2d = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);

        ints.clear();
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, ints);
        for (int i = 0; i < 4; i++) scissorBox[i] = ints.get(i);
        floats.clear();
        GL11.glGetFloatv(GL11.GL_CURRENT_COLOR, floats);
        for (int i = 0; i < 4; i++) color[i] = floats.get(i);
        captured = true;
    }

    /**
     * Puts back everything read by {@link #capture()}; does nothing if nothing was captured.
     */
    void restore() {
        if (!captured) return;
        captured = false;
        setBlend(blend);
        GlStateManager.tryBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        if (texture2d) GlStateManager.enableTexture2D();
        else GlStateManager.disableTexture2D();
        if (alphaTest) GlStateManager.enableAlpha();
        else GlStateManager.disableAlpha();
        if (depthTest) GlStateManager.enableDepth();
        else GlStateManager.disableDepth();
        if (lighting) GlStateManager.enableLighting();
        else GlStateManager.disableLighting();
        if (cull) GlStateManager.enableCull();
        else GlStateManager.disableCull();
        GlStateManager.shadeModel(shadeModel);
        GlStateManager.color(color[0], color[1], color[2], color[3]);
        GL11.glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
        if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
        else GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /**
     * Puts GL into the state every UniGUI draw command assumes: alpha blending, smooth shading, no depth, lighting, culling or scissor.
     */
    void applyUiDefaults() {
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.enableBlend();
        blend(BlendMode.NORMAL);
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.enableTexture2D();
        // Vanilla leaves GL_FLAT active in the GUI: a triangle would take the color of its last vertex, which breaks
        // the per-vertex coverage alpha of anti-aliased paths and gradient meshes.
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /**
     * Brings GL back to the UI state after vanilla drawing code (items, entities, tooltips), which leaves lighting,
     * depth test, rescale-normal, color material and its own blend function enabled.
     */
    static void afterVanilla() {
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableColorMaterial();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(true);
        GlStateManager.enableBlend();
        blend(BlendMode.NORMAL);
        GlStateManager.disableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Clears the depth buffer of the bound framebuffer (inside the scissor box, if one is active), so that depth
     * written by earlier items or by the world can neither hide nor be hidden by the next 3D preview.
     */
    static void clearDepth() {
        GlStateManager.depthMask(true);
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Selects the untextured path: vertex colors only.
     */
    static void flat() {
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
    }

    /**
     * Selects the textured path: a bound texture modulated by vertex colors, without vanilla's alpha cut-off.
     */
    static void textured() {
        GlStateManager.enableTexture2D();
        GlStateManager.disableAlpha();
    }

    /**
     * Applies the blend equation of a UniGUI blend mode.
     *
     * @param mode requested mode, {@code null} means {@link BlendMode#NORMAL}
     */
    static void blend(BlendMode mode, boolean premultipliedSource) {
        GlStateManager.tryBlendFuncSeparate(
                CleanroomRenderMath.blendSrcRgb(mode, premultipliedSource), CleanroomRenderMath.blendDstRgb(mode),
                CleanroomRenderMath.blendSrcAlpha(mode), CleanroomRenderMath.blendDstAlpha(mode));
    }

    static void blend(BlendMode mode) {
        GlStateManager.tryBlendFuncSeparate(
                CleanroomRenderMath.blendSrcRgb(mode), CleanroomRenderMath.blendDstRgb(mode),
                CleanroomRenderMath.blendSrcAlpha(mode), CleanroomRenderMath.blendDstAlpha(mode));
    }

    /**
     * Enables the scissor test with the given {@code glScissor} rectangle.
     *
     * @param box {@code x, y, width, height} with a bottom-left origin
     */
    static void scissor(int[] box) {
        GL11.glScissor(box[0], box[1], box[2], box[3]);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }

    /**
     * Disables the scissor test.
     */
    static void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /**
     * Logs pending GL errors when {@code -Dunigui.glDebug=true} is set; free otherwise.
     *
     * @param where label that identifies the call site in the log
     */
    static void checkError(String where) {
        if (!DEBUG) return;
        for (int error = GL11.glGetError(); error != GL11.GL_NO_ERROR; error = GL11.glGetError()) {
            LOGGER.error("OpenGL error 0x{} after {}", Integer.toHexString(error), where);
        }
    }

    /**
     * Bindings a shader pass changes: the current program, the vertex array object, the {@code GL_ARRAY_BUFFER}
     * binding and the 2D texture of the active unit (restored through {@link GlStateManager} so its cache stays valid). Vanilla 1.12.2 never uses VAOs, so the previous VAO is
     * normally {@code 0}; it is captured anyway because other mods may bind one.
     */
    static final class ShaderBindings {
        private int program;
        private int vertexArray;
        private int arrayBuffer;
        private int texture;

        /**
         * Reads the current program, VAO, array buffer and 2D texture bindings.
         */
        void capture() {
            program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        }

        /**
         * Restores the bindings read by {@link #capture()}.
         */
        void restore() {
            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
            GlStateManager.bindTexture(texture);
        }
    }

    private static void setBlend(boolean enabled) {
        if (enabled) GlStateManager.enableBlend();
        else GlStateManager.disableBlend();
    }
}
