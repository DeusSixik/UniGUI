package dev.sixik.unigui.api.math;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;

/**
 * Closed axis-aligned bounding box with finite coordinates. An axis may have zero extent.
 *
 * @param minX minimum X in scene units.
 * @param minY minimum Y in scene units.
 * @param minZ minimum Z in scene units.
 * @param maxX maximum X in scene units.
 * @param maxY maximum Y in scene units.
 * @param maxZ maximum Z in scene units.
 */
@WikiPage(
        title = "Aabb", slug = "aabb",
        category = "API/3D Scene", order = 220,
        summary = "Finite axis-aligned bounds in scene units.")
@WikiSection(title = "Local bounds", order = 10, markdown = """
        Use the same space as the enclosed geometry. A mesh already provides a local AABB through `Mesh3D.bounds()`.
        Use `SceneNode.setBounds(...)` to supply an explicit override.
        """)
@WikiExample(title = "Bound a unit block", code = """
        Aabb bounds = new Aabb(0, 0, 0, 1, 1, 1);
        node.setBounds(bounds);
        """)
@WikiLink(label = "SceneNode", target = "page:scene-node")
@WikiLink(label = "Ray", target = "page:scene-ray")
@WikiLink(label = "Frustum3D", target = "page:frustum-3d")
@AiDoc(
        summary = "Finite axis-aligned bounds in scene units.",
        purpose = "Supply local bounds for node culling, picking and hover frames.",
        criticalRules = {
                "Supply minimum and maximum coordinates for each axis. All values must be finite and zero extent is allowed."
        },
        related = {"SceneNode", "Ray", "Frustum3D"},
        examples = @AiDoc.Example(title = "Bound a unit block", code = """
                Aabb bounds = new Aabb(0, 0, 0, 1, 1, 1);
                node.setBounds(bounds);
                """))
public record Aabb(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
    /**
     * Validates and stores the bounds.
     *
     * @param minX minimum X.
     * @param minY minimum Y.
     * @param minZ minimum Z.
     * @param maxX maximum X.
     * @param maxY maximum Y.
     * @param maxZ maximum Z.
     * @throws IllegalArgumentException if any bound is nonfinite or a minimum exceeds its maximum.
     */
    @WikiMethod(section = "Bounds", sectionOrder = 10, order = 10,
            summary = "Validates and stores the bounds.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if any bound is nonfinite or a minimum exceeds its maximum.")
    @AiDoc(section = "Bounds", sectionOrder = 10, order = 10,
            summary = "Validates and stores the bounds.")
    public Aabb {
        if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(minZ)
                || !Float.isFinite(maxX) || !Float.isFinite(maxY) || !Float.isFinite(maxZ)
                || minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("AABB requires finite, ordered bounds on each axis");
        }
    }
}
