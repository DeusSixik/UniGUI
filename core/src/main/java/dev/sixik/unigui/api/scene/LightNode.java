package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.Objects;

/**
 * Places a local light in the scene hierarchy on Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3.
 */
@WikiPage(title = "LightNode", slug = "scene-light-node", category = "<key:scene.category>",
        summary = "<key:light.node.summary>")
@WikiSection(title = "<key:light.node.section.projection.title>", markdown = "<key:light.node.section.projection.body>")
@WikiLink(label = "PointLight", target = "page:scene-point-light")
@WikiLink(label = "SpotLight", target = "page:scene-spot-light")
@WikiLink(label = "SpotShadows", target = "page:scene-spot-shadows")
@WikiLink(label = "LightFlicker", target = "page:scene-light-flicker")
@WikiExample(title = "<key:light.node.example.spotlight.title>", code = """
        var node = new LightNode(new SpotLight(0xCDEAFF, 1.5f, 8f, 0.38f, 0.60f)
                .withMask(new SimpleTextureHandle("example:textures/light/window.png", 128, 128))
                .withShadows(new SpotShadows()));
        node.setPosition(0, 4, 2).setRotation(new MutableQuat().setYawPitch(0, -0.75));
        scene.addRoot(node);
        """)
@AiDoc(summary = "A transformable PointLight or SpotLight for Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3 scenes.",
        criticalRules = {
                "Use Scene.addRoot or attach LightNode as a child. A hidden ancestor disables the light.",
                "SpotLight emits along local -Z. Angles are half-angles in radians and range is in world units.",
                "Use SpotLight.withMask(TextureHandle) for a shaped or colored texture projection.",
                "Use SpotLight.withShadows(SpotShadows) to add a perspective map. PointLight has no shadow map.",
                "The renderer uses up to eight active lights and two spotlight shadow maps in scene order."
        }, related = {"Scene", "SceneNode", "SceneTimeline", "PointLight", "SpotLight", "SpotShadows"})
public final class LightNode extends SceneNode {
    /**
     * Local light configuration, always non-null.
     */
    private LocalLight light;

    /**
     * Creates a light node at the origin.
     *
     * @param light initial light configuration
     * @throws NullPointerException if light is null
     */
    public LightNode(LocalLight light) {
        this.light = Objects.requireNonNull(light, "light");
    }

    /**
     * @return the immutable light configuration
     */
    public LocalLight light() {
        return light;
    }

    /**
     * Replaces the configuration and invalidates the rendered scene if it changes.
     *
     * @param light new light configuration
     * @return this node
     * @throws NullPointerException if light is null
     */
    public LightNode setLight(LocalLight light) {
        if (!this.light.equals(Objects.requireNonNull(light, "light"))) {
            this.light = light;
            invalidateContent();
        }
        return this;
    }
}
