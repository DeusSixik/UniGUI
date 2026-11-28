package dev.sixik.unigui.demo.ui.scene;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.widgets.rendering.Scene3D;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Draws transform handles in the viewport and applies edits in the selected coordinate space.
 * Several nodes can be transformed together around the active node's pivot or the selection center,
 * optionally snapping to fixed increments.
 */
public final class SceneTransformGizmo {
    /**
     * Ways of transforming the selected node.
     */
    public enum Mode {
        /**
         * Translation along a handle axis or plane.
         */
        MOVE,
        /**
         * Rotation about a handle axis.
         */
        ROTATE,
        /**
         * Scaling along a handle axis or uniformly from the center.
         */
        SCALE
    }

    /**
     * Point the handles stand at when several nodes are selected.
     */
    public enum Pivot {
        /**
         * The active node's own pivot.
         */
        PIVOT,
        /**
         * The average of the selected nodes' pivots in world space.
         */
        CENTER
    }

    /**
     * Coordinate frame used by the transform handles.
     */
    public enum Space {
        /**
         * Axes follow the object's accumulated rotation.
         */
        LOCAL,
        /**
         * Axes are aligned with the scene.
         */
        GLOBAL
    }

    /**
     * Base gizmo radius in UI units.
     */
    private static final float RADIUS = 62;
    /**
     * Handle hit tolerance in UI units.
     */
    private static final float HIT_RADIUS = 8;
    /**
     * Number of segments approximating a rotation circle.
     */
    private static final int RING_SEGMENTS = 40;
    /**
     * Number of facets around a 3D move cone arrowhead.
     */
    private static final int CONE_SIDES = 12;
    /**
     * Fraction of the axis length where the move cone base sits.
     */
    private static final float CONE_BASE_RATIO = 0.76f;
    /**
     * Cone base radius relative to {@link #worldRadius}.
     */
    private static final float CONE_RADIUS_RATIO = 0.088f;
    /**
     * Inner and outer ratios of planar move quads along the two in-plane axes.
     */
    private static final float PLANE_MIN = 0.21f;
    private static final float PLANE_MAX = 0.37f;
    /**
     * Hover/active handle index offsets for planar move quads (3..5) and center uniform scale (6).
     */
    private static final int PLANE_HANDLE_BASE = 3;
    private static final int CENTER_SCALE_HANDLE = 6;

