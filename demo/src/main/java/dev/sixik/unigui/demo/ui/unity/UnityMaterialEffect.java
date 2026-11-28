package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.scene.SceneMaterialEffect;
import dev.sixik.unigui.api.scene.SceneMaterialEffects;

import java.util.function.Supplier;

/**
 * Settings of the Material Effect component: a ready-made surface shader with its color, strength and loop period.
 * The shaders come from {@link SceneMaterialEffects}, the same presets as the furnace tutorial. Their animation runs
 * over an interval, so the component repeats that interval every {@code period} seconds.
 *
 * @param preset   shader preset
 * @param colorRgb effect color as 0xRRGGBB, passed to the shader as parameters x, y, z
 * @param strength effect strength, passed to the shader as parameter w
 * @param period   loop length in seconds
 */
record UnityMaterialEffect(Preset preset, int colorRgb, float strength, float period) {
    /**
     * Shader presets the component offers.
     */
    enum Preset {
        /**
         * Top-down dissolve with an emissive edge.
         */
        DISSOLVE("Dissolve", SceneMaterialEffects::dissolve),
        /**
         * Emissive band sweeping from the bottom to the top.
         */
        SCANLINE("Scanline", SceneMaterialEffects::scanline),
        /**
         * Tinted hologram with animated stripes and gaps.
         */
        HOLOGRAM("Hologram", SceneMaterialEffects::hologram),
        /**
         * Emissive glow that builds and fades.
         */
        PULSE("Pulse", SceneMaterialEffects::pulse);

        /**
         * Name in the Shader dropdown.
         */
        private final String label;
        /**
         * Shared preset with its default parameters.
         */
        private final Supplier<SceneMaterialEffect> source;

        Preset(String label, Supplier<SceneMaterialEffect> source) {
            this.label = label;
            this.source = source;
        }

        /**
         * @return name in the Shader dropdown
         */
        String label() {
            return label;
        }

        /**
         * @return the shared preset with its default parameters
         */
        SceneMaterialEffect source() {
            return source.get();
        }
    }

    /**
     * Clamps the values to what the Inspector accepts.
     */
    UnityMaterialEffect {
        if (preset == null) preset = Preset.SCANLINE;
        colorRgb &= 0xFFFFFF;
        strength = Float.isFinite(strength) ? Math.max(0, Math.min(8, strength)) : 1;
        period = Float.isFinite(period) ? Math.max(0.25f, Math.min(60, period)) : 2;
    }

    /**
     * @param preset shader preset
     * @return the preset with its own color and strength and a two-second loop
     */
    static UnityMaterialEffect defaults(Preset preset) {
        SceneMaterialEffect source = preset.source();
        int rgb = channel(source.x()) << 16 | channel(source.y()) << 8 | channel(source.z());
        return new UnityMaterialEffect(preset, rgb, source.w(), 2);
    }

    /**
     * @param value color channel in [0, 1]
     * @return the channel in [0, 255]
     */
    private static int channel(float value) {
        return Math.round(Math.max(0, Math.min(1, value)) * 255);
    }

    /**
     * Builds the shader effect for the loop that contains a scene time. Loop n covers
     * {@code [n * period, (n + 1) * period)} with {@code n = floor(time / period)}.
     *
     * @param sceneTime scene material time in seconds
     * @return the effect timed to the current loop
     */
    SceneMaterialEffect toEffect(float sceneTime) {
        SceneMaterialEffect source = preset.source();
        float start = (float) Math.floor(sceneTime / period) * period;
        return new SceneMaterialEffect(source.shader(), (colorRgb >> 16 & 255) / 255f, (colorRgb >> 8 & 255) / 255f,
                (colorRgb & 255) / 255f, strength, null, source.alphaCutoff()).withTiming(start, period);
    }

    /**
     * @param next preset
     * @return a copy with another shader, keeping color, strength and period
     */
    UnityMaterialEffect withPreset(Preset next) {
        return new UnityMaterialEffect(next, colorRgb, strength, period);
    }

    /**
     * @param rgb color as 0xRRGGBB
     * @return a copy with another color
     */
    UnityMaterialEffect withColor(int rgb) {
        return new UnityMaterialEffect(preset, rgb, strength, period);
    }

    /**
     * @param value strength
     * @return a copy with another strength
     */
    UnityMaterialEffect withStrength(float value) {
        return new UnityMaterialEffect(preset, colorRgb, value, period);
    }

    /**
     * @param seconds loop length
     * @return a copy with another loop length
     */
    UnityMaterialEffect withPeriod(float seconds) {
        return new UnityMaterialEffect(preset, colorRgb, strength, seconds);
    }
}
