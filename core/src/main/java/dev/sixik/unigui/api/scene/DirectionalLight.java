package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;

/**
 * Light with parallel rays. The Lambertian contribution is
 * {@code intensity * max(0, -dot(n, direction))}, where n is the unit world-space normal
 * and direction points along the light rays.
 *
 * @param x X component of the normalized ray direction.
 * @param y Y component of the normalized ray direction.
 * @param z Z component of the normalized ray direction.
 * @param colorRgb color in the low 24 bits, in RGB order.
 * @param intensity finite nonnegative brightness multiplier. Zero disables the contribution.
 */
@WikiPage(
        title = "DirectionalLight", slug = "scene-directional-light",
        category = "API/3D Scene", order = 120,
        summary = "Parallel light rays with a world-space direction and intensity.")
@WikiSection(title = "Diffuse contribution", order = 10, markdown = """
        A surface with unit world-space normal `n` receives `intensity * max(0, -dot(n, direction))`, where
        `direction` points along the rays. Pass null to `scene.setDirectionalLight` to remove the light.
        """)
@WikiExample(title = "Add overhead light", code = """
        scene.setDirectionalLight(new DirectionalLight(1, -2, 1, 0xFFF1DB, 0.7f));
        """)
@WikiLink(label = "Scene", target = "page:scene")
@WikiLink(label = "Material", target = "page:scene-material")
@WikiLink(label = "AmbientLight", target = "page:scene-ambient-light")
@AiDoc(
        summary = "Parallel light rays with a world-space direction and intensity.",
        purpose = "Add directional contrast to custom meshes using LIT shading.",
        criticalRules = {
                "The vector points along the light rays. A negative Y component shines down.",
                "The direction must be nonzero and finite and is normalized automatically."
        },
        related = {"Scene", "Material", "AmbientLight"},
        examples = @AiDoc.Example(title = "Add overhead light", code = """
                scene.setDirectionalLight(new DirectionalLight(1, -2, 1, 0xFFF1DB, 0.7f));
                """))
public record DirectionalLight(float x, float y, float z, int colorRgb, float intensity) {
    /**
     * Normalizes the ray direction by dividing x, y and z by {@code sqrt(x*x + y*y + z*z)}.
     *
     * @param x world-space X component of the ray direction.
     * @param y world-space Y component of the ray direction.
     * @param z world-space Z component of the ray direction.
     * @param colorRgb RGB color. The high byte is ignored.
     * @param intensity finite nonnegative brightness multiplier.
     * @throws IllegalArgumentException if the direction is zero or nonfinite, or the intensity is invalid.
     */
    @WikiMethod(section = "Light", sectionOrder = 10, order = 10,
            summary = "Normalizes the ray direction by dividing x, y and z by `sqrt(x*x + y*y + z*z)`.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the direction is zero or nonfinite, or the intensity is invalid.")
    @AiDoc(section = "Light", sectionOrder = 10, order = 10,
            summary = "Normalizes the ray direction by dividing x, y and z by `sqrt(x*x + y*y + z*z)`.")
    public DirectionalLight {
        double length = Math.sqrt((double) x * x + (double) y * y + (double) z * z);
        if (!(length > 0.0) || !Double.isFinite(length) || !(intensity >= 0.0f) || !Float.isFinite(intensity)) {
            throw new IllegalArgumentException("Light requires a finite nonzero direction and nonnegative intensity");
        }
        x = (float) (x / length);
        y = (float) (y / length);
        z = (float) (z / length);
        colorRgb &= 0xFFFFFF;
    }
}