    /**
     * Unit directions of the X, Y, Z axes.
     */
    private static final MutableVec3[] AXES = {
            new MutableVec3(1, 0, 0), new MutableVec3(0, 1, 0), new MutableVec3(0, 0, 1)
    };
    /**
     * Axis names used in floating transform readouts.
     */
    private static final String[] AXIS_LABELS = {"X", "Y", "Z"};
    /**
     * Planar handle names matching each perpendicular axis (YZ, XZ, XY).
     */
    private static final String[] PLANE_LABELS = {"YZ", "XZ", "XY"};
    /**
     * Handle color for each axis.
     */
    private static final MutableColor[] COLORS = {
            MutableColor.fromHex("#F25056"),
            MutableColor.fromHex("#57D16A"),
            MutableColor.fromHex("#4D94FF")
    };
    /**
     * Color of a hovered or grabbed handle.
     */
    private static final MutableColor ACTIVE = MutableColor.fromHex("#FFE46B");
    /**
     * Neutral color of the center hub and uniform scale cube.
     */
    private static final MutableColor NEUTRAL = MutableColor.fromHex("#E6ECF5");
    /**
     * Dark outline and halo color for contrast against any scene background.
     */
    private static final MutableColor OUTLINE = MutableColor.rgba(0.055f, 0.075f, 0.115f, 0.84f);
    /**
     * Subtle trackball sphere fill and horizon rim in Rotate mode.
     */
    private static final MutableColor TRACKBALL_FILL = MutableColor.rgba(0.06f, 0.09f, 0.14f, 0.14f);
    private static final MutableColor TRACKBALL_RIM = MutableColor.rgba(0.82f, 0.88f, 0.96f, 0.26f);
    /**
     * Active rotation sweep sector fill and border.
     */
    private static final MutableColor SECTOR_FILL = MutableColor.rgba(1.0f, 0.89f, 0.42f, 0.24f);
    private static final MutableColor SECTOR_LINE = MutableColor.rgba(1.0f, 0.89f, 0.42f, 0.88f);
    /**
     * Floating readout badge background and text color.
     */
    private static final MutableColor BADGE_BG = MutableColor.rgba(0.08f, 0.105f, 0.15f, 0.92f);
    private static final MutableColor BADGE_TEXT = MutableColor.fromHex("#F2F6FC");
    /**
     * Faint reference guide line drawn along the active axis while dragging.
     */
    private static final MutableColor GUIDE_COLOR = MutableColor.rgba(1.0f, 0.89f, 0.42f, 0.42f);
    /**
     * Normalized key-light direction used to shade 3D cones and scale cubes.
     */
    private static final MutableVec3 KEY_LIGHT = normalized(0.45f, 0.82f, 0.35f);
    /**
     * Corner signs for projecting the 8 vertices of a 3D scale cube.
     */
    private static final int[][] CUBE_CORNERS = {
            {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
            {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}
    };
    /**
     * Outward faces of a cube (4 corner indices in CCW order looking from outside).
     */
    private static final int[][] CUBE_FACES = {
            {1, 2, 6, 5}, // +axis
            {0, 4, 7, 3}, // -axis
            {3, 7, 6, 2}, // +u
            {0, 1, 5, 4}, // -u
            {4, 5, 6, 7}, // +v
            {0, 3, 2, 1}  // -v
    };

    /**
     * Viewport whose camera defines the projection.
     */
    private final Scene3D viewport;
    /**
     * Applies the block showcase's original absolute limits when dragging a local scale handle.
     */
    private final boolean limitLocalScale;
    /**
     * Handle points [axis][vertex] in canvas coordinates, with depth or NaN in Z.
     */
    private final MutableVec3[][] handles = new MutableVec3[AXES.length][RING_SEGMENTS + 1];
    /**
     * Handle pivot in world space, in scene units.
     */
    private final MutableVec3 pivot = new MutableVec3();
    /**
     * Pivot on the canvas, with depth in Z.
     */
    private final MutableVec3 center = new MutableVec3();
    /**
     * Scratch point in world coordinates, in scene units.
     */
    private final MutableVec3 point = new MutableVec3();
    /**
     * Scratch projected point in canvas coordinates.
     */
    private final MutableVec3 scratch = new MutableVec3();
    /**
     * Scratch camera view direction from the pivot toward the eye.
     */
    private final MutableVec3 viewDir = new MutableVec3();
    /**
     * Scratch ray for planar move ray-plane intersections.
     */
    private final Ray dragRay = new Ray();
    /**
     * Pre-allocated ring points for 3D cone projection.
     */
    private final MutableVec3[] coneRing = new MutableVec3[CONE_SIDES + 1];
    /**
     * Pre-allocated corner points for 3D cube projection.
     */
    private final MutableVec3[] cubeCorners = new MutableVec3[8];
    /**
     * Pre-allocated planar quad corners [plane][corner] in canvas space.
     */
    private final MutableVec3[][] planeQuads = new MutableVec3[AXES.length][4];
    /**
     * Visibility flag for each planar move quad (hidden when edge-on to camera).
     */
    private final boolean[] planeVisible = new boolean[AXES.length];
    /**
     * Reusable color scratch for shaded faces, dimming and halos.
     */
    private final MutableColor shadedColor = new MutableColor();
    private final MutableColor cubeFaceColor = new MutableColor();
    /**
     * Accumulated orientation of the active node, independent of scale.
     */
    private final MutableQuat localAxes = new MutableQuat();
    /**
     * Accumulated orientation of the active node's parent in world space.
     */
    private final MutableQuat parentAxes = new MutableQuat();
    /**
     * Scratch quaternion for accumulating parent orientations.
     */
    private final MutableQuat ancestorRotation = new MutableQuat();
    /**
     * Unit handle axes expressed in world coordinates.
     */
    private final MutableVec3[] worldAxes = {new MutableVec3(), new MutableVec3(), new MutableVec3()};
    /**
     * Parent world matrix used to transform the pivot into world space.
     */
    private final MutableMat4 parentWorld = new MutableMat4();
    /**
     * Active node: its axes define local space and its pivot defines {@link Pivot#PIVOT} mode.
     */
    private SceneNode selected;
    /**
     * All nodes to transform without descendants of other selected nodes, including the active node.
     */
    private final List<SceneNode> targets = new ArrayList<>();
    /**
     * Where the handles stand for several nodes.
     */
    private Pivot pivotMode = Pivot.CENTER;
    /**
     * Translation snap step in scene units.
     */
    private float moveStep = 1;
    /**
     * Rotation snap step in degrees.
     */
    private float rotateStep = 15;
    /**
     * Scale snap step as a change of the scale factor.
     */
    private float scaleStep = 0.1f;
    /**
     * Type of handles displayed for the node.
     */
    private Mode mode = Mode.MOVE;
    /**
     * Coordinate frame of the current handles.
     */
    private Space space = Space.GLOBAL;
    /**
     * Scene and camera revisions from which the geometry was last computed.
     */
    private long sceneRevision = -1, cameraRevision = -1;
    /**
     * Whether the pivot lies between the near and far clipping planes.
     */
    private boolean visible;
    /**
     * Base gizmo radius in scene units at the current depth.
     */
    private float worldRadius;
    /**
     * Hovered handle index (0..2 axes/rings, 3..5 move planes, 6 center scale), or -1.
     */
    private int hovered = -1;
    /**
     * Parameters of the active transform gesture, or null.
     */
    private Drag drag;
    /**
     * Scratch quaternion for incremental and resulting rotation.
     */
    private final MutableQuat rotation = new MutableQuat();
    /**
     * Depth-sorted order of the three linear axes for painting back-to-front.
     */
    private final int[] axisOrder = {0, 1, 2};

    /**
     * Connects the gizmo to the camera and scene redraw requests.
     *
     * @param viewport scene over which the handles are drawn
     */
    public SceneTransformGizmo(Scene3D viewport) {
        this(viewport, false);
    }

    /**
     * Creates handles with optional absolute limits for the standalone block showcase.
     *
     * @param viewport        scene to which the handles are attached
     * @param limitLocalScale whether to constrain the edited local scale to [0.15, 6]
     */
    SceneTransformGizmo(Scene3D viewport, boolean limitLocalScale) {
        this.viewport = viewport;
        this.limitLocalScale = limitLocalScale;
        for (MutableVec3[] handle : handles) {
            for (int i = 0; i < handle.length; i++) handle[i] = new MutableVec3();
        }
        for (int i = 0; i < coneRing.length; i++) coneRing[i] = new MutableVec3();
        for (int i = 0; i < cubeCorners.length; i++) cubeCorners[i] = new MutableVec3();
        for (int p = 0; p < planeQuads.length; p++) {
            for (int c = 0; c < 4; c++) planeQuads[p][c] = new MutableVec3();
        }
    }

    /**
     * Moves the gizmo to another object.
     *
     * @param node selected node, or null to hide the handles
     */
    public void select(SceneNode node) {
        select(node, node == null ? List.of() : List.of(node));
    }

    /**
     * Transforms several nodes together. A node whose ancestor is also selected follows that ancestor and is
     * not transformed twice.
     *
     * @param active node whose axes and pivot the handles use, or null to hide the handles
     * @param nodes  every selected node; the active node is added if missing
     */
    public void select(SceneNode active, List<? extends SceneNode> nodes) {
        end();
        selected = active;
        targets.clear();
        if (active != null) {
            List<SceneNode> all = new ArrayList<>(nodes);
            if (!all.contains(active)) all.add(active);
            for (SceneNode node : all) {
                if (node != null && !hasSelectedAncestor(node, all) && !targets.contains(node)) targets.add(node);
            }
            if (!targets.contains(active)) {
                // The active node is inside another selected subtree: use that subtree's root instead.
                selected = targets.isEmpty() ? null : targets.get(0);
            }
        }
        invalidateGeometry();
    }

    /**
     * @param node  candidate target
     * @param nodes current selection
     * @return true if a proper ancestor of the node is selected
     */
    private static boolean hasSelectedAncestor(SceneNode node, List<SceneNode> nodes) {
        for (SceneNode parent = node.parent(); parent != null; parent = parent.parent()) {
            if (nodes.contains(parent)) return true;
        }
        return false;
    }

    /**
     * Chooses where the handles stand when several nodes are selected.
     *
     * @param pivot active node's pivot or the selection center
     */
    public void pivot(Pivot pivot) {
        end();
        pivotMode = pivot;
        invalidateGeometry();
    }

    /**
     * Sets the increments used while snapping. A non-positive or non-finite value keeps the previous step.
     *
     * @param move   translation step in scene units
     * @param rotate rotation step in degrees
     * @param scale  step of the scale factor
     */
    public void snapSteps(float move, float rotate, float scale) {
        if (move > 0 && Float.isFinite(move)) moveStep = move;
        if (rotate > 0 && Float.isFinite(rotate)) rotateStep = rotate;
        if (scale > 0 && Float.isFinite(scale)) scaleStep = scale;
    }

    /**
     * Changes the handle style and drag operation.
     *
     * @param mode new transform mode
     */
    public void mode(Mode mode) {
        end();
        this.mode = mode;
        invalidateGeometry();
    }

    /**
     * Changes the axes shown by the gizmo.
     *
     * @param space coordinate frame to use for the next gesture
     */
    public void space(Space space) {
        end();
        this.space = space;
        invalidateGeometry();
    }

    /**
     * Invalidates the cached projection after switching the object, frame or tool.
     */
    private void invalidateGeometry() {
        sceneRevision = -1;
        hovered = -1;
        viewport.requestRender();
    }

    /**
     * Clears the hover highlight when the pointer leaves the scene.
     */
    public void clearHover() {
        if (hovered == -1) return;
        hovered = -1;
        viewport.requestRender();
    }

    /**
     * Highlights the handle closest to the pointer.
     *
     * @param x pointer X coordinate in canvas units
     * @param y pointer Y coordinate in canvas units
     */
    public void hover(float x, float y) {
        int next = -1;
        int segment = hit(x, y);
        if (segment >= 0) {
            next = segment / RING_SEGMENTS;
        } else if (mode == Mode.MOVE) {
            int plane = hitPlane(x, y);
            if (plane >= 0) next = PLANE_HANDLE_BASE + plane;
        } else if (mode == Mode.SCALE && hitCenter(x, y)) {
            next = CENTER_SCALE_HANDLE;
        }
        if (hovered == next) return;
        hovered = next;
        viewport.requestRender();
    }

    /**
     * Captures the initial pose of the selected objects and the screen-space gesture direction.
     *
     * @param x press X coordinate in canvas units
     * @param y press Y coordinate in canvas units
     * @return true if an available handle was hit
     */
    public boolean begin(float x, float y) {
        int hit = hit(x, y);
        if (hit >= 0) return beginAxis(x, y, hit);
        if (mode == Mode.MOVE) {
            int plane = hitPlane(x, y);
            if (plane >= 0) return beginPlane(x, y, plane);
        } else if (mode == Mode.SCALE && hitCenter(x, y)) {
            return beginUniformScale(x, y);
        }
        return false;
    }

    /**
     * Starts an axis translation, rotation ring, or axis scale gesture.
     */
    private boolean beginAxis(float x, float y, int hit) {
        int axis = hit / RING_SEGMENTS;
        int segment = hit % RING_SEGMENTS;
        MutableVec3 start = handles[axis][segment], end = handles[axis][segment + 1];
        float dx = end.x() - start.x(), dy = end.y() - start.y();
        float length = (float) Math.hypot(dx, dy);
        float sensitivity = mode == Mode.ROTATE ? 1 / RADIUS : 1 / length;
        if (mode == Mode.MOVE) sensitivity *= worldRadius;
        MutableVec3 worldAxis = new MutableVec3().set(worldAxes[axis]);
        List<NodeStart> starts = new ArrayList<>(targets.size());
        NodeStart activeStart = null;
        for (SceneNode node : targets) {
            NodeStart nodeStart = capture(node, axis, worldAxis, false);
            if (nodeStart == null) return false;
            starts.add(nodeStart);
            if (node == selected) activeStart = nodeStart;
        }
        if (activeStart == null) return false;
        float startAngle = mode == Mode.ROTATE
                ? (float) (2.0 * Math.PI * (segment + 0.5) / RING_SEGMENTS)
                : 0;
        drag = new Drag(DragKind.AXIS, axis, x, y, dx / length * sensitivity, dy / length * sensitivity,
                worldAxis, new MutableVec3().set(worldAxes[(axis + 1) % 3]),
                new MutableVec3().set(worldAxes[(axis + 2) % 3]),
                new MutableVec3().set(pivot), new MutableVec3(), startAngle, activeStart, List.copyOf(starts));
        viewport.requestRender();
        return true;
    }

    /**
     * Starts a planar translation gesture constrained to the plane perpendicular to {@code planeAxis}.
     */
    private boolean beginPlane(float x, float y, int planeAxis) {
        MutableVec3 normal = new MutableVec3().set(worldAxes[planeAxis]);
        MutableVec3 origin = new MutableVec3().set(pivot);
        MutableVec3 hitPoint = new MutableVec3();
        if (!intersectWorkPlane(x, y, origin, normal, hitPoint)) return false;
        MutableVec3 axisU = new MutableVec3().set(worldAxes[(planeAxis + 1) % 3]);
        MutableVec3 axisV = new MutableVec3().set(worldAxes[(planeAxis + 2) % 3]);
        List<NodeStart> starts = new ArrayList<>(targets.size());
        NodeStart activeStart = null;
        for (SceneNode node : targets) {
            NodeStart nodeStart = capture(node, planeAxis, normal, false);
            if (nodeStart == null) return false;
            starts.add(nodeStart);
            if (node == selected) activeStart = nodeStart;
        }
        if (activeStart == null) return false;
        drag = new Drag(DragKind.PLANE, PLANE_HANDLE_BASE + planeAxis, x, y, 0, 0,
                normal, axisU, axisV, origin, hitPoint, 0, activeStart, List.copyOf(starts));
        viewport.requestRender();
        return true;
    }

    /**
     * Starts a uniform scale gesture from the center hub.
     */
    private boolean beginUniformScale(float x, float y) {
        MutableVec3 worldAxis = new MutableVec3().set(worldAxes[0]);
        List<NodeStart> starts = new ArrayList<>(targets.size());
        NodeStart activeStart = null;
        for (SceneNode node : targets) {
            NodeStart nodeStart = capture(node, 0, worldAxis, true);
            if (nodeStart == null) return false;
            starts.add(nodeStart);
            if (node == selected) activeStart = nodeStart;
        }
        if (activeStart == null) return false;
        float sensitivity = 1.0f / RADIUS;
        drag = new Drag(DragKind.UNIFORM_SCALE, CENTER_SCALE_HANDLE, x, y, sensitivity, -sensitivity,
                worldAxis, new MutableVec3(), new MutableVec3(), new MutableVec3().set(pivot),
                new MutableVec3(), 0, activeStart, List.copyOf(starts));
        viewport.requestRender();
        return true;
    }

    /**
     * Records one node's pose and the handle axis expressed in its parent space.
     *
     * @param node         node to transform
     * @param axis         handle index
     * @param worldAxis    handle direction in world space
     * @param uniformScale whether all three scale axes receive full weight
     * @return the starting state, or null if the parent transform cannot be inverted
     */
    private NodeStart capture(SceneNode node, int axis, MutableVec3 worldAxis, boolean uniformScale) {
        SceneNode parent = node.parent();
        MutableMat4 parentMatrix = null;
        MutableMat4 worldToParent = null;
        MutableQuat parentRotation = new MutableQuat();
        if (parent != null) {
            parentMatrix = parent.worldMatrix(new MutableMat4());
            worldToParent = new MutableMat4();
            if (!worldToParent.setInverse(parentMatrix)) return null;
            for (SceneNode ancestor = parent; ancestor != null; ancestor = ancestor.parent()) {
                parentRotation.setProduct(ancestor.rotation(ancestorRotation), parentRotation);
            }
        }
        // A displacement uses w=0 so the inverse matrix's translation does not affect it.
        MutableVec3 moveDirection = new MutableVec3().set(worldAxis);
        if (worldToParent != null) transformDirection(worldToParent, moveDirection);
        MutableVec3 rotateAxis = new MutableVec3();
        new MutableQuat().set(-parentRotation.x(), -parentRotation.y(), -parentRotation.z(), parentRotation.w())
                .rotate(worldAxis.x(), worldAxis.y(), worldAxis.z(), rotateAxis);
        MutableVec3 scaleWeights = uniformScale
                ? new MutableVec3(1, 1, 1)
                : mode == Mode.SCALE && space == Space.GLOBAL
                ? worldScaleWeights(axis, parentMatrix, node.rotation(new MutableQuat()))
                : AXES[axis];
        MutableQuat initialRotation = node.rotation(new MutableQuat());
        MutableVec3 position = node.position(new MutableVec3());
        MutableVec3 worldPosition = new MutableVec3().set(position);
        if (parentMatrix != null && !parentMatrix.transformPoint(worldPosition, worldPosition)) return null;
        return new NodeStart(node, position, worldPosition, node.scale(new MutableVec3()), initialRotation,
                moveDirection, rotateAxis, scaleWeights, worldToParent);
    }

    /**
     * Applies the gesture without snapping.
     *
     * @param x current pointer X coordinate in canvas units
     * @param y current pointer Y coordinate in canvas units
     */
    public void drag(float x, float y) {
        drag(x, y, false);
    }

    /**
     * Computes the transform relative to the pose at press time.
     * {@code delta = (pointer - start) · gradient}. For translation, gradient is the unit screen-axis
     * direction multiplied by the ratio of its length in scene units to its length on the canvas. For scale,
     * the divisor sets the relative change,
     * {@code scale_i = initialScale_i * sqrt(1 + (factor² - 1) * weight_i)}, where
     * {@code factor = 1 + delta} and the weight gives the share of axis i along the handle direction. For rotation,
     * gradient points along the ring tangent and delta gives radians for
     * {@code rotation = axisAngle(axis, delta) * initialRotation}.
     * For a nested node, translation is {@code position = initialPosition + inverse(parentWorld) * (axis * delta, 0)}.
     * The last component is zero because the axis and offset define a vector rather than a point.
     *
     * <p>When several nodes are selected, their pivots rotate around the shared point P and stretch from it:
     * {@code world' = P + R * (world - P)} and {@code world' = world + (factor - 1) * (o · a) * a}, where
     * {@code o = world - P} and {@code a} is the world-space handle direction.</p>
     *
     * <p>With snapping enabled, global-axis translation places the active node's pivot on the nearest grid line
     * {@code round((origin + delta) / step) * step}, while local-axis translation rounds the offset to the step.
     * The angle is rounded to the degree step, and the scale {@code delta} is rounded to the scale step.</p>
     *
     * @param x    current pointer X coordinate in canvas units
     * @param y    current pointer Y coordinate in canvas units
     * @param snap whether to round the change to the configured steps
     */
    public void drag(float x, float y, boolean snap) {
        if (drag == null) return;
        if (drag.kind == DragKind.PLANE) {
            dragPlane(x, y, snap);
            return;
        }
        float delta = (x - drag.x) * drag.gradientX + (y - drag.y) * drag.gradientY;
        if (!Float.isFinite(delta)) return;
        if (drag.kind == DragKind.UNIFORM_SCALE) {
            if (snap) delta = Math.round(delta / scaleStep) * scaleStep;
            double factor = 1.0 + delta;
            double stretch = Math.max(0.15, factor) - 1;
            drag.delta = delta;
            drag.moved = true;
            for (NodeStart start : drag.nodes) {
                float sx = scaled(start.scale.x(), 1, factor);
                float sy = scaled(start.scale.y(), 1, factor);
                float sz = scaled(start.scale.z(), 1, factor);
                if (Float.isFinite(sx) && Float.isFinite(sy) && Float.isFinite(sz)) start.node.setScale(sx, sy, sz);
                if (drag.nodes.size() < 2) continue;
                float ox = start.worldPosition.x() - drag.pivot.x();
                float oy = start.worldPosition.y() - drag.pivot.y();
                float oz = start.worldPosition.z() - drag.pivot.z();
                point.set(start.worldPosition).add((float) (ox * stretch), (float) (oy * stretch), (float) (oz * stretch));
                placeWorld(start, point);
            }
            return;
        }
        switch (mode) {
            case MOVE -> {
                if (snap) delta = snappedMove(delta, drag.axis);
                drag.delta = delta;
                drag.moved = true;
                for (NodeStart start : drag.nodes) {
                    float nextX = start.position.x() + start.moveDirection.x() * delta;
                    float nextY = start.position.y() + start.moveDirection.y() * delta;
                    float nextZ = start.position.z() + start.moveDirection.z() * delta;
                    if (Float.isFinite(nextX) && Float.isFinite(nextY) && Float.isFinite(nextZ)) {
                        start.node.setPosition(nextX, nextY, nextZ);
                    }
                }
            }
            case ROTATE -> {
                if (snap) {
                    double step = Math.toRadians(rotateStep);
                    delta = (float) (Math.round(delta / step) * step);
                }
                drag.delta = delta;
                drag.moved = true;
                MutableQuat worldRotation = new MutableQuat().setAxisAngle(drag.worldAxis, delta);
                for (NodeStart start : drag.nodes) {
                    if (delta == 0) start.node.setRotation(start.rotation);
                    else start.node.setRotation(rotation.setProduct(
                            rotation.setAxisAngle(start.rotateAxis, delta), start.rotation));
                    float ox = start.worldPosition.x() - drag.pivot.x();
                    float oy = start.worldPosition.y() - drag.pivot.y();
                    float oz = start.worldPosition.z() - drag.pivot.z();
                    if (ox * ox + oy * oy + oz * oz < 1e-10f) continue;
                    worldRotation.rotate(ox, oy, oz, point);
                    placeWorld(start, point.add(drag.pivot.x(), drag.pivot.y(), drag.pivot.z()));
                }
            }
            case SCALE -> {
                if (snap) delta = Math.round(delta / scaleStep) * scaleStep;
                drag.delta = delta;
                drag.moved = true;
                double factor = 1.0 + delta;
                double stretch = Math.max(0.15, factor) - 1;
                MutableVec3 a = drag.worldAxis;
                for (NodeStart start : drag.nodes) {
                    float sx = scaled(start.scale.x(), start.scaleWeights.x(), factor);
                    float sy = scaled(start.scale.y(), start.scaleWeights.y(), factor);
                    float sz = scaled(start.scale.z(), start.scaleWeights.z(), factor);
                    if (Float.isFinite(sx) && Float.isFinite(sy) && Float.isFinite(sz)) start.node.setScale(sx, sy, sz);
                    if (drag.nodes.size() < 2) continue;
                    float ox = start.worldPosition.x() - drag.pivot.x();
                    float oy = start.worldPosition.y() - drag.pivot.y();
                    float oz = start.worldPosition.z() - drag.pivot.z();
                    float along = (float) ((ox * a.x() + oy * a.y() + oz * a.z()) * stretch);
                    point.set(start.worldPosition).add(along * a.x(), along * a.y(), along * a.z());
                    placeWorld(start, point);
                }
            }
        }
    }

    /**
     * Translates all target nodes along the active plane by intersecting the pointer ray with it.
     */
    private void dragPlane(float x, float y, boolean snap) {
        if (!intersectWorkPlane(x, y, drag.pivot, drag.worldAxis, point)) return;
        float dx = point.x() - drag.planeHit.x();
        float dy = point.y() - drag.planeHit.y();
        float dz = point.z() - drag.planeHit.z();
        float du = dx * drag.planeU.x() + dy * drag.planeU.y() + dz * drag.planeU.z();
        float dv = dx * drag.planeV.x() + dy * drag.planeV.y() + dz * drag.planeV.z();
        int planeAxis = drag.axis - PLANE_HANDLE_BASE;
        if (snap) {
            du = snappedMove(du, (planeAxis + 1) % 3);
            dv = snappedMove(dv, (planeAxis + 2) % 3);
        }
        drag.delta = du;
        drag.deltaV = dv;
        drag.moved = true;
        float wx = drag.planeU.x() * du + drag.planeV.x() * dv;
        float wy = drag.planeU.y() * du + drag.planeV.y() * dv;
        float wz = drag.planeU.z() * du + drag.planeV.z() * dv;
        for (NodeStart start : drag.nodes) {
            point.set(start.worldPosition.x() + wx, start.worldPosition.y() + wy, start.worldPosition.z() + wz);
            placeWorld(start, point);
        }
    }

    /**
     * Intersects a canvas ray with a world-space plane.
     */
    private boolean intersectWorkPlane(float x, float y, MutableVec3 origin, MutableVec3 normal, MutableVec3 out) {
        if (!viewport.camera().screenRay(x, y, dragRay)) return false;
        double dist = dragRay.intersectPlane(origin, normal);
        if (!Double.isFinite(dist) || dist < 0 || dist > viewport.camera().far()) return false;
        dragRay.pointAt(dist, out);
        return true;
    }

    /**
     * Rounds a translation. In global axes the active pivot lands on the grid, otherwise the offset is rounded.
     *
     * @param delta raw offset along the handle in scene units
     * @param axis  world-axis index when {@link #space} is {@link Space#GLOBAL}
     * @return snapped offset
     */
    private float snappedMove(float delta, int axis) {
        if (space == Space.GLOBAL) {
            MutableVec3 start = drag.active.worldPosition;
            float origin = axis == 0 ? start.x() : axis == 1 ? start.y() : start.z();
            return Math.round((origin + delta) / moveStep) * moveStep - origin;
        }
        return Math.round(delta / moveStep) * moveStep;
    }

    /**
     * Moves a node so its pivot reaches a world-space point.
     *
     * @param start captured node state
     * @param world target pivot in world space, overwritten with the local position
     */
    private static void placeWorld(NodeStart start, MutableVec3 world) {
        if (start.worldToParent != null && !start.worldToParent.transformPoint(world, world)) return;
        if (Float.isFinite(world.x()) && Float.isFinite(world.y()) && Float.isFinite(world.z())) {
            start.node.setPosition(world.x(), world.y(), world.z());
        }
    }

    /**
     * Converts a directional stretch to the new length of a local basis vector.
     *
     * @param initial signed local scale factor
     * @param weight  squared projection of the unit basis vector onto the handle axis
     * @param factor  requested linear stretch along the handle direction
     * @return scale factor after applying the stretch and any configured limits
     */
    private float scaled(float initial, float weight, double factor) {
        if (weight == 0) return initial;
        if (limitLocalScale && space == Space.LOCAL) return (float) Math.max(0.15, Math.min(6, initial * factor));
        factor = Math.max(0.15, factor);
        double stretch = (factor - 1) * (factor + 1);
        return (float) (initial * Math.sqrt(1 + stretch * weight));
    }

    /**
     * Measures how much each transformed local basis vector points along the world-axis handle.
     * The weight for vector b on world axis i is {@code b_i² / (b_x² + b_y² + b_z²)}.
     * The resulting scale preserves the stretched basis lengths when the transform can only store TRS.
     *
     * @param axis            world-axis index
     * @param parentMatrix    parent's world matrix, or null for a scene root
     * @param initialRotation node's local orientation
     * @return weights for the X, Y, Z scale factors
     */
    private MutableVec3 worldScaleWeights(int axis, MutableMat4 parentMatrix, MutableQuat initialRotation) {
        return new MutableVec3(scaleWeight(axis, AXES[0], parentMatrix, initialRotation),
                scaleWeight(axis, AXES[1], parentMatrix, initialRotation),
                scaleWeight(axis, AXES[2], parentMatrix, initialRotation));
    }

    /**
     * Computes one squared projection for world-axis scaling.
     *
     * @param axis            world-axis index
     * @param basis           local unit basis vector
     * @param parentMatrix    parent's world matrix, or null
     * @param initialRotation node orientation in parent space
     * @return a weight in [0, 1], or zero for a degenerate basis
     */
    private float scaleWeight(int axis, MutableVec3 basis, MutableMat4 parentMatrix, MutableQuat initialRotation) {
        initialRotation.rotate(basis.x(), basis.y(), basis.z(), point);
        if (parentMatrix != null) transformDirection(parentMatrix, point);
        double x = point.x(), y = point.y(), z = point.z();
        double component = axis == 0 ? x : axis == 1 ? y : z;
        double squaredLength = x * x + y * y + z * z;
        return squaredLength > 0 && Double.isFinite(squaredLength) ? (float) (component * component / squaredLength) : 0;
    }

    /**
     * Applies {@code matrix * (direction, 0)} so matrix translation has no effect.
     *
     * @param matrix    transform whose linear part is used
     * @param direction input and output vector
     */
    private static void transformDirection(MutableMat4 matrix, MutableVec3 direction) {
        direction.set((float) matrix.transformComponent(0, direction.x(), direction.y(), direction.z(), 0),
                (float) matrix.transformComponent(1, direction.x(), direction.y(), direction.z(), 0),
                (float) matrix.transformComponent(2, direction.x(), direction.y(), direction.z(), 0));
    }

    /**
     * Releases the active handle when the pointer is released.
     */
    public void end() {
        if (drag == null) return;
        drag = null;
        hovered = -1;
        viewport.requestRender();
    }

    /**
     * @return true while a handle is being dragged
     */
    public boolean dragging() {
        return drag != null;
    }

    /**
     * Draws the projected axes or rings over the scene viewport.
     *
     * @param context render context in logical canvas coordinates
     */
    public void paint(RenderContext context) {
        if (!prepareGeometry()) return;
        resolveViewDirection(viewport.camera(), pivot, viewDir);
        int active = drag == null ? hovered : drag.axis;

        if (mode == Mode.ROTATE) {
            paintRotate(context, active);
        } else {
            paintLinear(context, active);
        }
        paintReadoutBadge(context, active);
    }

    /**
     * Draws depth-sorted Move or Scale handles, planar move quads, 3D shaded tips and the center hub.
     */
    private void paintLinear(RenderContext context, int active) {
        if (mode == Mode.MOVE) {
            paintMovePlanes(context, active);
            if (drag != null && drag.kind == DragKind.AXIS) paintAxisGuide(context);
        }
        sortAxesByDepth();
        boolean dragging = drag != null;
        for (int idx = 0; idx < AXES.length; idx++) {
            int axis = axisOrder[idx];
            MutableVec3 a = handles[axis][0], b = handles[axis][1];
            if (!drawable(a, b)) continue;
            boolean highlight = axis == active;
            float alpha = dragging && !highlight ? 0.30f : 1.0f;
            MutableColor color = axisColor(axis, highlight, alpha);

            float tipScale = 1.0f;
            if (mode == Mode.SCALE && drag != null && drag.kind == DragKind.AXIS && highlight) {
                tipScale = Math.max(0.42f, Math.min(1.85f, 1.0f + drag.delta));
            }
            paintLinearAxis(context, axis, a, b, color, highlight, alpha, tipScale);
        }
        if (mode == Mode.SCALE) {
            boolean centerActive = active == CENTER_SCALE_HANDLE;
            MutableColor centerColor = centerActive ? ACTIVE : NEUTRAL;
            paintCube(context, pivot, worldAxes[0], worldAxes[1], worldAxes[2],
                    worldRadius * (centerActive ? 0.088f : 0.074f), centerColor, 1.0f);
        } else {
            context.addCircleFilled(center.x(), center.y(), 5.0f, OUTLINE, 20);
            context.addCircleFilled(center.x(), center.y(), 3.2f, NEUTRAL, 20);
        }
    }

    /**
     * Draws the three translucent planar translation quads (YZ, XZ, XY) between axis pairs.
     */
    private void paintMovePlanes(RenderContext context, int active) {
        boolean dragging = drag != null;
        for (int plane = 0; plane < AXES.length; plane++) {
            if (!planeVisible[plane]) continue;
            int handleId = PLANE_HANDLE_BASE + plane;
            boolean highlight = active == handleId;
            float baseAlpha = dragging && !highlight ? 0.25f : 1.0f;
            MutableColor base = highlight ? ACTIVE : COLORS[plane];
            MutableVec3[] q = planeQuads[plane];
            DrawPoint p0 = new DrawPoint(q[0].x(), q[0].y());
            DrawPoint p1 = new DrawPoint(q[1].x(), q[1].y());
            DrawPoint p2 = new DrawPoint(q[2].x(), q[2].y());
            DrawPoint p3 = new DrawPoint(q[3].x(), q[3].y());
            shadedColor.set(OUTLINE.r(), OUTLINE.g(), OUTLINE.b(), 0.72f * baseAlpha);
            float bw = highlight ? 1.5f : 1.0f;
            addQuadBorder(context, p0, p1, p2, p3, bw, shadedColor);
            shadedColor.set(base.r(), base.g(), base.b(), (highlight ? 0.48f : 0.22f) * baseAlpha);
            context.addQuadFilled(p0, p1, p2, p3, shadedColor);
        }
    }

    /**
     * Draws a crisp border around a projected quad using filled edge strips (without emitting line commands).
     */
    private static void addQuadBorder(RenderContext context, DrawPoint p0, DrawPoint p1, DrawPoint p2, DrawPoint p3,
                                      float thickness, MutableColor color) {
        addEdgeQuad(context, p0, p1, thickness, color);
        addEdgeQuad(context, p1, p2, thickness, color);
        addEdgeQuad(context, p2, p3, thickness, color);
        addEdgeQuad(context, p3, p0, thickness, color);
    }

    private static void addEdgeQuad(RenderContext context, DrawPoint a, DrawPoint b, float thickness, MutableColor color) {
        float dx = b.x() - a.x(), dy = b.y() - a.y();
        float len = (float) Math.hypot(dx, dy);
        if (len < 1e-3f) return;
        float nx = -dy / len * (thickness * 0.5f);
        float ny = dx / len * (thickness * 0.5f);
        context.addQuadFilled(
                new DrawPoint(a.x() - nx, a.y() - ny),
                new DrawPoint(b.x() - nx, b.y() - ny),
                new DrawPoint(b.x() + nx, b.y() + ny),
                new DrawPoint(a.x() + nx, a.y() + ny), color);
    }

    /**
     * Draws a subtle world-space guide line extending through the active translation axis.
     */
    private void paintAxisGuide(RenderContext context) {
        float span = worldRadius * 3.5f;
        point.set(drag.pivot).add(-drag.worldAxis.x() * span, -drag.worldAxis.y() * span, -drag.worldAxis.z() * span);
        MutableVec3 p0 = new MutableVec3();
        if (!project(viewport.camera(), point, p0)) return;
        point.set(drag.pivot).add(drag.worldAxis.x() * span, drag.worldAxis.y() * span, drag.worldAxis.z() * span);
        if (!project(viewport.camera(), point, scratch)) return;
        context.addLine(p0.x(), p0.y(), scratch.x(), scratch.y(), GUIDE_COLOR, 1.2f);
    }

    /**
     * Draws one Move or Scale axis stem with dark halo and 3D-shaded tip geometry.
     */
    private void paintLinearAxis(RenderContext context, int axis, MutableVec3 a, MutableVec3 b,
                                 MutableColor color, boolean highlight, float alpha, float tipScale) {
        MutableVec3 uAxis = worldAxes[axis];
        MutableVec3 u = worldAxes[(axis + 1) % 3];
        MutableVec3 v = worldAxes[(axis + 2) % 3];
        Camera3D camera = viewport.camera();

        float startDist = worldRadius * 0.08f;
        point.set(pivot).add(uAxis.x() * startDist, uAxis.y() * startDist, uAxis.z() * startDist);
        MutableVec3 stemStart = new MutableVec3();
        if (!project(camera, point, stemStart)) stemStart.set(a);

        float endRatio = mode == Mode.MOVE ? CONE_BASE_RATIO + 0.02f : 0.93f * tipScale;
        point.set(pivot).add(uAxis.x() * (worldRadius * endRatio), uAxis.y() * (worldRadius * endRatio),
                uAxis.z() * (worldRadius * endRatio));
        MutableVec3 stemEnd = new MutableVec3();
        if (!project(camera, point, stemEnd)) stemEnd.set(b);

        if (tipScale != 1.0f) {
            // Tick mark showing the unstretched 1.0x reference tip while scaling.
            context.addCircleFilled(b.x(), b.y(), 2.8f, OUTLINE, 12);
            context.addCircleFilled(b.x(), b.y(), 1.8f, color, 12);
        }

        shadedColor.set(OUTLINE.r(), OUTLINE.g(), OUTLINE.b(), OUTLINE.a() * alpha);
        context.addLine(stemStart.x(), stemStart.y(), stemEnd.x(), stemEnd.y(), shadedColor, highlight ? 5.6f : 4.6f);
        context.addLine(stemStart.x(), stemStart.y(), stemEnd.x(), stemEnd.y(), color, highlight ? 3.4f : 2.5f);

        if (mode == Mode.SCALE) {
            point.set(pivot).add(uAxis.x() * (worldRadius * 0.93f * tipScale),
                    uAxis.y() * (worldRadius * 0.93f * tipScale), uAxis.z() * (worldRadius * 0.93f * tipScale));
            float half = worldRadius * (highlight ? 0.072f : 0.060f);
            paintCube(context, point, uAxis, u, v, half, color, alpha);
        } else {
            paintCone(context, uAxis, u, v, b, color, highlight, alpha);
        }
    }

    /**
     * Draws a 3D perspective-shaded cone arrowhead along {@code uAxis}.
     */
    private void paintCone(RenderContext context, MutableVec3 uAxis, MutableVec3 u, MutableVec3 v,
                           MutableVec3 tipScreen, MutableColor baseColor, boolean highlight, float alpha) {
        Camera3D camera = viewport.camera();
        float baseDist = worldRadius * CONE_BASE_RATIO;
        float radius = worldRadius * (highlight ? CONE_RADIUS_RATIO * 1.15f : CONE_RADIUS_RATIO);
        MutableVec3 baseCenter = new MutableVec3().set(pivot)
                .add(uAxis.x() * baseDist, uAxis.y() * baseDist, uAxis.z() * baseDist);
        MutableVec3 baseScreen = new MutableVec3();
        if (!project(camera, baseCenter, baseScreen)) return;

        for (int i = 0; i < CONE_SIDES; i++) {
            double theta = 2.0 * Math.PI * i / CONE_SIDES;
            float c = radius * (float) Math.cos(theta);
            float s = radius * (float) Math.sin(theta);
            point.set(baseCenter).add(c * u.x() + s * v.x(), c * u.y() + s * v.y(), c * u.z() + s * v.z());
            if (!project(camera, point, coneRing[i])) return;
        }
        coneRing[CONE_SIDES].set(coneRing[0]);

        float axisView = uAxis.x() * viewDir.x() + uAxis.y() * viewDir.y() + uAxis.z() * viewDir.z();
        float slope = CONE_RADIUS_RATIO / (1.0f - CONE_BASE_RATIO);
        DrawPoint tip = new DrawPoint(tipScreen.x(), tipScreen.y());

        // Crisp dark silhouette hull behind the cone using filled triangles.
        float dx = tipScreen.x() - baseScreen.x(), dy = tipScreen.y() - baseScreen.y();
        float screenLen = (float) Math.hypot(dx, dy);
        float ox = screenLen > 1e-3f ? dx / screenLen * 1.4f : 0;
        float oy = screenLen > 1e-3f ? dy / screenLen * 1.4f : 0;
        DrawPoint outerTip = new DrawPoint(tipScreen.x() + ox, tipScreen.y() + oy);
        DrawPoint bc = new DrawPoint(baseScreen.x(), baseScreen.y());
        shadedColor.set(OUTLINE.r(), OUTLINE.g(), OUTLINE.b(), OUTLINE.a() * alpha);
        for (int i = 0; i < CONE_SIDES; i++) {
            float r0x = coneRing[i].x() - baseScreen.x(), r0y = coneRing[i].y() - baseScreen.y();
            float r1x = coneRing[i + 1].x() - baseScreen.x(), r1y = coneRing[i + 1].y() - baseScreen.y();
            DrawPoint o0 = new DrawPoint(coneRing[i].x() + r0x * 0.16f - ox * 0.5f,
                    coneRing[i].y() + r0y * 0.16f - oy * 0.5f);
            DrawPoint o1 = new DrawPoint(coneRing[i + 1].x() + r1x * 0.16f - ox * 0.5f,
                    coneRing[i + 1].y() + r1y * 0.16f - oy * 0.5f);
            context.addTriangleFilled(o0, o1, outerTip, shadedColor);
            if (axisView < 0) context.addTriangleFilled(bc, o0, o1, shadedColor);
        }

        for (int i = 0; i < CONE_SIDES; i++) {
            double midTheta = 2.0 * Math.PI * (i + 0.5) / CONE_SIDES;
            float cosM = (float) Math.cos(midTheta);
            float sinM = (float) Math.sin(midTheta);
            float nx = cosM * u.x() + sinM * v.x() + slope * uAxis.x();
            float ny = cosM * u.y() + sinM * v.y() + slope * uAxis.y();
            float nz = cosM * u.z() + sinM * v.z() + slope * uAxis.z();
            float nLen = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            nx /= nLen;
            ny /= nLen;
            nz /= nLen;
            float facing = nx * viewDir.x() + ny * viewDir.y() + nz * viewDir.z();
            if (axisView >= 0 && facing < -0.05f) continue;

            float shade = shadeFactor(nx, ny, nz);
            setShaded(baseColor, shade, alpha);
            DrawPoint p0 = new DrawPoint(coneRing[i].x(), coneRing[i].y());
            DrawPoint p1 = new DrawPoint(coneRing[i + 1].x(), coneRing[i + 1].y());
            context.addTriangleFilled(p0, p1, tip, shadedColor);
        }

        if (axisView < 0) {
            // Looking at the cone from behind: draw the shadowed base disk on top of receding sides.
            setShaded(baseColor, 0.58f, alpha);
            for (int i = 0; i < CONE_SIDES; i++) {
                context.addTriangleFilled(bc,
                        new DrawPoint(coneRing[i].x(), coneRing[i].y()),
                        new DrawPoint(coneRing[i + 1].x(), coneRing[i + 1].y()), shadedColor);
            }
        }
    }

    /**
     * Draws a 3D perspective-shaded cube at {@code cubeCenter} aligned with {@code (uAxis, u, v)}.
     */
    private void paintCube(RenderContext context, MutableVec3 cubeCenter, MutableVec3 uAxis, MutableVec3 u,
                           MutableVec3 v, float half, MutableColor baseColor, float alpha) {
        Camera3D camera = viewport.camera();
        for (int i = 0; i < 8; i++) {
            int[] c = CUBE_CORNERS[i];
            float sa = c[0] * half, su = c[1] * half, sv = c[2] * half;
            point.set(cubeCenter).add(sa * uAxis.x() + su * u.x() + sv * v.x(),
                    sa * uAxis.y() + su * u.y() + sv * v.y(),
                    sa * uAxis.z() + su * u.z() + sv * v.z());
            if (!project(camera, point, cubeCorners[i])) return;
        }
        for (int face = 0; face < 6; face++) {
            MutableVec3 axisVec = face < 2 ? uAxis : face < 4 ? u : v;
            float sign = (face & 1) == 0 ? 1.0f : -1.0f;
            float nx = axisVec.x() * sign, ny = axisVec.y() * sign, nz = axisVec.z() * sign;
            if (nx * viewDir.x() + ny * viewDir.y() + nz * viewDir.z() <= 0) continue;

            int[] idx = CUBE_FACES[face];
            DrawPoint p0 = new DrawPoint(cubeCorners[idx[0]].x(), cubeCorners[idx[0]].y());
            DrawPoint p1 = new DrawPoint(cubeCorners[idx[1]].x(), cubeCorners[idx[1]].y());
            DrawPoint p2 = new DrawPoint(cubeCorners[idx[2]].x(), cubeCorners[idx[2]].y());
            DrawPoint p3 = new DrawPoint(cubeCorners[idx[3]].x(), cubeCorners[idx[3]].y());
            setShaded(baseColor, shadeFactor(nx, ny, nz), alpha);
            cubeFaceColor.set(shadedColor.r(), shadedColor.g(), shadedColor.b(), shadedColor.a());
            shadedColor.set(OUTLINE.r(), OUTLINE.g(), OUTLINE.b(), OUTLINE.a() * alpha);
            addQuadBorder(context, p0, p1, p2, p3, 1.4f, shadedColor);
            context.addQuadFilled(p0, p1, p2, p3, cubeFaceColor);
        }
    }

    /**
     * Draws depth-cued rotation rings, trackball backdrop, and active sweep sector.
     */
    private void paintRotate(RenderContext context, int active) {
        float trackballRadius = RADIUS * 1.28f;
        context.addCircleFilled(center.x(), center.y(), trackballRadius, TRACKBALL_FILL, 48);
        context.addCircle(center.x(), center.y(), trackballRadius, TRACKBALL_RIM, 48, 1.1f);

        boolean dragging = drag != null;
        float centerDepth = center.z();

        // Back-hemisphere segments first, thin and muted so front arcs read unambiguously in 3D.
        for (int axis = 0; axis < handles.length; axis++) {
            boolean highlight = axis == active;
            float alpha = dragging && !highlight ? 0.14f : 0.24f;
            MutableColor backColor = axisColor(axis, highlight, alpha);
            for (int segment = 0; segment < RING_SEGMENTS; segment++) {
                MutableVec3 a = handles[axis][segment], b = handles[axis][segment + 1];
                if (!drawable(a, b) || (a.z() + b.z()) * 0.5f <= centerDepth + 1e-4f) continue;
                context.addLine(a.x(), a.y(), b.x(), b.y(), backColor, highlight ? 1.8f : 1.3f);
            }
        }

        if (dragging && drag.kind == DragKind.AXIS && Math.abs(drag.delta) > 1e-4f) {
            paintRotationSector(context);
        }

        // Front-hemisphere segments on top, drawing non-active axes before the active ring.
        for (int pass = 0; pass < 2; pass++) {
            for (int axis = 0; axis < handles.length; axis++) {
                boolean highlight = axis == active;
                if ((pass == 0) == highlight) continue;
                float alpha = dragging && !highlight ? 0.34f : 1.0f;
                MutableColor color = axisColor(axis, highlight, alpha);
                shadedColor.set(OUTLINE.r(), OUTLINE.g(), OUTLINE.b(), OUTLINE.a() * alpha);
                float haloWidth = highlight ? 5.6f : 4.2f;
                float ringWidth = highlight ? 3.6f : 2.3f;
                for (int segment = 0; segment < RING_SEGMENTS; segment++) {
                    MutableVec3 a = handles[axis][segment], b = handles[axis][segment + 1];
                    if (!drawable(a, b) || (a.z() + b.z()) * 0.5f > centerDepth + 1e-4f) continue;
                    context.addLine(a.x(), a.y(), b.x(), b.y(), shadedColor, haloWidth);
                }
                for (int segment = 0; segment < RING_SEGMENTS; segment++) {
                    MutableVec3 a = handles[axis][segment], b = handles[axis][segment + 1];
                    if (!drawable(a, b) || (a.z() + b.z()) * 0.5f > centerDepth + 1e-4f) continue;
                    context.addLine(a.x(), a.y(), b.x(), b.y(), color, ringWidth);
                }
            }
        }

        context.addCircleFilled(center.x(), center.y(), 4.5f, OUTLINE, 20);
        context.addCircleFilled(center.x(), center.y(), 2.8f, NEUTRAL, 20);
    }

    /**
     * Draws a translucent swept sector on the active rotation ring showing the accumulated angle.
     */
    private void paintRotationSector(RenderContext context) {
        int axis = drag.axis;
        MutableVec3 u = drag.planeU;
        MutableVec3 v = drag.planeV;
        float radius = worldRadius * (1 + 0.12f * axis);
        float sweep = Math.max((float) (-2 * Math.PI), Math.min((float) (2 * Math.PI), drag.delta));
        int steps = Math.max(2, (int) Math.ceil(Math.abs(sweep) / (Math.PI / 24)));
        Camera3D camera = viewport.camera();
        DrawPoint hub = new DrawPoint(center.x(), center.y());
        MutableVec3 prev = new MutableVec3();
        MutableVec3 first = new MutableVec3();
        boolean hasPrev = false;
        for (int i = 0; i <= steps; i++) {
            double angle = drag.startAngle + sweep * i / steps;
            float c = radius * (float) Math.cos(angle), s = radius * (float) Math.sin(angle);
            point.set(drag.pivot).add(c * u.x() + s * v.x(), c * u.y() + s * v.y(), c * u.z() + s * v.z());
            if (!project(camera, point, scratch)) {
                hasPrev = false;
                continue;
            }
            if (i == 0) first.set(scratch);
            if (hasPrev) {
                context.addTriangleFilled(hub, new DrawPoint(prev.x(), prev.y()),
                        new DrawPoint(scratch.x(), scratch.y()), SECTOR_FILL);
            }
            prev.set(scratch);
            hasPrev = true;
        }
        if (Float.isFinite(first.z())) {
            context.addLine(center.x(), center.y(), first.x(), first.y(), SECTOR_LINE, 1.4f);
        }
        if (hasPrev && Float.isFinite(prev.z())) {
            context.addLine(center.x(), center.y(), prev.x(), prev.y(), ACTIVE, 1.8f);
        }
    }

    /**
     * Draws a floating rounded badge showing the active transform delta while dragging.
     */
    private void paintReadoutBadge(RenderContext context, int active) {
        if (drag == null || !drag.moved) return;
        String text = formatReadout();
        if (text == null) return;
        RichText rich = RichText.of(text, Fonts.defaultFace(), 10.5f, BADGE_TEXT);
        float width = Math.max(46, text.length() * 6.5f + 14);
        float height = 20;
        float bx = center.x() + 14;
        float by = center.y() - 30;
        MutableColor border = active >= 0 && active < 3 ? COLORS[active]
                : active >= PLANE_HANDLE_BASE && active < PLANE_HANDLE_BASE + 3 ? COLORS[active - PLANE_HANDLE_BASE]
                : ACTIVE;
        context.addRectFilled(bx, by, width, height, 4.0f, BADGE_BG);
        context.addRect(bx, by, width, height, 4.0f, border, 1.0f);
        context.addText(rich, bx + 7, by + 3, width - 14, height - 6, BADGE_TEXT);
    }

    /**
     * Formats the current transform delta for the floating readout badge.
     */
    private String formatReadout() {
        if (drag.kind == DragKind.PLANE) {
            int plane = drag.axis - PLANE_HANDLE_BASE;
            return String.format(Locale.ROOT, "%s %+.2f, %+.2f", PLANE_LABELS[plane], drag.delta, drag.deltaV);
        }
        if (drag.kind == DragKind.UNIFORM_SCALE) {
            return String.format(Locale.ROOT, "XYZ \u00D7%.2f", Math.max(0.15f, 1.0f + drag.delta));
        }
        String label = AXIS_LABELS[drag.axis];
        return switch (mode) {
            case MOVE -> String.format(Locale.ROOT, "%s %+.2f", label, drag.delta);
            case ROTATE -> String.format(Locale.ROOT, "%s %+.1f\u00B0", label, Math.toDegrees(drag.delta));
            case SCALE -> String.format(Locale.ROOT, "%s \u00D7%.2f", label, Math.max(0.15f, 1.0f + drag.delta));
        };
    }

    /**
     * Sorts {@link #axisOrder} back-to-front by each axis tip's projected depth (largest Z first).
     */
    private void sortAxesByDepth() {
        axisOrder[0] = 0;
        axisOrder[1] = 1;
        axisOrder[2] = 2;
        for (int i = 0; i < 2; i++) {
            for (int j = i + 1; j < 3; j++) {
                float zi = handles[axisOrder[i]][1].z();
                float zj = handles[axisOrder[j]][1].z();
                if (!Float.isFinite(zi)) zi = Float.NEGATIVE_INFINITY;
                if (!Float.isFinite(zj)) zj = Float.NEGATIVE_INFINITY;
                if (zi < zj) {
                    int tmp = axisOrder[i];
                    axisOrder[i] = axisOrder[j];
                    axisOrder[j] = tmp;
                }
            }
        }
    }

    /**
     * Returns the handle color for an axis, brightened on hover/drag and dimmed when another handle is active.
     */
    private MutableColor axisColor(int axis, boolean highlight, float alpha) {
        MutableColor base = highlight ? ACTIVE : COLORS[axis];
        if (alpha >= 0.999f) return base;
        return new MutableColor(base.r(), base.g(), base.b(), base.a() * alpha);
    }

    /**
     * Computes a directional shading multiplier in [0.64, 1.12] for 3D cone and cube facets.
     */
    private static float shadeFactor(float nx, float ny, float nz) {
        float dot = nx * KEY_LIGHT.x() + ny * KEY_LIGHT.y() + nz * KEY_LIGHT.z();
        return 0.64f + 0.48f * (0.5f + 0.5f * Math.max(-1.0f, Math.min(1.0f, dot)));
    }

    /**
     * Writes {@code baseColor} scaled by {@code shade} and {@code alpha} into {@link #shadedColor}.
     */
    private void setShaded(MutableColor baseColor, float shade, float alpha) {
        shadedColor.set(
                Math.min(1.0f, baseColor.r() * shade),
                Math.min(1.0f, baseColor.g() * shade),
                Math.min(1.0f, baseColor.b() * shade),
                baseColor.a() * alpha);
    }

    /**
     * Finds the closest segment of an axis or rotation ring. Projecting pointer {@code p} onto segment {@code ab}
     * uses {@code t = clamp((p - a) · (b - a) / |b - a|², 0, 1)}.
     *
     * @param x pointer X coordinate in canvas units
     * @param y pointer Y coordinate in canvas units
     * @return axis index times {@link #RING_SEGMENTS} plus segment index, or -1
     */
    private int hit(float x, float y) {
        if (!prepareGeometry()) return -1;
        int result = -1;
        float best = HIT_RADIUS * HIT_RADIUS;
        for (int axis = 0; axis < handles.length; axis++) {
            int count = mode == Mode.ROTATE ? RING_SEGMENTS : 1;
            for (int segment = 0; segment < count; segment++) {
                MutableVec3 a = handles[axis][segment], b = handles[axis][segment + 1];
                if (!drawable(a, b)) continue;
                float dx = b.x() - a.x(), dy = b.y() - a.y();
                float lengthSquared = dx * dx + dy * dy;
                float start = mode == Mode.ROTATE ? 0 : 12 / (float) Math.sqrt(lengthSquared);
                float t = Math.max(start, Math.min(1, ((x - a.x()) * dx + (y - a.y()) * dy) / lengthSquared));
                float ox = a.x() + t * dx - x, oy = a.y() + t * dy - y;
                float distance = ox * ox + oy * oy;
                if (distance >= best) continue;
                best = distance;
                result = axis * RING_SEGMENTS + segment;
            }
        }
        return result;
    }

    /**
     * @return index of the planar move quad under {@code (x, y)}, or -1
     */
    private int hitPlane(float x, float y) {
        if (!prepareGeometry()) return -1;
        for (int plane = 0; plane < AXES.length; plane++) {
            if (!planeVisible[plane]) continue;
            MutableVec3[] q = planeQuads[plane];
            if (pointInTriangle(x, y, q[0], q[1], q[2]) || pointInTriangle(x, y, q[0], q[2], q[3])) {
                return plane;
            }
        }
        return -1;
    }

    /**
     * @return true if {@code (x, y)} lies inside the center hub hit radius
     */
    private boolean hitCenter(float x, float y) {
        if (!prepareGeometry()) return false;
        float dx = x - center.x(), dy = y - center.y();
        return dx * dx + dy * dy <= HIT_RADIUS * HIT_RADIUS;
    }

    /**
     * Standard 2D point-in-triangle check independent of winding order.
     */
    private static boolean pointInTriangle(float px, float py, MutableVec3 a, MutableVec3 b, MutableVec3 c) {
        float d1 = cross2(px - a.x(), py - a.y(), b.x() - a.x(), b.y() - a.y());
        float d2 = cross2(px - b.x(), py - b.y(), c.x() - b.x(), c.y() - b.y());
        float d3 = cross2(px - c.x(), py - c.y(), a.x() - c.x(), a.y() - c.y());
        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);
        return !(hasNeg && hasPos);
    }

