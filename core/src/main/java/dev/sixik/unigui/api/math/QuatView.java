package dev.sixik.unigui.api.math;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Read-only access to a unit rotation quaternion. Components are dimensionless and ordered XYZW.
 */
@WikiPage(
        title = "QuatView", slug = "quat-view",
        category = "API/3D Scene", order = 260,
        summary = "Read-only view of a rotation quaternion in XYZW order.")
@WikiSection(title = "Rotation convention", order = 10, markdown = """
        The scene uses active rotations in a right-handed coordinate system. Use a `MutableQuat` when you need to
        construct or compose a rotation.
        """)
@WikiLink(label = "MutableQuat", target = "page:mutable-quat")
@WikiLink(label = "MutableMat4", target = "page:mutable-mat-4")
@WikiLink(label = "Camera3D", target = "page:camera-3d")
@AiDoc(
        summary = "Read-only view of a rotation quaternion in XYZW order.",
        purpose = "Pass unit rotations to transforms and camera APIs.",
        criticalRules = {
                "Components are dimensionless and W is the scalar component. The view may refer to mutable storage."
        },
        related = {"MutableQuat", "MutableMat4", "Camera3D"})
public interface QuatView {
    /**
     * Reads the X vector component.
     *
     * @return dimensionless X.
     */
    @WikiMethod(section = "Components", sectionOrder = 10, order = 10,
            summary = "Reads the X vector component.")
    @WikiReturn("dimensionless X")
    @AiDoc(section = "Components", sectionOrder = 10, order = 10,
            summary = "Reads the X vector component.")
    float x();

    /**
     * Reads the Y vector component.
     *
     * @return dimensionless Y.
     */
    @WikiMethod(section = "Components", sectionOrder = 10, order = 20,
            summary = "Reads the Y vector component.")
    @WikiReturn("dimensionless Y")
    @AiDoc(section = "Components", sectionOrder = 10, order = 20,
            summary = "Reads the Y vector component.")
    float y();

    /**
     * Reads the Z vector component.
     *
     * @return dimensionless Z.
     */
    @WikiMethod(section = "Components", sectionOrder = 10, order = 30,
            summary = "Reads the Z vector component.")
    @WikiReturn("dimensionless Z")
    @AiDoc(section = "Components", sectionOrder = 10, order = 30,
            summary = "Reads the Z vector component.")
    float z();

    /**
     * Reads the scalar component.
     *
     * @return dimensionless W.
     */
    @WikiMethod(section = "Components", sectionOrder = 10, order = 40,
            summary = "Reads the scalar component.")
    @WikiReturn("dimensionless W")
    @AiDoc(section = "Components", sectionOrder = 10, order = 40,
            summary = "Reads the scalar component.")
    float w();
}
