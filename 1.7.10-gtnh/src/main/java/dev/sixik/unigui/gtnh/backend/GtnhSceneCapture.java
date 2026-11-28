package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.backend.minecraft_impl.GlScenePlatform.Glint;
import dev.sixik.unigui.backend.minecraft_impl.GlScenePlatform.Surface;
import dev.sixik.unigui.backend.shared.LegacySceneQuads;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Captures compatibility-profile quads in the model's own coordinate space.
 */
public final class GtnhSceneCapture implements Mat4View {
    /**
     * Current capture on the render thread.
     */
    private static GtnhSceneCapture active;
    /**
     * Scratch buffer for the vanilla modelview, texture matrix or tint.
     */
    private final FloatBuffer matrix;
    /**
     * Reusable decoder for the native vertex format.
     */
    private final GtnhSceneVertices vertices = new GtnhSceneVertices();
    /**
     * Geometry grouped by draw state in submission order.
     */
    private final Map<Layer, LegacySceneQuads> layers = new LinkedHashMap<>();
    /**
     * True when a native batch requires the fixed-function fallback.
     */
    private boolean unsupported;
    /**
     * Single destination of a tessellator-only capture in tessellator coordinates, or null for model captures.
     */
    private LegacySceneQuads sink;

    /**
     * @param matrix stack-allocated storage for 16 floats
     */
    private GtnhSceneCapture(FloatBuffer matrix) {
        this.matrix = matrix;
    }

    /**
     * @return whether the tessellator is currently feeding a scene model
     */
    public static boolean active() {
        return active != null;
    }

    /**
     * @param model       model part rendered by vanilla
     * @param displayList original native list
     * @param scale       model units to blocks
     */
    public static void renderModel(ModelRenderer model, int displayList, float scale) {
        if (active == null) {
            GL11.glCallList(displayList);
            return;
        }
        for (Object box : model.cubeList) {
            ((ModelBox) box).render(Tessellator.instance, scale);
        }
    }

