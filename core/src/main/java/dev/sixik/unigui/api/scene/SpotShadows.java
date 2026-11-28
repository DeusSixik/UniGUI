package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;

/**
 * Configures one square perspective shadow map for a {@link SpotLight}.
 *
 * @param resolution side of the map in texels, from 128 through 2048
 * @param bias       comparison offset in normalized depth units, from zero through 0.05
 * @param strength   fraction of the direct light blocked by fully occluded geometry, from zero through one
 */
@WikiPage(title = "SpotShadows", slug = "scene-spot-shadows", category = "<key:scene.category>",
        summary = "<key:spot.shadows.summary>")
@AiDoc(summary = "Controls a spotlight's perspective shadow map on Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3.",
        criticalRules = {"Configure SpotLight.withShadows. The default map is 512 by 512 texels.",
                "Mesh casters and receivers honor their own SceneNode shadow settings. Strength zero disables the map."},
        related = {"SpotLight", "LightNode", "DirectionalShadows"})
public record SpotShadows(int resolution, float bias, float strength) {
    /**
     * @throws IllegalArgumentException if a setting is outside its domain
     */
    public SpotShadows {
        if (resolution < 128 || resolution > 2048
                || !(bias >= 0 && bias <= 0.05f) || !(strength >= 0 && strength <= 1)) {
            throw new IllegalArgumentException("Invalid spotlight shadow settings");
        }
    }

    /**
     * Creates a 512-texel map with a depth bias of 0.0015 and strength of one.
     */
    public SpotShadows() {
        this(512, 0.0015f, 1f);
    }
}
