package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiThrows;

/**
 * Emits light in every direction from its node's origin.
 * The distance weight is {@code max(0, 1 - d / range)^2}, where d is the distance from the light.
 *
 * @param colorRgb  RGB color, with the high byte ignored
 * @param intensity finite nonnegative brightness multiplier
 * @param range     finite cutoff distance in scene units, at least 0.001
 */
@WikiPage(title = "PointLight", slug = "scene-point-light", category = "<key:scene.category>",
        summary = "<key:point.light.summary>")
@AiDoc(summary = "A finite-range point source placed by a LightNode on GTNH 1.7.10, Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3.",
        criticalRules = {"Range is in scene units. Point lights illuminate LIT materials without shadow maps."},
        related = {"LightNode", "SpotLight"})
public record PointLight(
        @WikiParameter(description = "Packed RGB color. The high byte is ignored.") int colorRgb,
        @WikiParameter(description = "Finite nonnegative brightness multiplier.") float intensity,
        @WikiParameter(description = "Finite cutoff distance in scene units, at least 0.001.") float range) implements LocalLight {
    /**
     * @throws IllegalArgumentException if the intensity or range is outside its domain
     */
    @WikiMethod(order = 10, summary = "Creates a point light that emits in all directions.")
    @WikiThrows(type = IllegalArgumentException.class, description = "If a setting is outside its documented range.")
    @AiDoc(order = 10, summary = "Creates a point light that emits in all directions.")
    public PointLight {
        if (!(intensity >= 0) || !Float.isFinite(intensity) || !(range >= 0.001f) || !Float.isFinite(range)) {
            throw new IllegalArgumentException("Local lights require finite nonnegative intensity and a range of at least 0.001");
        }
        colorRgb &= 0xFFFFFF;
    }
}
