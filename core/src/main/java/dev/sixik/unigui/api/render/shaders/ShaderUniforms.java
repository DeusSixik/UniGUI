package dev.sixik.unigui.api.render.shaders;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
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
@AiDoc(
        summary = "Mutable collection of named shader parameters (floats, ints, vectors, colors) passed to GPU programs.",
        purpose = "Supplies procedural data, animation timers, screen resolutions, and tint colors to custom shaders in a fluent, safe container.",
        useWhen = {
                "Pass dynamic inputs to shaders in `DrawScope#shader` via `ShaderUniforms.create()`.",
                "Supply resolution (`setVec2`), time (`setFloat`), or colors (`setColor`) to fragment shaders."
        },
        criticalRules = {
                "Uniform values are defensively copied upon insertion; caller objects can be safely reused.",
                "Insertion order is preserved in an internal LinkedHashMap for consistent backend binding."
        },
        related = {"ShaderHandle", "ShaderUniform", "ShaderDrawOptions", "DrawScope"},
        examples = {
                @AiDoc.Example(
                        title = "Populate shader uniforms",
                        order = 10,
                        code = """
                                ShaderUniforms uniforms = ShaderUniforms.create()
                                        .setVec2("u_resolution", width, height)
                                        .setFloat("u_time", time)
                                        .setColor("u_tint", ColorView.CYAN);
                                """)
        })
@WikiPage(
        title = "<key:shader.uniforms.title>",
        slug = "shader-uniforms",
        category = "<key:shader.uniforms.category>",
        summary = "<key:shader.uniforms.summary>",
        order = 30)
@WikiSection(
        title = "<key:shader.uniforms.section.overview.title>",
        markdown = "<key:shader.uniforms.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:shader.uniforms.section.types.title>",
        markdown = "<key:shader.uniforms.section.types.body>",
        order = 20)
@WikiExample(
        title = "<key:shader.uniforms.example.title>",
        code = "<key:shader.uniforms.example.code>",
        order = 10)
public final class ShaderUniforms {
    private final LinkedHashMap<String, ShaderUniform> values = new LinkedHashMap<>();

    /**
     * @return пустой набор uniforms
     */
    @WikiMethod(
            title = "<key:shader.uniforms.method.empty.title>",
            summary = "<key:shader.uniforms.method.empty.summary>",
            section = "<key:shader.uniforms.api.section.factories>",
            sectionOrder = 10,
            order = 10)
    @WikiReturn("<key:shader.uniforms.method.empty.return>")
    public static ShaderUniforms empty() {
        return new ShaderUniforms();
    }

    /**
     * @return новый изменяемый набор uniforms
     */
    @WikiMethod(
            title = "<key:shader.uniforms.method.create.title>",
            summary = "<key:shader.uniforms.method.create.summary>",
            section = "<key:shader.uniforms.api.section.factories>",
            sectionOrder = 10,
            order = 20)
    @WikiReturn("<key:shader.uniforms.method.create.return>")
    public static ShaderUniforms create() {
        return new ShaderUniforms();
    }

    /**
     * Очищает uniforms без создания нового контейнера.
     */
    @WikiMethod(
            title = "<key:shader.uniforms.method.clear.title>",
            summary = "<key:shader.uniforms.method.clear.summary>",
            section = "<key:shader.uniforms.api.section.factories>",
            sectionOrder = 10,
            order = 30)
    @WikiReturn("<key:shader.uniforms.method.clear.return>")
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
    @WikiMethod(
            title = "<key:shader.uniforms.method.set_uniform.title>",
            summary = "<key:shader.uniforms.method.set_uniform.summary>",
            section = "<key:shader.uniforms.api.section.setters>",
            sectionOrder = 20,
            order = 10)
    @WikiReturn("<key:shader.uniforms.method.set_uniform.return>")
    public ShaderUniforms set(@WikiParameter(name = "name", description = "<key:shader.uniforms.param.name>") String name, @WikiParameter(name = "uniform", description = "<key:shader.uniforms.param.uniform>") ShaderUniform uniform) {
        values.put(normalizeName(name), Objects.requireNonNull(uniform, "uniform").copy());
        return this;
    }

