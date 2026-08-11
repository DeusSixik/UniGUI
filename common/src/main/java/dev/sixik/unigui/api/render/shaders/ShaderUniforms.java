package dev.sixik.unigui.api.render.shaders;

import dev.sixik.unigui.api.math.ColorView;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ShaderUniforms {
    private final LinkedHashMap<String, ShaderUniform> values = new LinkedHashMap<>();

    public static ShaderUniforms empty() {
        return new ShaderUniforms();
    }

    public static ShaderUniforms create() {
        return new ShaderUniforms();
    }

    public ShaderUniforms set(String name, ShaderUniform uniform) {
        values.put(normalizeName(name), Objects.requireNonNull(uniform, "uniform").copy());
        return this;
    }

    public ShaderUniforms setFloat(String name, float value) {
        return set(name, ShaderUniform.float1(value));
    }

    public ShaderUniforms setInt(String name, int value) {
        return set(name, ShaderUniform.int1(value));
    }

    public ShaderUniforms setVec2(String name, float x, float y) {
        return set(name, ShaderUniform.vec2(x, y));
    }

    public ShaderUniforms setVec3(String name, float x, float y, float z) {
        return set(name, ShaderUniform.vec3(x, y, z));
    }

    public ShaderUniforms setVec4(String name, float x, float y, float z, float w) {
        return set(name, ShaderUniform.vec4(x, y, z, w));
    }

    public ShaderUniforms setColor(String name, ColorView color) {
        if (color == null) {
            return setVec4(name, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        return setVec4(name, color.r(), color.g(), color.b(), color.a());
    }

    public ShaderUniforms setColorArgb(String name, int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        float r = ((argb >>> 16) & 0xFF) / 255.0f;
        float g = ((argb >>> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        return setVec4(name, r, g, b, a);
    }

    public ShaderUniforms setMat4(String name, float[] values) {
        return set(name, ShaderUniform.mat4(values));
    }

    public Map<String, ShaderUniform> values() {
        return Collections.unmodifiableMap(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public ShaderUniforms copy() {
        ShaderUniforms copy = new ShaderUniforms();
        for (Map.Entry<String, ShaderUniform> entry : values.entrySet()) {
            copy.values.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Uniform name must not be empty");
        }
        return normalized;
    }
}