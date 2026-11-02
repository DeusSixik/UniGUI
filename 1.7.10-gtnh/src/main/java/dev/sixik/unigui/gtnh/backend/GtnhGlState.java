package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.render.BlendMode;
import net.minecraft.client.renderer.RenderHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Captures the OpenGL state UniGUI touches and restores it after a frame.
 *
 * <p>Minecraft 1.7.10 has no {@code GlStateManager}: vanilla and mods call {@code GL11} directly and nothing keeps a
 * shadow copy that could go out of sync. The only contract is therefore "leave the state as you found it". The
 * fixed-function state UniGUI changes is read with a few {@code glGet*} calls in {@link #capture()} and written back
 * in {@link #restore()}; all of it is plain {@code GL11}/{@code GL14} from LWJGL 3 (this module is
 * {@code @Lwjgl3Aware}).</p>
 *
 * <p>An instance owns reusable buffers and is not thread safe: use it from the render thread only.</p>
 */
final class GtnhGlState {
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
    private int matrixMode;
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
        matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);

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
        set(GL11.GL_BLEND, blend);
        GL14.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        set(GL11.GL_TEXTURE_2D, texture2d);
        set(GL11.GL_ALPHA_TEST, alphaTest);
        set(GL11.GL_DEPTH_TEST, depthTest);
        set(GL11.GL_LIGHTING, lighting);
        set(GL11.GL_CULL_FACE, cull);
        GL11.glShadeModel(shadeModel);
        GL11.glColor4f(color[0], color[1], color[2], color[3]);
        GL11.glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
        set(GL11.GL_SCISSOR_TEST, scissor);
        GL11.glMatrixMode(matrixMode);
    }

    /**
     * Puts GL into the state every UniGUI draw command assumes: alpha blending, smooth shading, no depth, lighting,
     * culling, alpha test or scissor, modelview matrix selected.
     */
    void applyUiDefaults() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glEnable(GL11.GL_BLEND);
        blend(BlendMode.NORMAL);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        // Vanilla leaves GL_FLAT active in the GUI: a triangle would take the color of its last vertex, which breaks
        // the per-vertex coverage alpha of anti-aliased paths and gradient meshes.
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /**
     * Brings GL back to the UI state after vanilla drawing code (items, entities, tooltips), which leaves lighting,
     * depth test, rescale-normal, color material and its own blend function enabled.
     */
    static void afterVanilla() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_BLEND);
        blend(BlendMode.NORMAL);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Clears the depth buffer of the bound framebuffer (inside the scissor box, if one is active), so that depth
     * written by earlier items or by the world can neither hide nor be hidden by the next 3D preview.
     */
    static void clearDepth() {
        GL11.glDepthMask(true);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Selects the untextured path: vertex colors only.
     */
    static void flat() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
    }

    /**
     * Selects the textured path: a bound texture modulated by vertex colors, without vanilla's alpha cut-off.
     */
    static void textured() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
    }

    /**
     * Applies the blend equation of a UniGUI blend mode.
     *
     * @param mode                requested mode, {@code null} means {@link BlendMode#NORMAL}
     * @param premultipliedSource {@code true} if the source color is already multiplied by its alpha
     */
    static void blend(BlendMode mode, boolean premultipliedSource) {
        GL14.glBlendFuncSeparate(
            GtnhRenderMath.blendSrcRgb(mode, premultipliedSource), GtnhRenderMath.blendDstRgb(mode),
            GtnhRenderMath.blendSrcAlpha(mode), GtnhRenderMath.blendDstAlpha(mode));
    }

    static void blend(BlendMode mode) {
        GL14.glBlendFuncSeparate(
            GtnhRenderMath.blendSrcRgb(mode), GtnhRenderMath.blendDstRgb(mode),
            GtnhRenderMath.blendSrcAlpha(mode), GtnhRenderMath.blendDstAlpha(mode));
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
     * binding and the 2D texture of the active unit. Vanilla 1.7.10 never uses VAOs, so the previous VAO is normally
     * {@code 0}; it is captured anyway because other mods may bind one.
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
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }
    }

    private static void set(int capability, boolean enabled) {
        if (enabled) GL11.glEnable(capability);
        else GL11.glDisable(capability);
    }
}
