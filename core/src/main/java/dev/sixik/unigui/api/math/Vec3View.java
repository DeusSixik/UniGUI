package dev.sixik.unigui.api.math;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Read-only access to a three-component vector. Units are determined by the calling coordinate space.
 */
@WikiPage(
        title = "Vec3View", slug = "vec-3-view",
        category = "API/3D Scene", order = 250,
        summary = "Read-only view of three vector components.")
@WikiSection(title = "Components", order = 10, markdown = """
        `x()`, `y()` and `z()` read the three components. Copy a view into a `MutableVec3` if a callback exposes
        borrowed storage and you need to keep it.
        """)
@WikiLink(label = "MutableVec3", target = "page:mutable-vec-3")
@WikiLink(label = "SceneNode", target = "page:scene-node")
@WikiLink(label = "Camera3D", target = "page:camera-3d")
@AiDoc(
        summary = "Read-only view of three vector components.",
        purpose = "Accept a position, direction or scale without requiring a concrete mutable vector type.",
        criticalRules = {
                "Units and coordinate space come from the caller's contract. A read-only view may refer to mutable storage."
        },
        related = {"MutableVec3", "SceneNode", "Camera3D"})
public interface Vec3View {
    /**
     * Reads the X component.
     *
     * @return X in vector units.
     */
    @WikiMethod(section = "Components", sectionOrder = 10, order = 10,
            summary = "Reads the X component.")
    @WikiReturn("X in vector units")
    @AiDoc(section = "Components", sectionOrder = 10, order = 10,
            summary = "Reads the X component.")
    float x();

    /**
     * Reads the Y component.
     *
     * @return Y in vector units.
     */
    @WikiMethod(section = "Components", sectionOrder = 10, order = 20,
            summary = "Reads the Y component.")
    @WikiReturn("Y in vector units")
    @AiDoc(section = "Components", sectionOrder = 10, order = 20,
            summary = "Reads the Y component.")
    float y();

    /**
     * Reads the Z component.
     *
     * @return Z in vector units.
     */
    @WikiMethod(section = "Components", sectionOrder = 10, order = 30,
            summary = "Reads the Z component.")
    @WikiReturn("Z in vector units")
    @AiDoc(section = "Components", sectionOrder = 10, order = 30,
            summary = "Reads the Z component.")
    float z();
}
