package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;

import java.util.Objects;

/**
 * A transformable volume of colored haze on GTNH 1.7.10, Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3.
 */
@WikiPage(title = "FogVolume", slug = "scene-fog-volume", category = "<key:scene.category>",
        summary = "<key:fog.volume.summary>")
@WikiSection(title = "<key:fog.volume.section.shape.title>", markdown = "<key:fog.volume.section.shape.body>")
@WikiExample(title = "<key:fog.volume.example.floor.title>", code = """
        var haze = new FogVolume(FogVolume.Shape.BOX, 0x466487, 0.24f)
                .setEdgeSoftness(0.64f)
                .setNoise(new FogVolume.Noise(0.7f, 2.4f, 0.16f, 0, 0.07f))
                .setLightScattering(1.2f);
        haze.setPosition(0.5f, 1.5f, 0.5f).setScale(2.0f, 0.5f, 2.0f);
        scene.addRoot(haze);
        timeline.onSample(scene::setMaterialTime);
        """)
@WikiLink(label = "SpotLight", target = "page:scene-spot-light")
@WikiLink(label = "SceneTimeline", target = "page:scene-timeline")
@AiDoc(summary = "A transformable fog box or sphere for GTNH 1.7.10, Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3 scenes.",
        criticalRules = {
                "Add FogVolume as a root or child. Base half-extents or radius are one local unit.",
                "Density is extinction per world unit. Zero disables the volume.",
                "Drive drifting noise with Scene.setMaterialTime so it seeks with the timeline.",
                "Up to four visible volumes use the first active spotlight for volumetric scattering."
        }, related = {"Scene", "SceneNode", "SpotLight", "SceneTimeline"})
public final class FogVolume extends SceneNode {
    /**
     * Shape of the volume in local coordinates.
     */
    public enum Shape {
        /**
         * Unit half-extents along X, Y and Z.
         */
        BOX,
        /**
         * Unit radius, transformable into an ellipsoid by the node scale.
         */
        SPHERE
    }

    /**
     * Drifting local density variation. At strength a and a noise value n in [0, 1], the density
     * multiplier is {@code 1 - a + a * n}. Noise is sampled at {@code scale * (p - v * t)} for local
     * position p, local drift velocity v and scene material time t in seconds.
     *
     * @param strength  fraction of density modulated by noise, from zero through one
     * @param scale     noise samples per local unit, positive and finite
     * @param velocityX local X drift in units per second, finite
     * @param velocityY local Y drift in units per second, finite
     * @param velocityZ local Z drift in units per second, finite
     */
    public record Noise(float strength, float scale, float velocityX, float velocityY, float velocityZ) {
        /**
         * Uniform density with zero drift.
         */
        public static final Noise NONE = new Noise(0, 1, 0, 0, 0);

        /**
         * @throws IllegalArgumentException if any component is outside its domain
         */
        public Noise {
            if (!(strength >= 0 && strength <= 1) || !(scale > 0) || !Float.isFinite(scale)
                    || !Float.isFinite(velocityX) || !Float.isFinite(velocityY) || !Float.isFinite(velocityZ)) {
                throw new IllegalArgumentException("Invalid fog noise");
            }
        }
    }

    /**
     * Local shape of this fog, with unit radius or half-extents.
     */
    private final Shape shape;
    /**
     * Ambient haze color in the low 24 bits, RGB order.
     */
    private int colorRgb;
    /**
     * Extinction per world unit, finite and nonnegative.
     */
    private float density;
    /**
     * Smoothing width in [0, 1] of the local radius or half-extent.
     */
    private float edgeSoftness = 0.35f;
    /**
     * Local noise settings, never null.
     */
    private Noise noise = Noise.NONE;
    /**
     * Brightness multiplier for scattering from the spotlight, finite and nonnegative.
     */
    private float lightScattering = 1f;

    /**
     * Creates a fog node centered at the local origin.
     *
     * @param shape    local volume shape
     * @param colorRgb ambient haze RGB color, ignoring the high byte
     * @param density  finite nonnegative extinction per world unit
     * @throws NullPointerException     if shape is null
     * @throws IllegalArgumentException if density is invalid
     */
    @WikiMethod(order = 10, summary = "Creates a fog volume at its local origin.")
    @WikiThrows(type = NullPointerException.class, description = "If the shape is null.")
    @WikiThrows(type = IllegalArgumentException.class, description = "If the density is negative or nonfinite.")
    @AiDoc(order = 10, summary = "Creates a fog volume at its local origin.")
    public FogVolume(
            @WikiParameter(description = "Box or sphere in the node's local coordinates.") Shape shape,
            @WikiParameter(description = "Packed RGB color. The high byte is ignored.") int colorRgb,
            @WikiParameter(description = "Finite nonnegative extinction per world unit.") float density) {
        this.shape = Objects.requireNonNull(shape, "shape");
        this.colorRgb = colorRgb & 0xFFFFFF;
        this.density = nonnegative(density);
    }

