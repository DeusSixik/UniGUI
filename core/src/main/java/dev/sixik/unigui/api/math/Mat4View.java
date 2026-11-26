package dev.sixik.unigui.api.math;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;

/**
 * Read-only access to a 4-by-4 matrix that acts on column vectors from the left.
 */
@WikiPage(
        title = "Mat4View", slug = "mat-4-view",
        category = "API/3D Scene", order = 270,
        summary = "Read-only access to a 4-by-4 matrix for column vectors.")
@WikiSection(title = "Matrix entries", order = 10, markdown = """
        Use `get(row, column)` to read an entry. With `M * p`, the columns of an affine transform contain the basis
        and translation.
        """)
@WikiLink(label = "MutableMat4", target = "page:mutable-mat-4")
@WikiLink(label = "SceneNode", target = "page:scene-node")
@WikiLink(label = "Camera3D", target = "page:camera-3d")
@AiDoc(
        summary = "Read-only access to a 4-by-4 matrix for column vectors.",
        purpose = "Pass matrix data between camera, node and backend APIs.",
        criticalRules = {
                "`get(row, column)` uses zero-based row and column indices. Column-major layout is a storage convention."
        },
        related = {"MutableMat4", "SceneNode", "Camera3D"})
public interface Mat4View {
    /**
     * Reads a matrix element.
     *
     * @param row row index from 0 to 3.
     * @param column column index from 0 to 3.
     * @return the element value.
     * @throws IndexOutOfBoundsException if either index is outside its range.
     */
    @WikiMethod(section = "Matrix", sectionOrder = 10, order = 10,
            summary = "Reads a matrix element.")
    @WikiReturn("the element value")
    @WikiThrows(type = IndexOutOfBoundsException.class, description = "if either index is outside its range.")
    @AiDoc(section = "Matrix", sectionOrder = 10, order = 10,
            summary = "Reads a matrix element.")
    double get(
            @WikiParameter(description = "row index from 0 to 3.") int row,
            @WikiParameter(description = "column index from 0 to 3.") int column);
}
