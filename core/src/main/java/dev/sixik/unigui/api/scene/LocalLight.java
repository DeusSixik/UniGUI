package dev.sixik.unigui.api.scene;

/**
 * A finite-range light placed in a scene by a {@link LightNode}.
 */
public sealed interface LocalLight permits PointLight, SpotLight {
    /**
     * @return packed RGB color in the low 24 bits
     */
    int colorRgb();

    /**
     * @return finite, nonnegative brightness multiplier, with zero disabling the light
     */
    float intensity();

    /**
     * @return cutoff distance in scene units, finite and at least 0.001
     */
    float range();
}
