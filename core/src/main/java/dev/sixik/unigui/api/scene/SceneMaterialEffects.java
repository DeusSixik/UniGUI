package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;

/**
 * Reusable, seekable material effects for GTNH 1.7.10, Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3 scene nodes.
 * Each factory returns an immutable preset with RGB in the first three parameters and intensity in the fourth.
 * Set an interval with {@link SceneMaterialEffect#withTiming(float, float)} and advance {@link Scene#setMaterialTime(float)}.
 */
@WikiPage(title = "SceneMaterialEffects", slug = "scene-material-effects", category = "<key:scene.category>",
        summary = "<key:scene.material.effects.summary>")
@WikiSection(title = "<key:scene.material.effects.section.presets.title>", markdown = "<key:scene.material.effects.section.presets.body>")
@WikiExample(title = "<key:scene.material.effects.example.scanline.title>", code = """
        node.setMaterialEffect(SceneMaterialEffects.scanline().withTiming(6f, 2f));
        scene.setMaterialTime(7f);
        """)
@AiDoc(summary = "Built-in dissolve, scanline, hologram and pulse material effects for GTNH 1.7.10, Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3 scenes.",
        purpose = "Apply a seekable shader preset to a SceneNode without supplying GLSL.",
        criticalRules = {
                "Apply a preset with SceneNode.setMaterialEffect(preset.withTiming(startSeconds, durationSeconds)).",
                "Advance or seek all effects with Scene.setMaterialTime(seconds).",
                "Preset parameters x, y, z are the RGB tint and w is intensity.",
                "Dissolve discards the rendered surface at progress one. Use node visibility separately for picking."
        }, related = {"SceneMaterialEffect", "SceneNode", "SceneTimeline"})
public final class SceneMaterialEffects {
    /**
     * Blue dissolve. The removal threshold is {@code 0.03 + 0.55 * (1 - h) + 0.29 * c + 0.10 * f}.
     * Here h is normalized local height, and c and f are value noise at 5 and 19 samples per scene unit.
     * The surviving edge spans 0.095 units of progress and emits the preset color.
     */
    private static final SceneMaterialEffect DISSOLVE = new SceneMaterialEffect(
            ShaderHandle.fragmentSource("unigui:scene_effect/dissolve", """
                    float dissolveHash(vec3 cell) {
                        cell = fract(cell * vec3(0.1031, 0.11369, 0.13787));
                        cell += dot(cell, cell.yzx + 19.19);
                        return fract((cell.x + cell.y) * cell.z);
                    }
                    
                    float dissolveNoise(vec3 point) {
                        vec3 cell = floor(point);
                        vec3 f = smoothstep(vec3(0.0), vec3(1.0), fract(point));
                        vec2 lower = vec2(
                                mix(dissolveHash(cell), dissolveHash(cell + vec3(1, 0, 0)), f.x),
                                mix(dissolveHash(cell + vec3(0, 1, 0)), dissolveHash(cell + vec3(1, 1, 0)), f.x));
                        vec2 upper = vec2(
                                mix(dissolveHash(cell + vec3(0, 0, 1)), dissolveHash(cell + vec3(1, 0, 1)), f.x),
                                mix(dissolveHash(cell + vec3(0, 1, 1)), dissolveHash(cell + vec3(1, 1, 1)), f.x));
                        return mix(mix(lower.x, lower.y, f.y), mix(upper.x, upper.y, f.y), f.z);
                    }
                    
                    vec4 sceneSurface(vec4 color, vec2 uv, vec3 localPosition, vec3 worldPosition,
                                      vec3 normal, vec4 parameters, float timeSeconds, out vec3 emission) {
                        emission = vec3(0.0);
                        float progress = sceneEffectProgress();
                        if (progress > 0.0) {
                            float height = sceneNormalizedHeight(localPosition);
                            float coarse = dissolveNoise(localPosition * 5.0 + vec3(11.0, 23.0, 7.0));
                            float fine = dissolveNoise(localPosition * 19.0);
                            float threshold = 0.03 + 0.55 * (1.0 - height) + 0.29 * coarse + 0.10 * fine;
                            float edge = sceneDissolve(threshold, 0.095)
                                    * smoothstep(0.0, 0.10, progress) * (1.0 - smoothstep(0.85, 1.0, progress));
                            emission = parameters.rgb * parameters.w * (edge * (0.5 + 0.5 * edge));
                        }
                        return color;
                    }
                    """), 0.25f, 0.85f, 1f, 1f);

