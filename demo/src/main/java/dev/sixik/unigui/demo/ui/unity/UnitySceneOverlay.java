package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.scene.AmbientLight;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.DirectionalLight;
import dev.sixik.unigui.api.scene.FogVolume;
import dev.sixik.unigui.api.scene.LightNode;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.widgets.rendering.Scene3D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Editor-only drawing on top of the Scene view: selection outlines, light and fog icons and their shape gizmos.
 * Wireframes are drawn by the scene renderer, see {@link dev.sixik.unigui.api.scene.SceneWireframe}.
 * Everything is projected with the viewport camera and drawn in canvas units, so the scene itself and the
 * Game view are not affected.
 */
final class UnitySceneOverlay {
    /**
     * Screen radius in canvas units within which a light icon can be clicked.
     */
    private static final float ICON_HIT_RADIUS = 10;
    /**
     * Wire and outline color of selected objects, like Unity's selection orange.
     */
    private static final MutableColor SELECTED = MutableColor.rgba255(255, 160, 40, 255);
    /**
     * Outline of light icons.
     */
    private static final MutableColor ICON_OUTLINE = MutableColor.rgba255(21, 29, 43, 230);
    /**
     * Light shape gizmo color.
     */
    private static final MutableColor LIGHT_GIZMO = MutableColor.rgba(1.0f, 0.93f, 0.55f, 0.9f);
    /**
     * Dimmer color for the inner spot cone.
     */
    private static final MutableColor LIGHT_GIZMO_DIM = MutableColor.rgba(1.0f, 0.93f, 0.55f, 0.45f);
    /**
     * Faint outline of the object under the pointer.
     */
    /**
     * Bounds of a selected fog volume.
     */
    private static final MutableColor FOG_GIZMO = MutableColor.rgba(0.62f, 0.82f, 1.0f, 0.95f);
    /**
     * Bounds of fog volumes that are not selected.
     */
    private static final MutableColor FOG_GIZMO_DIM = MutableColor.rgba(0.62f, 0.82f, 1.0f, 0.3f);
    /**
     * Hover outline color.
     */
    private static final MutableColor HOVER = MutableColor.rgba255(255, 190, 110, 150);
    /**
     * Selection outline width in canvas units.
     */
    private static final float OUTLINE_WIDTH = 2.4f;
    /**
     * Hover outline width in canvas units.
     */
    private static final float HOVER_WIDTH = 1.4f;
    /**
     * Triangles of a unit box, used to outline Minecraft content that has bounds but no mesh.
     */
    private static final Mesh3D UNIT_BOX = new Mesh3D(new float[]{
            0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1
    }, null, null, null, new int[]{
            0, 3, 2, 0, 2, 1, 4, 5, 6, 4, 6, 7, 0, 4, 7, 0, 7, 3,
            1, 2, 6, 1, 6, 5, 0, 1, 5, 0, 5, 4, 3, 7, 6, 3, 6, 2
    });
    /**
     * Edge adjacency of each outlined mesh, built once per mesh.
     */
    private static final Map<Mesh3D, Topology> TOPOLOGY = new WeakHashMap<>();
    /**
     * Number of segments for projected circles.
     */
    private static final int CIRCLE_SEGMENTS = 40;

    /**
     * Viewport whose camera projects the overlay.
     */
    private final Scene3D viewport;
    /**
     * Projected canvas X, Y and depth of the current mesh's vertices, depth NaN when outside the clip range.
     */
    private float[] screen = new float[0];
    /**
     * World positions of the current mesh's vertices.
     */
    private float[] worldPoints = new float[0];
    /**
     * Whether each triangle of the current mesh faces the camera.
     */
    private boolean[] facing = new boolean[0];
    /**
     * Scratch matrices and vectors reused while drawing.
     */
    private final MutableMat4 world = new MutableMat4();
    private final MutableVec3 point = new MutableVec3(), projected = new MutableVec3(),
            a = new MutableVec3(), b = new MutableVec3(), normal = new MutableVec3(), eye = new MutableVec3(),
            forward = new MutableVec3();
    /**
     * Scratch point pair reused for projected polylines.
     */
    private final MutableVec3 previous = new MutableVec3();

