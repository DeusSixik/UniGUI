package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;

/**
 * Configures a square orthographic shadow map for the scene's directional light (GTNH 1.7.10, Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3).
 * The volume is a cube in light space. Keep it close to the subjects so the map retains detail.
 *
 * @param centerX    world X of the volume center in scene units
 * @param centerY    world Y of the volume center in scene units
 * @param centerZ    world Z of the volume center in scene units
 * @param size       side of the volume in scene units, finite and at least 0.001
 * @param resolution shadow map side in texels, from 128 through 2048
 * @param bias       depth comparison offset in normalized depth units, from 0 through 0.05
 * @param strength   fraction of direct light blocked in a fully occluded area, from 0 through 1
 */
@WikiPage(title = "DirectionalShadows", slug = "scene-directional-shadows", category = "<key:scene.category>",
        summary = "<key:directional.shadows.summary>")
@WikiSection(title = "<key:directional.shadows.section.quality.title>", markdown = "<key:directional.shadows.section.quality.body>")
@AiDoc(summary = "Controls a square orthographic shadow map on GTNH 1.7.10, Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3.",
        purpose = "Choose shadow coverage, precision, bias and intensity for a scene's directional light.",
        criticalRules = {
                "The volume is centered at the given world position and extends in light space.",
                "The size is in scene units. Resolution is in texels, bias is normalized depth, and strength is in [0, 1].",
                "Pass this value to Scene.setDirectionalShadows and configure Scene.setDirectionalLight."
        })
public record DirectionalShadows(
        @WikiParameter(description = "World X of the shadow volume center.") float centerX,
        @WikiParameter(description = "World Y of the shadow volume center.") float centerY,
        @WikiParameter(description = "World Z of the shadow volume center.") float centerZ,
        @WikiParameter(description = "Side of the shadow volume in scene units, at least 0.001.") float size,
        @WikiParameter(description = "Map side in texels, from 128 through 2048.") int resolution,
        @WikiParameter(description = "Normalized depth bias in [0, 0.05].") float bias,
        @WikiParameter(description = "Fraction of direct light blocked, in [0, 1].") float strength) {
    /**
     * Validates the shadow camera and texture settings.
     *
     * @throws IllegalArgumentException if the center, size, resolution, bias or strength is out of range
     */
    @WikiMethod(order = 10, summary = "Creates a directional shadow map configuration.")
    @WikiThrows(type = IllegalArgumentException.class, description = "If a setting is outside its documented range.")
    @AiDoc(order = 10, summary = "Creates a directional shadow map configuration.")
    public DirectionalShadows {
        if (!Float.isFinite(centerX) || !Float.isFinite(centerY) || !Float.isFinite(centerZ)
                || !(size >= 0.001f) || !Float.isFinite(size)
                || resolution < 128 || resolution > 2048
                || !(bias >= 0 && bias <= 0.05f) || !(strength >= 0 && strength <= 1)) {
            throw new IllegalArgumentException("Invalid directional shadow settings");
        }
    }

    /**
     * Creates a 1024-texel shadow map with a normalized depth bias of 0.0015 and strength of 0.85.
     *
     * @param centerX world X in scene units
     * @param centerY world Y in scene units
     * @param centerZ world Z in scene units
     * @param size    side of the shadow volume in scene units
     */
    @WikiMethod(order = 20, summary = "Uses 1024 texels, a depth bias of 0.0015 and a strength of 0.85.")
    @WikiThrows(type = IllegalArgumentException.class, description = "If a setting is outside its documented range.")
    @AiDoc(order = 20, summary = "Uses 1024 texels, a depth bias of 0.0015 and a strength of 0.85.")
    public DirectionalShadows(
            @WikiParameter(description = "World X coordinate of the volume center.") float centerX,
            @WikiParameter(description = "World Y coordinate of the volume center.") float centerY,
            @WikiParameter(description = "World Z coordinate of the volume center.") float centerZ,
            @WikiParameter(description = "Side of the volume in scene units, at least 0.001.") float size) {
        this(centerX, centerY, centerZ, size, 1024, 0.0015f, 0.85f);
    }
}
