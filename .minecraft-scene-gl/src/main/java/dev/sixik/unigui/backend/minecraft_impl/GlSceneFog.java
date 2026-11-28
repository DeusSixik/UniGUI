package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Frustum3D;
import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.FogVolume;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryStack;

import java.util.Arrays;

/**
 * Integrates shared volumetric fog at half resolution and composites it with depth-aware weights.
 */
final class GlSceneFog implements AutoCloseable {
    /**
     * Binding point for the camera and fog volume block.
     */
    private static final int BINDING = 4;
    /**
     * Shared fog collection and uniform staging.
     */
    private final SceneFogData data = new SceneFogData();
    /**
     * Integration shaders by spotlight bindings, zero until linked.
     */
    private final int[] programs = new int[8];
    /**
     * Composite program and triangle resources, zero before initialization.
     */
    private int composite, vao, triangle, uniforms;
    /**
     * Integrated fog color and transmittance.
     */
    private GlSceneTarget scratch;
    /**
     * State operations tracked by the client.
     */
    private final GlScenePlatform.State gl;

    /**
     * @param gl client GL state adapter
     */
    GlSceneFog(GlScenePlatform.State gl) {
        this.gl = gl;
    }

    /**
     * @param volume  scene fog descriptor
     * @param frustum camera visibility region
     */
    void collect(FogVolume volume, Frustum3D frustum) {
        data.collect(volume, frustum);
    }

    /**
     * Composites any visible fog into the existing framebuffer.
     *
     * @param overlay    color-only view used for the final pass
     * @param target     scene color and depth
     * @param clip       canonical world-to-clip camera transform
     * @param camera     active scene camera
     * @param seconds    scene clock in seconds
     * @param lights     frame light data for scattering
     * @param width      viewport width in pixels before rounding
     * @param height     viewport height in pixels before rounding
     * @param pixelsWide scissor width of the active viewport
     * @param pixelsHigh scissor height of the active viewport
     */
    void render(GlSceneOverlay overlay, GlScenePlatform.Target target, Mat4View clip, Camera3D camera,
                float seconds, GlSceneLighting lights, float width, float height, int pixelsWide, int pixelsHigh) {
        if (!data.prepare(clip)) return;
        int spot = data.scattering() ? lights.fogSpotlight() : -1;
        int flags = lights.fogVariant(spot);
        if (vao == 0) initialize();
        if (programs[flags] == 0) programs[flags] = program(false, flags, lights, spot);
        if (composite == 0) composite = program(true, 0, lights, -1);

        // Render-target allocation changes the framebuffer and texture binding. Do it before the post scope.
        int w = (pixelsWide + 1) / 2;
        int h = (pixelsHigh + 1) / 2;
        if (scratch == null || scratch.width() != w || scratch.height() != h) {
            var replaced = scratch;
            scratch = new GlSceneTarget(w, h, false);
            if (replaced != null) replaced.close();
        }

        try {
            overlay.begin(target, pixelsWide, pixelsHigh);
            var bytes = data.write(width / target.width(), height / target.height(), camera, seconds, spot);
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uniforms);
            GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, bytes);
            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, BINDING, uniforms);
            GL30.glBindVertexArray(vao);

            scratch.bind();
            gl.disableScissor();
            gl.blend(false);
            GL20.glUseProgram(programs[flags]);
            lights.linkFogSamplers(programs[flags], spot);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);

            overlay.bindOutput(target, pixelsWide, pixelsHigh);
            GL20.glUseProgram(composite);
            GlSceneOverlay.bindTexture(GlSceneOverlay.COLOR, scratch.color());
            gl.blend(true);
            gl.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        } finally {
            overlay.end();
        }
    }

    /**
     * Allocates uniform storage and the clip-space triangle.
     */
    private void initialize() {
        vao = GL30.glGenVertexArrays();
        triangle = GL15.glGenBuffers();
        uniforms = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, triangle);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, stack.floats(-1, -1, 0, 3, -1, 0, -1, 3, 0), GL15.GL_STATIC_DRAW);
        }
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uniforms);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, SceneFogShaders.UNIFORM_BYTES, GL15.GL_STREAM_DRAW);
    }

    /**
     * Links the integration or composite shader for OpenGL's forward scene depth.
     *
     * @param blend  true for compositing
     * @param flags  active integration shader resources
     * @param lights source of spotlight sampler locations
     * @param spot   active spotlight index, or -1
     * @return linked program
     */
    private static int program(boolean blend, int flags, GlSceneLighting lights, int spot) {
        int program = GlScenePrograms.link(SceneFogShaders.vertex(150, blend),
                blend ? SceneFogShaders.composite(150, true) : SceneFogShaders.integrate(150, true, flags), "Position");
        GL31.glUniformBlockBinding(program, GL31.glGetUniformBlockIndex(program, "SceneFog"), BINDING);
        GL20.glUseProgram(program);
        GlSceneOverlay.sampler(program, "FogSceneDepth", GlSceneOverlay.DEPTH);
        if (blend) GlSceneOverlay.sampler(program, "FogIntegrated", GlSceneOverlay.COLOR);
        if (spot >= 0) {
            GL31.glUniformBlockBinding(program, GL31.glGetUniformBlockIndex(program, "SceneLocalLights"),
                    GlSceneLighting.BINDING);
            lights.linkFogSamplers(program, spot);
        }
        return program;
    }

    /**
     * Clears volume selection between frames.
     */
    void reset() {
        data.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (scratch != null) scratch.close();
        scratch = null;
        if (triangle != 0) GL15.glDeleteBuffers(triangle);
        if (uniforms != 0) GL15.glDeleteBuffers(uniforms);
        if (vao != 0) GL30.glDeleteVertexArrays(vao);
        if (composite != 0) GL20.glDeleteProgram(composite);
        for (int id : programs) if (id != 0) GL20.glDeleteProgram(id);
        Arrays.fill(programs, 0);
        composite = vao = triangle = uniforms = 0;
    }
}
