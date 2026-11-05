package dev.sixik.unigui.api.render.shaders;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Backend-facing параметры shader draw-команды.
 *
 * <p>Options управляют тем, должен ли backend добавить стандартные uniforms, включить blending и
 * как сместить автоматически передаваемый {@code SquareVertex}. Это отдельный объект, чтобы
 * {@link dev.sixik.unigui.api.render.DrawCommand} оставался backend-neutral.</p>
 */
@AiDoc(
        summary = "Backend-facing configuration for shader draw commands governing blending, built-in uniforms, and vertex offsets.",
        purpose = "Decouples shader hardware state parameters from draw commands, enabling zero-allocation pooling.",
        useWhen = {
                "Pass into `DrawScope#shader` to configure blending and vertex offset.",
                "Toggle `builtinUniforms` to control automatic injection of projection matrices and SquareVertex."
        },
        criticalRules = {
                "`squareVertexOffset` defaults to -0.25f to preserve compatibility with existing quad shaders.",
                "Options are snapshotted or pooled during command replay."
        },
        related = {"ShaderHandle", "ShaderUniforms", "DrawScope"},
        examples = {
                @AiDoc.Example(
                        title = "Configure blending and built-in uniforms",
                        order = 10,
                        code = """
                                ShaderDrawOptions options = ShaderDrawOptions.defaults()
                                        .blend(true)
                                        .builtinUniforms(true);
                                """)
        })
@WikiPage(
        title = "<key:shader.draw.options.title>",
        slug = "shader-draw-options",
        category = "<key:shader.draw.options.category>",
        summary = "<key:shader.draw.options.summary>",
        order = 20)
@WikiSection(
        title = "<key:shader.draw.options.section.overview.title>",
        markdown = "<key:shader.draw.options.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:shader.draw.options.section.builtins.title>",
        markdown = "<key:shader.draw.options.section.builtins.body>",
        order = 20)
@WikiExample(
        title = "<key:shader.draw.options.example.title>",
        code = "<key:shader.draw.options.example.code>",
        order = 10)
public final class ShaderDrawOptions {
    private boolean builtinUniforms = true;
    private boolean blend = true;
    private float squareVertexOffset = -0.25f;

    /**
     * @return новый options object со стандартными значениями
     */
    @WikiMethod(
            title = "<key:shader.draw.options.method.defaults.title>",
            summary = "<key:shader.draw.options.method.defaults.summary>",
            section = "<key:shader.draw.options.api.section.factories>",
            sectionOrder = 10,
            order = 10)
    @WikiReturn("<key:shader.draw.options.method.defaults.return>")
    public static ShaderDrawOptions defaults() {
        return new ShaderDrawOptions();
    }

    /**
     * @return {@code true}, если backend должен добавить стандартные uniforms
     */
    @WikiMethod(
            title = "<key:shader.draw.options.method.builtin_uniforms_get.title>",
            summary = "<key:shader.draw.options.method.builtin_uniforms_get.summary>",
            section = "<key:shader.draw.options.api.section.options>",
            sectionOrder = 20,
            order = 10)
    @WikiReturn("<key:shader.draw.options.method.builtin_uniforms_get.return>")
    public boolean builtinUniforms() {
        return builtinUniforms;
    }

    /**
     * Включает или выключает стандартные uniforms.
     *
     * @param builtinUniforms новое значение
     * @return этот options object
     */
    @WikiMethod(
            title = "<key:shader.draw.options.method.builtin_uniforms_set.title>",
            summary = "<key:shader.draw.options.method.builtin_uniforms_set.summary>",
            section = "<key:shader.draw.options.api.section.options>",
            sectionOrder = 20,
            order = 20)
    @WikiReturn("<key:shader.draw.options.method.builtin_uniforms_set.return>")
    public ShaderDrawOptions builtinUniforms(@WikiParameter(name = "builtinUniforms", description = "<key:shader.draw.options.param.builtinUniforms>") boolean builtinUniforms) {
        this.builtinUniforms = builtinUniforms;
        return this;
    }

