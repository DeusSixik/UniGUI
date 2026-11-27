package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.demo.ui.scene.SceneTransformGizmo;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.dnd.DragDropEvent;
import dev.sixik.unigui.widgets.dnd.DragOverEvent;
import dev.sixik.unigui.widgets.dnd.DragSession;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.rendering.Scene3D;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Scene workspace with orbit controls, mesh picking and camera framing.
 */
public final class UnityEditorSceneView extends LinearBox {
    /**
     * Default vertical field of view in degrees.
     */
    private static final float FIELD_OF_VIEW = 48.0f;
    /**
     * Maximum movement in UI units for a background press to count as a click.
     */
    private static final float CLICK_DISTANCE = 3.0f;
    /**
     * Origin of the default XZ work plane, in scene units.
     */
    private static final MutableVec3 PLANE_ORIGIN = new MutableVec3();
    /**
     * Upward normal of the XZ work plane.
     */
    private static final MutableVec3 PLANE_NORMAL = new MutableVec3(0, 1, 0);

    /**
     * Viewport rendering the shared scene graph.
     */
    private final Scene3D viewport;
    /**
     * On-screen translation, rotation and scale handles.
     */
    private final SceneTransformGizmo gizmo;
    /**
     * Tracks the shared tool selection for buttons and keyboard shortcuts.
     */
    private final UnityToolsOverlay tools;
    /**
     * Initial floor acts as editable scenery in Hierarchy and as background in Scene.
     */
    private final SceneNode ground;
    /**
     * Receives a viewport selection or a null selection on a background click.
     */
    private final Consumer<SceneNode> onPick;
    /**
     * Refreshes Inspector fields after dragging a handle.
     */
    private final Consumer<UnitySceneObject> onTransform;
    /**
     * Camera action enabled when an object is selected.
     */
    private final Button frameButton;
    /**
     * Selected scene object, or null.
     */
    private UnitySceneObject selected;
    /**
     * Active transform mode, or null for tools without handles.
     */
    private SceneTransformGizmo.Mode mode = SceneTransformGizmo.Mode.MOVE;
    /**
     * Whether scene transformations are allowed in the current play state.
     */
    private boolean editable = true;
    /**
     * Places the dragged template at its projected world point.
     */
    private BiConsumer<UnityPrefab, MutableVec3> onPlacePrefab;
    /**
     * Scratch ray used to locate a drop point.
     */
    private final Ray dropRay = new Ray();
    /**
     * Scratch world-space point on the work plane, in scene units.
     */
    private final MutableVec3 dropPoint = new MutableVec3();
    /**
     * Pointer captured by a handle, or -1.
     */
    private int gizmoPointer = -1;
    /**
     * Pointer pressed on empty background, or -1.
     */
    private int backgroundPointer = -1;
    /**
     * Background press coordinates in viewport units.
     */
    private float pressX, pressY;

    /**
     * Creates an isolated empty scene view for callers that only need the widget shell.
     */
    public UnityEditorSceneView() {
        this(new Scene(), null, node -> {
        }, node -> {
        });
    }