    /**
     * Green scanline with center height {@code 1.26 * p - 0.13}, where p is interval progress.
     * Its falloff spans normalized distances 0.015 through 0.115 on either side of the center.
     */
    private static final SceneMaterialEffect SCANLINE = new SceneMaterialEffect(
            ShaderHandle.fragmentSource("unigui:scene_effect/scanline", """
                    vec4 sceneSurface(vec4 color, vec2 uv, vec3 localPosition, vec3 worldPosition,
                                      vec3 normal, vec4 parameters, float timeSeconds, out vec3 emission) {
                        float center = 1.26 * sceneEffectProgress() - 0.13;
                        float distanceToCenter = abs(sceneNormalizedHeight(localPosition) - center);
                        float band = 1.0 - smoothstep(0.015, 0.115, distanceToCenter);
                        emission = parameters.rgb * (parameters.w * band);
                        return color;
                    }
                    """), 0.20f, 1f, 0.46f, 0.9f);

    /**
     * Violet hologram. Stripe phase is {@code fract(14 * h - 3 * t)} for normalized height h and elapsed seconds t.
     * The first and last 15 percent of the interval ease the tint and gaps in and out.
     */
    private static final SceneMaterialEffect HOLOGRAM = new SceneMaterialEffect(
            ShaderHandle.fragmentSource("unigui:scene_effect/hologram", """
                    vec4 sceneSurface(vec4 color, vec2 uv, vec3 localPosition, vec3 worldPosition,
                                      vec3 normal, vec4 parameters, float timeSeconds, out vec3 emission) {
                        float progress = sceneEffectProgress();
                        float fade = smoothstep(0.0, 0.15, progress) * (1.0 - smoothstep(0.85, 1.0, progress));
                        float strength = parameters.w * fade;
                        if (strength <= 0.0) {
                            emission = vec3(0.0);
                            return color;
                        }
                        float phase = fract(sceneNormalizedHeight(localPosition) * 14.0 - sceneEffectElapsed() * 3.0);
                        if (phase > 1.0 - 0.24 * clamp(strength, 0.0, 1.0)) discard;
                        float stripe = smoothstep(0.32, 0.68, phase);
                        float luminance = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
                        vec3 tint = parameters.rgb * (0.20 + 0.80 * luminance);
                        color.rgb = mix(color.rgb, tint, clamp(strength, 0.0, 1.0));
                        emission = parameters.rgb * (strength * (0.12 + 0.48 * stripe));
                        return color;
                    }
                    """), 0.59f, 0.29f, 1f, 0.85f);

    /**
     * Amber emissive pulse with strength {@code w * sin(pi * p)} for preset intensity w and interval progress p.
     */
    private static final SceneMaterialEffect PULSE = new SceneMaterialEffect(
            ShaderHandle.fragmentSource("unigui:scene_effect/pulse", """
                    vec4 sceneSurface(vec4 color, vec2 uv, vec3 localPosition, vec3 worldPosition,
                                      vec3 normal, vec4 parameters, float timeSeconds, out vec3 emission) {
                        float progress = sceneEffectProgress();
                        float pulse = progress < 1.0 ? sin(3.14159265 * progress) : 0.0;
                        emission = parameters.rgb * (parameters.w * pulse);
                        return color;
                    }
                    """), 1f, 0.34f, 0.06f, 0.52f);

    /**
     * Prevents instantiation of the preset library.
     */
    private SceneMaterialEffects() {
    }

    /**
     * Creates a top-down dissolve with a blue emissive edge.
     *
     * @return shared untimed dissolve preset
     */
    @WikiMethod(order = 10, summary = "Dissolves the surface with a highlighted edge.")
    @WikiReturn("the dissolve preset")
    @AiDoc(order = 10, summary = "Dissolves the surface with a highlighted edge.")
    public static SceneMaterialEffect dissolve() {
        return DISSOLVE;
    }

    /**
     * Creates a green emissive sweep from the bottom of the model to the top.
     *
     * @return shared untimed scanline preset
     */
    @WikiMethod(order = 20, summary = "Sweeps a scanline across the surface.")
    @WikiReturn("the scanline preset")
    @AiDoc(order = 20, summary = "Sweeps a scanline across the surface.")
    public static SceneMaterialEffect scanline() {
        return SCANLINE;
    }

    /**
     * Creates a violet hologram with animated local-space gaps and emissive stripes.
     *
     * @return shared untimed hologram preset
     */
    @WikiMethod(order = 30, summary = "Applies holographic stripes and gaps.")
    @WikiReturn("the hologram preset")
    @AiDoc(order = 30, summary = "Applies holographic stripes and gaps.")
    public static SceneMaterialEffect hologram() {
        return HOLOGRAM;
    }

    /**
     * Creates an amber glow that peaks halfway through its interval.
     *
     * @return shared untimed pulse preset
     */
    @WikiMethod(order = 40, summary = "Pulses the surface emission.")
    @WikiReturn("the pulse preset")
    @AiDoc(order = 40, summary = "Pulses the surface emission.")
    public static SceneMaterialEffect pulse() {
        return PULSE;
    }
}
