package dev.sixik.unigui.api.posteffect;

import dev.sixik.unigui.api.render.shaders.ShaderHandle;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Один shader pass внутри PostEffect chain.
 *
 * <p>Pass описывает данные, а не Java-рендерер: shader id/source, uniforms и режим blending.
 * Backend получает source texture через стандартные uniforms {@code SourceTexture}, {@code SourceSize},
 * {@code SourceFlipY}; пользовательские uniforms из этого объекта добавляются поверх них.</p>
 */
public final class UiPostEffectPass {
    private final ShaderHandle shader;
    private final UiPostEffectUniforms uniforms;
    private final UiPostEffectBlendMode blendMode;

    private UiPostEffectPass(ShaderHandle shader, UiPostEffectUniforms uniforms, UiPostEffectBlendMode blendMode) {
        this.shader = Objects.requireNonNull(shader, "shader").copy();
        this.uniforms = uniforms == null ? UiPostEffectUniforms.empty() : uniforms.copy();
        this.blendMode = blendMode == null ? UiPostEffectBlendMode.REPLACE : blendMode;
    }

    /** Создаёт pass по resource id shader'а. */
    public static UiPostEffectPass shader(String shaderId) {
        return new UiPostEffectPass(ShaderHandle.resource(shaderId), UiPostEffectUniforms.empty(), UiPostEffectBlendMode.REPLACE);
    }

    /** Создаёт pass по готовому shader handle. */
    public static UiPostEffectPass shader(ShaderHandle shader) {
        return new UiPostEffectPass(shader, UiPostEffectUniforms.empty(), UiPostEffectBlendMode.REPLACE);
    }

    /** @return shader handle pass'а */
    public ShaderHandle shader() {
        return shader.copy();
    }

    /** @return стабильный id shader'а */
    public String shaderId() {
        return shader.id();
    }

    /** @return uniforms pass'а */
    public UiPostEffectUniforms uniforms() {
        return uniforms.copy();
    }

    /** @return режим blending pass'а */
    public UiPostEffectBlendMode blendMode() {
        return blendMode;
    }

    /** Возвращает копию pass'а с новым набором uniforms. */
    public UiPostEffectPass uniforms(UiPostEffectUniforms uniforms) {
        return new UiPostEffectPass(shader, uniforms, blendMode);
    }

    /** Возвращает копию pass'а после настройки uniforms через callback. */
    public UiPostEffectPass uniforms(Consumer<UiPostEffectUniforms> config) {
        UiPostEffectUniforms copy = uniforms.copy();
        if (config != null) {
            config.accept(copy);
        }
        return new UiPostEffectPass(shader, copy, blendMode);
    }

    /** Возвращает копию pass'а с новым режимом blending. */
    public UiPostEffectPass blendMode(UiPostEffectBlendMode blendMode) {
        return new UiPostEffectPass(shader, uniforms, blendMode);
    }

    /** @return независимая копия pass'а */
    public UiPostEffectPass copy() {
        return new UiPostEffectPass(shader, uniforms, blendMode);
    }
}