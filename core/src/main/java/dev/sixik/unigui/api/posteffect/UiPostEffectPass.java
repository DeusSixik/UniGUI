package dev.sixik.unigui.api.posteffect;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.TextureHandle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Один shader pass внутри PostEffect chain.
 *
 * <p>Pass описывает данные, а не Java-рендерер: shader id/source, uniforms и режим blending.
 * Backend получает source texture через стандартные uniforms {@code SourceTexture}, {@code SourceSize},
 * {@code SourceFlipY}; пользовательские uniforms из этого объекта добавляются поверх них.</p>
 */
@WikiPage(
        title = "UiPostEffectPass",
        slug = "ui-post-effect-pass",
        category = "<key:ui.post.effect.pass.category>",
        summary = "<key:ui.post.effect.pass.summary>",
        order = 40
)
@WikiSection(
        title = "<key:ui.post.effect.pass.section.overview.title>",
        markdown = "<key:ui.post.effect.pass.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:ui.post.effect.pass.example.pass.title>",
        code = "<key:ui.post.effect.pass.example.pass.code>",
        order = 20
)
@AiDoc(
        summary = "Configuration for a single shader pass in a post-processing chain.",
        purpose = "Pairs a ShaderHandle with custom uniforms, secondary sampler textures, and a blend mode.",
        useWhen = {"Configuring individual render passes inside a UiPostEffectDefinition."},
        related = {"ShaderHandle", "UiPostEffectUniforms", "UiPostEffectBlendMode", "TextureHandle"}
)
public final class UiPostEffectPass {
    private final ShaderHandle shader;
    private final UiPostEffectUniforms uniforms;
    private final UiPostEffectBlendMode blendMode;
    private final Map<String, TextureHandle> textures;

    private UiPostEffectPass(ShaderHandle shader, UiPostEffectUniforms uniforms, UiPostEffectBlendMode blendMode) {
        this(shader, uniforms, blendMode, Map.of());
    }

    private UiPostEffectPass(ShaderHandle shader,
                             UiPostEffectUniforms uniforms,
                             UiPostEffectBlendMode blendMode,
                             Map<String, TextureHandle> textures) {
        this.shader = Objects.requireNonNull(shader, "shader").copy();
        this.uniforms = uniforms == null ? UiPostEffectUniforms.empty() : uniforms.copy();
        this.blendMode = blendMode == null ? UiPostEffectBlendMode.REPLACE : blendMode;
        this.textures = copyTextures(textures);
    }

    /** Создаёт pass по resource id shader'а. */
    @WikiMethod(
            title = "<key:ui.post.effect.pass.method.shader_id_factory.title>",
            summary = "<key:ui.post.effect.pass.method.shader_id_factory.summary>"
    )
    @WikiReturn("<key:ui.post.effect.pass.method.shader_id_factory.return>")
    public static UiPostEffectPass shader(
            @WikiParameter(description = "<key:ui.post.effect.pass.method.shader_id_factory.shader_id>") String shaderId) {
        return new UiPostEffectPass(ShaderHandle.resource(shaderId), UiPostEffectUniforms.empty(), UiPostEffectBlendMode.REPLACE);
    }

    /** Создаёт pass по готовому shader handle. */
    @WikiMethod(
            title = "<key:ui.post.effect.pass.method.shader_handle_factory.title>",
            summary = "<key:ui.post.effect.pass.method.shader_handle_factory.summary>"
    )
    @WikiReturn("<key:ui.post.effect.pass.method.shader_handle_factory.return>")
    public static UiPostEffectPass shader(
            @WikiParameter(description = "<key:ui.post.effect.pass.method.shader_handle_factory.shader>") ShaderHandle shader) {
        return new UiPostEffectPass(shader, UiPostEffectUniforms.empty(), UiPostEffectBlendMode.REPLACE);
    }

    /** @return shader handle pass'а */
    @WikiMethod(
            title = "<key:ui.post.effect.pass.method.shader_get.title>",
            summary = "<key:ui.post.effect.pass.method.shader_get.summary>"
    )
    @WikiReturn("<key:ui.post.effect.pass.method.shader_get.return>")
    public ShaderHandle shader() {
        return shader.copy();
    }

    /** @return стабильный id shader'а */
    @WikiMethod(
            title = "<key:ui.post.effect.pass.method.shader_id.title>",
            summary = "<key:ui.post.effect.pass.method.shader_id.summary>"
    )
    @WikiReturn("<key:ui.post.effect.pass.method.shader_id.return>")
    public String shaderId() {
        return shader.id();
    }

    /** @return uniforms pass'а */
    @WikiMethod(
            title = "<key:ui.post.effect.pass.method.uniforms_get.title>",
            summary = "<key:ui.post.effect.pass.method.uniforms_get.summary>"
    )
    @WikiReturn("<key:ui.post.effect.pass.method.uniforms_get.return>")
    public UiPostEffectUniforms uniforms() {
        return uniforms.copy();
    }

