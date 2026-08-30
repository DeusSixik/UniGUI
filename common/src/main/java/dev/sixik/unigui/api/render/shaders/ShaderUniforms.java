package dev.sixik.unigui.api.render.shaders;

import dev.sixik.unigui.api.math.ColorView;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Изменяемый набор shader uniforms для одной draw-команды.
 *
 * <p>Имена uniforms сохраняются в порядке добавления, что полезно для diagnostics и стабильного
 * backend upload. Значения копируются при записи, поэтому внешний код может переиспользовать
 * временные uniform objects.</p>
 */
public final class ShaderUniforms {
    private final LinkedHashMap<String, ShaderUniform> values = new LinkedHashMap<>();

    /** @return пустой набор uniforms */
    public static ShaderUniforms empty() {
        return new ShaderUniforms();
    }

    /** @return новый изменяемый набор uniforms */
    public static ShaderUniforms create() {
        return new ShaderUniforms();
    }

    /** Очищает uniforms без создания нового контейнера. */
    public ShaderUniforms clear() {
        values.clear();
        return this;
    }

    /**
     * Записывает uniform по имени.
     *
     * @param name имя uniform в shader source
     * @param uniform значение uniform
     * @return этот набор для fluent-настройки
     */
    public ShaderUniforms set(String name, ShaderUniform uniform) {
        values.put(normalizeName(name), Objects.requireNonNull(uniform, "uniform").copy());
        return this;
    }

    /** @return этот набор после записи float uniform */
    public ShaderUniforms setFloat(String name, float value) {
        return set(name, ShaderUniform.float1(value));
    }

    /** @return этот набор после записи int uniform */
    public ShaderUniforms setInt(String name, int value) {
        return set(name, ShaderUniform.int1(value));
    }

    /** @return этот набор после записи vec2 uniform */
    public ShaderUniforms setVec2(String name, float x, float y) {
        return set(name, ShaderUniform.vec2(x, y));
    }

    /** @return этот набор после записи vec3 uniform */
    public ShaderUniforms setVec3(String name, float x, float y, float z) {
        return set(name, ShaderUniform.vec3(x, y, z));
    }

    /** @return этот набор после записи vec4 uniform */
    public ShaderUniforms setVec4(String name, float x, float y, float z, float w) {
        return set(name, ShaderUniform.vec4(x, y, z, w));
    }

    /**
     * Записывает цвет как vec4 uniform.
     *
     * @param name имя uniform
     * @param color цвет или {@code null} для белого цвета
     * @return этот набор для fluent-настройки
     */
    public ShaderUniforms setColor(String name, ColorView color) {
        if (color == null) {
            return setVec4(name, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        return setVec4(name, color.r(), color.g(), color.b(), color.a());
    }

    /**
     * Записывает ARGB integer как vec4 uniform.
     *
     * @param name имя uniform
     * @param argb цвет в формате ARGB
     * @return этот набор для fluent-настройки
     */
    public ShaderUniforms setColorArgb(String name, int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        float r = ((argb >>> 16) & 0xFF) / 255.0f;
        float g = ((argb >>> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        return setVec4(name, r, g, b, a);
    }

    /**
     * Записывает mat4 uniform.
     *
     * @param name имя uniform
     * @param values ровно 16 float-значений
     * @return этот набор для fluent-настройки
     */
    public ShaderUniforms setMat4(String name, float[] values) {
        return set(name, ShaderUniform.mat4(values));
    }

    /** @return read-only view uniforms по имени */
    public Map<String, ShaderUniform> values() {
        return Collections.unmodifiableMap(values);
    }

    /** @return {@code true}, если uniforms отсутствуют */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** @return независимая копия uniforms */
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
