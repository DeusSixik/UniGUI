package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.backend.minecraft_impl.GlScenePlatform.Glint;
import dev.sixik.unigui.backend.minecraft_impl.GlScenePlatform.Surface;
import dev.sixik.unigui.backend.shared.LegacySceneQuads;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
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
 * Captures posed compatibility-profile quads before Minecraft submits them to OpenGL.
 */
public final class CleanroomSceneCapture implements Mat4View {
    /**
     * Capture runs on the render thread and never nests.
     */
    private static CleanroomSceneCapture active;
    /**
     * Column-major modelview read from the current vanilla pose.
     */
    private final FloatBuffer matrix;
    private final CleanroomSceneVertices vertices = new CleanroomSceneVertices();
    /**
     * Vertices collected by their native render state, in first-submission order.
     */
    private final Map<Layer, LegacySceneQuads> layers = new LinkedHashMap<>();
    /**
     * True when the model could not be represented as indexed surfaces.
     */
    private boolean unsupported;

    /**
     * @param matrix scratch matrix storage valid for the scope of one capture
     */
    private CleanroomSceneCapture(FloatBuffer matrix) {
        this.matrix = matrix;
    }

    /**
     * @return whether a vanilla display list must stream its model boxes instead
     */
    public static boolean active() {
        return active != null;
    }

    /**
     * @param buffer completed vanilla vertex buffer
     * @return whether to consume the buffer without issuing its GL draw
     */
    public static boolean intercept(BufferBuilder buffer) {
        if (active == null) return false;
        try {
            if (!active.unsupported && buffer.getVertexCount() != 0) active.read(buffer);
        } finally {
            buffer.reset();
        }
        return true;
    }

    /**
     * @param node        item or entity being drawn at the local origin
     * @param partialTick fractional game tick
     * @return indexed surfaces, or null to draw it with its native renderer
     */
    static List<Surface> capture(SceneNode node, float partialTick) {
        if (active()) return null;
        int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int array = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean discard = GL11.glIsEnabled(GL30.GL_RASTERIZER_DISCARD);
        int activeUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int lightmap = OpenGlHelper.lightmapTexUnit;
        GlStateManager.setActiveTexture(lightmap);
        boolean lightmapEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        var state = new CleanroomGlState();
        state.capture();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var capture = new CleanroomSceneCapture(stack.mallocFloat(16));
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.loadIdentity();
                GL20.glUseProgram(0);
                GL30.glBindVertexArray(0);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                // Mod renderers may issue direct GL draws. They must never write into the enclosing target.
                GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
                CleanroomSceneContent.beginVanilla();
                GlStateManager.disableBlend();
                active = capture;
                if (!CleanroomGlScenePlatform.draw(node, partialTick) || capture.unsupported) return null;
                return capture.surfaces();
            } finally {
                active = null;
                CleanroomSceneContent.endVanilla();
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(mode);
                OpenGlHelper.setLightmapTextureCoords(lightmap, lightX, lightY);
                GlStateManager.setActiveTexture(lightmap);
                if (lightmapEnabled) GlStateManager.enableTexture2D();
                else GlStateManager.disableTexture2D();
                GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
                GlStateManager.bindTexture(texture);
                GlStateManager.depthMask(depthMask);
                GlStateManager.depthFunc(depthFunction);
                state.restore();
                GlStateManager.setActiveTexture(activeUnit);
                if (!discard) GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);
                GL30.glBindVertexArray(vao);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, array);
                GL20.glUseProgram(program);
            }
        }
    }

    /**
     * @param buffer completed vertex stream in the original quad order
     */
    private void read(BufferBuilder buffer) {
        int count = buffer.getVertexCount();
        if (buffer.getDrawMode() != GL11.GL_QUADS || count % 4 != 0) {
            unsupported = true;
            return;
        }
        if (!vertices.format(buffer.getVertexFormat())) {
            unsupported = true;
            return;
        }
        var layer = layer(vertices.textured());
        int tint = 0xFFFFFFFF;
        if (!vertices.colored()) {
            GL11.glGetFloatv(GL11.GL_CURRENT_COLOR, matrix);
            tint = pack(matrix.get(0), matrix.get(1), matrix.get(2), matrix.get(3));
        }
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, matrix);
        var quads = layers.computeIfAbsent(layer, key -> new LegacySceneQuads());
        unsupported = !vertices.append(quads, buffer.getByteBuffer(), count, this, tint);
    }

    /**
     * @param textured whether the submitted format supplies primary texture coordinates @return native layer state
     */
    private Layer layer(boolean textured) {
        int texture = textured && GL11.glIsEnabled(GL11.GL_TEXTURE_2D) ? GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) : 0;
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean cutout = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        float cutoff = cutout ? GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF) : 0;
        Glint glint = null;
        if (blend && !write && GL11.glGetInteger(GL11.GL_DEPTH_FUNC) == GL11.GL_EQUAL
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
     * @return indexed geometry and material of each captured layer
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
     * Native texture, foil transform, lighting, alpha and cull state at submission time.
     */
    private record Layer(int texture, Glint glint, boolean lit, boolean blend, boolean write,
                         boolean cutout, float cutoff, Material.Culling culling) {
    }
}