    private static float cross2(float ax, float ay, float bx, float by) {
        return ax * by - ay * bx;
    }

    /**
     * Updates the projection when the camera or scene changes. The base radius in scene units equals
     * the distance from the pivot to a point at the same depth offset by {@link #RADIUS} on screen.
     * Ring points lie at {@code pivot + r * (cos(angle) * u + sin(angle) * v)},
     * where u and v are two axes perpendicular to the rotation axis forming a right-handed basis.
     * In {@link Pivot#CENTER} mode the pivot equals the average of the selected nodes' world pivots.
     *
     * @return true if the pivot lies within the view frustum
     */
    private boolean prepareGeometry() {
        if (selected == null || selected.scene() != viewport.scene()) return false;
        Camera3D camera = viewport.camera();
        long nextScene = viewport.scene().revision(), nextCamera = camera.revision();
        if (nextScene == sceneRevision && nextCamera == cameraRevision) return visible;
        sceneRevision = nextScene;
        cameraRevision = nextCamera;
        visible = false;
        for (SceneNode node = selected; node != null; node = node.parent()) {
            if (!node.visible()) return false;
        }
        SceneNode parent = selected.parent();
        if (!worldPivot(selected, pivot)) return false;
        if (pivotMode == Pivot.CENTER && targets.size() > 1) centerPivot();
        visible = project(camera, pivot, center)
                && camera.unproject(center.x() + RADIUS, center.y(), center.z(), point);
        if (!visible) return false;
        float x = point.x() - pivot.x(), y = point.y() - pivot.y(), z = point.z() - pivot.z();
        worldRadius = (float) Math.sqrt(x * x + y * y + z * z);
        parentAxes.set(0, 0, 0, 1);
        for (SceneNode ancestor = parent; ancestor != null; ancestor = ancestor.parent()) {
            parentAxes.setProduct(ancestor.rotation(ancestorRotation), parentAxes);
        }
        localAxes.setProduct(parentAxes, selected.rotation(localAxes));
        for (int axis = 0; axis < AXES.length; axis++) {
            MutableVec3 unit = AXES[axis];
            if (space == Space.LOCAL) localAxes.rotate(unit.x(), unit.y(), unit.z(), worldAxes[axis]);
            else worldAxes[axis].set(unit);
        }
        resolveViewDirection(camera, pivot, viewDir);
        for (int axis = 0; axis < handles.length; axis++) {
            MutableVec3 unit = worldAxes[axis];
            MutableVec3[] handle = handles[axis];
            if (mode == Mode.ROTATE) {
                MutableVec3 u = worldAxes[(axis + 1) % 3], v = worldAxes[(axis + 2) % 3];
                float radius = worldRadius * (1 + 0.12f * axis);
                for (int segment = 0; segment < RING_SEGMENTS; segment++) {
                    double angle = 2 * Math.PI * segment / RING_SEGMENTS;
                    float c = radius * (float) Math.cos(angle), s = radius * (float) Math.sin(angle);
                    point.set(pivot).add(c * u.x() + s * v.x(), c * u.y() + s * v.y(), c * u.z() + s * v.z());
                    project(camera, point, handle[segment]);
                }
                handle[RING_SEGMENTS].set(handle[0]);
            } else {
                handle[0].set(center);
                point.set(pivot.x() + worldRadius * unit.x(), pivot.y() + worldRadius * unit.y(),
                        pivot.z() + worldRadius * unit.z());
                project(camera, point, handle[1]);
                if (mode == Mode.MOVE) projectMovePlane(camera, axis);
            }
        }
        return true;
    }

