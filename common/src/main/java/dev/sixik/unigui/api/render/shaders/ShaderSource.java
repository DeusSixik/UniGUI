package dev.sixik.unigui.api.render.shaders;

import java.util.Objects;

/**
 * Исходный код shader'а, разрешённый для {@link ShaderHandle}.
 *
 * <p>Fragment source обязателен, vertex source опционален. Если vertex source отсутствует,
 * backend может использовать свой стандартный UI quad vertex shader.</p>
 */
public final class ShaderSource {
    private final String id;
    private final String vertexSource;
    private final String fragmentSource;

    private ShaderSource(String id, String vertexSource, String fragmentSource) {
        this.id = normalizeId(id);
        this.vertexSource = emptyToNull(vertexSource);
        this.fragmentSource = Objects.requireNonNull(fragmentSource, "fragmentSource");
        if (this.fragmentSource.isBlank()) {
            throw new IllegalArgumentException("Fragment shader source must not be blank");
        }
    }

    /**
     * Создаёт source только с fragment shader'ом.
     *
     * @param id id shader'а
     * @param fragmentSource GLSL-код fragment shader
     * @return новый source
     */
    public static ShaderSource fragment(String id, String fragmentSource) {
        return new ShaderSource(id, null, fragmentSource);
    }

    /**
     * Создаёт source с vertex и fragment shader'ами.
     *
     * @param id id shader'а
     * @param vertexSource GLSL vertex source или {@code null}
     * @param fragmentSource GLSL-код fragment shader
     * @return новый source
     */
    public static ShaderSource source(String id, String vertexSource, String fragmentSource) {
        return new ShaderSource(id, vertexSource, fragmentSource);
    }

    /** @return id источника shader source */
    public String id() {
        return id;
    }

    /** @return GLSL vertex source или {@code null} */
    public String vertexSource() {
        return vertexSource;
    }

    /** @return GLSL-код fragment shader */
    public String fragmentSource() {
        return fragmentSource;
    }

    /** @return {@code true}, если source содержит явный vertex shader */
    public boolean hasVertexSource() {
        return vertexSource != null && !vertexSource.isBlank();
    }

    /** @return независимая копия source */
    public ShaderSource copy() {
        return new ShaderSource(id, vertexSource, fragmentSource);
    }

    private static String normalizeId(String id) {
        String normalized = id == null ? "" : id.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Shader source id must not be empty");
        }
        return normalized;
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value;
    }
}