    /**
     * Creates a view for the editor's graph.
     *
     * @param scene       editable scene
     * @param ground      ground plane treated as background when clicked
     * @param onPick      callback for object and background clicks
     * @param onTransform callback for changes through the gizmo
     */
    UnityEditorSceneView(Scene scene, SceneNode ground, Consumer<SceneNode> onPick,
                         Consumer<UnitySceneObject> onTransform) {
        super(Orientation.VERTICAL);
        this.ground = ground;
        this.onPick = onPick;
        this.onTransform = onTransform;
        spacing(0.0f);
        focusable(true);
        layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));

        viewport = new Scene3D(scene).highlightObjects(true).highlightBlocks(true).maxZoomDistance(64.0f);
        viewport.clearColor().set(0.09f, 0.105f, 0.135f, 1.0f);
        viewport.camera().setPerspective((float) Math.toRadians(FIELD_OF_VIEW), 0.05f, 128.0f);
        viewport.layout(style -> style.sizePercent(100.0f, 100.0f));
        gizmo = new SceneTransformGizmo(viewport);
        viewport.painter((draw, width, height) -> gizmo.paint(draw));
        viewport.onNodeClick(event -> onPick.accept(event.node() == ground ? null : event.node()));
        viewport.on(PointerPressedEvent.TYPE, this::press);
        viewport.on(PointerMovedEvent.TYPE, this::move);
        viewport.on(PointerReleasedEvent.TYPE, this::release);
        viewport.on(PointerExitedEvent.TYPE, event -> gizmo.clearHover());
        viewport.on(DragOverEvent.TYPE, event -> {
            if (event.phase() != EventPhase.CAPTURE && prefabDropPoint(event.session()) != null) event.accept(viewport);
        });
        viewport.on(DragDropEvent.TYPE, event -> {
            UnityPrefab prefab = prefabDropPoint(event.session());
            if (prefab == null) return;
            onPlacePrefab.accept(prefab, dropPoint);
            event.cancel();
        });
        resetView();

        HBox toolbar = new HBox();
        toolbar.themeEnabled(false);
        toolbar.background(UnityStyles.BG_TOOLBAR);
        toolbar.spacing(4.0f);
        toolbar.layout(style -> style.widthPercent(100.0f).height(26.0f).padding(4.0f, 0.0f)
                .alignItems(Align.CENTER).flexShrink(0.0f));
        toolbar.addChild(viewButton("Reset View", this::resetView));
        frameButton = viewButton("Frame Selected", this::frameSelected);
        frameButton.enabled(false);
        toolbar.addChild(frameButton);
        Button projection = viewButton("Perspective", null);
        projection.onClick(event -> toggleProjection(projection));
        toolbar.addChild(projection);

        addChild(UnitySceneToolbar.createTopBar(space -> {
            finishGizmo();
            gizmo.space(space);
        }));
        addChild(UnityStyles.separatorLine());
        addChild(toolbar);
        addChild(UnityStyles.separatorLine());
        StackPanel host = new StackPanel();
        host.layout(style -> style.widthPercent(100.0f).height(0.0f).flexGrow(1.0f).flexShrink(1.0f));
        host.addChild(viewport);
        tools = new UnityToolsOverlay(host, UnitySceneToolbar.Tool.MOVE, tool -> {
            finishGizmo();
            mode = switch (tool) {
                case MOVE, TRANSFORM -> SceneTransformGizmo.Mode.MOVE;
                case ROTATE -> SceneTransformGizmo.Mode.ROTATE;
                case SCALE -> SceneTransformGizmo.Mode.SCALE;
                default -> null;
            };
            if (mode != null) gizmo.mode(mode);
            updateGizmoSelection();
        });
        addChild(host);
    }

    /**
     * Selects a tool and updates the gizmo and the active toolbar button together.
     *
     * @param tool tool selected by a keyboard shortcut
     */
    void selectTool(UnitySceneToolbar.Tool tool) {
        tools.selectTool(tool);
    }

    /**
     * Connects the viewport to scene placement.
     *
     * @param onPlacePrefab receives the dragged template and its placement point
     */
    void onPlacePrefab(BiConsumer<UnityPrefab, MutableVec3> onPlacePrefab) {
        this.onPlacePrefab = onPlacePrefab;
    }

    /**
     * Projects the pointer ray onto the horizontal work plane at Y = 0.
     *
     * @param session active drag with pointer position in root UI units
     * @return the asset if the work plane intersects the view ray, or null
     */
    private UnityPrefab prefabDropPoint(DragSession session) {
        if (!editable || onPlacePrefab == null || !UnityPrefab.DRAG_TYPE.equals(session.payload().type())) return null;
        UnityPrefab prefab = session.payload().data(UnityPrefab.class);
        if (prefab == null) return null;
        var area = viewport.layoutBounds();
        Camera3D camera = viewport.camera();
        if (!camera.screenRay(session.pointerX() - area.x(), session.pointerY() - area.y(), dropRay)) return null;
        double distance = dropRay.intersectPlane(PLANE_ORIGIN, PLANE_NORMAL);
        if (!Double.isFinite(distance) || distance > camera.far()) return null;
        dropRay.pointAt(distance, dropPoint);
        return prefab;
    }

    /**
     * Places the editor's handles on the selected object.
     *
     * @param object newly selected object, or null
     */
    void selectObject(UnitySceneObject object) {
        if (selected == object) return;
        finishGizmo();
        selected = object;
        updateGizmoSelection();
        frameButton.enabled(object != null);
    }

    /**
     * Enables or disables scene edits while leaving navigation available.
     *
     * @param editable whether any transform handle can be used
     */
    void setEditable(boolean editable) {
        finishGizmo();
        this.editable = editable;
        updateGizmoSelection();
    }

    /**
     * Shows the selected object's handles when the active tool and play state allow editing.
     */
    private void updateGizmoSelection() {
        gizmo.select(editable && mode != null ? selected : null);
    }

    /**
     * @param event primary pointer press in the viewport
     */
    private void press(PointerPressedEvent event) {
        if (!atViewport(event) || event.isCancelled() || event.button() != PointerButton.PRIMARY
                || uiContext() == null) return;
        if (gizmoPointer >= 0) {
            event.cancel();
            return;
        }
        if (uiContext().capturedPointer(event.pointerId()) != null) return;
        SceneNode hit = viewport.pickNode(event.localX(), event.localY());
        backgroundPointer = -1;
        if (gizmo.begin(event.localX(), event.localY())) {
            gizmoPointer = event.pointerId();
            uiContext().capturePointer(gizmoPointer, viewport);
            event.cancel();
        } else if (hit == null) {
            backgroundPointer = event.pointerId();
            pressX = event.localX();
            pressY = event.localY();
        }
    }

    /**
     * @param event pointer movement in the viewport
     */
    private void move(PointerMovedEvent event) {
        if (!atViewport(event) || uiContext() == null) return;
        if (gizmoPointer == event.pointerId()) {
            gizmo.drag(event.localX(), event.localY());
            onTransform.accept(selected);
            event.cancel();
            return;
        }
        if (backgroundPointer == event.pointerId() && !withinClickDistance(event)) backgroundPointer = -1;
        if (uiContext().capturedPointer(event.pointerId()) == null) gizmo.hover(event.localX(), event.localY());
    }

    /**
     * @param event pointer release in the viewport
     */
    private void release(PointerReleasedEvent event) {
        if (!atViewport(event) || event.button() != PointerButton.PRIMARY) return;
        if (gizmoPointer == event.pointerId()) {
            gizmo.drag(event.localX(), event.localY());
            onTransform.accept(selected);
            finishGizmo();
            event.cancel();
        } else if (backgroundPointer == event.pointerId()) {
            backgroundPointer = -1;
            if (!event.isCancelled() && withinClickDistance(event)
                    && viewport.pickNode(event.localX(), event.localY()) == null) onPick.accept(null);
        }
    }

    /**
     * Releases the pointer after dragging a handle.
     */
    private void finishGizmo() {
        if (gizmoPointer < 0) return;
        if (uiContext() != null) uiContext().releasePointer(gizmoPointer, viewport);
        gizmoPointer = -1;
        gizmo.end();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void tick(FrameContext frame) {
        if (gizmoPointer >= 0 && (uiContext() == null
                || uiContext().capturedPointer(gizmoPointer) != viewport)) finishGizmo();
        super.tick(frame);
    }

    /**
     * Checks whether a pointer event targets the scene texture itself.
     *
     * @param event routed input
     * @return true for viewport target events
     */
    private boolean atViewport(PointerEvent event) {
        return event.target() == viewport && event.phase() == EventPhase.TARGET;
    }

    /**
     * Compares the squared distance from the background press with the click radius squared.
     *
     * @param event current pointer position in viewport units
     * @return true inside the click radius
     */
    private boolean withinClickDistance(PointerEvent event) {
        float dx = event.localX() - pressX;
        float dy = event.localY() - pressY;
        return dx * dx + dy * dy < CLICK_DISTANCE * CLICK_DISTANCE;
    }

    /**
     * Restores the starting orbit pose.
     */
    private void resetView() {
        if (viewport.camera().orthographic()) {
            Camera3D camera = viewport.camera();
            camera.setOrthographic(6.5f, camera.near(), camera.far());
        }
        viewport.orbit().setOrbit(new MutableVec3(0, 0.5f, 0), 8.0f, 0.55, 0.48);
        viewport.requestRender();
    }

    /**
     * Moves the camera target to the selected object's pivot in world space.
     */
    private void frameSelected() {
        if (selected == null) return;
        MutableVec3 target = selected.origin(new MutableVec3());
        if (!selected.worldMatrix(new MutableMat4()).transformPoint(target, target)) return;
        viewport.orbit().setOrbit(target, viewport.orbit().distance(), viewport.orbit().yaw(), viewport.orbit().pitch());
        viewport.requestRender();
    }

    /**
     * Switches between perspective and orthographic projection.
     *
     * @param button control whose label indicates the current projection
     */
    private void toggleProjection(Button button) {
        Camera3D camera = viewport.camera();
        if (camera.orthographic()) {
            camera.setPerspective((float) Math.toRadians(FIELD_OF_VIEW), camera.near(), camera.far());
            button.text("Perspective");
        } else {
            camera.setOrthographic(6.5f, camera.near(), camera.far());
            button.text("Orthographic");
        }
        viewport.requestRender();
    }

    /**
     * Creates a styled toolbar control.
     *
     * @param label  control text
     * @param action click handler, or null when wired by the caller
     * @return toolbar button
     */
    private static Button viewButton(String label, Runnable action) {
        Button button = new Button(label);
        UnityStyles.styleButton(button);
        button.layout(style -> style.height(20.0f).padding(6.0f, 0.0f).flexShrink(0.0f));
        if (action != null) button.onClick(event -> action.run());
        return button;
    }
}