    /**
     * @param data     tessellator buffer valid until the draw returns
     * @param count    number of vertices
     * @param mode     OpenGL primitive mode
     * @param textured whether primary UVs are present
     * @param colored  whether RGBA bytes are present
     * @param normals  whether packed normals are present
     */
    public static void submit(int[] data, int count, int mode, boolean textured, boolean colored, boolean normals) {
        var capture = active;
        if (capture == null || capture.unsupported || count == 0) return;
        if (mode != GL11.GL_QUADS || count % 4 != 0) {
            capture.unsupported = true;
            return;
        }
        if (capture.sink != null) {
            // Block batches are positioned by the tessellator alone; the matrix buffer keeps the identity.
            capture.unsupported = !capture.vertices.append(capture.sink, data, count, capture, textured, colored,
                normals, 0xFFFFFFFF);
            return;
        }
        var layer = capture.layer(textured);
        int tint = 0xFFFFFFFF;
        if (!colored) {
            GL11.glGetFloatv(GL11.GL_CURRENT_COLOR, capture.matrix);
            tint = pack(capture.matrix.get(0), capture.matrix.get(1), capture.matrix.get(2), capture.matrix.get(3));
        }
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, capture.matrix);
        var quads = capture.layers.computeIfAbsent(layer, key -> new LegacySceneQuads());
        capture.unsupported = !capture.vertices.append(quads, data, count, capture, textured, colored, normals, tint);
    }

    /**
     * @param node        native item or entity owner
     * @param partialTick fractional render tick
     * @return captured surfaces, or null for the native draw fallback
     */
    static List<Surface> capture(SceneNode node, float partialTick) {
        if (active()) return null;
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int array = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        boolean discard = GL11.glIsEnabled(GL30.GL_RASTERIZER_DISCARD);
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var capture = new GtnhSceneCapture(stack.mallocFloat(16));
            GL11.glLoadIdentity();
            GL20.glUseProgram(0);
            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            // Suppress direct GL draws made by native renderers while streaming their tessellator batches.
            GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GtnhSceneContent.beginVanilla();
            GL11.glDisable(GL11.GL_BLEND);
            active = capture;
            if (!GtnhGlScenePlatform.draw(node, partialTick) || capture.unsupported) return null;
            return capture.surfaces();
        } finally {
            active = null;
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            if (!discard) GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, array);
            GL20.glUseProgram(program);
        }
    }

    /**
     * Collects the tessellator quads of a draw in the tessellator's own coordinates, without touching GL state.
     * Used for block shapes drawn by {@code RenderBlocks}, which only feed the tessellator.
     *
     * @param draw code that starts, fills and draws tessellator batches
     * @return all quads in submission order, or null when another capture is running, nothing was drawn or a batch
     * was not made of quads
     */
    static LegacySceneQuads captureTessellator(Runnable draw) {
        if (active()) return null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var capture = new GtnhSceneCapture(stack.mallocFloat(16));
            for (int i = 0; i < 16; i++) capture.matrix.put(i, i % 5 == 0 ? 1 : 0);
            capture.sink = new LegacySceneQuads();
            active = capture;
            draw.run();
            return capture.unsupported ? null : capture.sink;
        } finally {
            active = null;
        }
    }

    /**
     * @param textured whether the tessellator supplies primary UVs @return current native material state
     */
    private Layer layer(boolean textured) {
        int texture = textured && GL11.glIsEnabled(GL11.GL_TEXTURE_2D) ? GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) : 0;
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean cutout = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        float cutoff = cutout ? GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF) : 0;
        Glint glint = null;
        if (blend && GL11.glGetInteger(GL11.GL_DEPTH_FUNC) == GL11.GL_EQUAL
            && GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB) == GL11.GL_SRC_COLOR
            && GL11.glGetInteger(GL14.GL_BLEND_DST_RGB) == GL11.GL_ONE) {
            GL11.glGetFloatv(GL11.GL_TEXTURE_MATRIX, matrix);
            glint = new Glint(matrix.get(0), matrix.get(4), matrix.get(12), matrix.get(1), matrix.get(5), matrix.get(13));
        }
        boolean lit = glint == null && !(blend && !cutout && GL11.glGetInteger(GL14.GL_BLEND_DST_RGB) == GL11.GL_ONE);
        Material.Culling culling = Material.Culling.NONE;
        if (GL11.glIsEnabled(GL11.GL_CULL_FACE)) {
            culling = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE) == GL11.GL_FRONT
                ? Material.Culling.FRONT : Material.Culling.BACK;
        }
        return new Layer(texture, glint, lit, blend, write, cutout, cutoff, culling);
    }

    /**
     * @return complete indexed surfaces, or null when the capture produced no geometry
     */
    private List<Surface> surfaces() {
        List<Surface> result = new ArrayList<>(layers.size());
        layers.forEach((layer, quads) -> {
            var texture = layer.texture == 0 ? null : new SimpleTextureHandle(
                "unigui:captured_scene_texture", 0, 0, layer.texture);
            var mode = layer.blend ? Material.AlphaMode.TRANSLUCENT
                : layer.cutout ? Material.AlphaMode.CUTOUT : Material.AlphaMode.OPAQUE;
            var shading = layer.lit ? Material.Shading.LIT : Material.Shading.UNLIT;
            var material = new Material(0xFFFFFFFF, texture, shading, mode, layer.cutoff, layer.culling);
            result.add(new Surface(quads.build(), material, layer.glint, layer.blend && layer.write, layer.blend && layer.cutout));
        });
        return result.isEmpty() ? null : result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double get(int row, int column) {
        return matrix.get(4 * column + row);
    }

    /**
     * @param r red @param g green @param b blue @param a opacity @return packed ARGB
     */
    private static int pack(float r, float g, float b, float a) {
        return Math.round(a * 255) << 24 | Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
    }

    /**
     * Fixed-function texture, foil, lighting, alpha and cull state of a submitted batch.
     */
    private record Layer(int texture, Glint glint, boolean lit, boolean blend, boolean write,
                         boolean cutout, float cutoff, Material.Culling culling) {
    }
}
