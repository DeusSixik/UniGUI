package dev.sixik.unigui.api.posteffect;

import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderProvider;
import dev.sixik.unigui.api.render.shaders.ShaderProviders;
import dev.sixik.unigui.api.render.shaders.ShaderSource;

import java.util.Map;
import java.util.Optional;

/**
 * Стандартные UI PostEffect id и встроенные shader sources UniGUI.
 *
 * <p>Встроенные эффекты нужны как базовые building blocks для HUD/overlay/editor пресетов. Пользователь
 * может зарегистрировать свои {@link UiPostEffectDefinition} и {@link ShaderProvider}, но эти id дают
 * готовый минимальный набор: passthrough, tint, vignette, barrel distortion, chromatic aberration и
 * scanline.</p>
 */
public final class UiPostEffects {
    public static final String PASSTHROUGH = "unigui:passthrough";
    public static final String TINT = "unigui:tint";
    public static final String VIGNETTE = "unigui:vignette";
    public static final String BARREL_DISTORTION = "unigui:barrel_distortion";
    public static final String CHROMATIC_ABERRATION = "unigui:chromatic_aberration";
    public static final String SCANLINE = "unigui:scanline";

    private static final ShaderProvider BUILTIN_SHADER_PROVIDER = new BuiltinShaderProvider();
    private static volatile boolean providerRegistered;

    private UiPostEffects() {
    }

    /**
     * Регистрирует стандартные effects и shader provider в global registries.
     *
     * <p>Метод idempotent: его безопасно вызывать из backend constructor'а, editor startup и тестов.</p>
     */
    public static synchronized void ensureRegistered() {
        UiPostEffectRegistry registry = UiPostEffectRegistry.global();
        registry.register(passthrough());
        registry.register(tint());
        registry.register(vignette());
        registry.register(barrelDistortion());
        registry.register(chromaticAberration());
        registry.register(scanline());
        if (!providerRegistered) {
            ShaderProviders.register(BUILTIN_SHADER_PROVIDER);
            providerRegistered = true;
        }
    }

    /** @return definition эффекта без визуального изменения */
    public static UiPostEffectDefinition passthrough() {
        return UiPostEffectDefinition.create(PASSTHROUGH)
                .pass(UiPostEffectPass.shader(PASSTHROUGH).blendMode(UiPostEffectBlendMode.REPLACE))
                .build();
    }

    /** @return definition эффекта цветового tint'а */
    public static UiPostEffectDefinition tint() {
        return UiPostEffectDefinition.create(TINT)
                .pass(UiPostEffectPass.shader(TINT).uniforms(uniforms -> uniforms
                        .vec4("color", 0.38f, 0.85f, 1.0f, 1.0f)
                        .floatValue("amount", 0.35f)))
                .build();
    }

    /** @return definition виньетки по краям слоя */
    public static UiPostEffectDefinition vignette() {
        return UiPostEffectDefinition.create(VIGNETTE)
                .pass(UiPostEffectPass.shader(VIGNETTE).uniforms(uniforms -> uniforms
                        .vec2("center", 0.5f, 0.5f)
                        .floatValue("radius", 0.72f)
                        .floatValue("softness", 0.30f)
                        .vec4("color", 0.0f, 0.0f, 0.0f, 1.0f)
                        .floatValue("amount", 0.35f)))
                .build();
    }
    /** @return definition barrel distortion для helmet/visor эффектов */
    public static UiPostEffectDefinition barrelDistortion() {
        return UiPostEffectDefinition.create(BARREL_DISTORTION)
                .pass(UiPostEffectPass.shader(BARREL_DISTORTION).uniforms(uniforms -> uniforms
                        .vec2("center", 0.5f, 0.5f)
                        .floatValue("strength", 0.085f)
                        .floatValue("zoom", 1.02f)))
                .build();
    }

    /** @return definition chromatic aberration */
    public static UiPostEffectDefinition chromaticAberration() {
        return UiPostEffectDefinition.create(CHROMATIC_ABERRATION)
                .pass(UiPostEffectPass.shader(CHROMATIC_ABERRATION).uniforms(uniforms -> uniforms
                        .floatValue("amount", 0.0035f)
                        .vec2("direction", 1.0f, 0.2f)))
                .build();
    }

    /** @return definition scanline эффекта */
    public static UiPostEffectDefinition scanline() {
        return UiPostEffectDefinition.create(SCANLINE)
                .pass(UiPostEffectPass.shader(SCANLINE).uniforms(uniforms -> uniforms
                        .floatValue("density", 260.0f)
                        .floatValue("amount", 0.18f)
                        .floatSupplier("phase", () -> (System.nanoTime() % 1_000_000_000L) / 1_000_000_000.0f)))
                .build();
    }

    /** @return chain на зарегистрированный standard effect */
    public static UiPostEffectChain chain(String effectId) {
        ensureRegistered();
        return UiPostEffectChain.of(effectId);
    }

