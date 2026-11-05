package dev.sixik.unigui.api.render.shaders;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
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
@AiDoc(
        summary = "Identifier and container for UI shaders, referencing external resource paths or embedding GLSL source code.",
        purpose = "Decouples shader execution commands from graphics driver shader compilation, supporting both static resources and dynamic procedural GLSL.",
        useWhen = {
                "Execute custom GPU shaders via `DrawScope#shader(ShaderHandle, x, y, w, h, uniforms, options)`.",
                "Reference external mod or Minecraft shaders with `ShaderHandle.resource(id)`.",
                "Inject embedded procedural procedural GLSL shaders using `ShaderHandle.fragmentSource(id, code)`."
        },
        criticalRules = {
                "Shader programs are compiled and cached on GPU by `id()`; keep IDs consistent to prevent continuous recompilation.",
                "When using embedded shaders, ensure GLSL versions match backend requirements (#version 150 core in modern Minecraft)."
        },
        related = {"ShaderUniforms", "ShaderDrawOptions", "ShaderProvider", "DrawScope"},
        examples = {
                @AiDoc.Example(
                        title = "Create embedded fragment shader",
                        order = 10,
                        code = """
                                ShaderHandle shader = ShaderHandle.fragmentSource("mod:glow", glslCode);
                                scope.shader(shader, x, y, width, height, uniforms, ShaderDrawOptions.defaults());
                                """)
        })
@WikiPage(
        title = "<key:shader.handle.title>",
        slug = "shader-handle",
        category = "<key:shader.handle.category>",
        summary = "<key:shader.handle.summary>",
        order = 10)
@WikiSection(
        title = "<key:shader.handle.section.overview.title>",
        markdown = "<key:shader.handle.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:shader.handle.section.execution.title>",
        markdown = "<key:shader.handle.section.execution.body>",
        order = 20)
@WikiExample(
        title = "<key:shader.handle.example.procedural.title>",
        code = "<key:shader.handle.example.procedural.code>",
        order = 10)
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
    @WikiMethod(
            title = "<key:shader.handle.method.resource.title>",
            summary = "<key:shader.handle.method.resource.summary>",
            section = "<key:shader.handle.api.section.factories>",
            sectionOrder = 10,
            order = 10)
    @WikiReturn("<key:shader.handle.method.resource.return>")
    public static ShaderHandle resource(@WikiParameter(name = "id", description = "<key:shader.handle.param.id>") String id) {
        return new ShaderHandle(id, null, null);
    }

    /**
     * Создаёт handle с embedded fragment source.
     *
     * @param id id shader'а
     * @param fragmentSource GLSL-код fragment shader
     * @return новый handle
     */
    @WikiMethod(
            title = "<key:shader.handle.method.fragment_source.title>",
            summary = "<key:shader.handle.method.fragment_source.summary>",
            section = "<key:shader.handle.api.section.factories>",
            sectionOrder = 10,
            order = 20)
    @WikiReturn("<key:shader.handle.method.fragment_source.return>")
    public static ShaderHandle fragmentSource(@WikiParameter(name = "id", description = "<key:shader.handle.param.id>") String id, @WikiParameter(name = "fragmentSource", description = "<key:shader.handle.param.fragmentSource>") @Language("GLSL") String fragmentSource) {
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
    @WikiMethod(
            title = "<key:shader.handle.method.source.title>",
            summary = "<key:shader.handle.method.source.summary>",
            section = "<key:shader.handle.api.section.factories>",
            sectionOrder = 10,
            order = 30)
    @WikiReturn("<key:shader.handle.method.source.return>")
    public static ShaderHandle source(@WikiParameter(name = "id", description = "<key:shader.handle.param.id>") String id, @WikiParameter(name = "vertexSource", description = "<key:shader.handle.param.vertexSource>") @Language("GLSL") String vertexSource, @WikiParameter(name = "fragmentSource", description = "<key:shader.handle.param.fragmentSource>") @Language("GLSL") String fragmentSource) {
        return new ShaderHandle(id,
                Objects.requireNonNull(vertexSource, "vertexSource"),
                Objects.requireNonNull(fragmentSource, "fragmentSource"));
    }

    /**
     * @return shader id или resource id
     */
    @WikiMethod(
            title = "<key:shader.handle.method.id.title>",
            summary = "<key:shader.handle.method.id.summary>",
            section = "<key:shader.handle.api.section.properties>",
            sectionOrder = 20,
            order = 10)
    @WikiReturn("<key:shader.handle.method.id.return>")
    public String id() {
        return id;
    }

    /**
     * @return embedded vertex source или {@code null}
     */
    @WikiMethod(
            title = "<key:shader.handle.method.vertex_source.title>",
            summary = "<key:shader.handle.method.vertex_source.summary>",
            section = "<key:shader.handle.api.section.properties>",
            sectionOrder = 20,
            order = 20)
    @WikiReturn("<key:shader.handle.method.vertex_source.return>")
    public String vertexSource() {
        return vertexSource;
    }

    /**
     * @return embedded fragment source или {@code null}
     */
    @WikiMethod(
            title = "<key:shader.handle.method.fragment_source_get.title>",
            summary = "<key:shader.handle.method.fragment_source_get.summary>",
            section = "<key:shader.handle.api.section.properties>",
            sectionOrder = 20,
            order = 30)
    @WikiReturn("<key:shader.handle.method.fragment_source_get.return>")
    public String fragmentSource() {
        return fragmentSource;
    }

    /**
     * @return {@code true}, если handle содержит embedded vertex source
     */
    @WikiMethod(
            title = "<key:shader.handle.method.has_vertex.title>",
            summary = "<key:shader.handle.method.has_vertex.summary>",
            section = "<key:shader.handle.api.section.properties>",
            sectionOrder = 20,
            order = 40)
    @WikiReturn("<key:shader.handle.method.has_vertex.return>")
    public boolean hasEmbeddedVertexSource() {
        return vertexSource != null;
    }

    /**
     * @return {@code true}, если handle содержит embedded fragment source
     */
    @WikiMethod(
            title = "<key:shader.handle.method.has_fragment.title>",
            summary = "<key:shader.handle.method.has_fragment.summary>",
            section = "<key:shader.handle.api.section.properties>",
            sectionOrder = 20,
            order = 50)
    @WikiReturn("<key:shader.handle.method.has_fragment.return>")
    public boolean hasEmbeddedFragmentSource() {
        return fragmentSource != null;
    }

    /**
     * @return независимая копия handle
     */
    @WikiMethod(
            title = "<key:shader.handle.method.copy.title>",
            summary = "<key:shader.handle.method.copy.summary>",
            section = "<key:shader.handle.api.section.properties>",
            sectionOrder = 20,
            order = 60)
    @WikiReturn("<key:shader.handle.method.copy.return>")
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
