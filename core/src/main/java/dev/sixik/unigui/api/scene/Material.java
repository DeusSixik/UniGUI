package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import dev.sixik.unigui.api.render.TextureHandle;

import java.util.Objects;

/**
 * Immutable surface settings for a mesh.
 *
 * @param colorArgb ARGB tint multiplied by the vertex and texture colors.
 * @param texture backend texture, or null for an untextured surface.
 * @param shading lighting mode.
 * @param alphaMode alpha handling mode.
 * @param alphaCutoff minimum alpha kept in {@link AlphaMode#CUTOUT} mode, in [0, 1].
 * @param culling triangle faces to cull.
 */
@WikiPage(
        title = "Material", slug = "scene-material",
        category = "API/3D Scene", order = 100,
        summary = "Immutable mesh surface settings for tint, texture, lighting, alpha and face culling.")
@WikiSection(title = "Rendering options", order = 10, markdown = """
        Use `Material.DEFAULT` for untextured opaque white with `UNLIT` shading and back-face culling. The alpha
        modes are `OPAQUE`, `CUTOUT` (with `alphaCutoff` in `[0, 1]`) and `TRANSLUCENT`. Culling accepts `NONE`,
        `BACK` or `FRONT`.
        """)
@WikiExample(title = "Make a translucent two-sided surface", code = """
        Material glass = new Material(0x6688CCFF, null, Material.Shading.UNLIT,
                Material.AlphaMode.TRANSLUCENT, 0.5f, Material.Culling.NONE);
        node.setMaterial(glass);
        """)
@WikiLink(label = "SceneNode", target = "page:scene-node")
@WikiLink(label = "Mesh3D", target = "page:mesh-3d")
@WikiLink(label = "AmbientLight", target = "page:scene-ambient-light")
@WikiLink(label = "DirectionalLight", target = "page:scene-directional-light")
@AiDoc(
        summary = "Immutable mesh surface settings for tint, texture, lighting, alpha and face culling.",
        purpose = "Configure the render state of custom meshes and hover highlights.",
        criticalRules = {
                "Color is packed ARGB and multiplies the vertex and texture colors.",
                "Use LIT to apply scene lights. UNLIT uses the supplied color data without scene lighting.",
                "Choose CUTOUT for binary alpha. TRANSLUCENT blends and is order-dependent. Set Culling.NONE for two-sided geometry."
        },
        related = {"SceneNode", "Mesh3D", "AmbientLight", "DirectionalLight"},
        examples = @AiDoc.Example(title = "Make a translucent two-sided surface", code = """
                Material glass = new Material(0x6688CCFF, null, Material.Shading.UNLIT,
                        Material.AlphaMode.TRANSLUCENT, 0.5f, Material.Culling.NONE);
                node.setMaterial(glass);
                """))
public record Material(
        @WikiParameter(description = "ARGB tint multiplied by vertex and texture colors.") int colorArgb,
        @WikiParameter(description = "Backend texture handle, or null for an untextured surface.") TextureHandle texture,
        @WikiParameter(description = "Scene lighting mode.") Shading shading,
        @WikiParameter(description = "Alpha handling mode.") AlphaMode alphaMode,
        @WikiParameter(description = "Minimum alpha in CUTOUT mode, in [0, 1].") float alphaCutoff,
        @WikiParameter(description = "Faces excluded from rendering.") Culling culling) {
    /**
     * Opaque white unlit surface with back-face culling.
     */
    public static final Material DEFAULT = new Material(0xFFFFFFFF, null, Shading.UNLIT,
            AlphaMode.OPAQUE, 0.5f, Culling.BACK);

    /**
     * Validates and stores surface settings.
     *
     * @param colorArgb ARGB tint.
     * @param texture backend texture, or null.
     * @param shading lighting mode.
     * @param alphaMode alpha handling mode.
     * @param alphaCutoff alpha threshold in [0, 1].
     * @param culling triangle faces to cull.
     * @throws NullPointerException if shading, alphaMode or culling is null.
     * @throws IllegalArgumentException if alphaCutoff is outside [0, 1] or NaN.
     */
    @WikiMethod(section = "Surface", sectionOrder = 10, order = 10,
            summary = "Validates and stores surface settings.")
    @WikiThrows(type = NullPointerException.class, description = "if shading, alphaMode or culling is null.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if alphaCutoff is outside [0, 1] or NaN.")
    @AiDoc(section = "Surface", sectionOrder = 10, order = 10,
            summary = "Validates and stores surface settings.")
    public Material {
        Objects.requireNonNull(shading, "shading");
        Objects.requireNonNull(alphaMode, "alphaMode");
        Objects.requireNonNull(culling, "culling");
        if (!(alphaCutoff >= 0.0f && alphaCutoff <= 1.0f)) {
            throw new IllegalArgumentException("Alpha cutoff must be between 0 and 1");
        }
    }

    /**
     * Controls how lights affect the surface color.
     */
    public enum Shading {
        /**
         * Uses the material, vertex and texture colors.
         */
        UNLIT,
        /**
         * Includes ambient and directional lighting.
         */
        LIT
    }

    /**
     * Controls how the renderer handles surface alpha.
     */
    public enum AlphaMode {
        /**
         * Draws an opaque surface.
         */
        OPAQUE,
        /**
         * Discards fragments with alpha below the cutoff.
         */
        CUTOUT,
        /**
         * Blends the surface with the background according to alpha.
         */
        TRANSLUCENT
    }

    /**
     * Controls triangle culling by face orientation.
     */
    public enum Culling {
        /**
         * Draws both faces.
         */
        NONE,
        /**
         * Culls back faces.
         */
        BACK,
        /**
         * Culls front faces.
         */
        FRONT
    }
}
