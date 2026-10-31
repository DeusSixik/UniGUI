package dev.sixik.unigui.gtnh.backend;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

/**
 * Feeds the fixed-function matrix stacks to shaders written for explicit {@code ModelViewMat} / {@code ProjMat}
 * uniforms. In 1.7.10 the GUI is positioned with {@code glTranslate / glScale} on the modelview stack and an
 * orthographic projection set by {@code Minecraft.setupOverlayRendering}, so both are read back from GL.
 *
 * <p>Uses a shared scratch buffer: call from the render thread only.</p>
 */
final class GtnhGlMatrices {
    private static final FloatBuffer BUFFER = BufferUtils.createFloatBuffer(16);

    private GtnhGlMatrices() {
    }

    /**
     * Uploads the current modelview and projection matrices to a program that is already in use.
     *
     * @param program current program; absent uniforms are skipped
     */
    static void upload(GtnhShaderProgram program) {
        upload(program.uniform("ModelViewMat"), GL11.GL_MODELVIEW_MATRIX);
        upload(program.uniform("ProjMat"), GL11.GL_PROJECTION_MATRIX);
    }

    private static void upload(int location, int matrixName) {
        if (location < 0) return;
        BUFFER.clear();
        GL11.glGetFloatv(matrixName, BUFFER);
        GL20.glUniformMatrix4fv(location, false, BUFFER);
    }
}
