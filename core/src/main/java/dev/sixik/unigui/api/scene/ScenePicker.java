package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;

import java.util.function.Consumer;

/**
 * Reusable UI-thread picker that finds the nearest node along a viewport ray.
 */
@WikiPage(
        title = "ScenePicker", slug = "scene-picker",
        category = "API/3D Scene", order = 80,
        summary = "Ray picking for scene meshes, bounds and block cells.")
@WikiSection(title = "Picking coordinates", order = 10, markdown = """
        A camera ray starts on the near plane and is clipped at the far plane. Distances are compared in world units
        even when a node is scaled. Use the same camera and logical viewport for rendering and picking.
        """)
@WikiExample(title = "Pick a cell at a canvas point", code = """
        ScenePicker picker = new ScenePicker();
        MutableVec3 cell = new MutableVec3();
        SceneNode hit = picker.pick(scene, camera, canvasX, canvasY, cell);
        if (hit != null && Float.isFinite(cell.x())) {
            System.out.println("Cell: " + cell.x() + ", " + cell.y() + ", " + cell.z());
        }
        """)
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@WikiLink(label = "Scene", target = "page:scene")
@WikiLink(label = "SceneNode", target = "page:scene-node")
@WikiLink(label = "Camera3D", target = "page:camera-3d")
@AiDoc(
        summary = "Ray picking for scene meshes, bounds and block cells.",
        purpose = "Pick a node from logical canvas coordinates when rendering or handling input outside Scene3D.",
        criticalRules = {
                "Use `Scene3D.pickNode` for widget-local coordinates. The standalone picker expects logical camera-canvas coordinates.",
                "Tests between near and far planes and returns the nearest hit. It tests both sides of mesh triangles independently of material culling.",
                "The optional cell output is filled with NaN for misses or object-only hits."
        },
        related = {"Scene3D", "Scene", "SceneNode", "Camera3D"},
        examples = @AiDoc.Example(title = "Pick a cell at a canvas point", code = """
                ScenePicker picker = new ScenePicker();
                MutableVec3 cell = new MutableVec3();
                SceneNode hit = picker.pick(scene, camera, canvasX, canvasY, cell);
                if (hit != null && Float.isFinite(cell.x())) {
                    System.out.println("Cell: " + cell.x() + ", " + cell.y() + ", " + cell.z());
                }
                """))
public final class ScenePicker {
    /**
     * Reusable candidate visitor.
     */
    private final Consumer<SceneNode> visit = this::testNode;
    /**
     * World-space ray starting on the near plane.
     */
    private final Ray worldRay = new Ray();
    /**
     * Ray in the candidate node's local coordinates.
     */
    private final Ray localRay = new Ray();
    /**
     * World-space ray origin.
     */
    private final MutableVec3 origin = new MutableVec3();
    /**
     * Unit world-space ray direction.
     */
    private final MutableVec3 direction = new MutableVec3();
    /**
     * Point on the far plane, reused for local coordinate conversion.
     */
    private final MutableVec3 point = new MutableVec3();
    /**
     * Candidate cell's lower corner, or NaN for an object hit.
     */
    private final MutableVec3 candidateCell = new MutableVec3();
    /**
     * Winning cell's lower corner, or NaN for an object hit.
     */
    private final MutableVec3 hitCell = new MutableVec3();
    /**
     * Working position of the first triangle vertex.
     */
    private final MutableVec3 a = new MutableVec3();
    /**
     * Working position of the second triangle vertex.
     */
    private final MutableVec3 b = new MutableVec3();
    /**
     * Working position of the third triangle vertex.
     */
    private final MutableVec3 c = new MutableVec3();
    /**
     * Transform from the candidate's space to world space.
     */
    private final MutableMat4 world = new MutableMat4();
    /**
     * Transform from world space to the candidate's space.
     */
    private final MutableMat4 inverse = new MutableMat4();
    /**
     * Closest distance in world units, initially the distance to the far plane along the ray.
     */
    private double closest;
    /**
     * Closest node found so far, or null.
     */
    private SceneNode hit;

    /**
     * Allocates the workspace for repeated picks.
     */
    @WikiMethod(section = "Picking", sectionOrder = 10, order = 10,
            summary = "Allocates the workspace for repeated picks.")
    @AiDoc(section = "Picking", sectionOrder = 10, order = 10,
            summary = "Allocates the workspace for repeated picks.")
    public ScenePicker() {
    }

    /**
     * Finds the nearest hit between the clip planes. For a mesh it checks both sides of the triangles after
     * a bounds test. For a node without a mesh it checks local content bounds or cells when the transform is invertible.
     *
     * @param scene node hierarchy and visibility.
     * @param camera camera sized to the logical canvas.
     * @param x horizontal canvas coordinate in logical UI units.
     * @param y vertical canvas coordinate in logical UI units.
     * @return closest node, or null for a miss.
     */
    @WikiMethod(section = "Picking", sectionOrder = 10, order = 20,
            summary = "Finds the nearest hit between the clip planes. For a mesh it checks both sides of the triangles after a bounds test. For a node without a mesh it checks local content bounds or cells when the transform is invertible.")
    @WikiReturn("closest node, or null for a miss")
    @AiDoc(section = "Picking", sectionOrder = 10, order = 20,
            summary = "Finds the nearest hit between the clip planes. For a mesh it checks both sides of the triangles after a bounds test. For a node without a mesh it checks local content bounds or cells when the transform is invertible.")
    public SceneNode pick(
            @WikiParameter(description = "node hierarchy and visibility.") Scene scene,
            @WikiParameter(description = "camera sized to the logical canvas.") Camera3D camera,
            @WikiParameter(description = "horizontal canvas coordinate in logical UI units.") float x,
            @WikiParameter(description = "vertical canvas coordinate in logical UI units.") float y) {
        return pick(scene, camera, x, y, null);
    }

