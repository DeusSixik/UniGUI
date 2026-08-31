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
    private final int intId;

    private ShaderSource(String id, String vertexSource, String fragmentSource) {
        this.id = normalizeId(id);
        this.vertexSource = emptyToNull(vertexSource);
        this.fragmentSource = Objects.requireNonNull(fragmentSource, "fragmentSource");
        if (this.fragmentSource.isBlank()) {
            throw new IllegalArgumentException("Fragment shader source must not be blank");
        }
        this.intId = calculateIntId(this.id, this.vertexSource, this.fragmentSource);
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

    /**
     * Возвращает компактный ключ исходного кода shader'а.
     *
     * <p>Ключ вычисляется один раз при создании source и включает его id,
     * vertex source и fragment source. Это позволяет backend'ам использовать
     * primitive cache без конкатенации строк во время рендера.</p>
     *
     * @return стабильный 32-битный ключ исходного кода
     */
    public int intId() {
        return intId;
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

    private static int calculateIntId(String id, String vertexSource, String fragmentSource) {
        int hash = 0x811C9DC5;
        hash = hashString(hash, id);
        hash = hashString(hash, vertexSource);
        hash = hashString(hash, fragmentSource);
        hash ^= hash >>> 16;
        hash *= 0x85EBCA6B;
        hash ^= hash >>> 13;
        hash *= 0xC2B2AE35;
        hash ^= hash >>> 16;
        return hash == 0 ? 1 : hash;
    }

    private static int hashString(int hash, String value) {
        if (value == null) {
            return hash * 16777619 ^ 0xFF;
        }
        hash = hash * 16777619 ^ value.length();
        for (int i = 0; i < value.length(); i++) {
            hash = hash * 16777619 ^ value.charAt(i);
        }
        return hash;
    }
}