    /** @return режим blending pass'а */
    @WikiMethod(
            title = "<key:ui.post.effect.pass.method.blend_mode_get.title>",
            summary = "<key:ui.post.effect.pass.method.blend_mode_get.summary>"
    )
    @WikiReturn("<key:ui.post.effect.pass.method.blend_mode_get.return>")
    public UiPostEffectBlendMode blendMode() {
        return blendMode;
    }

    /** @return дополнительные shader textures, keyed by sampler uniform name */
    @WikiMethod(
            title = "<key:ui.post.effect.pass.method.textures_get.title>",
            summary = "<key:ui.post.effect.pass.method.textures_get.summary>"
    )
    @WikiReturn("<key:ui.post.effect.pass.method.textures_get.return>")
    public Map<String, TextureHandle> textures() {
        return textures;
    }

    /** Возвращает копию pass'а с новым набором uniforms. */
    @WikiMethod(
            title = "<key:ui.post.effect.pass.method.uniforms_set.title>",
            summary = "<key:ui.post.effect.pass.method.uniforms_set.summary>"
    )
    @WikiReturn("<key:ui.post.effect.pass.method.uniforms_set.return>")
    public UiPostEffectPass uniforms(
            @WikiParameter(description = "<key:ui.post.effect.pass.method.uniforms_set.uniforms>") UiPostEffectUniforms uniforms) {
        return new UiPostEffectPass(shader, uniforms, blendMode, textures);
    }

    /** Возвращает копию pass'а после настройки uniforms через callback. */
    @WikiMethod(
            title = "<key:ui.post.effect.pass.method.uniforms_config.title>",
            summary = "<key:ui.post.effect.pass.method.uniforms_config.summary>"
    )
    @WikiReturn("<key:ui.post.effect.pass.method.uniforms_config.return>")
    public UiPostEffectPass uniforms(
            @WikiParameter(description = "<key:ui.post.effect.pass.method.uniforms_config.config>") Consumer<UiPostEffectUniforms> config) {
        UiPostEffectUniforms copy = uniforms.copy();
        if (config != null) {
            config.accept(copy);
        }
        return new UiPostEffectPass(shader, copy, blendMode, textures);
    }

    /** Возвращает копию pass'а с новым режимом blending. */
    @WikiMethod(
            title = "<key:ui.post.effect.pass.method.blend_mode_set.title>",
            summary = "<key:ui.post.effect.pass.method.blend_mode_set.summary>"
    )
    @WikiReturn("<key:ui.post.effect.pass.method.blend_mode_set.return>")
    public UiPostEffectPass blendMode(
            @WikiParameter(description = "<key:ui.post.effect.pass.method.blend_mode_set.blend_mode>") UiPostEffectBlendMode blendMode) {
        return new UiPostEffectPass(shader, uniforms, blendMode, textures);
    }

    /** Возвращает копию pass'а с дополнительной texture для sampler uniform. */
    @WikiMethod(
            title = "<key:ui.post.effect.pass.method.texture_set.title>",
            summary = "<key:ui.post.effect.pass.method.texture_set.summary>"
    )
    @WikiReturn("<key:ui.post.effect.pass.method.texture_set.return>")
    public UiPostEffectPass texture(
            @WikiParameter(description = "<key:ui.post.effect.pass.method.texture_set.uniform_name>") String uniformName,
            @WikiParameter(description = "<key:ui.post.effect.pass.method.texture_set.texture>") TextureHandle texture) {
        LinkedHashMap<String, TextureHandle> copy = new LinkedHashMap<>(textures);
        String name = uniformName == null ? "" : uniformName.trim();
        if (!name.isEmpty()) {
            if (texture == null) copy.remove(name);
            else copy.put(name, texture);
        }
        return new UiPostEffectPass(shader, uniforms, blendMode, copy);
    }

    /** @return независимая копия pass'а */
    @WikiMethod(
            title = "<key:ui.post.effect.pass.method.copy.title>",
            summary = "<key:ui.post.effect.pass.method.copy.summary>"
    )
    @WikiReturn("<key:ui.post.effect.pass.method.copy.return>")
    public UiPostEffectPass copy() {
        return new UiPostEffectPass(shader, uniforms, blendMode, textures);
    }

    private static Map<String, TextureHandle> copyTextures(Map<String, TextureHandle> source) {
        if (source == null || source.isEmpty()) return Map.of();
        LinkedHashMap<String, TextureHandle> copy = new LinkedHashMap<>();
        for (Map.Entry<String, TextureHandle> entry : source.entrySet()) {
            String name = entry.getKey() == null ? "" : entry.getKey().trim();
            TextureHandle texture = entry.getValue();
            if (!name.isEmpty() && texture != null) copy.put(name, texture);
        }
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }
}
