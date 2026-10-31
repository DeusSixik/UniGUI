package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.BlendMode;
import dev.sixik.unigui.api.text.TextBrush;
import dev.sixik.unigui.cleanroom.backend.CleanroomGlCapabilities.ShaderTier;
import dev.sixik.unigui.impl.render.shaders.SdfTextShaderSources;
import net.minecraft.client.renderer.GlStateManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Draws SDF glyph quads (see {@link SdfTextShaderSources}) accumulated by the text layout.
 *
 * <p>Usage per text batch: {@link #ready()} once, then for every glyph {@link #beginCommand} +
 * {@link #quad}, and {@link #flush()} before any other drawing happens (vanilla text segments, other batches) so
 * draw order is preserved. Glyphs are grouped into one instanced draw per consecutive atlas page.</p>
 *
 * <p>Needs the {@link ShaderTier#INSTANCED} tier; on lower tiers {@link #ready()} is {@code false} and callers draw
 * with the vanilla font. Process-wide singleton because it owns context-bound GL objects: use {@link #shared()}.</p>
 */
final class CleanroomSdfTextRenderer {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static final boolean ENABLED = !"false".equalsIgnoreCase(System.getProperty("unigui.sdfText"));
    private static final int FLOATS = SdfTextShaderSources.FLOATS_PER_INSTANCE;
    private static CleanroomSdfTextRenderer shared;

    private final CleanroomGlState.ShaderBindings bindings = new CleanroomGlState.ShaderBindings();
    private final CleanroomSdfAtlas atlas = new CleanroomSdfAtlas();
    private final List<InstanceBatch> batches = new ArrayList<>();
    private final List<InstanceBatch> pool = new ArrayList<>();
    private final float[] cornerColor = new float[4];
    private FloatBuffer upload = BufferUtils.createFloatBuffer(FLOATS * 64);

    private CleanroomShaderProgram program;
    private int vao;
    private int quadBuffer;
    private int instanceBuffer;
    private boolean unavailable;

    // Per-command state set by beginCommand.
    private float[] affine;
    private ColorView paintColor;
    private RectView bounds;
    private float opacity = 1.0f;

    private CleanroomSdfTextRenderer() {
    }

    /**
     * @return the process-wide renderer
     */
    static synchronized CleanroomSdfTextRenderer shared() {
        if (shared == null) shared = new CleanroomSdfTextRenderer();
        return shared;
    }

    /**
     * @return the glyph source to hand to the layout
     */
    CleanroomTextLayout.Atlas atlas() {
        return atlas;
    }

    /**
     * Prepares the renderer (compiles the shader on first use).
     *
     * @return {@code false} if SDF text cannot be drawn in this context; use the vanilla font instead
     */
    boolean ready() {
        if (!ENABLED || unavailable) return false;
        if (CleanroomGlCapabilities.get().tier() != ShaderTier.INSTANCED) return false;
        if (program != null) return true;
        bindings.capture();
        try {
            return initialize();
        } catch (Throwable failure) {
            LOGGER.error("UniGUI SDF text renderer failed to initialize; the vanilla font will be used", failure);
            unavailable = true;
            return false;
        } finally {
            bindings.restore();
        }
    }

    /**
     * Sets the transform and color context for the quads that follow.
     *
     * @param affine     collapsed command transform {@code a, b, c, d, tx, ty}; the array is kept by reference
     * @param paintColor paint color of the command
     * @param bounds     bounds of the command, the reference frame of gradient brushes
     * @param opacity    extra alpha multiplier of the surface
     */
    void beginCommand(float[] affine, ColorView paintColor, RectView bounds, float opacity) {
        this.affine = affine;
        this.paintColor = paintColor;
        this.bounds = bounds;
        this.opacity = opacity;
    }

    /**
     * Appends one glyph quad in command-local coordinates.
     *
     * @param page     atlas page from {@link CleanroomTextLayout.Glyph#page()}
     * @param runColor color of the run, may be {@code null}
     * @param brush    brush of the run, may be {@code null}
     */
    void quad(Object page, float x, float y, float width, float height, float u0, float v0, float u1, float v1,
              ColorView runColor, TextBrush brush) {
        FloatArray out = batchFor(page).instances;
        out.ensure(out.size + FLOATS);
        float[] values = out.values;
        int index = out.size;
        index = position(values, index, x, y);
        index = position(values, index, x, y + height);
        index = position(values, index, x + width, y + height);
        index = position(values, index, x + width, y);
        values[index++] = u0;
        values[index++] = v0;
        values[index++] = u1;
        values[index++] = v1;
        index = color(values, index, x, y, runColor, brush);
        index = color(values, index, x, y + height, runColor, brush);
        index = color(values, index, x + width, y + height, runColor, brush);
        index = color(values, index, x + width, y, runColor, brush);
        out.size = index;
    }

    /**
     * Draws everything accumulated since the last flush and clears it.
     */
    void flush() {
        if (batches.isEmpty()) return;
        long started = CleanroomSdfStats.start();
        int glyphs = 0;
        int draws = 0;
        bindings.capture();
        try {
            CleanroomGlState.blend(BlendMode.NORMAL);
            program.use();
            CleanroomGlMatrices.upload(program);
            program.set("SdfSpread", (float) SdfTextShaderSources.SPREAD);
            program.set("GlyphAtlas", 0);
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceBuffer);
            for (int i = 0, size = batches.size(); i < size; i++) {
                InstanceBatch batch = batches.get(i);
                if (batch.instances.size == 0) continue;
                GlStateManager.bindTexture(CleanroomSdfAtlas.textureOf(batch.page));
                FloatBuffer data = stage(batch.instances);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STREAM_DRAW);
                int count = batch.instances.size / FLOATS;
                GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, 0, 6, count);
                glyphs += count;
                draws++;
            }
            CleanroomSdfStats.record(started, glyphs, draws, 4, draws > 0);
        } catch (Throwable failure) {
            LOGGER.error("UniGUI SDF text draw failed; SDF text is disabled", failure);
            unavailable = true;
        } finally {
            bindings.restore();
            CleanroomGlState.checkError("SDF text");
            batches.clear();
        }
    }

    private int position(float[] values, int index, float x, float y) {
        values[index++] = affine[0] * x + affine[2] * y + affine[4];
        values[index++] = affine[1] * x + affine[3] * y + affine[5];
        values[index++] = 0.0f;
        return index;
    }

    private int color(float[] values, int index, float x, float y, ColorView runColor, TextBrush brush) {
        CleanroomTextColors.rgba(cornerColor, 0, x, y, paintColor, runColor, brush, bounds, opacity);
        values[index++] = cornerColor[0];
        values[index++] = cornerColor[1];
        values[index++] = cornerColor[2];
        values[index++] = cornerColor[3];
        return index;
    }

    private InstanceBatch batchFor(Object page) {
        if (!batches.isEmpty()) {
            InstanceBatch last = batches.get(batches.size() - 1);
            if (last.page == page) return last;
        }
        int index = batches.size();
        InstanceBatch batch;
        if (index < pool.size()) {
            batch = pool.get(index);
            batch.page = page;
            batch.instances.size = 0;
        } else {
            batch = new InstanceBatch(page);
            pool.add(batch);
        }
        batches.add(batch);
        return batch;
    }

    private FloatBuffer stage(FloatArray values) {
        if (upload.capacity() < values.size) {
            upload = BufferUtils.createFloatBuffer(Math.max(values.size, upload.capacity() * 2));
        }
        upload.clear();
        upload.put(values.values, 0, values.size);
        upload.flip();
        return upload;
    }

    private boolean initialize() {
        String[] instanceNames = SdfTextShaderSources.INSTANCE_ATTRIBUTE_NAMES;
        String[] names = new String[1 + instanceNames.length];
        int[] locations = new int[names.length];
        names[0] = "Position";
        locations[0] = SdfTextShaderSources.POSITION_LOCATION;
        for (int i = 0; i < instanceNames.length; i++) {
            names[1 + i] = instanceNames[i];
            locations[1 + i] = SdfTextShaderSources.FIRST_INSTANCE_LOCATION + i;
        }
        CleanroomShaderProgram linked = CleanroomShaderProgram.link("sdf_text",
                SdfTextShaderSources.VERTEX, SdfTextShaderSources.FRAGMENT, names, locations);
        if (linked == null) {
            unavailable = true;
            return false;
        }

        vao = GL30.glGenVertexArrays();
        quadBuffer = GL15.glGenBuffers();
        instanceBuffer = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadBuffer);
        FloatBuffer quad = BufferUtils.createFloatBuffer(12);
        quad.put(0.0f).put(0.0f).put(0.0f).put(1.0f).put(1.0f).put(1.0f);
        quad.put(0.0f).put(0.0f).put(1.0f).put(1.0f).put(1.0f).put(0.0f).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, quad, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(SdfTextShaderSources.POSITION_LOCATION);
        GL20.glVertexAttribPointer(SdfTextShaderSources.POSITION_LOCATION, 2, GL11.GL_FLOAT, false,
                2 * Float.BYTES, 0L);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceBuffer);
        int stride = FLOATS * Float.BYTES;
        int offsetFloats = 0;
        boolean core33 = GL.getCapabilities().OpenGL33;
        int[] components = SdfTextShaderSources.INSTANCE_ATTRIBUTE_COMPONENTS;
        for (int i = 0; i < components.length; i++) {
            int location = SdfTextShaderSources.FIRST_INSTANCE_LOCATION + i;
            GL20.glEnableVertexAttribArray(location);
            GL20.glVertexAttribPointer(location, components[i], GL11.GL_FLOAT, false, stride,
                    (long) offsetFloats * Float.BYTES);
            if (core33) GL33.glVertexAttribDivisor(location, 1);
            else ARBInstancedArrays.glVertexAttribDivisorARB(location, 1);
            offsetFloats += components[i];
        }
        program = linked;
        LOGGER.info("UniGUI SDF text ready ({})", linked);
        return true;
    }

    private static final class InstanceBatch {
        private Object page;
        private final FloatArray instances = new FloatArray();

        private InstanceBatch(Object page) {
            this.page = page;
        }
    }

    private static final class FloatArray {
        private float[] values = new float[FLOATS * 64];
        private int size;

        private void ensure(int required) {
            if (required > values.length) values = Arrays.copyOf(values, Math.max(required, values.length * 2));
        }
    }
}
