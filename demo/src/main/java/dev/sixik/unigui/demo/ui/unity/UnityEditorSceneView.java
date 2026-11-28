package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.event.*;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.SceneWireframe;
import dev.sixik.unigui.api.widget.Widget;
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

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
     * Active scene object, or null.
     */
    private UnitySceneObject selected;
    /**
     * Every selected scene object, including the active one.
     */
    private List<UnitySceneObject> selectedObjects = List.of();
    /**
     * Top bar settings: handle space and pivot, snapping, draw mode and lighting.
     */
    private final UnitySceneViewOptions options = new UnitySceneViewOptions();
    /**
     * Wireframe and light gizmos drawn over the scene.
     */
    private final UnitySceneOverlay overlay;
    /**
     * Wire color in Wireframe mode, light over the dark background.
     */
    private static final int WIRE_LIGHT = 0xD9D1D9E6;
    /**
     * Wire color over shaded surfaces.
     */
    private static final int WIRE_DARK = 0xB305080D;
    /**
     * Wires of selected objects and their content, Unity's selection orange.
     */
    private static final int WIRE_SELECTED = 0xFFFFA028;
    /**
     * Wireframe mode: every edge, no surfaces.
     */
    private final SceneWireframe edgesOnly;
    /**
     * Shaded Wireframe mode: surfaces with their visible edges.
     */
    private final SceneWireframe shadedEdges;
    /**
     * Top bar built from {@link #options}; rebuilt when the options change from code.
     */
    private Widget topBar;
    /**
     * Position of the top bar among this view's children.
     */
    private final int topBarIndex;
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

        // No frame on hover: selected objects get an orange outline and the hovered one a faint outline instead.
        viewport = new Scene3D(scene).highlightObjects(false).highlightBlocks(false).maxZoomDistance(64.0f);
        viewport.clearColor().set(0.09f, 0.105f, 0.135f, 1.0f);
        viewport.camera().setPerspective((float) Math.toRadians(FIELD_OF_VIEW), 0.05f, 128.0f);
        viewport.layout(style -> style.sizePercent(100.0f, 100.0f));
        gizmo = new SceneTransformGizmo(viewport);
        overlay = new UnitySceneOverlay(viewport);
        // Wireframes are drawn by the scene renderer from the geometry it renders, Minecraft content included.
        Predicate<SceneNode> wireSelected = node -> UnitySceneOverlay.isSelected(node, selectedObjects);
        edgesOnly = new SceneWireframe(WIRE_LIGHT, WIRE_SELECTED, false, wireSelected);
        shadedEdges = new SceneWireframe(WIRE_DARK, WIRE_SELECTED, true, wireSelected);
        viewport.painter((draw, width, height) -> {
            overlay.paintOutlines(draw, selectedObjects, hoveredObject());
            overlay.paintFog(draw, selectedObjects);
            overlay.paintLights(draw, selectedObjects);
            gizmo.paint(draw);
        });
        viewport.onNodeHover(event -> viewport.requestRender());
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

        topBarIndex = children().size();
        topBar = UnitySceneToolbar.createTopBar(options, this::applyOptions);
        addChild(topBar);
        applyOptions();
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
     * Applies the top bar settings to the handles and the viewport. Only this view changes.
     */
    private void applyOptions() {
        finishGizmo();
        gizmo.space(options.space);
        gizmo.pivot(options.pivot);
        gizmo.snapSteps(options.moveStep, options.rotateStep, options.scaleStep);
        viewport.sceneLighting(options.lighting);
        viewport.wireframe(switch (options.drawMode) {
            case SHADED -> null;
            case WIREFRAME -> edgesOnly;
            case SHADED_WIREFRAME -> shadedEdges;
        });
        viewport.requestRender();
    }

    /**
     * @return the editor object under the pointer, or null over the ground, the background or while dragging
     */
    private UnitySceneObject hoveredObject() {
        if (gizmoPointer >= 0 || gizmo.dragging()) return null;
        for (SceneNode node = viewport.hoveredNode(); node != null && node != ground; node = node.parent()) {
            if (node instanceof UnitySceneObject object) return object;
        }
        return null;
    }

    /**
     * @return the Scene view top bar settings
     */
    UnitySceneViewOptions options() {
        return options;
    }

    /**
     * Changes the top bar settings from code and refreshes the view.
     *
     * @param change edits the settings
     */
    void updateOptions(java.util.function.Consumer<UnitySceneViewOptions> change) {
        change.accept(options);
        applyOptions();
        // The bar only reads the options when it is built or clicked, so rebuild it to show the new state.
        removeChild(topBar);
        topBar = UnitySceneToolbar.createTopBar(options, this::applyOptions);
        insertChild(topBarIndex, topBar);
    }

    /**
     * @return the viewport, for tests and snapshots
     */
    Scene3D viewport() {
        return viewport;
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
        selectObjects(object == null ? List.of() : List.of(object), object);
    }

    /**
     * Places the handles on several objects. They move, rotate and scale together around the active
     * object pivot or the selection center.
     *
     * @param objects every selected object
     * @param active  active object, or null
     */
    void selectObjects(List<UnitySceneObject> objects, UnitySceneObject active) {
        List<UnitySceneObject> next = active == null ? List.of() : List.copyOf(objects);
        if (selected == active && selectedObjects.equals(next)) return;
        finishGizmo();
        selected = active;
        selectedObjects = next;
        updateGizmoSelection();
        frameButton.enabled(active != null);
        viewport.requestRender();
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
        if (editable && mode != null) gizmo.select(selected, selectedObjects);
        else gizmo.select(null);
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
        UnitySceneObject light;
        if (gizmo.begin(event.localX(), event.localY())) {
            gizmoPointer = event.pointerId();
            uiContext().capturePointer(gizmoPointer, viewport);
            event.cancel();
        } else if ((light = overlay.pickLight(event.localX(), event.localY())) != null) {
            // Light icons sit in front of the geometry: select the light instead of orbiting or picking a mesh.
            onPick.accept(light);
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
            gizmo.drag(event.localX(), event.localY(), snapping());
            notifyTransform();
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
            gizmo.drag(event.localX(), event.localY(), snapping());
            notifyTransform();
            finishGizmo();
            event.cancel();
        } else if (backgroundPointer == event.pointerId()) {
            backgroundPointer = -1;
            if (!event.isCancelled() && withinClickDistance(event)
                    && viewport.pickNode(event.localX(), event.localY()) == null) onPick.accept(null);
        }
    }

    /**
     * Snapping is on while the magnet toggle is on, and temporarily while Ctrl (or Cmd) is held, as in Unity.
     *
     * @return whether the current drag snaps to the configured steps
     */
    private boolean snapping() {
        return options.snap || uiContext() != null && uiContext().keyboard().isAnyDown(KeyCodes.LEFT_CONTROL,
                KeyCodes.RIGHT_CONTROL, KeyCodes.LEFT_SUPER, KeyCodes.RIGHT_SUPER);
    }

    /**
     * Refreshes the Inspector for every object moved by the handles.
     */
    private void notifyTransform() {
        for (UnitySceneObject object : selectedObjects) onTransform.accept(object);
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
        UnityPlayMode.tick(viewport.scene(), frame);
        UnitySceneOverlay.syncSceneLighting(viewport.scene());
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
        MutableVec3 target = new MutableVec3();
        MutableVec3 pivot = new MutableVec3();
        int count = 0;
        for (UnitySceneObject object : selectedObjects) {
            object.origin(pivot);
            if (!object.worldMatrix(new MutableMat4()).transformPoint(pivot, pivot)) continue;
            target.add(pivot.x(), pivot.y(), pivot.z());
            count++;
        }
        if (count == 0) return;
        target.set(target.x() / count, target.y() / count, target.z() / count);
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
