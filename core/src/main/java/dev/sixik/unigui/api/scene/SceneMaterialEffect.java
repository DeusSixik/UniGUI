package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;

import java.util.Objects;

/**
 * Adds a GLSL surface function to indexed meshes, baked Minecraft blocks and supported item and entity models in Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3.
 *
 * <p>Create the handle with {@link ShaderHandle#fragmentSource(String, String)}. The source defines this
 * function. The scene supplies the version directive, vertex stage, base texture, shading and blending.
 * The alpha test also runs in the shadow pass, so an animated cutout can cast an animated shadow.</p>
 *
 * <pre>{@code
 * vec4 sceneSurface(vec4 color, vec2 uv, vec3 localPosition, vec3 worldPosition,
 *                   vec3 worldNormal, vec4 parameters, float timeSeconds, out vec3 emission)
 * }</pre>
 *
 * <p>Color is straight RGBA after tint and texture sampling. UV is the texture UV, including atlas coordinates
 * for Minecraft blocks and items. Positions are in scene units and the normal is a world-space unit direction, or zero.
 * Return straight RGBA and write the additive emissive RGB contribution, or write zero for no emission.
 * Translucent materials blend according to the returned alpha.</p>
 *
 * <p>The GLSL helpers expose the node's local bounds and the effect clock. Use {@code sceneNormalizedHeight(localPosition)}
 * for a height in [0, 1], {@code sceneEffectProgress()} for interval progress, and {@code sceneDissolve(threshold, width)}
 * to discard a fragment and obtain a mask for its surviving edge.</p>
 *
 * @param shader      embedded fragment function, without a version directive, vertex source or a main function
 * @param x           first user parameter, in units chosen by the shader
 * @param y           second user parameter, in units chosen by the shader
 * @param z           third user parameter, in units chosen by the shader
 * @param w           fourth user parameter, in units chosen by the shader
 * @param timing      optional effect interval on the scene clock, or null for an untimed effect
 * @param alphaCutoff optional minimum result alpha in [0, 1], with zero disabling the effect-specific cutoff
 */
@WikiPage(title = "SceneMaterialEffect", slug = "scene-material-effect", category = "<key:scene.category>",
        summary = "<key:scene.material.effect.summary>")
@WikiSection(title = "<key:scene.material.effect.section.surface.title>", order = 10,
        markdown = "<key:scene.material.effect.section.surface.body>")
@WikiSection(title = "<key:scene.material.effect.section.geometry.title>", order = 20,
        markdown = "<key:scene.material.effect.section.geometry.body>")
@WikiSection(title = "<key:scene.material.effect.section.cutouts.title>", order = 30,
        markdown = "<key:scene.material.effect.section.cutouts.body>")
@WikiSection(title = "<key:scene.material.effect.section.models.title>", order = 40,
        markdown = "<key:scene.material.effect.section.models.body>")
@WikiExample(title = "<key:scene.material.effect.example.pulse.title>", code = """
        var pulse = ShaderHandle.fragmentSource("example:pulse", \"""
                vec4 sceneSurface(vec4 color, vec2 uv, vec3 localPosition, vec3 worldPosition,
                                  vec3 normal, vec4 parameters, float timeSeconds, out vec3 emission) {
                    emission = vec3(0.1, 0.3, 1.0) * parameters.x * (0.5 + 0.5 * sin(timeSeconds));
                    return color;
                }
                \""");
        node.setMaterialEffect(new SceneMaterialEffect(pulse, 0.3f, 0, 0, 0));
        """)
@WikiExample(title = "<key:scene.material.effect.example.dissolve.title>", code = """
        var shader = ShaderHandle.fragmentSource("example:dissolve", \"""
                vec4 sceneSurface(vec4 color, vec2 uv, vec3 localPosition, vec3 worldPosition,
                                  vec3 normal, vec4 parameters, float timeSeconds, out vec3 emission) {
                    float threshold = 1.0 - sceneNormalizedHeight(localPosition);
                    float edge = sceneDissolve(threshold, 0.08);
                    emission = vec3(0.15, 0.5, 1.0) * edge;
                    return color;
                }
                \""");
        node.setMaterialEffect(new SceneMaterialEffect(shader).withTiming(6f, 2f));
        scene.setMaterialTime(7f);
        """)
@AiDoc(summary = "GLSL fragment surface functions for indexed 3D geometry on Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3.",
        purpose = "Modify color and alpha or add emission while retaining scene lighting and shadow behavior.",
        criticalRules = {
                "Supply an embedded GLSL function with ShaderHandle.fragmentSource. UniGUI supplies the version, main and vertex stage.",
                "The signature is vec4 sceneSurface(vec4 color, vec2 uv, vec3 localPosition, vec3 worldPosition, vec3 normal, vec4 parameters, float timeSeconds, out vec3 emission).",
                "Return straight RGBA and write additive RGB to emission. The cutout shadow pass calls the same function.",
                "Read sceneLocalBoundsMin(), sceneLocalBoundsMax(), sceneNormalizedPosition(localPosition) and sceneNormalizedHeight(localPosition) for model-local bounds and coordinates.",
                "Set withTiming(startSeconds, durationSeconds) for sceneEffectElapsed() and sceneEffectProgress(). Elapsed seconds are nonnegative and progress is clamped to [0, 1].",
                "Use withAlphaCutoff(minimumAlpha) to mask by returned alpha or sceneDissolve(threshold, edgeWidth) to discard using progress and obtain an edge mask for emission.",
                "Supported native layers include standard textured item and entity models, held items, armor and glint. Trim and emissive-layer availability depends on the Minecraft version and renderer.",
                "Specialized geometry and unsupported layers retain the vanilla renderer for the entire node.",
                "Use Scene.setMaterialTime to advance or seek shader time."
        })
