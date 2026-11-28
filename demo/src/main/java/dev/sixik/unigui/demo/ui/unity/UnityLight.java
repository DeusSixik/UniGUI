package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.scene.DirectionalShadows;
import dev.sixik.unigui.api.scene.SpotShadows;

/**
 * Settings of a light object in the editor, in the units the Inspector shows.
 *
 * <p>Point and spot lights become a {@link dev.sixik.unigui.api.scene.LightNode} child of their object.
 * A directional light sets the scene's single directional light. Every light shines along its object's local
 * -Z axis.</p>
 *
 * @param kind             light type
 * @param colorRgb         packed RGB color
 * @param intensity        brightness multiplier, at least zero
 * @param range            reach of a point or spot light in scene units, at least 0.01
 * @param spotAngle        full outer cone angle of a spot light in degrees, from 1 to 170
 * @param innerSpotAngle   full inner cone angle in degrees, from 0 to {@code spotAngle}
 * @param shadows          whether shadow casting is enabled for supported light types (directional and spot)
 * @param shadowResolution shadow map side in texels, from 128 to 2048
 * @param shadowBias       depth comparison offset in normalized depth units, from 0 to 0.05
 * @param shadowStrength   fraction of direct light blocked in occluded areas, from 0 to 1
 * @param shadowSize       orthographic shadow volume side in scene units for directional lights, from 0.1 to 256
 */
