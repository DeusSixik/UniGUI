package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.gtnh.backend.GtnhGlCapabilities.ShaderTier;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.impl.render.DrawBatch;
import dev.sixik.unigui.impl.render.shaders.SdfShapeShaderSources;
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

/**
 * Draws rounded rectangles, ellipses and lines with the shared SDF shader (see {@link SdfShapeShaderSources}).
 *
 * <p>One quad per shape; edges are anti-aliased in the fragment shader. In the {@link ShaderTier#INSTANCED} tier a
 * whole batch is a single instanced draw call; in {@link ShaderTier#BASIC} each shape is drawn separately with
 * uniforms. Any failure marks the path unavailable and returns {@code false} so the caller can fall back to
 * tessellated geometry. Disable with {@code -Dunigui.sdf=false} to compare against the fallback.</p>
 *
 * <p>The renderer owns OpenGL objects tied to the context, so it is a process-wide singleton: use {@link #shared()}.</p>
 */
final class GtnhSdfShapeRenderer {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static final boolean ENABLED = !"false".equalsIgnoreCase(System.getProperty("unigui.sdf"));
    private static final int INITIAL_CAPACITY = 256;
    private static final int BASIC_VERTEX_FLOATS = 5;
    private static final int FLOAT_BYTES = Float.BYTES;
    private static GtnhSdfShapeRenderer shared;

    private final GtnhGlState.ShaderBindings bindings = new GtnhGlState.ShaderBindings();
    private final FloatBuffer basicVertices = BufferUtils.createFloatBuffer(4 * BASIC_VERTEX_FLOATS);
    private final float[] scratch = new float[2];
    private FloatBuffer instanceData = BufferUtils.createFloatBuffer(INITIAL_CAPACITY * SdfShapeShaderSources.INSTANCE_FLOATS);

    private GtnhShaderProgram instancedProgram;
    private int instancedVao;
    private int quadBuffer;
    private int instanceBuffer;
    private boolean instancedUnavailable;

    private GtnhShaderProgram basicProgram;
    private int basicVao;
    private int basicBuffer;
    private boolean basicUnavailable;

    private GtnhSdfShapeRenderer() {
    }

    /**
     * @return the process-wide renderer instance
     */
    static synchronized GtnhSdfShapeRenderer shared() {
        if (shared == null) shared = new GtnhSdfShapeRenderer();
        return shared;
    }

    /**
     * Tries to render a batch of shape commands.
     *
     * @param batch   batch from the draw batcher
     * @param opacity extra alpha multiplier of the surface
     * @return {@code true} if the batch was fully drawn; {@code false} if the caller must draw it itself
     */
    boolean render(DrawBatch batch, float opacity) {
        if (!ENABLED || !GtnhSdfShapeInstances.shouldRender(batch)) return false;
        ShaderTier tier = GtnhGlCapabilities.get().tier();
        if (tier != ShaderTier.INSTANCED && tier != ShaderTier.BASIC) return false;

        long started = GtnhSdfStats.start();
        int count = pack(batch, opacity);
        if (count == 0) return true;

        bindings.capture();
        try {
            GtnhGlState.blend(batch.blendMode());
            if (tier == ShaderTier.INSTANCED && !instancedUnavailable) {
                if (drawInstanced(count)) {
                    GtnhSdfStats.record(started, count, 1, 2, true);
                    return true;
                }
            }
            if (!basicUnavailable) {
                boolean drawn = drawBasic(count);
                // Per shape: eight uniforms and one buffer upload, plus the shared matrices.
                if (drawn) GtnhSdfStats.record(started, count, count, count * 8 + 2, true);
                return drawn;
            }
            return false;
        } catch (Throwable failure) {
            LOGGER.error("UniGUI SDF shape renderer failed; falling back to tessellated shapes", failure);
            instancedUnavailable = true;
            basicUnavailable = true;
            return false;
        } finally {
            bindings.restore();
            GtnhGlState.checkError("SDF shapes");
        }
    }

