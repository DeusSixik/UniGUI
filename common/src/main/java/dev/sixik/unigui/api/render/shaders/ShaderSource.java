package dev.sixik.unigui.api.render.shaders;

import java.util.Objects;

/**
 * Source code resolved for a {@link ShaderHandle}.
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

    public static ShaderSource fragment(String id, String fragmentSource) {
        return new ShaderSource(id, null, fragmentSource);
    }

    public static ShaderSource source(String id, String vertexSource, String fragmentSource) {
        return new ShaderSource(id, vertexSource, fragmentSource);
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

    public boolean hasVertexSource() {
        return vertexSource != null && !vertexSource.isBlank();
    }

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