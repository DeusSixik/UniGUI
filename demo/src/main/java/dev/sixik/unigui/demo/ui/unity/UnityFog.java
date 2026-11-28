package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.scene.FogVolume;

/**
 * Fog Volume component settings: an immutable value turned into a {@link FogVolume} node. Invalid numbers are clamped
 * to the ranges the Inspector offers, so a value read from a file or typed into a field always yields a valid node.
 * The volume has unit half-extents (box) or a unit radius (sphere) in the object's local space, so the object's
 * Transform scale sets its size.
 *
 * @param shape           local volume shape
 * @param colorRgb        haze color as 0xRRGGBB
 * @param density         extinction per world unit, in [0, 8]
 * @param edgeSoftness    smoothing width as a fraction of the half-extent or radius, in [0, 1]
 * @param noiseStrength   fraction of the density modulated by noise, in [0, 1]
 * @param noiseScale      noise samples per local unit, in [0.05, 32]
 * @param driftX          local X noise drift in units per second, in [-16, 16]
 * @param driftY          local Y noise drift in units per second, in [-16, 16]
 * @param driftZ          local Z noise drift in units per second, in [-16, 16]
 * @param lightScattering brightness multiplier of spotlight scattering, in [0, 16]
 */
record UnityFog(FogVolume.Shape shape, int colorRgb, float density, float edgeSoftness, float noiseStrength,
                float noiseScale, float driftX, float driftY, float driftZ, float lightScattering) {
    /**
     * Normalizes the shape, color and numeric ranges.
     */
    UnityFog {
        if (shape == null) shape = FogVolume.Shape.BOX;
        colorRgb &= 0xFFFFFF;
        density = clamp(density, 0.3f, 0, 8);
        edgeSoftness = clamp(edgeSoftness, 0.35f, 0, 1);
        noiseStrength = clamp(noiseStrength, 0, 0, 1);
        noiseScale = clamp(noiseScale, 1, 0.05f, 32);
        driftX = clamp(driftX, 0, -16, 16);
        driftY = clamp(driftY, 0, -16, 16);
        driftZ = clamp(driftZ, 0, -16, 16);
        lightScattering = clamp(lightScattering, 1, 0, 16);
    }

    /**
     * @param shape volume shape
     * @return a soft blue-gray haze with light drifting noise, like the floor fog of the scene tutorial
     */
    static UnityFog defaults(FogVolume.Shape shape) {
        return new UnityFog(shape, 0x8FA3BF, 0.3f, 0.5f, 0.5f, 1.5f, 0.15f, 0, 0.05f, 1);
    }

    /**
     * @return whether the noise moves over time, so the scene clock must run
     */
    boolean animated() {
        return noiseStrength > 0 && (driftX != 0 || driftY != 0 || driftZ != 0);
    }

    /**
     * Creates the render node for these settings. The node shape is fixed at construction.
     *
     * @return a detached fog node at the local origin
     */
    FogVolume toNode() {
        return apply(new FogVolume(shape, colorRgb, density));
    }

    /**
     * Copies every setting except the shape onto an existing node.
     *
     * @param node node of the same shape
     * @return node
     */
    FogVolume apply(FogVolume node) {
        node.setColorRgb(colorRgb).setDensity(density).setEdgeSoftness(edgeSoftness)
                .setNoise(new FogVolume.Noise(noiseStrength, noiseScale, driftX, driftY, driftZ))
                .setLightScattering(lightScattering);
        return node;
    }

    UnityFog withShape(FogVolume.Shape shape) {
        return new UnityFog(shape, colorRgb, density, edgeSoftness, noiseStrength, noiseScale, driftX, driftY, driftZ,
                lightScattering);
    }

    UnityFog withColor(int colorRgb) {
        return new UnityFog(shape, colorRgb, density, edgeSoftness, noiseStrength, noiseScale, driftX, driftY, driftZ,
                lightScattering);
    }

    UnityFog withDensity(float density) {
        return new UnityFog(shape, colorRgb, density, edgeSoftness, noiseStrength, noiseScale, driftX, driftY, driftZ,
                lightScattering);
    }

    UnityFog withEdgeSoftness(float edgeSoftness) {
        return new UnityFog(shape, colorRgb, density, edgeSoftness, noiseStrength, noiseScale, driftX, driftY, driftZ,
                lightScattering);
    }

    UnityFog withNoiseStrength(float noiseStrength) {
        return new UnityFog(shape, colorRgb, density, edgeSoftness, noiseStrength, noiseScale, driftX, driftY, driftZ,
                lightScattering);
    }

    UnityFog withNoiseScale(float noiseScale) {
        return new UnityFog(shape, colorRgb, density, edgeSoftness, noiseStrength, noiseScale, driftX, driftY, driftZ,
                lightScattering);
    }

    /**
     * @param axis  0 for X, 1 for Y, 2 for Z
     * @param value drift along that local axis in units per second
     * @return settings with that drift component replaced
     */
    UnityFog withDrift(int axis, float value) {
        return new UnityFog(shape, colorRgb, density, edgeSoftness, noiseStrength, noiseScale,
                axis == 0 ? value : driftX, axis == 1 ? value : driftY, axis == 2 ? value : driftZ, lightScattering);
    }

    /**
     * @param axis 0 for X, 1 for Y, 2 for Z
     * @return drift along that local axis
     */
    float drift(int axis) {
        return axis == 0 ? driftX : axis == 1 ? driftY : driftZ;
    }

    UnityFog withLightScattering(float lightScattering) {
        return new UnityFog(shape, colorRgb, density, edgeSoftness, noiseStrength, noiseScale, driftX, driftY, driftZ,
                lightScattering);
    }

    /**
     * @return the value inside [min, max], or the fallback for NaN or infinity
     */
    private static float clamp(float value, float fallback, float min, float max) {
        if (!Float.isFinite(value)) value = fallback;
        return Math.max(min, Math.min(max, value));
    }
}