public record SceneMaterialEffect(ShaderHandle shader, float x, float y, float z, float w,
                                  Timing timing, float alphaCutoff) {
    /**
     * Stores the shader function and its four finite parameters.
     *
     * @throws NullPointerException     if the shader is null
     * @throws IllegalArgumentException if the handle has no embedded fragment function, supplies a vertex stage,
     *                                  any parameter is nonfinite or the cutoff is outside [0, 1]
     */
    public SceneMaterialEffect {
        Objects.requireNonNull(shader, "shader");
        if (!shader.hasEmbeddedFragmentSource() || shader.vertexSource() != null
                || !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z) || !Float.isFinite(w)
                || !(alphaCutoff >= 0 && alphaCutoff <= 1)) {
            throw new IllegalArgumentException(
                    "Expected an embedded surface function, four finite parameters and an alpha cutoff in [0, 1]");
        }
    }

    /**
     * Creates an untimed effect with the supplied shader parameters.
     *
     * @param shader embedded scene surface function
     * @param x      first user parameter
     * @param y      second user parameter
     * @param z      third user parameter
     * @param w      fourth user parameter
     */
    public SceneMaterialEffect(ShaderHandle shader, float x, float y, float z, float w) {
        this(shader, x, y, z, w, null, 0);
    }

    /**
     * Creates an effect with zeroed shader parameters.
     *
     * @param shader embedded scene surface function
     */
    public SceneMaterialEffect(ShaderHandle shader) {
        this(shader, 0, 0, 0, 0);
    }

    /**
     * Sets the interval sampled by the GLSL effect progress and elapsed time functions.
     *
     * @param startSeconds    finite start on the scene clock
     * @param durationSeconds finite, strictly positive duration in seconds
     * @return effect with the same shader, user parameters and cutoff
     * @throws IllegalArgumentException if the start or duration is invalid
     */
    public SceneMaterialEffect withTiming(float startSeconds, float durationSeconds) {
        return new SceneMaterialEffect(shader, x, y, z, w, new Timing(startSeconds, durationSeconds), alphaCutoff);
    }

    /**
     * Sets the alpha mask applied after the custom surface function in both passes.
     *
     * @param minimumAlpha minimum surviving alpha in [0, 1], or zero to disable the mask
     * @return effect using the new cutoff
     * @throws IllegalArgumentException if the cutoff is outside [0, 1]
     */
    public SceneMaterialEffect withAlphaCutoff(float minimumAlpha) {
        return new SceneMaterialEffect(shader, x, y, z, w, timing, minimumAlpha);
    }

    /**
     * Changes the user data passed to the shader.
     *
     * @param x first user parameter
     * @param y second user parameter
     * @param z third user parameter
     * @param w fourth user parameter
     * @return effect with the same shader, interval and cutoff
     * @throws IllegalArgumentException if any parameter is nonfinite
     */
    public SceneMaterialEffect withParameters(float x, float y, float z, float w) {
        return new SceneMaterialEffect(shader, x, y, z, w, timing, alphaCutoff);
    }

    /**
     * Defines a seekable effect interval in the coordinate system of {@link Scene#materialTime()}.
     *
     * @param startSeconds    finite start time in seconds
     * @param durationSeconds finite, strictly positive duration in seconds
     */
    public record Timing(float startSeconds, float durationSeconds) {
        /**
         * Validates an interval.
         *
         * @throws IllegalArgumentException if either value is nonfinite or the duration is nonpositive
         */
        public Timing {
            if (!Float.isFinite(startSeconds) || !Float.isFinite(durationSeconds) || !(durationSeconds > 0)) {
                throw new IllegalArgumentException("Effect timing requires a finite start and positive finite duration");
            }
        }

        /**
         * Returns {@code clamp(time - startSeconds, 0, Float.MAX_VALUE)}. Subtraction uses double precision so
         * two finite scene times can be compared across the full float range.
         *
         * @param sceneSeconds finite current scene time in seconds
         * @return nonnegative elapsed seconds, including time beyond the end of the interval
         * @throws IllegalArgumentException if the time is nonfinite
         */
        public float elapsedAt(float sceneSeconds) {
            return (float) Math.max(0, Math.min(Float.MAX_VALUE, delta(sceneSeconds)));
        }

        /**
         * Returns {@code clamp((time - startSeconds) / durationSeconds, 0, 1)}. The difference and quotient
         * use double precision before clamping and converting the normalized result to float.
         *
         * @param sceneSeconds finite current scene time in seconds
         * @return linear progress in [0, 1]
         * @throws IllegalArgumentException if the time is nonfinite
         */
        public float progressAt(float sceneSeconds) {
            return (float) Math.max(0, Math.min(1, delta(sceneSeconds) / durationSeconds));
        }

        /**
         * @param sceneSeconds finite scene time in seconds
         * @return signed time relative to the start, in seconds
         * @throws IllegalArgumentException if the time is nonfinite
         */
        private double delta(float sceneSeconds) {
            if (!Float.isFinite(sceneSeconds)) throw new IllegalArgumentException("Material time must be finite");
            return (double) sceneSeconds - startSeconds;
        }
    }
}