    /**
     * @param viewport Scene view viewport
     */
    UnitySceneOverlay(Scene3D viewport) {
        this.viewport = viewport;
    }

    /**
     * @param node     visible node
     * @param selected selected editor objects
     * @return true if the node or an ancestor is selected
     */
    static boolean isSelected(SceneNode node, Collection<UnitySceneObject> selected) {
        if (selected.isEmpty()) return false;
        for (SceneNode current = node; current != null; current = current.parent()) {
            if (current instanceof UnitySceneObject object && selected.contains(object)) return true;
        }
        return false;
    }

    /**
     * Outlines the selected objects in orange and the hovered object faintly, like Unity's selection outline.
     * The outline follows each mesh's silhouette: an edge is drawn when one of its two triangles faces the camera and
     * the other faces away, or when it borders a single triangle, as on the rim of a plane. A triangle with world
     * vertices p0, p1, p2 faces the camera when {@code ((p1 - p0) x (p2 - p0)) . (eye - p0) > 0}; only a difference
     * between neighbours matters, so the test does not depend on the mesh's winding. The lines are drawn on top of
     * the scene with the UI's line shaders, so the outline stays visible through other objects, as in Unity.
     *
     * @param draw     canvas of the viewport painter
     * @param selected selected editor objects
     * @param hovered  object under the pointer, or null
     */
    void paintOutlines(RenderContext draw, Collection<UnitySceneObject> selected, UnitySceneObject hovered) {
        Camera3D camera = viewport.camera();
        camera.position(eye);
        camera.orientation(new MutableQuat()).rotate(0, 0, -1, forward);
        if (hovered != null && !selected.contains(hovered)) outlineSubtree(draw, camera, hovered, HOVER, HOVER_WIDTH);
        for (UnitySceneObject object : selected) outlineSubtree(draw, camera, object, SELECTED, OUTLINE_WIDTH);
    }

    /**
     * Outlines every visible mesh of an object and its descendants, and the bounds of Minecraft content.
     */
    private void outlineSubtree(RenderContext draw, Camera3D camera, SceneNode node, MutableColor color, float width) {
        // Lights and fog have no surface to outline; their gizmos show the selection instead.
        if (!node.visible() || node instanceof LightNode || node instanceof FogVolume) return;
        if (node.mesh() != null) {
            outlineMesh(draw, camera, node.mesh(), node.worldMatrix(world), color, width);
        } else if (!(node instanceof UnitySceneObject) && node.bounds() != null) {
            // Minecraft content draws its own model; its bounds already cover its children.
            Aabb box = node.bounds();
            MutableMat4 boxMatrix = new MutableMat4().setTransform(
                    new MutableVec3(box.minX(), box.minY(), box.minZ()), new MutableQuat(),
                    new MutableVec3(box.maxX() - box.minX(), box.maxY() - box.minY(), box.maxZ() - box.minZ()));
            outlineMesh(draw, camera, UNIT_BOX, world.setProduct(node.worldMatrix(new MutableMat4()), boxMatrix),
                    color, width);
            return;
        }
        for (SceneNode child : node.children()) outlineSubtree(draw, camera, child, color, width);
    }