    /**
     * Projects the planar move quad for {@code plane} and checks that it is not edge-on to the camera.
     */
    private void projectMovePlane(Camera3D camera, int plane) {
        MutableVec3 normal = worldAxes[plane];
        float facing = Math.abs(normal.x() * viewDir.x() + normal.y() * viewDir.y() + normal.z() * viewDir.z());
        if (facing < 0.14f) {
            planeVisible[plane] = false;
            return;
        }
        MutableVec3 u = worldAxes[(plane + 1) % 3];
        MutableVec3 v = worldAxes[(plane + 2) % 3];
        float min = worldRadius * PLANE_MIN;
        float max = worldRadius * PLANE_MAX;
        MutableVec3[] q = planeQuads[plane];
        boolean ok = projectPlaneCorner(camera, u, v, min, min, q[0])
                && projectPlaneCorner(camera, u, v, max, min, q[1])
                && projectPlaneCorner(camera, u, v, max, max, q[2])
                && projectPlaneCorner(camera, u, v, min, max, q[3]);
        planeVisible[plane] = ok;
    }

    private boolean projectPlaneCorner(Camera3D camera, MutableVec3 u, MutableVec3 v,
                                       float du, float dv, MutableVec3 out) {
        point.set(pivot).add(du * u.x() + dv * v.x(), du * u.y() + dv * v.y(), du * u.z() + dv * v.z());
        return project(camera, point, out);
    }

