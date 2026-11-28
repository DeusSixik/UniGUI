package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.Objects;

/**
 * Samples a deterministic fire flicker for a point light or spotlight.
 */
@WikiPage(title = "LightFlicker", slug = "scene-light-flicker", category = "<key:scene.category>",
        summary = "<key:light.flicker.summary>")
@WikiSection(title = "<key:light.flicker.section.playback.title>", markdown = "<key:light.flicker.section.playback.body>")
@WikiLink(label = "LightNode", target = "page:scene-light-node")
@WikiLink(label = "SceneTimeline", target = "page:scene-timeline")
@WikiExample(title = "<key:light.flicker.example.fire.title>", code = """
        var base = new PointLight(0xFF9E55, 1.65f, 3f);
        var flame = new LightNode(base);
        var flicker = LightFlicker.fire(42);
        scene.addRoot(flame);
        timeline.onSample(seconds -> flame.setLight(flicker.sample(base, seconds)));
        """)
@AiDoc(summary = "A seeded fire flicker for PointLight and SpotLight.",
        criticalRules = {
                "Call LightFlicker.fire(seed) once and sample it against a stable base light.",
                "Use absolute elapsed animation seconds. The same time always gives the same RGB and intensity.",
                "Set the sample on the LightNode. Spotlight mask, geometry and shadows are preserved."
        }, related = {"LightNode", "PointLight", "SpotLight", "SceneTimeline"})
public final class LightFlicker {
    /**
     * RGB color approached as a flame flares.
     */
    private static final int FLARE_RGB = 0xFFCB69;
    /**
     * Period of each noise channel in samples, a power of two.
     */
    private static final int NOISE_PERIOD = 1 << 16;
    /**
     * Seed identifying the repeatable flame pattern.
     */
    private final long seed;

    /**
     * @param seed seed identifying the repeatable flame pattern
     */
    private LightFlicker(long seed) {
        this.seed = seed;
    }

    /**
     * Creates a fire preset with smooth irregular brightness changes and brief amber flares.
     *
     * @param seed arbitrary seed that distinguishes one flame from another
     * @return reusable preset
     */
    public static LightFlicker fire(long seed) {
        return new LightFlicker(seed);
    }

    /**
     * Computes {@code I = I0 * (0.82 + 0.32*s + 0.17*f + 0.48*p)}, where I0 is base intensity,
     * s and f are smooth noise at 3 and 11 samples per second, and p is the flare envelope.
     * For noise r at 2 samples per second, {@code p = smoothstep(0, 1, clamp((r - 0.74) / 0.23, 0, 1))}.
     * Each RGB channel blends from the base to amber by the weight {@code 0.12*s + 0.44*p}.
     *
     * @param base           original light configuration used for every sample
     * @param elapsedSeconds finite time in seconds since the effect started, with negative times sampled at zero
     * @return light with sampled intensity and tint and the base light's other settings
     * @throws NullPointerException     if the base light is null
     * @throws IllegalArgumentException if time is nonfinite
     */
    public LocalLight sample(LocalLight base, float elapsedSeconds) {
        Objects.requireNonNull(base, "base");
        if (!Float.isFinite(elapsedSeconds)) throw new IllegalArgumentException("Flicker time must be finite");
        if (base.intensity() == 0 || base.colorRgb() == 0) return base;

        double time = Math.max(0, elapsedSeconds);
        double slow = noise(time * 3, seed);
        double fast = noise(time * 11, seed ^ 0x63D83595A9F6F4B1L);
        double flareInput = noise(time * 2, seed ^ 0xC13FA9A902A6328FL);
        double flare = smooth(Math.max(0, Math.min(1, (flareInput - 0.74) / 0.23)));
        double multiplier = 0.82 + 0.32 * slow + 0.17 * fast + 0.48 * flare;
        float intensity = (float) Math.min(base.intensity() * multiplier, Float.MAX_VALUE);
        double heat = 0.12 * slow + 0.44 * flare;
        int rgb = blendChannel(base.colorRgb(), 16, heat) << 16
                | blendChannel(base.colorRgb(), 8, heat) << 8
                | blendChannel(base.colorRgb(), 0, heat);
        if (base instanceof PointLight) return new PointLight(rgb, intensity, base.range());
        SpotLight spot = (SpotLight) base;
        return new SpotLight(rgb, intensity, spot.range(), spot.innerAngle(), spot.outerAngle(), spot.mask(), spot.shadows());
    }

    /**
     * Interpolates adjacent hashed samples as {@code a + (b - a) * smooth(t)}, where t is the fractional
     * part of the sample coordinate, a is the value at its floor and b is the value at the next sample.
     *
     * @param sampleCoordinate nonnegative finite time multiplied by the noise rate
     * @param channelSeed      seed for this noise channel
     * @return periodic noise in [0, 1)
     */
    private static double noise(double sampleCoordinate, long channelSeed) {
        double wrapped = sampleCoordinate % NOISE_PERIOD;
        int index = (int) wrapped;
        double a = value(index, channelSeed);
        double b = value((index + 1) & (NOISE_PERIOD - 1), channelSeed);
        return a + (b - a) * smooth(wrapped - index);
    }

    /**
     * Hashes the seed plus index times the SplitMix64 increment, then scales the high 53 bits by 2^-53.
     *
     * @param index       wrapped integer sample index
     * @param channelSeed seed for this noise channel
     * @return unit sample in [0, 1)
     */
    private static double value(int index, long channelSeed) {
        long bits = channelSeed + index * 0x9E3779B97F4A7C15L;
        bits = (bits ^ (bits >>> 30)) * 0xBF58476D1CE4E5B9L;
        bits = (bits ^ (bits >>> 27)) * 0x94D049BB133111EBL;
        return ((bits ^ (bits >>> 31)) >>> 11) * 0x1.0p-53;
    }

    /**
     * Applies the cubic curve {@code t² * (3 - 2*t)} with flat derivatives at the endpoints.
     *
     * @param t input fraction in [0, 1]
     * @return eased fraction in [0, 1]
     */
    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    /**
     * Interpolates one byte as {@code round(from + (to - from) * heat)} with to taken from the flare RGB.
     *
     * @param rgb   base packed RGB
     * @param shift channel position in bits, 16 for red, 8 for green or 0 for blue
     * @param heat  blend fraction in [0, 1]
     * @return interpolated byte in [0, 255]
     */
    private static int blendChannel(int rgb, int shift, double heat) {
        int from = (rgb >>> shift) & 0xFF;
        int to = (FLARE_RGB >>> shift) & 0xFF;
        return (int) Math.round(from + (to - from) * heat);
    }
}