    /**
     * @return этот набор после записи float uniform
     */
    @WikiMethod(
            title = "<key:shader.uniforms.method.set_float.title>",
            summary = "<key:shader.uniforms.method.set_float.summary>",
            section = "<key:shader.uniforms.api.section.setters>",
            sectionOrder = 20,
            order = 20)
    @WikiReturn("<key:shader.uniforms.method.set_float.return>")
    public ShaderUniforms setFloat(@WikiParameter(name = "name", description = "<key:shader.uniforms.param.name>") String name, @WikiParameter(name = "value", description = "<key:shader.uniforms.param.value>") float value) {
        return set(name, ShaderUniform.float1(value));
    }

    /**
     * @return этот набор после записи int uniform
     */
    @WikiMethod(
            title = "<key:shader.uniforms.method.set_int.title>",
            summary = "<key:shader.uniforms.method.set_int.summary>",
            section = "<key:shader.uniforms.api.section.setters>",
            sectionOrder = 20,
            order = 30)
    @WikiReturn("<key:shader.uniforms.method.set_int.return>")
    public ShaderUniforms setInt(@WikiParameter(name = "name", description = "<key:shader.uniforms.param.name>") String name, @WikiParameter(name = "value", description = "<key:shader.uniforms.param.value>") int value) {
        return set(name, ShaderUniform.int1(value));
    }

    /**
     * @return этот набор после записи vec2 uniform
     */
    @WikiMethod(
            title = "<key:shader.uniforms.method.set_vec2.title>",
            summary = "<key:shader.uniforms.method.set_vec2.summary>",
            section = "<key:shader.uniforms.api.section.setters>",
            sectionOrder = 20,
            order = 40)
    @WikiReturn("<key:shader.uniforms.method.set_vec2.return>")
    public ShaderUniforms setVec2(@WikiParameter(name = "name", description = "<key:shader.uniforms.param.name>") String name, @WikiParameter(name = "x", description = "<key:shader.uniforms.param.x>") float x, @WikiParameter(name = "y", description = "<key:shader.uniforms.param.y>") float y) {
        return set(name, ShaderUniform.vec2(x, y));
    }

    /**
     * @return этот набор после записи vec3 uniform
     */
    @WikiMethod(
            title = "<key:shader.uniforms.method.set_vec3.title>",
            summary = "<key:shader.uniforms.method.set_vec3.summary>",
            section = "<key:shader.uniforms.api.section.setters>",
            sectionOrder = 20,
            order = 50)
    @WikiReturn("<key:shader.uniforms.method.set_vec3.return>")
    public ShaderUniforms setVec3(@WikiParameter(name = "name", description = "<key:shader.uniforms.param.name>") String name, @WikiParameter(name = "x", description = "<key:shader.uniforms.param.x>") float x, @WikiParameter(name = "y", description = "<key:shader.uniforms.param.y>") float y, @WikiParameter(name = "z", description = "<key:shader.uniforms.param.z>") float z) {
        return set(name, ShaderUniform.vec3(x, y, z));
    }

    /**
     * @return этот набор после записи vec4 uniform
     */
    @WikiMethod(
            title = "<key:shader.uniforms.method.set_vec4.title>",
            summary = "<key:shader.uniforms.method.set_vec4.summary>",
            section = "<key:shader.uniforms.api.section.setters>",
            sectionOrder = 20,
            order = 60)
    @WikiReturn("<key:shader.uniforms.method.set_vec4.return>")
    public ShaderUniforms setVec4(@WikiParameter(name = "name", description = "<key:shader.uniforms.param.name>") String name, @WikiParameter(name = "x", description = "<key:shader.uniforms.param.x>") float x, @WikiParameter(name = "y", description = "<key:shader.uniforms.param.y>") float y, @WikiParameter(name = "z", description = "<key:shader.uniforms.param.z>") float z, @WikiParameter(name = "w", description = "<key:shader.uniforms.param.w>") float w) {
        return set(name, ShaderUniform.vec4(x, y, z, w));
    }