    /**
     * Draws the silhouette edges of one mesh placed by a world matrix.
     */
    private void outlineMesh(RenderContext draw, Camera3D camera, Mesh3D mesh, MutableMat4 matrix,
                             MutableColor color, float width) {
        Topology topology = TOPOLOGY.computeIfAbsent(mesh, Topology::new);
        int vertices = mesh.vertexCount();
        if (screen.length < vertices * 3) screen = new float[vertices * 3];
        if (worldPoints.length < vertices * 3) worldPoints = new float[vertices * 3];
        for (int v = 0; v < vertices; v++) {
            point.set(mesh.position(v, 0), mesh.position(v, 1), mesh.position(v, 2));
            int offset = v * 3;
            if (!matrix.transformPoint(point, point)) return;
            worldPoints[offset] = point.x();
            worldPoints[offset + 1] = point.y();
            worldPoints[offset + 2] = point.z();
            if (project(camera, point, projected)) {
                screen[offset] = projected.x();
                screen[offset + 1] = projected.y();
                screen[offset + 2] = projected.z();
            } else {
                screen[offset + 2] = Float.NaN;
            }
        }
        int triangles = mesh.indexCount() / 3;
        if (facing.length < triangles) facing = new boolean[triangles];
        boolean orthographic = camera.orthographic();
        for (int t = 0; t < triangles; t++) facing[t] = facesEye(mesh, t, orthographic);
        float cap = width * 0.5f;
        for (int e = 0; e < topology.edgeCount; e++) {
            int a = topology.edges[e * 4], b = topology.edges[e * 4 + 1];
            int first = topology.edges[e * 4 + 2], second = topology.edges[e * 4 + 3];
            if (second >= 0 && facing[first] == facing[second]) continue;
            if (Float.isNaN(screen[a * 3 + 2]) || Float.isNaN(screen[b * 3 + 2])) continue;
            draw.addLine(screen[a * 3], screen[a * 3 + 1], screen[b * 3], screen[b * 3 + 1], color, width);
            // Round joins: thick segments meeting at an angle would otherwise leave notches.
            draw.addCircleFilled(screen[a * 3], screen[a * 3 + 1], cap, color, 8);
            draw.addCircleFilled(screen[b * 3], screen[b * 3 + 1], cap, color, 8);
        }
    }

    /**
     * @return whether triangle t, by its geometric world normal, faces the camera
     */
    private boolean facesEye(Mesh3D mesh, int t, boolean orthographic) {
        int i0 = mesh.index(t * 3) * 3, i1 = mesh.index(t * 3 + 1) * 3, i2 = mesh.index(t * 3 + 2) * 3;
        float ux = worldPoints[i1] - worldPoints[i0], uy = worldPoints[i1 + 1] - worldPoints[i0 + 1],
                uz = worldPoints[i1 + 2] - worldPoints[i0 + 2];
        float vx = worldPoints[i2] - worldPoints[i0], vy = worldPoints[i2 + 1] - worldPoints[i0 + 1],
                vz = worldPoints[i2 + 2] - worldPoints[i0 + 2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        if (orthographic) return nx * forward.x() + ny * forward.y() + nz * forward.z() < 0;
        return nx * (eye.x() - worldPoints[i0]) + ny * (eye.y() - worldPoints[i0 + 1])
                + nz * (eye.z() - worldPoints[i0 + 2]) > 0;
    }

    /**
     * Edges of a mesh with the one or two triangles beside each. Vertices at the same position are merged, so seams
     * between faces with split normals or UVs do not show up as rims.
     */
    static final class Topology {
        /**
         * Per edge: first vertex, second vertex, first triangle, and second triangle or -1.
         */
        final int[] edges;
        /**
         * Number of unique edges.
         */
        final int edgeCount;

        /**
         * @param mesh mesh to analyse
         */
        Topology(Mesh3D mesh) {
            int vertices = mesh.vertexCount();
            int[] canonical = new int[vertices];
            Map<List<Long>, Integer> byPosition = new HashMap<>();
            for (int v = 0; v < vertices; v++) {
                // Rounded to 1e-4 so seam copies that differ by float error or by the sign of zero still merge.
                Integer first = byPosition.putIfAbsent(List.of(Math.round(mesh.position(v, 0) * 1e4),
                        Math.round(mesh.position(v, 1) * 1e4), Math.round(mesh.position(v, 2) * 1e4)), v);
                canonical[v] = first == null ? v : first;
            }
            int triangles = mesh.indexCount() / 3;
            int[] data = new int[triangles * 3 * 4];
            Map<Long, Integer> byEdge = new HashMap<>();
            int count = 0;
            for (int t = 0; t < triangles; t++) {
                int c0 = canonical[mesh.index(t * 3)], c1 = canonical[mesh.index(t * 3 + 1)],
                        c2 = canonical[mesh.index(t * 3 + 2)];
                // Collapsed triangles, such as those at a sphere pole, have no facing and would add false rims.
                if (c0 == c1 || c1 == c2 || c2 == c0) continue;
                for (int side = 0; side < 3; side++) {
                    int a = canonical[mesh.index(t * 3 + side)], b = canonical[mesh.index(t * 3 + (side + 1) % 3)];
                    long key = (long) Math.min(a, b) << 32 | Math.max(a, b);
                    Integer edge = byEdge.get(key);
                    if (edge == null) {
                        byEdge.put(key, count);
                        data[count * 4] = a;
                        data[count * 4 + 1] = b;
                        data[count * 4 + 2] = t;
                        data[count * 4 + 3] = -1;
                        count++;
                    } else if (data[edge * 4 + 3] < 0) {
                        data[edge * 4 + 3] = t;
                    }
                }
            }
            edges = data;
            edgeCount = count;
        }

        /**
         * Counts edges bordering a single triangle.
         *
         * @return number of open edges
         */
        int openEdges() {
            int open = 0;
            for (int e = 0; e < edgeCount; e++) if (edges[e * 4 + 3] < 0) open++;
            return open;
        }
    }

    /**
     * Draws an icon for every visible light and the shape gizmo of each selected light.
     *
     * @param draw     canvas of the viewport painter
     * @param selected selected editor objects
     */
    void paintLights(RenderContext draw, Collection<UnitySceneObject> selected) {
        Camera3D camera = viewport.camera();
        for (UnitySceneObject light : lights(viewport.scene())) {
            if (selected.contains(light)) paintLightShape(draw, camera, light);
        }
        for (UnitySceneObject light : lights(viewport.scene())) {
            if (!lightPosition(light, point) || !project(camera, point, projected)) continue;
            paintIcon(draw, light.light(), projected.x(), projected.y(), selected.contains(light));
        }
    }

    /**
     * Draws a sun for a directional light and a bulb for point and spot lights, tinted with the light color.
     */
    private static void paintIcon(RenderContext draw, UnityLight light, float x, float y, boolean selected) {
        int rgb = light.colorRgb();
        MutableColor color = MutableColor.rgba255(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, 255);
        if (selected) draw.addCircleFilled(x, y, 10, SELECTED, 20);
        draw.addCircleFilled(x, y, 8, ICON_OUTLINE, 20);
        switch (light.kind()) {
            case DIRECTIONAL -> {
                draw.addCircleFilled(x, y, 3.5f, color, 16);
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI * 0.25 * i;
                    float cos = (float) Math.cos(angle), sin = (float) Math.sin(angle);
                    draw.addLine(x + cos * 5, y + sin * 5, x + cos * 7, y + sin * 7, color, 1.3f);
                }
            }
            case POINT -> {
                draw.addCircleFilled(x, y - 1, 4.5f, color, 16);
                draw.addRectFilled(x - 2, y + 3, 4, 2.5f, MutableColor.rgba(0.7f, 0.7f, 0.72f, 1));
            }
            case SPOT -> {
                draw.addTriangleFilled(new DrawPoint(x, y - 5), new DrawPoint(x - 5, y + 4),
                        new DrawPoint(x + 5, y + 4), color);
                draw.addCircleFilled(x, y - 4, 2.2f, MutableColor.rgba(0.7f, 0.7f, 0.72f, 1), 12);
            }
        }
    }