    private int pack(DrawBatch batch, float opacity) {
        int size = batch.size();
        int required = size * SdfShapeShaderSources.INSTANCE_FLOATS;
        if (required > instanceData.capacity()) {
            int capacity = instanceData.capacity();
            while (capacity < required) capacity *= 2;
            instanceData = BufferUtils.createFloatBuffer(capacity);
        }
        instanceData.clear();
        Object[] commands = batch.commandElements();
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (GtnhSdfShapeInstances.pack(instanceData, (DrawCommand) commands[i], opacity, scratch)) count++;
        }
        instanceData.flip();
        return count;
    }

    private boolean drawInstanced(int count) {
        if (!initializeInstanced()) return false;
        instancedProgram.use();
        GtnhGlMatrices.upload(instancedProgram);
        GL30.glBindVertexArray(instancedVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceBuffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, instanceData, GL15.GL_STREAM_DRAW);
        GL31.glDrawArraysInstanced(GL11.GL_TRIANGLE_STRIP, 0, 4, count);
        return true;
    }

    /**
     * Draws each packed instance as one quad with uniforms, reusing the instance data layout.
     */
    private boolean drawBasic(int count) {
        if (!initializeBasic()) return false;
        basicProgram.use();
        GtnhGlMatrices.upload(basicProgram);
        GL30.glBindVertexArray(basicVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, basicBuffer);
        int stride = SdfShapeShaderSources.INSTANCE_FLOATS;
        for (int i = 0; i < count; i++) {
            int base = i * stride;
            float localX1 = instanceData.get(base + 12);
            float localY1 = instanceData.get(base + 13);
            float localX2 = instanceData.get(base + 14);
            float localY2 = instanceData.get(base + 15);
            basicVertices.clear();
            putBasicVertex(base, 0, localX1, localY1);
            putBasicVertex(base, 3, localX1, localY2);
            putBasicVertex(base, 6, localX2, localY2);
            putBasicVertex(base, 9, localX2, localY1);
            basicVertices.flip();

            basicProgram.set("Color", instanceData.get(base + 16), instanceData.get(base + 17),
                instanceData.get(base + 18), instanceData.get(base + 19));
            basicProgram.set("Size", instanceData.get(base + 20), instanceData.get(base + 21));
            basicProgram.set("LineStart", instanceData.get(base + 22), instanceData.get(base + 23));
            basicProgram.set("LineEnd", instanceData.get(base + 24), instanceData.get(base + 25));
            basicProgram.set("Radius", instanceData.get(base + 26));
            basicProgram.set("StrokeWidth", instanceData.get(base + 27));
            basicProgram.set("ShapeType", Math.round(instanceData.get(base + 28)));
            basicProgram.set("IsStroke", Math.round(instanceData.get(base + 29)));
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, basicVertices, GL15.GL_STREAM_DRAW);
            GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        }
        return true;
    }

    private void putBasicVertex(int base, int positionOffset, float u, float v) {
        basicVertices.put(instanceData.get(base + positionOffset))
            .put(instanceData.get(base + positionOffset + 1))
            .put(instanceData.get(base + positionOffset + 2))
            .put(u).put(v);
    }

    private boolean initializeInstanced() {
        if (instancedProgram != null) return true;
        if (instancedUnavailable) return false;
        GtnhShaderProgram program = GtnhShaderProgram.link("sdf_shapes_instanced",
            SdfShapeShaderSources.INSTANCED_VERTEX, SdfShapeShaderSources.INSTANCED_FRAGMENT,
            SdfShapeShaderSources.INSTANCED_ATTRIBUTE_NAMES);
        if (program == null) {
            instancedUnavailable = true;
            return false;
        }

        instancedVao = GL30.glGenVertexArrays();
        quadBuffer = GL15.glGenBuffers();
        instanceBuffer = GL15.glGenBuffers();
        GL30.glBindVertexArray(instancedVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadBuffer);
        FloatBuffer quad = BufferUtils.createFloatBuffer(8);
        quad.put(0.0f).put(0.0f).put(0.0f).put(1.0f).put(1.0f).put(0.0f).put(1.0f).put(1.0f).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, quad, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * FLOAT_BYTES, 0L);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceBuffer);
        int stride = SdfShapeShaderSources.INSTANCE_FLOATS * FLOAT_BYTES;
        int offsetFloats = 0;
        int[] components = SdfShapeShaderSources.INSTANCED_ATTRIBUTE_COMPONENTS;
        boolean core33 = GL.getCapabilities().OpenGL33;
        for (int i = 0; i < components.length; i++) {
            int location = 1 + i;
            GL20.glEnableVertexAttribArray(location);
            GL20.glVertexAttribPointer(location, components[i], GL11.GL_FLOAT, false, stride,
                (long) offsetFloats * FLOAT_BYTES);
            if (core33) GL33.glVertexAttribDivisor(location, 1);
            else ARBInstancedArrays.glVertexAttribDivisorARB(location, 1);
            offsetFloats += components[i];
        }
        instancedProgram = program;
        LOGGER.info("UniGUI SDF shapes: instanced path ready ({})", program);
        return true;
    }

    private boolean initializeBasic() {
        if (basicProgram != null) return true;
        if (basicUnavailable) return false;
        GtnhShaderProgram program = GtnhShaderProgram.link("sdf_shapes_basic",
            SdfShapeShaderSources.VERTEX, SdfShapeShaderSources.FRAGMENT,
            SdfShapeShaderSources.BASIC_ATTRIBUTE_NAMES);
        if (program == null) {
            basicUnavailable = true;
            return false;
        }
        basicVao = GL30.glGenVertexArrays();
        basicBuffer = GL15.glGenBuffers();
        GL30.glBindVertexArray(basicVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, basicBuffer);
        int stride = BASIC_VERTEX_FLOATS * FLOAT_BYTES;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 3L * FLOAT_BYTES);
        basicProgram = program;
        LOGGER.info("UniGUI SDF shapes: basic path ready ({})", program);
        return true;
    }
}
