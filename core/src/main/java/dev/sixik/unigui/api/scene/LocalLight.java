package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * A finite-range light placed in a scene by a {@link LightNode}.
 */
@WikiPage(title = "LocalLight", slug = "scene-local-light", category = "<key:scene.category>", order = 123,
        summary = "<key:scene.local.light.summary>")
@WikiSection(title = "<key:scene.local.light.placement.title>", order = 10,
        markdown = "<key:scene.local.light.placement.body>")
@WikiLink(label = "LightNode", target = "page:scene-light-node")
@WikiLink(label = "PointLight", target = "page:scene-point-light")
@WikiLink(label = "SpotLight", target = "page:scene-spot-light")
@WikiLink(label = "LightFlicker", target = "page:scene-light-flicker")
@AiDoc(summary = "Common light settings accepted by LightNode.",
        criticalRules = {
                "LocalLight is sealed to PointLight and SpotLight.",
                "Intensity is finite and nonnegative. Range is the finite cutoff distance in scene units."
        }, related = {"LightNode", "PointLight", "SpotLight", "LightFlicker"})
public sealed interface LocalLight permits PointLight, SpotLight {
    /**
     * @return packed RGB color in the low 24 bits
     */
    @WikiMethod(order = 10, summary = "<key:scene.local.light.color>")
    @WikiReturn("<key:scene.local.light.color.return>")
    @AiDoc(order = 10, summary = "Returns the packed RGB color in the low 24 bits.")
    int colorRgb();

    /**
     * @return finite, nonnegative brightness multiplier, with zero disabling the light
     */
    @WikiMethod(order = 20, summary = "<key:scene.local.light.intensity>")
    @WikiReturn("<key:scene.local.light.intensity.return>")
    @AiDoc(order = 20, summary = "Returns the nonnegative brightness multiplier.")
    float intensity();

    /**
     * @return cutoff distance in scene units, finite and at least 0.001
     */
    @WikiMethod(order = 30, summary = "<key:scene.local.light.range>")
    @WikiReturn("<key:scene.local.light.range.return>")
    @AiDoc(order = 30, summary = "Returns the cutoff distance in scene units.")
    float range();
}
