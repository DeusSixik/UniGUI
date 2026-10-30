package dev.sixik.unigui.api.render.shaders;

import dev.sixik.unigui.api.render.DrawCommandType;
import org.intellij.lang.annotations.Language;

import java.util.Objects;

/**
 * Идентификатор UI shader'а для команды {@link DrawCommandType#SHADER}.
 *
 * <p>Handle может быть ссылкой на resource id или контейнером embedded GLSL source. Backend сначала
 * смотрит, есть ли embedded fragment source, а если его нет - просит {@link ShaderProviders}
 * разрешить id через зарегистрированные providers.</p>
 */
public final class ShaderHandle {
    private final String id;
    private final String vertexSource;
    private final String fragmentSource;

    private ShaderHandle(String id, @Language("GLSL") String vertexSource, @Language("GLSL") String fragmentSource) {
        this.id = normalizeId(id);
        this.vertexSource = emptyToNull(vertexSource);
        this.fragmentSource = emptyToNull(fragmentSource);
    }

    /**
     * Создаёт handle на shader resource.
     *
     * @param id resource id shader'а
     * @return новый handle
     */
    public static ShaderHandle resource(String id) {
        return new ShaderHandle(id, null, null);
    }

    /**
     * Создаёт handle с embedded fragment source.
     *
     * @param id id shader'а
     * @param fragmentSource GLSL-код fragment shader
     * @return новый handle
     */
    public static ShaderHandle fragmentSource(String id, @Language("GLSL") String fragmentSource) {
        return new ShaderHandle(id, null, Objects.requireNonNull(fragmentSource, "fragmentSource"));
    }

    /**
     * Создаёт handle с embedded vertex и fragment source.
     *
     * @param id id shader'а
     * @param vertexSource GLSL-код vertex shader
     * @param fragmentSource GLSL-код fragment shader
     * @return новый handle
     */
    public static ShaderHandle source(String id, @Language("GLSL") String vertexSource, @Language("GLSL") String fragmentSource) {
        return new ShaderHandle(id,
                Objects.requireNonNull(vertexSource, "vertexSource"),
                Objects.requireNonNull(fragmentSource, "fragmentSource"));
    }

    /** @return shader id или resource id */
    public String id() {
        return id;
    }

    /** @return embedded vertex source или {@code null} */
    public String vertexSource() {
        return vertexSource;
    }

    /** @return embedded fragment source или {@code null} */
    public String fragmentSource() {
        return fragmentSource;
    }

    /** @return {@code true}, если handle содержит embedded vertex source */
    public boolean hasEmbeddedVertexSource() {
        return vertexSource != null;
    }

    /** @return {@code true}, если handle содержит embedded fragment source */
    public boolean hasEmbeddedFragmentSource() {
        return fragmentSource != null;
    }

    /** @return независимая копия handle */
    public ShaderHandle copy() {
        return new ShaderHandle(id, vertexSource, fragmentSource);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ShaderHandle other)) return false;
        return id.equals(other.id)
                && Objects.equals(vertexSource, other.vertexSource)
                && Objects.equals(fragmentSource, other.fragmentSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, vertexSource, fragmentSource);
    }

    @Override
    public String toString() {
        return id;
    }

    private static String normalizeId(String id) {
        String normalized = id == null ? "" : id.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Shader id must not be empty");
        }
        return normalized;
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isEmpty()) return null;
        return value;
    }
}