    /**
     * @return the local volume shape
     */
    public Shape shape() {
        return shape;
    }

    /**
     * @return packed haze RGB
     */
    public int colorRgb() {
        return colorRgb;
    }

    /**
     * Sets the ambient haze color.
     *
     * @param colorRgb packed RGB, ignoring the high byte
     * @return this volume
     */
    @WikiMethod(order = 20, summary = "Changes the fog color.")
    @WikiReturn("this volume")
    @AiDoc(order = 20, summary = "Changes the fog color.")
    public FogVolume setColorRgb(
            @WikiParameter(description = "Packed RGB color. The high byte is ignored.") int colorRgb) {
        int rgb = colorRgb & 0xFFFFFF;
        if (this.colorRgb != rgb) {
            this.colorRgb = rgb;
            invalidateContent();
        }
        return this;
    }

    /**
     * @return extinction per world unit
     */
    public float density() {
        return density;
    }

    /**
     * Sets extinction and refreshes the rendered scene.
     *
     * @param density finite nonnegative extinction per world unit, zero disables the fog
     * @return this volume
     * @throws IllegalArgumentException if density is invalid
     */
    @WikiMethod(order = 30, summary = "Changes extinction per world unit.")
    @WikiReturn("this volume")
    @WikiThrows(type = IllegalArgumentException.class, description = "If the density is negative or nonfinite.")
    @AiDoc(order = 30, summary = "Changes extinction per world unit.")
    public FogVolume setDensity(
            @WikiParameter(description = "Finite nonnegative extinction. Zero disables the fog.") float density) {
        if (this.density != nonnegative(density)) {
            this.density = density;
            invalidateContent();
        }
        return this;
    }

    /**
     * @return width of the density fade in units of local half-extent or radius
     */
    public float edgeSoftness() {
        return edgeSoftness;
    }

    /**
     * Fades density using {@code 1 - smoothstep(1 - width, 1, r)}, where r is local distance from center
     * in the chosen shape. Zero width gives a hard edge.
     *
     * @param width edge fade fraction from zero through one
     * @return this volume
     * @throws IllegalArgumentException if width is outside [0, 1]
     */
    @WikiMethod(order = 40, summary = "Changes how gradually fog fades at its boundary.")
    @WikiReturn("this volume")
    @WikiThrows(type = IllegalArgumentException.class, description = "If the width is outside [0, 1] or NaN.")
    @AiDoc(order = 40, summary = "Changes how gradually fog fades at its boundary.")
    public FogVolume setEdgeSoftness(
            @WikiParameter(description = "Fade fraction in [0, 1]. Zero uses a hard boundary.") float width) {
        if (!(width >= 0 && width <= 1)) throw new IllegalArgumentException("Invalid fog edge width");
        if (edgeSoftness != width) {
            edgeSoftness = width;
            invalidateContent();
        }
        return this;
    }

    /**
     * @return the current local noise configuration
     */
    public Noise noise() {
        return noise;
    }

    /**
     * Sets local density variation.
     *
     * @param noise configuration with scale and drift, or Noise.NONE for uniform density
     * @return this volume
     * @throws NullPointerException if noise is null
     */
    @WikiMethod(order = 50, summary = "Changes local density variation.")
    @WikiReturn("this volume")
    @WikiThrows(type = NullPointerException.class, description = "If the noise settings are null.")
    @AiDoc(order = 50, summary = "Changes local density variation.")
    public FogVolume setNoise(
            @WikiParameter(description = "Noise scale, strength and velocity, or Noise.NONE for uniform density.") Noise noise) {
        if (!this.noise.equals(Objects.requireNonNull(noise, "noise"))) {
            this.noise = noise;
            invalidateContent();
        }
        return this;
    }

    /**
     * @return the brightness multiplier for light scattered by this fog
     */
    public float lightScattering() {
        return lightScattering;
    }

    /**
     * Sets how strongly the first active spotlight appears in the fog.
     *
     * @param strength finite nonnegative scattering brightness multiplier
     * @return this volume
     * @throws IllegalArgumentException if strength is invalid
     */
    @WikiMethod(order = 60, summary = "Changes scattering from the first active spotlight.")
    @WikiReturn("this volume")
    @WikiThrows(type = IllegalArgumentException.class, description = "If the strength is negative or nonfinite.")
    @AiDoc(order = 60, summary = "Changes scattering from the first active spotlight.")
    public FogVolume setLightScattering(
            @WikiParameter(description = "Finite nonnegative scattering brightness multiplier.") float strength) {
        if (lightScattering != nonnegative(strength)) {
            lightScattering = strength;
            invalidateContent();
        }
        return this;
    }

    /**
     * Validates an extinction or scattering coefficient.
     *
     * @param value candidate coefficient
     * @return the value
     * @throws IllegalArgumentException if value is negative or nonfinite
     */
    private static float nonnegative(float value) {
        if (!(value >= 0) || !Float.isFinite(value)) throw new IllegalArgumentException("Invalid fog coefficient");
        return value;
    }
}