    /**
     * Draws a point light's range as three great circles, a spotlight's outer and inner cones, or a directional
     * light's disk with parallel rays. A cone of half-angle θ and length r ends in a circle of radius
     * {@code r * tan(θ)}.
     */
    private void paintLightShape(RenderContext draw, Camera3D camera, UnitySceneObject object) {
        UnityLight light = object.light();
        MutableVec3 origin = new MutableVec3();
        if (!lightPosition(object, origin)) return;
        MutableVec3 direction = lightDirection(object, new MutableVec3());
        MutableVec3 u = new MutableVec3(), v = new MutableVec3();
        basis(direction, u, v);
        switch (light.kind()) {
            case POINT -> {
                float r = light.range();
                circle(draw, camera, origin, new MutableVec3(1, 0, 0), new MutableVec3(0, 1, 0), r, LIGHT_GIZMO);
                circle(draw, camera, origin, new MutableVec3(1, 0, 0), new MutableVec3(0, 0, 1), r, LIGHT_GIZMO);
                circle(draw, camera, origin, new MutableVec3(0, 1, 0), new MutableVec3(0, 0, 1), r, LIGHT_GIZMO);
            }
            case SPOT -> {
                cone(draw, camera, origin, direction, u, v, light.range(), light.spotAngle(), LIGHT_GIZMO, true);
                if (light.innerSpotAngle() > 0.5f) {
                    cone(draw, camera, origin, direction, u, v, light.range(), light.innerSpotAngle(),
                            LIGHT_GIZMO_DIM, false);
                }
            }
            case DIRECTIONAL -> {
                float radius = 0.45f;
                circle(draw, camera, origin, u, v, radius, LIGHT_GIZMO);
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI * 0.25 * i;
                    float c = (float) Math.cos(angle) * radius, s = (float) Math.sin(angle) * radius;
                    a.set(origin).add(c * u.x() + s * v.x(), c * u.y() + s * v.y(), c * u.z() + s * v.z());
                    b.set(a).add(direction.x() * 1.6f, direction.y() * 1.6f, direction.z() * 1.6f);
                    line(draw, camera, a, b, LIGHT_GIZMO);
                }
                b.set(origin).add(direction.x() * 2.2f, direction.y() * 2.2f, direction.z() * 2.2f);
                line(draw, camera, origin, b, LIGHT_GIZMO);
            }
        }
    }

    /**
     * Draws a cone from the light position along its direction.
     */
    private void cone(RenderContext draw, Camera3D camera, MutableVec3 origin, MutableVec3 direction,
                      MutableVec3 u, MutableVec3 v, float range, float fullAngle, MutableColor color, boolean edges) {
        float radius = range * (float) Math.tan(Math.toRadians(fullAngle * 0.5));
        MutableVec3 end = new MutableVec3().set(origin).add(direction.x() * range, direction.y() * range,
                direction.z() * range);
        circle(draw, camera, end, u, v, radius, color);
        if (!edges) return;
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI * 0.5 * i;
            float c = (float) Math.cos(angle) * radius, s = (float) Math.sin(angle) * radius;
            b.set(end).add(c * u.x() + s * v.x(), c * u.y() + s * v.y(), c * u.z() + s * v.z());
            line(draw, camera, origin, b, color);
        }
    }

    /**
     * Draws {@code center + r * (cos(t) * u + sin(t) * v)} as a projected polyline, breaking it where points
     * leave the clip range.
     */
    private void circle(RenderContext draw, Camera3D camera, MutableVec3 center, MutableVec3 u, MutableVec3 v,
                        float radius, MutableColor color) {
        boolean hasPrevious = false;
        for (int segment = 0; segment <= CIRCLE_SEGMENTS; segment++) {
            double angle = 2 * Math.PI * segment / CIRCLE_SEGMENTS;
            float c = (float) Math.cos(angle) * radius, s = (float) Math.sin(angle) * radius;
            point.set(center).add(c * u.x() + s * v.x(), c * u.y() + s * v.y(), c * u.z() + s * v.z());
            if (!project(camera, point, projected)) {
                hasPrevious = false;
                continue;
            }
            if (hasPrevious) draw.addLine(previous.x(), previous.y(), projected.x(), projected.y(), color, 1.2f);
            previous.set(projected);
            hasPrevious = true;
        }
    }

    /**
     * Draws one projected world-space segment when both ends are inside the clip range.
     */
    private void line(RenderContext draw, Camera3D camera, MutableVec3 from, MutableVec3 to, MutableColor color) {
        if (!project(camera, from, previous) || !project(camera, to, projected)) return;
        draw.addLine(previous.x(), previous.y(), projected.x(), projected.y(), color, 1.2f);
    }

    /**
     * Draws the bounds of every visible fog volume, faint unless selected, and a cloud icon at its center. The volume
     * spans {@code [-1, 1]} on each local axis of its object, a box or the ellipsoid inside it, so its corners and
     * circles are mapped through the object's world matrix.
     *
     * @param draw     canvas of the viewport painter
     * @param selected selected editor objects
     */
    void paintFog(RenderContext draw, Collection<UnitySceneObject> selected) {
        Camera3D camera = viewport.camera();
        List<UnitySceneObject> fogs = fogs(viewport.scene());
        MutableMat4 matrix = new MutableMat4();
        for (UnitySceneObject object : fogs) {
            object.worldMatrix(matrix);
            MutableColor color = selected.contains(object) ? FOG_GIZMO : FOG_GIZMO_DIM;
            if (object.fog().shape() == FogVolume.Shape.BOX) {
                for (int corner = 0; corner < 8; corner++) {
                    for (int bit = 1; bit < 8; bit <<= 1) {
                        if ((corner & bit) != 0) continue;
                        if (!matrix.transformPoint(corner(corner, a), a)
                                || !matrix.transformPoint(corner(corner | bit, b), b)) continue;
                        line(draw, camera, a, b, color);
                    }
                }
            } else {
                localCircle(draw, camera, matrix, 0, 1, color);
                localCircle(draw, camera, matrix, 0, 2, color);
                localCircle(draw, camera, matrix, 1, 2, color);
            }
        }
        for (UnitySceneObject object : fogs) {
            if (!lightPosition(object, point) || !project(camera, point, projected)) continue;
            paintFogIcon(draw, object.fog().colorRgb(), projected.x(), projected.y(), selected.contains(object));
        }
    }

    /**
     * @param index corner number whose bits 0, 1 and 2 choose +1 or -1 on X, Y and Z
     * @param out   destination for the local corner
     * @return out
     */
    private static MutableVec3 corner(int index, MutableVec3 out) {
        return out.set((index & 1) != 0 ? 1 : -1, (index & 2) != 0 ? 1 : -1, (index & 4) != 0 ? 1 : -1);
    }

    /**
     * Draws the local unit circle {@code cos(t) * e_u + sin(t) * e_v} mapped through a world matrix, which makes it
     * an ellipse of the scaled volume.
     */
    private void localCircle(RenderContext draw, Camera3D camera, MutableMat4 matrix, int u, int v,
                             MutableColor color) {
        boolean hasPrevious = false;
        for (int segment = 0; segment <= CIRCLE_SEGMENTS; segment++) {
            double angle = 2 * Math.PI * segment / CIRCLE_SEGMENTS;
            float c = (float) Math.cos(angle), s = (float) Math.sin(angle);
            point.set(u == 0 ? c : 0, u == 1 ? c : v == 1 ? s : 0, v == 2 ? s : 0);
            if (!matrix.transformPoint(point, point) || !project(camera, point, projected)) {
                hasPrevious = false;
                continue;
            }
            if (hasPrevious) draw.addLine(previous.x(), previous.y(), projected.x(), projected.y(), color, 1.2f);
            previous.set(projected);
            hasPrevious = true;
        }
    }

    /**
     * Draws a cloud tinted with the haze color.
     */
    private static void paintFogIcon(RenderContext draw, int rgb, float x, float y, boolean selected) {
        MutableColor color = MutableColor.rgba255(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, 255);
        if (selected) draw.addCircleFilled(x, y, 10, SELECTED, 20);
        draw.addCircleFilled(x, y, 8, ICON_OUTLINE, 20);
        draw.addCircleFilled(x - 2.2f, y + 0.6f, 2.6f, color, 12);
        draw.addCircleFilled(x + 1.0f, y - 1.2f, 3.3f, color, 12);
        draw.addCircleFilled(x + 3.4f, y + 1.0f, 2.2f, color, 12);
        draw.addRectFilled(x - 2.2f, y + 1.0f, 5.6f, 2.2f, color);
    }

    /**
     * @param scene edited scene
     * @return visible objects with a Fog Volume component, in hierarchy order
     */
    static List<UnitySceneObject> fogs(Scene scene) {
        List<UnitySceneObject> result = new ArrayList<>();
        scene.visitVisible(null, node -> {
            if (node instanceof UnitySceneObject object && object.fog() != null) result.add(object);
        });
        return result;
    }

    /**
     * Finds the light or fog volume whose icon is closest to a canvas point.
     *
     * @param x canvas X
     * @param y canvas Y
     * @return the light or fog object under the pointer, or null
     */
    UnitySceneObject pickLight(float x, float y) {
        Camera3D camera = viewport.camera();
        UnitySceneObject best = null;
        float bestDistance = ICON_HIT_RADIUS * ICON_HIT_RADIUS;
        List<UnitySceneObject> candidates = new ArrayList<>(lights(viewport.scene()));
        candidates.addAll(fogs(viewport.scene()));
        for (UnitySceneObject light : candidates) {
            if (!lightPosition(light, point) || !project(camera, point, projected)) continue;
            float dx = projected.x() - x, dy = projected.y() - y;
            float distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = light;
            }
        }
        return best;
    }

    /**
     * Sky ambient while a directional light object lights the scene.
     */
    static final AmbientLight SKY_AMBIENT = new AmbientLight(0xDBE8FF, 0.48f);
    /**
     * Faint ambient left when the scene has no directional light object, as in Unity without a sun.
     */
    static final AmbientLight DARK_AMBIENT = new AmbientLight(0x4A5266, 0.12f);

    /**
     * Lights the scene from its first visible directional light object in hierarchy order: that object becomes
     * the sun and the sky ambient is on. Without one the scene has no sun and only a faint ambient, so it is dark.
     * The scene revision only changes when the lighting does.
     *
     * @param scene edited scene
     */
    static void syncSceneLighting(Scene scene) {
        MutableVec3 direction = new MutableVec3();
        for (UnitySceneObject object : lights(scene)) {
            UnityLight light = object.light();
            if (light.kind() != UnityLight.Kind.DIRECTIONAL
                    || !object.componentEnabled(UnitySceneObject.Component.LIGHT)) continue;
            lightDirection(object, direction);
            scene.setDirectionalLight(new DirectionalLight(direction.x(), direction.y(), direction.z(),
                    light.colorRgb(), light.intensity()));
            scene.setDirectionalShadows(light.toDirectionalShadows());
            scene.setAmbientLight(SKY_AMBIENT);
            return;
        }
        scene.setDirectionalLight(null);
        scene.setDirectionalShadows(null);
        scene.setAmbientLight(DARK_AMBIENT);
    }

    /**
     * @param scene edited scene
     * @return visible light objects in hierarchy order
     */
    static List<UnitySceneObject> lights(Scene scene) {
        List<UnitySceneObject> result = new ArrayList<>();
        scene.visitVisible(null, node -> {
            if (node instanceof UnitySceneObject object && object.light() != null) result.add(object);
        });
        return result;
    }

    /**
     * @param object light object
     * @param out    destination for the light position in world space
     * @return false for a degenerate transform
     */
    private static boolean lightPosition(UnitySceneObject object, MutableVec3 out) {
        return object.worldMatrix(new MutableMat4()).transformPoint(out.set(0, 0, 0), out);
    }

    /**
     * Rotates local -Z by the accumulated rotation of the object and its ancestors.
     *
     * @param object light object
     * @param out    destination for the unit direction in world space
     * @return out
     */
    static MutableVec3 lightDirection(UnitySceneObject object, MutableVec3 out) {
        MutableQuat rotation = new MutableQuat();
        MutableQuat scratch = new MutableQuat();
        for (SceneNode node = object; node != null; node = node.parent()) {
            rotation.setProduct(node.rotation(scratch), rotation);
        }
        return rotation.rotate(0, 0, -1, out);
    }

    /**
     * Builds two unit vectors perpendicular to a direction and to each other.
     */
    private static void basis(MutableVec3 direction, MutableVec3 u, MutableVec3 v) {
        float x = direction.x(), y = direction.y(), z = direction.z();
        // Cross with the world axis least aligned with the direction.
        if (Math.abs(y) < 0.9f) u.set(z, 0, -x);
        else u.set(0, -z, y);
        float length = (float) Math.sqrt(u.x() * u.x() + u.y() * u.y() + u.z() * u.z());
        u.set(u.x() / length, u.y() / length, u.z() / length);
        v.set(y * u.z() - z * u.y(), z * u.x() - x * u.z(), x * u.y() - y * u.x());
    }

    /**
     * Applies the linear part of a matrix to a direction.
     */
    private static void transformDirection(MutableMat4 matrix, MutableVec3 direction) {
        direction.set((float) matrix.transformComponent(0, direction.x(), direction.y(), direction.z(), 0),
                (float) matrix.transformComponent(1, direction.x(), direction.y(), direction.z(), 0),
                (float) matrix.transformComponent(2, direction.x(), direction.y(), direction.z(), 0));
    }

    /**
     * Projects a world point, accepting only depths inside the clip range.
     */
    private static boolean project(Camera3D camera, MutableVec3 world, MutableVec3 out) {
        return camera.project(world, out) && out.z() >= 0 && out.z() <= 1;
    }
}