    /**
     * Resolves a unit vector pointing from {@code target} toward the camera.
     */
    private void resolveViewDirection(Camera3D camera, MutableVec3 target, MutableVec3 out) {
        if (camera.orthographic()) {
            camera.orientation(rotation).rotate(0, 0, 1, out);
            return;
        }
        camera.position(out);
        out.set(out.x() - target.x(), out.y() - target.y(), out.z() - target.z());
        float len = (float) Math.sqrt(out.x() * out.x() + out.y() * out.y() + out.z() * out.z());
        if (len > 1e-6f) out.set(out.x() / len, out.y() / len, out.z() / len);
        else out.set(0, 0, 1);
    }

    /**
     * Replaces the pivot with the average world pivot of all targets,
     * {@code (p_1 + ... + p_n) / n}.
     */
    private void centerPivot() {
        float sumX = 0, sumY = 0, sumZ = 0;
        int count = 0;
        for (SceneNode node : targets) {
            if (!worldPivot(node, point)) continue;
            sumX += point.x();
            sumY += point.y();
            sumZ += point.z();
            count++;
        }
        if (count > 0) pivot.set(sumX / count, sumY / count, sumZ / count);
    }

    /**
     * Converts a node's local position to world space.
     *
     * @param node scene node
     * @param out  destination for the pivot in world space
     * @return false if the parent transform is degenerate
     */
    private boolean worldPivot(SceneNode node, MutableVec3 out) {
        node.position(out);
        SceneNode parent = node.parent();
        return parent == null || parent.worldMatrix(parentWorld).transformPoint(out, out);
    }