    /**
     * Записывает цвет как vec4 uniform.
     *
     * @param name имя uniform
     * @param color цвет или {@code null} для белого цвета
     * @return этот набор для fluent-настройки
     */
    @WikiMethod(
            title = "<key:shader.uniforms.method.set_color.title>",
            summary = "<key:shader.uniforms.method.set_color.summary>",
            section = "<key:shader.uniforms.api.section.setters>",
            sectionOrder = 20,
            order = 70)
    @WikiReturn("<key:shader.uniforms.method.set_color.return>")
    public ShaderUniforms setColor(@WikiParameter(name = "name", description = "<key:shader.uniforms.param.name>") String name, @WikiParameter(name = "color", description = "<key:shader.uniforms.param.color>") ColorView color) {
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
    @WikiMethod(
            title = "<key:shader.uniforms.method.set_color_argb.title>",
            summary = "<key:shader.uniforms.method.set_color_argb.summary>",
            section = "<key:shader.uniforms.api.section.setters>",
            sectionOrder = 20,
            order = 80)
    @WikiReturn("<key:shader.uniforms.method.set_color_argb.return>")
    public ShaderUniforms setColorArgb(@WikiParameter(name = "name", description = "<key:shader.uniforms.param.name>") String name, @WikiParameter(name = "argb", description = "<key:shader.uniforms.param.argb>") int argb) {
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
    @WikiMethod(
            title = "<key:shader.uniforms.method.set_mat4.title>",
            summary = "<key:shader.uniforms.method.set_mat4.summary>",
            section = "<key:shader.uniforms.api.section.setters>",
            sectionOrder = 20,
            order = 90)
    @WikiReturn("<key:shader.uniforms.method.set_mat4.return>")
    public ShaderUniforms setMat4(@WikiParameter(name = "name", description = "<key:shader.uniforms.param.name>") String name, @WikiParameter(name = "values", description = "<key:shader.uniforms.param.values>") float[] values) {
        return set(name, ShaderUniform.mat4(values));
    }

    /**
     * @return read-only view uniforms по имени
     */
    @WikiMethod(
            title = "<key:shader.uniforms.method.values.title>",
            summary = "<key:shader.uniforms.method.values.summary>",
            section = "<key:shader.uniforms.api.section.access>",
            sectionOrder = 30,
            order = 10)
    @WikiReturn("<key:shader.uniforms.method.values.return>")
    public Map<String, ShaderUniform> values() {
        return Collections.unmodifiableMap(values);
    }

    /**
     * @return {@code true}, если uniforms отсутствуют
     */
    @WikiMethod(
            title = "<key:shader.uniforms.method.is_empty.title>",
            summary = "<key:shader.uniforms.method.is_empty.summary>",
            section = "<key:shader.uniforms.api.section.access>",
            sectionOrder = 30,
            order = 20)
    @WikiReturn("<key:shader.uniforms.method.is_empty.return>")
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * @return независимая копия uniforms
     */
    @WikiMethod(
            title = "<key:shader.uniforms.method.copy.title>",
            summary = "<key:shader.uniforms.method.copy.summary>",
            section = "<key:shader.uniforms.api.section.factories>",
            sectionOrder = 10,
            order = 40)
    @WikiReturn("<key:shader.uniforms.method.copy.return>")
    public ShaderUniforms copy() {
        ShaderUniforms copy = new ShaderUniforms();
        for (Map.Entry<String, ShaderUniform> entry : values.entrySet()) {
            copy.values.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    /**
     * Копирует значения в уже существующий набор без создания новых
     * {@link ShaderUniform}. Uniform является неизменяемым, поэтому его
     * экземпляры можно безопасно переиспользовать во время replay.
     *
     * @param source источник значений; {@code null} очищает этот набор
     * @return этот набор uniforms
     */
    @WikiMethod(
            title = "<key:shader.uniforms.method.copy_from.title>",
            summary = "<key:shader.uniforms.method.copy_from.summary>",
            section = "<key:shader.uniforms.api.section.factories>",
            sectionOrder = 10,
            order = 50)
    @WikiReturn("<key:shader.uniforms.method.copy_from.return>")
    public ShaderUniforms copyFrom(@WikiParameter(name = "source", description = "<key:shader.uniforms.param.source>") ShaderUniforms source) {
        if (source == this) return this;
        values.clear();
        if (source != null && !source.values.isEmpty()) {
            values.putAll(source.values);
        }
        return this;
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Uniform name must not be empty");
        }
        return normalized;
    }
}
