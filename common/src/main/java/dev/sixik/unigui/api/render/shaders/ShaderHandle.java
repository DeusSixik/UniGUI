package dev.sixik.unigui.api.render.shaders;

import dev.sixik.unigui.api.render.DrawCommandType;

import java.util.Objects;

/**
 * Identifies a UI shader used by a {@link DrawCommandType#SHADER} command.
 *
 * <p>Backends may resolve {@link #id()} as a resource id, or compile the embedded
 * sources when {@link #vertexSource()} / {@link #fragmentSource()} are provided.</p>
 */
public final class ShaderHandle {
    private final String id;
    private final String vertexSource;
    private final String fragmentSource;

    private ShaderHandle(String id, String vertexSource, String fragmentSource) {
        this.id = normalizeId(id);
        this.vertexSource = emptyToNull(vertexSource);
        this.fragmentSource = emptyToNull(fragmentSource);
    }

    public static ShaderHandle resource(String id) {
        return new ShaderHandle(id, null, null);
    }

    public static ShaderHandle fragmentSource(String id, String fragmentSource) {
        return new ShaderHandle(id, null, Objects.requireNonNull(fragmentSource, "fragmentSource"));
    }

    public static ShaderHandle source(String id, String vertexSource, String fragmentSource) {
        return new ShaderHandle(id,
                Objects.requireNonNull(vertexSource, "vertexSource"),
                Objects.requireNonNull(fragmentSource, "fragmentSource"));
    }

    public String id() {
        return id;
    }

    public String vertexSource() {
        return vertexSource;
    }

    public String fragmentSource() {
        return fragmentSource;
    }

    public boolean hasEmbeddedVertexSource() {
        return vertexSource != null;
    }

    public boolean hasEmbeddedFragmentSource() {
        return fragmentSource != null;
    }

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