record UnityLight(Kind kind, int colorRgb, float intensity, float range, float spotAngle, float innerSpotAngle,
                  boolean shadows, int shadowResolution, float shadowBias, float shadowStrength, float shadowSize) {
    /**
     * Light types supported by the scene renderer.
     */
    enum Kind {
        /**
         * Parallel rays covering the whole scene.
         */
        DIRECTIONAL("Directional"),
        /**
         * Light radiating from a point up to its range.
         */
        POINT("Point"),
        /**
         * Cone of light along local -Z.
         */
        SPOT("Spot");

        /**
         * Label shown in menus and the Inspector.
         */
        private final String label;

        Kind(String label) {
            this.label = label;
        }

        /**
         * @return label shown in menus and the Inspector
         */
        String label() {
            return label;
        }

        /**
         * @param label text from the Inspector's dropdown
         * @return matching kind, or null
         */
        static Kind fromLabel(String label) {
            for (Kind kind : values()) {
                if (kind.label.equals(label)) return kind;
            }
            return null;
        }
    }

    /**
     * Clamps every value into its valid range.
     */
    UnityLight {
        if (kind == null) kind = Kind.POINT;
        colorRgb &= 0xFFFFFF;
        intensity = finite(intensity, 1, 0, 64);
        range = finite(range, 5, 0.01f, 512);
        spotAngle = finite(spotAngle, 45, 1, 170);
        innerSpotAngle = finite(innerSpotAngle, spotAngle * 0.7f, 0, spotAngle);
        shadowResolution = Math.max(128, Math.min(2048, shadowResolution));
        shadowBias = finite(shadowBias, 0.0015f, 0, 0.05f);
        shadowStrength = finite(shadowStrength, 0.85f, 0, 1);
        shadowSize = finite(shadowSize, 12, 0.1f, 256);
    }

    /**
     * Creates light settings with shadows disabled and default shadow quality parameters.
     *
     * @param kind           light type
     * @param colorRgb       packed RGB color
     * @param intensity      brightness multiplier, at least zero
     * @param range          reach of a point or spot light in scene units, at least 0.01
     * @param spotAngle      full outer cone angle of a spot light in degrees, from 1 to 170
     * @param innerSpotAngle full inner cone angle in degrees, from 0 to {@code spotAngle}
     */
    UnityLight(Kind kind, int colorRgb, float intensity, float range, float spotAngle, float innerSpotAngle) {
        this(kind, colorRgb, intensity, range, spotAngle, innerSpotAngle,
                false, 1024, kind == Kind.SPOT ? 0.0012f : 0.0015f, kind == Kind.SPOT ? 0.95f : 0.85f, 12);
    }

    /**
     * @param kind light type
     * @return settings a new light of that type starts with
     */
    static UnityLight defaults(Kind kind) {
        return switch (kind) {
            case DIRECTIONAL -> new UnityLight(kind, 0xFFF1DB, 0.75f, 10, 45, 30, false, 1024, 0.0015f, 0.85f, 12);
            case POINT -> new UnityLight(kind, 0xFFE2B0, 1.2f, 5, 45, 30, false, 1024, 0.0015f, 0.85f, 12);
            case SPOT -> new UnityLight(kind, 0xFFFFFF, 1.5f, 8, 45, 30, false, 1024, 0.0012f, 0.95f, 12);
        };
    }

    /**
     * @param kind new type; the other values are kept
     * @return updated settings
     */
    UnityLight withKind(Kind kind) {
        return new UnityLight(kind, colorRgb, intensity, range, spotAngle, innerSpotAngle,
                shadows, shadowResolution, shadowBias, shadowStrength, shadowSize);
    }

    /**
     * @param colorRgb new packed RGB color
     * @return updated settings
     */
    UnityLight withColor(int colorRgb) {
        return new UnityLight(kind, colorRgb, intensity, range, spotAngle, innerSpotAngle,
                shadows, shadowResolution, shadowBias, shadowStrength, shadowSize);
    }

    /**
     * @param intensity new brightness
     * @return updated settings
     */
    UnityLight withIntensity(float intensity) {
        return new UnityLight(kind, colorRgb, intensity, range, spotAngle, innerSpotAngle,
                shadows, shadowResolution, shadowBias, shadowStrength, shadowSize);
    }

    /**
     * @param range new reach in scene units
     * @return updated settings
     */
    UnityLight withRange(float range) {
        return new UnityLight(kind, colorRgb, intensity, range, spotAngle, innerSpotAngle,
                shadows, shadowResolution, shadowBias, shadowStrength, shadowSize);
    }

    /**
     * Changes the outer cone and keeps the inner cone inside it.
     *
     * @param spotAngle new full outer angle in degrees
     * @return updated settings
     */
    UnityLight withSpotAngle(float spotAngle) {
        return new UnityLight(kind, colorRgb, intensity, range, spotAngle, Math.min(innerSpotAngle, spotAngle),
                shadows, shadowResolution, shadowBias, shadowStrength, shadowSize);
    }

    /**
     * @param innerSpotAngle new full inner angle in degrees
     * @return updated settings
     */
    UnityLight withInnerSpotAngle(float innerSpotAngle) {
        return new UnityLight(kind, colorRgb, intensity, range, spotAngle, innerSpotAngle,
                shadows, shadowResolution, shadowBias, shadowStrength, shadowSize);
    }

    /**
     * @param shadows whether shadows are enabled for this light
     * @return updated settings
     */
    UnityLight withShadows(boolean shadows) {
        return new UnityLight(kind, colorRgb, intensity, range, spotAngle, innerSpotAngle,
                shadows, shadowResolution, shadowBias, shadowStrength, shadowSize);
    }

    /**
     * @param shadowResolution new shadow map side in texels
     * @return updated settings
     */
    UnityLight withShadowResolution(int shadowResolution) {
        return new UnityLight(kind, colorRgb, intensity, range, spotAngle, innerSpotAngle,
                shadows, shadowResolution, shadowBias, shadowStrength, shadowSize);
    }

    /**
     * @param shadowBias new normalized depth comparison offset
     * @return updated settings
     */
    UnityLight withShadowBias(float shadowBias) {
        return new UnityLight(kind, colorRgb, intensity, range, spotAngle, innerSpotAngle,
                shadows, shadowResolution, shadowBias, shadowStrength, shadowSize);
    }

    /**
     * @param shadowStrength new shadow occlusion strength in [0, 1]
     * @return updated settings
     */
    UnityLight withShadowStrength(float shadowStrength) {
        return new UnityLight(kind, colorRgb, intensity, range, spotAngle, innerSpotAngle,
                shadows, shadowResolution, shadowBias, shadowStrength, shadowSize);
    }

    /**
     * @param shadowSize new directional shadow volume side in scene units
     * @return updated settings
     */
    UnityLight withShadowSize(float shadowSize) {
        return new UnityLight(kind, colorRgb, intensity, range, spotAngle, innerSpotAngle,
                shadows, shadowResolution, shadowBias, shadowStrength, shadowSize);
    }

    /**
     * @return true for directional and spot lights, false for point lights
     */
    boolean supportsShadows() {
        return kind != Kind.POINT;
    }

    /**
     * @return perspective shadow settings when this is a shadow-casting spot light, or null
     */
    SpotShadows toSpotShadows() {
        return shadows && kind == Kind.SPOT ? new SpotShadows(shadowResolution, shadowBias, shadowStrength) : null;
    }

    /**
     * @return orthographic shadow settings when this is a shadow-casting directional light, or null
     */
    DirectionalShadows toDirectionalShadows() {
        return shadows && kind == Kind.DIRECTIONAL
                ? new DirectionalShadows(0, 0, 0, shadowSize, shadowResolution, shadowBias, shadowStrength)
                : null;
    }

    /**
     * @param value    requested value
     * @param fallback value used when the request is not finite
     * @param min      lower bound
     * @param max      upper bound
     * @return the value clamped to [min, max]
     */
    private static float finite(float value, float fallback, float min, float max) {
        if (!Float.isFinite(value)) value = fallback;
        return Math.max(min, Math.min(max, value));
    }
}
