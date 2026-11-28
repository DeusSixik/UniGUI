package dev.sixik.unigui.backend.minecraft_impl;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

/**
 * Compiles version-independent scene GLSL and assigns its attribute and block bindings.
 */
final class GlScenePrograms {
    private GlScenePrograms() {
    }

    /**
     * @param vertex     complete vertex GLSL
     * @param fragment   complete fragment GLSL
     * @param attributes vertex attribute names in buffer order
     * @return linked GL program
     * @throws IllegalStateException when compilation or linking fails
     */
    static int link(String vertex, String fragment, String... attributes) {
        int vs = compile(GL20.GL_VERTEX_SHADER, vertex);
        int fs = 0;
        int id = GL20.glCreateProgram();
        try {
            fs = compile(GL20.GL_FRAGMENT_SHADER, fragment);
            GL20.glAttachShader(id, vs);
            GL20.glAttachShader(id, fs);
            for (int i = 0; i < attributes.length; i++) GL20.glBindAttribLocation(id, i, attributes[i]);
            GL20.glLinkProgram(id);
            if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == 0) {
                throw new IllegalStateException(GL20.glGetProgramInfoLog(id));
            }
            return id;
        } catch (RuntimeException | Error failure) {
            GL20.glDeleteProgram(id);
            throw failure;
        } finally {
            GL20.glDeleteShader(vs);
            if (fs != 0) GL20.glDeleteShader(fs);
        }
    }

    /**
     * @param type   OpenGL shader stage
     * @param source complete GLSL
     * @return compiled shader
     * @throws IllegalStateException when the driver rejects the source
     */
    private static int compile(int type, String source) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, source);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) != 0) return id;
        String message = GL20.glGetShaderInfoLog(id);
        GL20.glDeleteShader(id);
        throw new IllegalStateException(message);
    }

    /**
     * @param program linked GL program
     * @param name    GLSL uniform block
     * @param binding indexed binding point
     */
    static void block(int program, String name, int binding) {
        int index = GL31.glGetUniformBlockIndex(program, name);
        if (index != GL31.GL_INVALID_INDEX) GL31.glUniformBlockBinding(program, index, binding);
    }
}
