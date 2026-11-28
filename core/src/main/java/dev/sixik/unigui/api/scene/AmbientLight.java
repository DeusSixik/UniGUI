package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;

/**
 * Ambient light that contributes equally to every surface orientation.
 *
 * @param colorRgb color in the low 24 bits, in RGB order.
 * @param intensity finite nonnegative brightness multiplier. Zero disables the contribution.
 */
@WikiPage(
        title = "AmbientLight", slug = "scene-ambient-light",
        category = "API/3D Scene", order = 110,
        summary = "Uniform light contribution to lit scene meshes.")
@WikiSection(title = "Default light", order = 10, markdown = """
        `AmbientLight.WHITE` has color `0xFFFFFF` and intensity `1`. Lower the intensity if the directional
        contribution should stand out.
        """)
@WikiExample(title = "Lower ambient intensity", code = """
        scene.setAmbientLight(new AmbientLight(0xBFD6FF, 0.4f));
        """)
@WikiLink(label = "Scene", target = "page:scene")
@WikiLink(label = "Material", target = "page:scene-material")
@WikiLink(label = "DirectionalLight", target = "page:scene-directional-light")
@AiDoc(
        summary = "Uniform light contribution to lit scene meshes.",
        purpose = "Control the baseline color and brightness on surfaces with LIT shading.",
        criticalRules = {
                "Use packed RGB and a nonnegative finite intensity. Zero disables this contribution."
        },
        related = {"Scene", "Material", "DirectionalLight"},
        examples = @AiDoc.Example(title = "Lower ambient intensity", code = """
                scene.setAmbientLight(new AmbientLight(0xBFD6FF, 0.4f));
                """))
public record AmbientLight(
        @WikiParameter(description = "RGB color in the low 24 bits.") int colorRgb,
        @WikiParameter(description = "Finite nonnegative multiplier. Zero disables the light.") float intensity) {
    /**
     * Neutral white ambient light with unit intensity.
     */
    public static final AmbientLight WHITE = new AmbientLight(0xFFFFFF, 1.0f);

    /**
     * Stores the color and intensity.
     *
     * @param colorRgb RGB color. The high byte is ignored.
     * @param intensity finite nonnegative brightness multiplier.
     * @throws IllegalArgumentException if intensity is negative or nonfinite.
     */
    @WikiMethod(section = "Light", sectionOrder = 10, order = 10,
            summary = "Stores the color and intensity.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if intensity is negative or nonfinite.")
    @AiDoc(section = "Light", sectionOrder = 10, order = 10,
            summary = "Stores the color and intensity.")
    public AmbientLight {
        if (!(intensity >= 0.0f) || !Float.isFinite(intensity)) {
            throw new IllegalArgumentException("Light intensity must be finite and nonnegative");
        }
        colorRgb &= 0xFFFFFF;
    }
}
