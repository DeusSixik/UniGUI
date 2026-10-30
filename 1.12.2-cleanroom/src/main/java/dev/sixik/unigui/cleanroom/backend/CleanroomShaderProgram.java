package dev.sixik.unigui.cleanroom.backend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.HashMap;
import java.util.Map;

/**
 * A linked OpenGL shader program with a uniform location cache.
 *
 * <p>Compilation or link errors are logged once with the driver's info log and reported by returning {@code null}
 * from {@link #link}; callers treat that as "this render path is unavailable" and fall back to tessellation, the
 * same way the 1.20 backend marks its SDF renderers {@code unavailable}.</p>
 */
final class CleanroomShaderProgram implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static final int NOT_CACHED = Integer.MIN_VALUE;

    private final int id;
    private final String label;
    private final Map<String, Integer> uniforms = new HashMap<>();

    private CleanroomShaderProgram(int id, String label) {
        this.id = id;
        this.label = label;
    }

    /**
     * Compiles and links a program, binding attribute {@code names[i]} to location {@code i}.
     *
     * @param label          name used in log messages
     * @param vertexSource   GLSL vertex shader
     * @param fragmentSource GLSL fragment shader
     * @param attributeNames attribute names; the array index is the attribute location
     * @return linked program, or {@code null} if compilation or linking failed
     */
    static CleanroomShaderProgram link(String label, String vertexSource, String fragmentSource,
                                       String[] attributeNames) {
        int[] locations = new int[attributeNames.length];
        for (int i = 0; i < locations.length; i++) locations[i] = i;
        return link(label, vertexSource, fragmentSource, attributeNames, locations);
    }

    /**
     * Compiles and links a program with explicit attribute locations.
     *
     * @param label              name used in log messages
     * @param vertexSource       GLSL vertex shader
     * @param fragmentSource     GLSL fragment shader
     * @param attributeNames     attribute names to bind
     * @param attributeLocations location of each attribute, parallel to {@code attributeNames}
     * @return linked program, or {@code null} if compilation or linking failed
     */
    static CleanroomShaderProgram link(String label, String vertexSource, String fragmentSource,
                                       String[] attributeNames, int[] attributeLocations) {
        int vertex = compile(label, GL20.GL_VERTEX_SHADER, vertexSource);
        int fragment = compile(label, GL20.GL_FRAGMENT_SHADER, fragmentSource);
        if (vertex == 0 || fragment == 0) {
            if (vertex != 0) GL20.glDeleteShader(vertex);
            if (fragment != 0) GL20.glDeleteShader(fragment);
            return null;
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
        for (int i = 0; i < attributeNames.length; i++) {
            GL20.glBindAttribLocation(program, attributeLocations[i], attributeNames[i]);
        }
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            LOGGER.error("Failed to link UniGUI shader '{}': {}", label, GL20.glGetProgramInfoLog(program));
            GL20.glDeleteProgram(program);
            return null;
        }
        return new CleanroomShaderProgram(program, label);
    }

    /**
     * @return OpenGL program object name
     */
    int id() {
        return id;
    }

    /**
     * Makes this program current.
     */
    void use() {
        GL20.glUseProgram(id);
    }

    /**
     * Looks up (and caches) a uniform location.
     *
     * @param name uniform name
     * @return location, or {@code -1} if the uniform is absent or optimized out
     */
    int uniform(String name) {
        Integer cached = uniforms.get(name);
        if (cached != null && cached != NOT_CACHED) return cached;
        int location = GL20.glGetUniformLocation(id, name);
        uniforms.put(name, location);
        return location;
    }

    /**
     * Uploads a {@code float} uniform; silently ignored if the uniform does not exist.
     */
    void set(String name, float value) {
        int location = uniform(name);
        if (location >= 0) GL20.glUniform1f(location, value);
    }

    /**
     * Uploads an {@code int} uniform; silently ignored if the uniform does not exist.
     */
    void set(String name, int value) {
        int location = uniform(name);
        if (location >= 0) GL20.glUniform1i(location, value);
    }

    /**
     * Uploads a {@code vec2} uniform; silently ignored if the uniform does not exist.
     */
    void set(String name, float x, float y) {
        int location = uniform(name);
        if (location >= 0) GL20.glUniform2f(location, x, y);
    }

    /**
     * Uploads a {@code vec4} uniform; silently ignored if the uniform does not exist.
     */
    void set(String name, float x, float y, float z, float w) {
        int location = uniform(name);
        if (location >= 0) GL20.glUniform4f(location, x, y, z, w);
    }

    @Override
    public void close() {
        GL20.glDeleteProgram(id);
        uniforms.clear();
    }

    @Override
    public String toString() {
        return "CleanroomShaderProgram[" + label + "#" + id + "]";
    }

    private static int compile(String label, int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            LOGGER.error("Failed to compile UniGUI {} shader '{}': {}",
                    type == GL20.GL_VERTEX_SHADER ? "vertex" : "fragment", label, GL20.glGetShaderInfoLog(shader));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}
