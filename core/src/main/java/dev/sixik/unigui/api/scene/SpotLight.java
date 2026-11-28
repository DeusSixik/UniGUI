package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.render.TextureHandle;

/**
 * A cone of light along the node's local negative Z axis. The distance falloff is the same as in
 * {@link PointLight}. The angular weight interpolates smoothly between the cosines of the outer and
 * inner half-angles. With equal angles it uses a hard cutoff at that cosine.
 *
 * @param colorRgb   RGB color, with the high byte ignored
 * @param intensity  finite nonnegative brightness multiplier
 * @param range      finite cutoff distance in scene units, at least 0.001
 * @param innerAngle inner half-angle in radians, from zero through outerAngle
 * @param outerAngle outer half-angle in radians, at least 0.001 and strictly below pi / 2
 * @param mask       projected texture or null for a clear cone. Its RGB and alpha multiply the light color
 * @param shadows    perspective shadow settings, or null to disable shadows for this light
 */
@WikiPage(title = "SpotLight", slug = "scene-spot-light", category = "<key:scene.category>",
        summary = "<key:spot.light.summary>")
@WikiSection(title = "<key:spot.light.section.mask.title>", markdown = "<key:spot.light.section.mask.body>")
@AiDoc(summary = "A soft cone with a texture mask and perspective shadows on Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3.",
        criticalRules = {"Spotlights point along local -Z and use cone half-angles in radians.",
                "The mask multiplies light by texture RGB and alpha. Shadow visibility is evaluated independently."},
        related = {"LightNode", "PointLight", "SpotShadows"})
public record SpotLight(int colorRgb, float intensity, float range, float innerAngle, float outerAngle,
                        TextureHandle mask, SpotShadows shadows) implements LocalLight {
    /**
     * @throws IllegalArgumentException if the light or cone parameters are outside their domains
     */
    public SpotLight {
        if (!(intensity >= 0) || !Float.isFinite(intensity) || !(range >= 0.001f) || !Float.isFinite(range)) {
            throw new IllegalArgumentException("Local lights require finite nonnegative intensity and a range of at least 0.001");
        }
        if (!(innerAngle >= 0 && innerAngle <= outerAngle && outerAngle >= 0.001f && outerAngle < Math.PI * 0.5)) {
            throw new IllegalArgumentException("Spotlight angles must be ordered half-angles below pi / 2");
        }
        colorRgb &= 0xFFFFFF;
    }

    /**
     * Creates a cone with just the distance and angular falloff.
     *
     * @param colorRgb   RGB color
     * @param intensity  finite nonnegative brightness multiplier
     * @param range      finite cutoff distance in scene units, at least 0.001
     * @param innerAngle inner half-angle in radians
     * @param outerAngle outer half-angle in radians
     * @throws IllegalArgumentException if any argument is outside the light's domain
     */
    public SpotLight(int colorRgb, float intensity, float range, float innerAngle, float outerAngle) {
        this(colorRgb, intensity, range, innerAngle, outerAngle, null, null);
    }

    /**
     * Sets a projected image. The image fills the square around the outer cone at each distance from the source.
     *
     * @param mask texture with white for full contribution and black or transparent for no contribution, or null
     * @return light with the new mask
     */
    public SpotLight withMask(TextureHandle mask) {
        return new SpotLight(colorRgb, intensity, range, innerAngle, outerAngle, mask, shadows);
    }

    /**
     * Sets the shadow quality for this source.
     *
     * @param shadows perspective shadow settings, or null to disable shadow casting for this light
     * @return light with the new shadow settings
     */
    public SpotLight withShadows(SpotShadows shadows) {
        return new SpotLight(colorRgb, intensity, range, innerAngle, outerAngle, mask, shadows);
    }
}
