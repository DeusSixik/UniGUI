package dev.sixik.unigui.api.math;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Six view half-spaces for a perspective or orthographic camera.
 */
@WikiPage(
        title = "Frustum3D", slug = "frustum-3d",
        category = "API/3D Scene", order = 230,
        summary = "Six-plane view volume for conservative culling.")
@WikiSection(title = "Using a frustum", order = 10, markdown = """
        Update the frustum after the camera's pose or projection changes. Pass it to `Scene.visitVisible` to cull a
        graph. Test an individual bound with `intersects(bounds, world)`.
        """)
@WikiExample(title = "Visit potentially visible nodes", code = """
        Frustum3D frustum = new Frustum3D().set(camera.viewProjection(new MutableMat4()));
        scene.visitVisible(frustum, node -> renderNode(node));
        """)
@WikiLink(label = "Camera3D", target = "page:camera-3d")
@WikiLink(label = "Scene", target = "page:scene")
@WikiLink(label = "Aabb", target = "page:aabb")
@WikiLink(label = "MutableMat4", target = "page:mutable-mat-4")
@AiDoc(
        summary = "Six-plane view volume for conservative culling.",
        purpose = "Reject scene content whose transformed local bounds lie outside the view.",
        criticalRules = {
                "Build from projection times view, with NDC depth in [-1, 1].",
                "Test a node's local AABB with its local-to-world matrix. A true result means potentially visible."
        },
        related = {"Camera3D", "Scene", "Aabb", "MutableMat4"},
        examples = @AiDoc.Example(title = "Visit potentially visible nodes", code = """
                Frustum3D frustum = new Frustum3D().set(camera.viewProjection(new MutableMat4()));
                scene.visitVisible(frustum, node -> renderNode(node));
                """))
public final class Frustum3D {
    /**
     * Rounding tolerance in units of clip W for projection coefficients.
     */
    private static final double CLIP_BOUNDARY = 1.0 + 8.0 * Math.ulp(1.0);
    /**
     * Plane coefficients (a, b, c, d) for left, right, bottom, top, near and far.
     */
    private final double[] planes = {1, 0, 0, 1, -1, 0, 0, 1, 0, 1, 0, 1,
            0, -1, 0, 1, 0, 0, 1, 1, 0, 0, -1, 1};

    /**
     * Creates the identity projection volume, from -1 through 1 on each axis.
     */
    @WikiMethod(section = "Culling", sectionOrder = 10, order = 10,
            summary = "Creates the identity projection volume, from -1 through 1 on each axis.")
    @AiDoc(section = "Culling", sectionOrder = 10, order = 10,
            summary = "Creates the identity projection volume, from -1 through 1 on each axis.")
    public Frustum3D() {
    }

    /**
     * Extracts planes from V = projection * view. The clip inequalities
     * {@code -clip.w <= clip.x,y,z <= clip.w} produce rows {@code V[3] ± V[i]}, where i is the clip axis.
     * {@link #CLIP_BOUNDARY} expands the bounds relative to clip W to allow for rounding at contact.
     *
     * @param viewProjection world-to-clip transform with NDC depth from -1 to 1.
     * @return this frustum.
     */
    @WikiMethod(section = "Culling", sectionOrder = 10, order = 20,
            summary = "Extracts planes from V = projection * view. The clip inequalities `-clip.w <= clip.x,y,z <= clip.w` produce rows `V[3] ± V[i]`, where i is the clip axis. `CLIP_BOUNDARY` expands the bounds relative to clip W to allow for rounding at contact.")
    @WikiReturn("this frustum")
    @AiDoc(section = "Culling", sectionOrder = 10, order = 20,
            summary = "Extracts planes from V = projection * view. The clip inequalities `-clip.w <= clip.x,y,z <= clip.w` produce rows `V[3] ± V[i]`, where i is the clip axis. `CLIP_BOUNDARY` expands the bounds relative to clip W to allow for rounding at contact.")
    public Frustum3D set(
            @WikiParameter(description = "world-to-clip transform with NDC depth from -1 to 1.") Mat4View viewProjection) {
        for (int axis = 0; axis < 3; axis++) {
            int plane = axis * 8;
            for (int column = 0; column < 4; column++) {
                double w = CLIP_BOUNDARY * viewProjection.get(3, column);
                double coordinate = viewProjection.get(axis, column);
                planes[plane + column] = w + coordinate;
                planes[plane + 4 + column] = w - coordinate;
            }
        }
        return this;
    }

    /**
     * Tests whether to cull the local AABB transformed by M. Each world plane p maps to the local
     * equation {@code p * M}. Maximize that equation over the AABB by selecting the corner according to
     * the signs of the X, Y and Z coefficients. A negative maximum puts the box entirely outside that plane.
     *
     * @param bounds local node bounds.
     * @param world affine matrix M from node to world space.
     * @return false if a plane excludes the entire box, true if the box may be visible.
     */
    @WikiMethod(section = "Culling", sectionOrder = 10, order = 30,
            summary = "Tests whether to cull the local AABB transformed by M. Each world plane p maps to the local equation `p * M`. Maximize that equation over the AABB by selecting the corner according to the signs of the X, Y and Z coefficients. A negative maximum puts the box entirely outside that plane.")
    @WikiReturn("false if a plane excludes the entire box, true if the box may be visible")
    @AiDoc(section = "Culling", sectionOrder = 10, order = 30,
            summary = "Tests whether to cull the local AABB transformed by M. Each world plane p maps to the local equation `p * M`. Maximize that equation over the AABB by selecting the corner according to the signs of the X, Y and Z coefficients. A negative maximum puts the box entirely outside that plane.")
    public boolean intersects(
            @WikiParameter(description = "local node bounds.") Aabb bounds,
            @WikiParameter(description = "affine matrix M from node to world space.") Mat4View world) {
        for (int plane = 0; plane < planes.length; plane += 4) {
            double x = localCoefficient(plane, world, 0);
            double y = localCoefficient(plane, world, 1);
            double z = localCoefficient(plane, world, 2);
            double wx = x * (x >= 0.0 ? bounds.maxX() : bounds.minX());
            double wy = y * (y >= 0.0 ? bounds.maxY() : bounds.minY());
            double wz = z * (z >= 0.0 ? bounds.maxZ() : bounds.minZ());
            double d = localCoefficient(plane, world, 3);
            double support = wx + wy + wz + d;
            if (support < 0.0) {
                // Allow for rounding when summing the contributions to the local plane equation.
                double roundoff = 8.0 * Math.ulp(Math.max(Math.max(Math.abs(wx), Math.abs(wy)),
                        Math.max(Math.abs(wz), Math.abs(d))));
                if (support < -roundoff) return false;
            }
        }
        return true;
    }

    /**
     * Multiplies a world-plane row by a column of the node matrix.
     *
     * @param plane array offset of the four world-plane coefficients.
     * @param world node-to-world matrix.
     * @param column local coefficient index from 0 to 3.
     * @return coefficient of X, Y or Z, or the constant term.
     */
    private double localCoefficient(int plane, Mat4View world, int column) {
        return planes[plane] * world.get(0, column) + planes[plane + 1] * world.get(1, column)
                + planes[plane + 2] * world.get(2, column) + planes[plane + 3] * world.get(3, column);
    }
}