    /**
     * Picks a node and, for a unit-cell hit, copies the cell's lower corner.
     *
     * @param scene scene graph.
     * @param camera camera sized to the logical canvas.
     * @param x horizontal canvas coordinate in logical UI units.
     * @param y vertical canvas coordinate in logical UI units.
     * @param cell optional output for the local cell origin. Receives NaN when no cell is hit.
     * @return closest node, or null for a miss.
     */
    @WikiMethod(section = "Picking", sectionOrder = 10, order = 20,
            summary = "Picks a node and, for a unit-cell hit, copies the cell's lower corner.")
    @WikiReturn("closest node, or null for a miss")
    @AiDoc(section = "Picking", sectionOrder = 10, order = 20,
            summary = "Picks a node and, for a unit-cell hit, copies the cell's lower corner.")
    public SceneNode pick(
            @WikiParameter(description = "scene graph.") Scene scene,
            @WikiParameter(description = "camera sized to the logical canvas.") Camera3D camera,
            @WikiParameter(description = "horizontal canvas coordinate in logical UI units.") float x,
            @WikiParameter(description = "vertical canvas coordinate in logical UI units.") float y,
            @WikiParameter(description =
                    "optional output for the local cell origin. Receives NaN when no cell is hit.")
            MutableVec3 cell) {
        if (cell != null) cell.set(Float.NaN, Float.NaN, Float.NaN);
        if (!camera.screenRay(x, y, worldRay) || !camera.unproject(x, y, 1.0, point)) return null;
        worldRay.origin(origin);
        worldRay.direction(direction);
        double dx = (double) point.x() - origin.x();
        double dy = (double) point.y() - origin.y();
        double dz = (double) point.z() - origin.z();
        closest = Math.sqrt(dx * dx + dy * dy + dz * dz);
        try {
            scene.visitVisible(null, visit);
            if (cell != null && hit != null) cell.set(hitCell);
            return hit;
        } finally {
            hit = null;
        }
    }

    /**
     * Transforms the ray to node space and compares distances in world units. The unnormalized local direction
     * is {@code inverse * (worldDirection, 0)}. Divide the local hit distance by its length to undo normalization.
     *
     * @param node visible candidate.
     */
    private void testNode(SceneNode node) {
        Aabb bounds = node.bounds();
        if (bounds == null) return;
        Mesh3D mesh = node.mesh();
        candidateCell.set(Float.NaN, Float.NaN, Float.NaN);
        node.worldMatrix(world);
        if (!inverse.setInverse(world)) {
            if (mesh != null) accept(node, intersectTriangles(mesh, worldRay, world));
            return;
        }

        double dx = inverse.transformComponent(0, direction.x(), direction.y(), direction.z(), 0);
        double dy = inverse.transformComponent(1, direction.x(), direction.y(), direction.z(), 0);
        double dz = inverse.transformComponent(2, direction.x(), direction.y(), direction.z(), 0);
        double directionLength = Math.hypot(Math.hypot(dx, dy), dz);
        if (!(directionLength > 0.0) || !Double.isFinite(directionLength)
                || !inverse.transformPoint(origin, point) || !localRay.set(point, dx, dy, dz)) {
            return;
        }
        double limit = closest * directionLength;
        double boxDistance = localRay.intersectAabb(bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ());
        if (!Double.isFinite(boxDistance) || boxDistance > limit) return;
        double distance = node.intersectPickBounds(localRay, boxDistance, limit, candidateCell);
        if (mesh != null) {
            double triangle = intersectTriangles(mesh, localRay, null);
            if (triangle < distance) {
                distance = triangle;
                candidateCell.set(Float.NaN, Float.NaN, Float.NaN);
            }
        }
        accept(node, distance / directionLength);
    }

    /**
     * Finds the nearest triangle in an indexed mesh.
     *
     * @param mesh geometry in local coordinates.
     * @param ray ray in the coordinate space used to test the vertices.
     * @param transform optional local-to-world transform used when the node transform is singular.
     * @return closest distance along ray, or positive infinity for a miss.
     */
    private double intersectTriangles(Mesh3D mesh, Ray ray, MutableMat4 transform) {
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0, size = mesh.indexCount(); i < size; i += 3) {
            vertex(mesh, mesh.index(i), a);
            vertex(mesh, mesh.index(i + 1), b);
            vertex(mesh, mesh.index(i + 2), c);
            if (transform != null && (!transform.transformPoint(a, a) || !transform.transformPoint(b, b)
                    || !transform.transformPoint(c, c))) {
                continue;
            }
            best = Math.min(best, ray.intersectTriangle(a, b, c));
        }
        return best;
    }

    /**
     * Keeps the nearest hit and breaks ties by traversal order.
     *
     * @param node candidate node.
     * @param distance distance in world scene units.
     */
    private void accept(SceneNode node, double distance) {
        if (distance <= closest && (hit == null || distance < closest)) {
            closest = distance;
            hit = node;
            hitCell.set(candidateCell);
        }
    }

    /**
     * Reads a vertex position into reusable storage.
     *
     * @param mesh source mesh.
     * @param index vertex index.
     * @param out output vector in mesh-local units.
     */
    private static void vertex(Mesh3D mesh, int index, MutableVec3 out) {
        out.set(mesh.position(index, 0), mesh.position(index, 1), mesh.position(index, 2));
    }
}