    /**
     * Projects a point, marking clipped depth with {@code NaN}.
     *
     * @param camera scene camera
     * @param world  world-space point in scene units
     * @param out    destination for canvas coordinates and depth
     * @return true when between the near and far clipping planes
     */
    private static boolean project(Camera3D camera, MutableVec3 world, MutableVec3 out) {
        if (camera.project(world, out) && out.z() >= 0 && out.z() <= 1) return true;
        out.set(0, 0, Float.NaN);
        return false;
    }

    /**
     * Rejects degenerate segments and overly foreshortened linear axis projections.
     *
     * @param a segment start on canvas
     * @param b segment end on canvas
     * @return true for a valid segment of sufficient screen length
     */
    private boolean drawable(MutableVec3 a, MutableVec3 b) {
        if (!Float.isFinite(a.z()) || !Float.isFinite(b.z())) return false;
        float dx = b.x() - a.x(), dy = b.y() - a.y();
        return dx * dx + dy * dy > (mode == Mode.ROTATE ? 0.25f : 20 * 20);
    }

    private static MutableVec3 normalized(float x, float y, float z) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        return new MutableVec3(x / len, y / len, z / len);
    }

    /**
     * Active gesture kind.
     */
    private enum DragKind {
        AXIS, PLANE, UNIFORM_SCALE
    }

    /**
     * Snapshot of the gesture at press time, plus live delta for visual feedback.
     */
    private static final class Drag {
        final DragKind kind;
        final int axis;
        final float x;
        final float y;
        final float gradientX;
        final float gradientY;
        final MutableVec3 worldAxis;
        final MutableVec3 planeU;
        final MutableVec3 planeV;
        final MutableVec3 pivot;
        final MutableVec3 planeHit;
        final float startAngle;
        final NodeStart active;
        final List<NodeStart> nodes;
        float delta;
        float deltaV;
        boolean moved;

        Drag(DragKind kind, int axis, float x, float y, float gradientX, float gradientY,
             MutableVec3 worldAxis, MutableVec3 planeU, MutableVec3 planeV,
             MutableVec3 pivot, MutableVec3 planeHit, float startAngle,
             NodeStart active, List<NodeStart> nodes) {
            this.kind = kind;
            this.axis = axis;
            this.x = x;
            this.y = y;
            this.gradientX = gradientX;
            this.gradientY = gradientY;
            this.worldAxis = worldAxis;
            this.planeU = planeU;
            this.planeV = planeV;
            this.pivot = pivot;
            this.planeHit = planeHit;
            this.startAngle = startAngle;
            this.active = active;
            this.nodes = nodes;
        }
    }

    /**
     * Pose of one node at the start of a gesture.
     *
     * @param node          transformed node
     * @param position      local position in parent space
     * @param worldPosition pivot in world space
     * @param scale         local scale factors
     * @param rotation      local orientation
     * @param moveDirection handle axis in parent space, scaled for translation
     * @param rotateAxis    unit handle axis in parent space for rotation
     * @param scaleWeights  squared projections of the local axes onto the scale handle
     * @param worldToParent inverse parent matrix, or null for a root
     */
    private record NodeStart(SceneNode node, MutableVec3 position, MutableVec3 worldPosition, MutableVec3 scale,
                             MutableQuat rotation, MutableVec3 moveDirection, MutableVec3 rotateAxis,
                             MutableVec3 scaleWeights, MutableMat4 worldToParent) {
    }
}