    /** @return shader handle на встроенный PostEffect shader */
    public static ShaderHandle shader(String effectId) {
        ensureRegistered();
        return ShaderHandle.resource(effectId);
    }

    private static final class BuiltinShaderProvider implements ShaderProvider {
        private static final Map<String, String> SOURCES = Map.of(
                PASSTHROUGH, source("""
                        vec4 applyEffect(vec2 sourceUv, vec4 base) {
                            return base;
                        }
                        """),
                TINT, source("""
                        uniform vec4 color;
                        uniform float amount;

                        vec4 applyEffect(vec2 sourceUv, vec4 base) {
                            float a = clamp(amount, 0.0, 1.0);
                            vec4 tinted = vec4(base.rgb * color.rgb, base.a * color.a);
                            return mix(base, tinted, a);
                        }
                        """),
                VIGNETTE, source("""
                        uniform vec2 center;
                        uniform float radius;
                        uniform float softness;
                        uniform vec4 color;
                        uniform float amount;

                        vec4 applyEffect(vec2 sourceUv, vec4 base) {
                            float dist = distance(sourceUv, center);
                            float edge = smoothstep(max(0.0, radius - softness), max(0.001, radius), dist);
                            float a = clamp(edge * amount, 0.0, 1.0);
                            vec3 rgb = mix(base.rgb, color.rgb, a * color.a);
                            return vec4(rgb, base.a);
                        }
                        """),                BARREL_DISTORTION, source("""
                        uniform vec2 center;
                        uniform float strength;
                        uniform float zoom;

                        vec4 applyEffect(vec2 sourceUv, vec4 base) {
                            vec2 p = sourceUv - center;
                            float r2 = dot(p, p);
                            float safeZoom = max(0.01, zoom);
                            vec2 distortedUv = center + (p * (1.0 + strength * r2)) / safeZoom;
                            if (distortedUv.x < 0.0 || distortedUv.x > 1.0 || distortedUv.y < 0.0 || distortedUv.y > 1.0) {
                                return vec4(0.0);
                            }
                            return sampleSource(distortedUv);
                        }
                        """),
                CHROMATIC_ABERRATION, source("""
                        uniform float amount;
                        uniform vec2 direction;

                        vec4 applyEffect(vec2 sourceUv, vec4 base) {
                            vec2 dir = direction;
                            float len = length(dir);
                            if (len < 0.0001) {
                                dir = vec2(1.0, 0.0);
                            } else {
                                dir /= len;
                            }
                            vec2 offset = dir * amount;
                            float r = sampleSource(sourceUv + offset).r;
                            float g = base.g;
                            float b = sampleSource(sourceUv - offset).b;
                            return vec4(r, g, b, base.a);
                        }
                        """),
                SCANLINE, source("""
                        uniform float density;
                        uniform float amount;
                        uniform float phase;

                        vec4 applyEffect(vec2 sourceUv, vec4 base) {
                            float wave = sin((sourceUv.y + phase * 0.08) * max(1.0, density));
                            float line = 0.5 + 0.5 * wave;
                            float factor = 1.0 - clamp(amount, 0.0, 1.0) * smoothstep(0.35, 1.0, line);
                            return vec4(base.rgb * factor, base.a);
                        }
                        """));

        @Override
        public Optional<ShaderSource> load(ShaderHandle handle) {
            if (handle == null) return Optional.empty();
            String fragment = SOURCES.get(handle.id());
            return fragment == null ? Optional.empty() : Optional.of(ShaderSource.fragment(handle.id(), fragment));
        }

        private static String source(String body) {
            return """
                    #version 150

                    in vec2 uv;
                    out vec4 FragColor;

                    uniform sampler2D SourceTexture;
                    uniform vec2 SourceSize;
                    uniform float SourceFlipY;

                    vec2 normalizedSourceUv() {
                        vec2 sourceUv = uv * 0.5 + 0.5;
                        if (SourceFlipY > 0.5) {
                            sourceUv.y = 1.0 - sourceUv.y;
                        }
                        return clamp(sourceUv, vec2(0.0), vec2(1.0));
                    }

                    vec2 textureUv(vec2 sourceUv) {
                        vec2 uvValue = clamp(sourceUv, vec2(0.0), vec2(1.0));
                        if (SourceFlipY > 0.5) {
                            uvValue.y = 1.0 - uvValue.y;
                        }
                        return uvValue;
                    }

                    vec4 sampleSource(vec2 sourceUv) {
                        return texture(SourceTexture, textureUv(sourceUv));
                    }

                    %s

                    void main() {
                        vec2 sourceUv = normalizedSourceUv();
                        vec4 base = sampleSource(sourceUv);
                        FragColor = applyEffect(sourceUv, base);
                    }
                    """.formatted(body);
        }
    }
}