    /**
     * @return {@code true}, если shader-команда должна использовать blending
     */
    @WikiMethod(
            title = "<key:shader.draw.options.method.blend_get.title>",
            summary = "<key:shader.draw.options.method.blend_get.summary>",
            section = "<key:shader.draw.options.api.section.options>",
            sectionOrder = 20,
            order = 30)
    @WikiReturn("<key:shader.draw.options.method.blend_get.return>")
    public boolean blend() {
        return blend;
    }

    /**
     * Включает или выключает blending.
     *
     * @param blend новое значение
     * @return этот options object
     */
    @WikiMethod(
            title = "<key:shader.draw.options.method.blend_set.title>",
            summary = "<key:shader.draw.options.method.blend_set.summary>",
            section = "<key:shader.draw.options.api.section.options>",
            sectionOrder = 20,
            order = 40)
    @WikiReturn("<key:shader.draw.options.method.blend_set.return>")
    public ShaderDrawOptions blend(@WikiParameter(name = "blend", description = "<key:shader.draw.options.param.blend>") boolean blend) {
        this.blend = blend;
        return this;
    }

    /**
     * Возвращает смещение для автоматически передаваемого {@code SquareVertex} uniform.
     *
     * <p>Дефолт сохраняет совместимость с существующими full-screen quad shaders из SDMShop2.</p>
     *
     * @return uniform со смещением вершин квадрата
     */
    @WikiMethod(
            title = "<key:shader.draw.options.method.square_vertex_offset_get.title>",
            summary = "<key:shader.draw.options.method.square_vertex_offset_get.summary>",
            section = "<key:shader.draw.options.api.section.options>",
            sectionOrder = 20,
            order = 50)
    @WikiReturn("<key:shader.draw.options.method.square_vertex_offset_get.return>")
    public float squareVertexOffset() {
        return squareVertexOffset;
    }

    /**
     * Задаёт смещение для {@code SquareVertex} uniform.
     *
     * @param squareVertexOffset новое смещение
     * @return этот options object
     */
    @WikiMethod(
            title = "<key:shader.draw.options.method.square_vertex_offset_set.title>",
            summary = "<key:shader.draw.options.method.square_vertex_offset_set.summary>",
            section = "<key:shader.draw.options.api.section.options>",
            sectionOrder = 20,
            order = 60)
    @WikiReturn("<key:shader.draw.options.method.square_vertex_offset_set.return>")
    public ShaderDrawOptions squareVertexOffset(@WikiParameter(name = "squareVertexOffset", description = "<key:shader.draw.options.param.squareVertexOffset>") float squareVertexOffset) {
        this.squareVertexOffset = Float.isFinite(squareVertexOffset) ? squareVertexOffset : 0.0f;
        return this;
    }

    /**
     * @return независимая копия options
     */
    @WikiMethod(
            title = "<key:shader.draw.options.method.copy.title>",
            summary = "<key:shader.draw.options.method.copy.summary>",
            section = "<key:shader.draw.options.api.section.factories>",
            sectionOrder = 10,
            order = 20)
    @WikiReturn("<key:shader.draw.options.method.copy.return>")
    public ShaderDrawOptions copy() {
        return new ShaderDrawOptions()
                .builtinUniforms(builtinUniforms)
                .blend(blend)
                .squareVertexOffset(squareVertexOffset);
    }

    /**
     * Копирует параметры в уже существующий объект без создания нового options object.
     * Используется кадровым replay-путём, где объект команды берётся из пула.
     *
     * @param source источник параметров; {@code null} восстанавливает значения по умолчанию
     * @return этот options object
     */
    @WikiMethod(
            title = "<key:shader.draw.options.method.copy_from.title>",
            summary = "<key:shader.draw.options.method.copy_from.summary>",
            section = "<key:shader.draw.options.api.section.factories>",
            sectionOrder = 10,
            order = 30)
    @WikiReturn("<key:shader.draw.options.method.copy_from.return>")
    public ShaderDrawOptions copyFrom(@WikiParameter(name = "source", description = "<key:shader.draw.options.param.source>") ShaderDrawOptions source) {
        if (source == null) {
            builtinUniforms = true;
            blend = true;
            squareVertexOffset = -0.25f;
            return this;
        }
        builtinUniforms = source.builtinUniforms;
        blend = source.blend;
        squareVertexOffset = source.squareVertexOffset;
        return this;
    }
}
