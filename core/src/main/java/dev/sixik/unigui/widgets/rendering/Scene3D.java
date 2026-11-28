package dev.sixik.unigui.widgets.rendering;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.BaseEvent;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec2;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Vec3View;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.Scene3DRenderer;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.OrbitCameraController;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.ScenePicker;
import dev.sixik.unigui.api.scene.SceneWireframe;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.ZOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Offscreen 3D viewport with ordinary UI widgets drawn over the scene. The primary button orbits
 * on drag and picks nodes on click. The middle or secondary button pans, and the wheel zooms.
 * The backend draws the 3D pass through {@link RenderBackend#scene3D()}.
 */
@WikiPage(
        title = "Scene3D", slug = "scene-3d",
        category = "API/3D Scene", order = 10,
        summary = "Interactive 3D viewport with a scene graph, picking and projected UI.")
@WikiSection(title = "Geometry and UI layers", order = 10, markdown = """
        Add geometry to `scene()` and mount the viewport in the UI tree. The backend renders the 3D pass first, then
        `painter(...)` and `content(...)` into the target. Anchors are widgets rendered over the displayed texture.
        They follow world or node points and use ordinary UI hit testing.
        """)
@WikiSection(title = "Camera and picking", order = 20, markdown = """
        Primary drag orbits. Middle or secondary drag pans. The wheel zooms. A short primary click emits a node
        click when press and release hit the same node. `onNodeHover` reports transitions, including a null node
        when the pointer leaves a hit. Set `highlightBlocks(true)` for cell highlights and `highlightObjects(true)`
        for object frames or custom highlight geometry.
        """)
@WikiSection(title = "Custom viewport tools", order = 25, markdown = """
        For a transform gizmo, project the selected node's world-space pivot and handles with
        `camera().project(...)`. You can paint handles over the 3D pass through `painter(...)`.
        With the default canvas size and `ImageFit.STRETCH`, projected coordinates and widget-local pointer
        coordinates use the same logical units. Capture the pointer during a handle drag. Cancel the routed
        pointer event when the tool owns the gesture so the built-in orbit controller leaves it to the tool.
        Use the node setters for position, rotation and scale. They advance the scene revision.
        """)
@WikiSection(title = "Clipping and update modes", order = 30, markdown = """
        `camera().setClipPlanes(near, far)` takes distances from the camera in scene units. Extend `far` to show
        more distant objects. For tighter depth precision, keep the near plane as far from zero as the scene allows.
        `maxZoomDistance` caps the perspective orbit radius on scroll. In `ON_CHANGE`, scene and camera revisions
        trigger a refresh. Call `requestRender()` when only external painter data changes.
        """)
@WikiSection(title = "XAML", order = 40, markdown = """
        Camera attributes include `camera="orbit|fixed"`, angles `yaw` and `pitch` in **degrees**, and `distance`,
        `farClip` and `maxZoomDistance` in scene units. Each node supports `pos="x,y,z"`. A structure can specify
        `topVisibleY` and `assemblySeconds`. One ordinary UI child is allowed.

        ```xml
        <Scene3D width="320" height="200" yaw="35" pitch="20" distance="8"
                 farClip="128" maxZoomDistance="48" highlightBlocks="true">
            <Block state="minecraft:furnace[facing=south]" pos="-1,0,0"/>
            <Item item="minecraft:diamond_sword" pos="1,0.5,0"/>
            <Entity type="minecraft:zombie" pos="2,0,-1"/>
            <Label text="Furnace preview"/>
        </Scene3D>
        ```

        Use a metadata specification such as `minecraft:furnace@3` on 1.7.10. `<Structure
        src="minecraft:igloo/top"/>` requires a platform that supports structure resources.
        """)
@WikiExample(title = "Build a furnace preview", code = """
        Scene3D viewport = new Scene3D().highlightBlocks(true);
        viewport.layout(style -> style.width(320).height(200));
        viewport.camera().setClipPlanes(0.1f, 128);
        viewport.orbit().setOrbit(new MutableVec3(0.5f, 0.5f, 0.5f), 6, 0.65, 0.4);
        BlockNode furnace = new BlockNode("minecraft:furnace[facing=south]");
        viewport.scene().addRoot(furnace);
        viewport.addAnchor(new Label("Furnace"), furnace, new MutableVec3(0.5f, 1.2f, 0.5f))
                .pivot(0.5f, 1);
        viewport.onNodeClick(event -> System.out.println(event.node()));
        """)
@WikiLink(label = "Scene", target = "page:scene")
@WikiLink(label = "Camera3D", target = "page:camera-3d")
@WikiLink(label = "OrbitCameraController", target = "page:orbit-camera-controller")
@WikiLink(label = "FreeFlyCameraController", target = "page:free-fly-camera-controller")
@WikiLink(label = "RenderToTexture", target = "page:render-to-texture")
@WikiLink(label = "SceneAnchor3D", target = "page:scene-anchor-3d")
@WikiLink(label = "SceneTimeline", target = "page:scene-timeline")
@WikiLink(label = "ScenePicker", target = "page:scene-picker")
@WikiLink(label = "Scene3DRenderer", target = "page:scene-3d-renderer")
@AiDoc(
        summary = "Interactive 3D viewport with a scene graph, picking and projected UI.",
        purpose = "Embed Minecraft content or custom meshes in a widget tree and compose ordinary UI over the result.",
        criticalRules = {
                "Use `scene().addRoot(node)` for Java scene content. Use `content(widget)` for one UI overlay and `addAnchor` for projected labels or controls.",
                "The camera viewport uses logical canvas units. `pickNode` takes widget-local coordinates and handles texture placement.",
                "Adjust `camera().setClipPlanes(near, far)` to change visibility range. `maxZoomDistance` only limits perspective wheel zoom.",
                "For WASD or other custom camera input, disable built-in gestures with `cameraControls(false)` and drive `FreeFlyCameraController` yourself.",
                "Use `requestRender()` for updates to external painter data in ON_CHANGE mode.",
                "A custom drag tool should capture its pointer and cancel consumed target events before the built-in orbit gesture."
        },
        related = {"Scene", "Camera3D", "OrbitCameraController", "FreeFlyCameraController", "RenderToTexture",
                "SceneAnchor3D", "SceneTimeline", "ScenePicker", "Scene3DRenderer"},
        examples = @AiDoc.Example(title = "Build a furnace preview", code = """
                Scene3D viewport = new Scene3D().highlightBlocks(true);
                viewport.layout(style -> style.width(320).height(200));
                viewport.camera().setClipPlanes(0.1f, 128);
                viewport.orbit().setOrbit(new MutableVec3(0.5f, 0.5f, 0.5f), 6, 0.65, 0.4);
                BlockNode furnace = new BlockNode("minecraft:furnace[facing=south]");
                viewport.scene().addRoot(furnace);
                viewport.addAnchor(new Label("Furnace"), furnace, new MutableVec3(0.5f, 1.2f, 0.5f))
                        .pivot(0.5f, 1);
                viewport.onNodeClick(event -> System.out.println(event.node()));
                """))
public final class Scene3D extends RenderToTexture {
    /**
     * Depth-enabled target cleared by the 3D pass.
     */
    private static final RenderTargetOptions SCENE_TARGET = new RenderTargetOptions(true, false, "scene_3d");
    /**
     * Displacement from the press that starts a drag, in logical UI units.
     */
    private static final float DRAG_THRESHOLD = 3.0f;
    /**
     * Camera rotation angle per logical pixel of drag, in radians.
     */
    private static final double ROTATION_PER_PIXEL = 0.01;
    /**
     * Zoom multiplier per scroll step.
     */
    private static final double WHEEL_SCALE = 0.85;
    /**
     * Projection tolerance at the near plane in normalized depth units.
     */
    private static final double DEPTH_ROUNDOFF = 8.0 * Math.ulp(1.0);

    /**
     * Scene graph displayed by this viewport.
     */
    private final Scene scene;
    /**
     * Camera sized to the logical canvas.
     */
    private final Camera3D camera = new Camera3D();
    /**
     * Orbit controller around a point of interest.
     */
    private final OrbitCameraController orbit = new OrbitCameraController(camera, new MutableVec3(), 5);
    /**
     * Reusable scene picker.
     */
    private final ScenePicker picker = new ScenePicker();
    /**
     * Texture placement shared by picking and anchors.
     */
    private final SceneViewport viewport = new SceneViewport();
    /**
     * Reusable two-dimensional point in UI units.
     */
    private final MutableVec2 mapped = new MutableVec2();
    /**
     * Reusable anchor point, then projected coordinates with depth in Z.
     */
    private final MutableVec3 anchorPoint = new MutableVec3();
    /**
     * Reusable world or projection matrix.
     */
    private final MutableMat4 matrix = new MutableMat4();
    /**
     * UI anchors in insertion order.
     */
    private final List<SceneAnchor3D> anchors = new ArrayList<>();
    /**
     * Render-to-texture content and anchors in UI tree order.
     */
    private final List<Widget> children = new ArrayList<>();
    /**
     * Live view of the child list.
     */
    private final List<Widget> childrenView = Collections.unmodifiableList(children);
    /**
     * XAML declarations in insertion order, paired with their root nodes.
     */
    private final List<NodeContent> nodeContent = new ArrayList<>();

    /**
     * Last known ratio of physical pixels to outer UI units.
     */
    private float pixelScale = 1;
    /**
     * Partial tick from the latest frame.
     */
    private float partialTick;
    /**
     * Scene revision used by the last 3D pass.
     */
    private long renderedSceneRevision = -1;
    /**
     * Camera revision used by the last 3D pass.
     */
    private long renderedCameraRevision = -1;
    /**
     * Whether pointer orbit, pan and zoom gestures are enabled.
     */
    private boolean cameraControls = true;
    /**
     * Maximum perspective wheel-zoom radius in scene units. Zero uses the far plane.
     */
    private float maxZoomDistance;
    /**
     * Whether to highlight unit-cell hits.
     */
    private boolean highlightBlocks;
    /**
     * Whether to highlight object hits without a unit cell.
     */
    private boolean highlightObjects;
    /**
     * Whether the 3D pass applies the scene's lights. Only this viewport is affected.
     */
    private boolean sceneLighting = true;
    /**
     * Whether the 3D pass draws scene geometry. When false only the clear color is drawn, for overlays such as
     * an editor wireframe.
     */
    private boolean sceneGeometry = true;
    /**
     * Wireframe drawing of this viewport, or null for filled surfaces only.
     */
    private SceneWireframe wireframe;
    /**
     * Empty graph rendered when scene geometry is hidden.
     */
    private static final Scene EMPTY_SCENE = new Scene();
    /**
     * Lower corner of the cell found during the latest hover sample.
     */
    private final MutableVec3 hoverCell = new MutableVec3();
    /**
     * Highlight and its workspace for this viewport.
     */
    private HoverHighlight highlight;

    /**
     * Node under the pointer, or null.
     */
    private SceneNode hoveredNode;
    /**
     * Whether the pointer is over the scene itself.
     */
    private boolean pointerOnScene;
    /**
     * Last local pointer X in logical UI units.
     */
    private float pointerX;
    /**
     * Last local pointer Y in logical UI units.
     */
    private float pointerY;
    /**
     * Whether the cached hover result is valid.
     */
    private boolean hoverSampleValid;
    /**
     * Local X of the last hover sample in UI units.
     */
    private float sampledX;
    /**
     * Local Y of the last hover sample in UI units.
     */
    private float sampledY;
    /**
     * Scene revision of the last hover sample.
     */
    private long sampledSceneRevision;
    /**
     * Camera revision of the last hover sample.
     */
    private long sampledCameraRevision;
    /**
     * Viewport placement revision of the last hover sample.
     */
    private long sampledViewportRevision;
    /**
     * Button that started the active gesture, or null.
     */
    private PointerButton gestureButton;
    /**
     * ID of the pointer that started the active gesture.
     */
    private int gesturePointer;
    /**
     * Node under the pointer at press time, or null.
     */
    private SceneNode pressedNode;
    /**
     * Gesture start X in logical UI units.
     */
    private float startX;
    /**
     * Gesture start Y in logical UI units.
     */
    private float startY;
    /**
     * Local X of the last processed drag update.
     */
    private float dragX;
    /**
     * Local Y of the last processed drag update.
     */
    private float dragY;
    /**
     * Whether motion has crossed the drag threshold.
     */
    private boolean dragging;
    /**
     * Whether the UI context granted capture for this gesture.
     */
    private boolean captured;

    /**
     * Creates an empty scene in ON_CHANGE update mode.
     */
    @WikiMethod(section = "Configuration", sectionOrder = 10, order = 10,
            summary = "Creates an empty scene in ON_CHANGE update mode.")
    @AiDoc(section = "Configuration", sectionOrder = 10, order = 10,
            summary = "Creates an empty scene in ON_CHANGE update mode.")
    public Scene3D() {
        this(new Scene());
    }

    /**
     * Creates a viewport for the supplied scene.
     *
     * @param scene scene graph, which can be shared by several viewports.
     */
    @WikiMethod(section = "Configuration", sectionOrder = 10, order = 10,
            summary = "Creates a viewport for the supplied scene.")
    @AiDoc(section = "Configuration", sectionOrder = 10, order = 10,
            summary = "Creates a viewport for the supplied scene.")
    public Scene3D(
            @WikiParameter(description = "scene graph, which can be shared by several viewports.") Scene scene) {
        this.scene = Objects.requireNonNull(scene, "scene");
        super.targetOptions(SCENE_TARGET);
        updateMode(UpdateMode.ON_CHANGE);
    }

    /**
     * Reads the scene graph for node and lighting changes.
     *
     * @return scene model.
     */
    @WikiMethod(section = "Configuration", sectionOrder = 10, order = 20,
            summary = "Reads the scene graph for node and lighting changes.")
    @WikiReturn("scene model")
    @AiDoc(section = "Configuration", sectionOrder = 10, order = 20,
            summary = "Reads the scene graph for node and lighting changes.")
    public Scene scene() {
        return scene;
    }

    /**
     * Reads the viewport camera. The widget sets the camera's viewport dimensions.
     *
     * @return camera for pose and projection configuration.
     */
    @WikiMethod(section = "Configuration", sectionOrder = 10, order = 30,
            summary = "Reads the viewport camera. The widget sets the camera's viewport dimensions.")
    @WikiReturn("camera for pose and projection configuration")
    @AiDoc(section = "Configuration", sectionOrder = 10, order = 30,
            summary = "Reads the viewport camera. The widget sets the camera's viewport dimensions.")
    public Camera3D camera() {
        return camera;
    }

    /**
     * Reads the controller used by pointer gestures.
     *
     * @return controller for the target, angles and distance.
     */
    @WikiMethod(section = "Configuration", sectionOrder = 10, order = 40,
            summary = "Reads the controller used by pointer gestures.")
    @WikiReturn("controller for the target, angles and distance")
    @AiDoc(section = "Configuration", sectionOrder = 10, order = 40,
            summary = "Reads the controller used by pointer gestures.")
    public OrbitCameraController orbit() {
        return orbit;
    }

    /**
     * Enables or disables pointer camera controls. Picking remains available in both modes.
     *
     * @param enabled true to allow drag and wheel camera gestures.
     * @return this viewport.
     */
    @WikiMethod(section = "Configuration", sectionOrder = 10, order = 50,
            summary = "Enables or disables pointer camera controls. Picking remains available in both modes.")
    @WikiReturn("this viewport")
    @AiDoc(section = "Configuration", sectionOrder = 10, order = 50,
            summary = "Enables or disables pointer camera controls. Picking remains available in both modes.")
    public Scene3D cameraControls(
            @WikiParameter(description = "true to allow drag and wheel camera gestures.") boolean enabled) {
        if (cameraControls != enabled) {
            cameraControls = enabled;
            releaseGesture();
        }
        return this;
    }

    /**
     * Reads the pointer camera controls setting.
     *
     * @return true if camera drag and wheel gestures are enabled.
     */
    @WikiMethod(section = "Configuration", sectionOrder = 10, order = 50,
            summary = "Reads the pointer camera controls setting.")
    @WikiReturn("true if camera drag and wheel gestures are enabled")
    @AiDoc(section = "Configuration", sectionOrder = 10, order = 50,
            summary = "Reads the pointer camera controls setting.")
    public boolean cameraControls() {
        return cameraControls;
    }

    /**
     * Sets the maximum perspective orbit radius for wheel zoom. Wheel input also respects the clipping range.
     *
     * @param distance finite nonnegative distance in scene units. Zero uses the far plane.
     * @return this viewport.
     * @throws IllegalArgumentException if distance is negative or nonfinite.
     */
    @WikiMethod(section = "Configuration", sectionOrder = 10, order = 60,
            summary = "Sets the maximum perspective orbit radius for wheel zoom. Wheel input also respects the clipping range.")
    @WikiReturn("this viewport")
    @WikiThrows(type = IllegalArgumentException.class, description = "if distance is negative or nonfinite.")
    @AiDoc(section = "Configuration", sectionOrder = 10, order = 60,
            summary = "Sets the maximum perspective orbit radius for wheel zoom. Wheel input also respects the clipping range.")
    public Scene3D maxZoomDistance(
            @WikiParameter(description = "finite nonnegative distance in scene units. Zero uses the far plane.") float distance) {
        if (distance < 0 || !Float.isFinite(distance)) {
            throw new IllegalArgumentException("Maximum zoom distance must be finite and nonnegative");
        }
        maxZoomDistance = distance;
        return this;
    }

    /**
     * Reads the wheel-zoom limit.
     *
     * @return maximum orbit radius in scene units, or zero to use the far plane.
     */
    @WikiMethod(section = "Configuration", sectionOrder = 10, order = 60,
            summary = "Reads the wheel-zoom limit.")
    @WikiReturn("maximum orbit radius in scene units, or zero to use the far plane")
    @AiDoc(section = "Configuration", sectionOrder = 10, order = 60,
            summary = "Reads the wheel-zoom limit.")
    public float maxZoomDistance() {
        return maxZoomDistance;
    }

    /**
     * Toggles highlighting for blocks that support unit-cell hits.
     *
     * @param enabled true to enable cell highlighting.
     * @return this viewport.
     */
    @WikiMethod(section = "Picking and highlighting", sectionOrder = 20, order = 10,
            summary = "Toggles highlighting for blocks that support unit-cell hits.")
    @WikiReturn("this viewport")
    @AiDoc(section = "Picking and highlighting", sectionOrder = 20, order = 10,
            summary = "Toggles highlighting for blocks that support unit-cell hits.")
    public Scene3D highlightBlocks(
            @WikiParameter(description = "true to enable cell highlighting.") boolean enabled) {
        if (highlightBlocks != enabled) {
            highlightBlocks = enabled;
            hoverSampleValid = false;
            refreshHover();
        }
        return this;
    }

    /**
     * Reads the cell highlighting setting.
     *
     * @return true if hovered supported cells are highlighted.
     */
    @WikiMethod(section = "Picking and highlighting", sectionOrder = 20, order = 10,
            summary = "Reads the cell highlighting setting.")
    @WikiReturn("true if hovered supported cells are highlighted")
    @AiDoc(section = "Picking and highlighting", sectionOrder = 20, order = 10,
            summary = "Reads the cell highlighting setting.")
    public boolean highlightBlocks() {
        return highlightBlocks;
    }

    /**
     * Toggles object highlights, for example on items or mobs. The default is an AABB frame when bounds
     * are available. A node can supply custom geometry through {@link SceneNode#hoverHighlight(Vec3View)}.
     *
     * @param enabled true to enable object highlighting.
     * @return this viewport.
     */
    @WikiMethod(section = "Picking and highlighting", sectionOrder = 20, order = 20,
            summary = "Toggles object highlights, for example on items or mobs. The default is an AABB frame when bounds are available. A node can supply custom geometry through `SceneNode#hoverHighlight(Vec3View)`.")
    @WikiReturn("this viewport")
    @AiDoc(section = "Picking and highlighting", sectionOrder = 20, order = 20,
            summary = "Toggles object highlights, for example on items or mobs. The default is an AABB frame when bounds are available. A node can supply custom geometry through `SceneNode#hoverHighlight(Vec3View)`.")
    public Scene3D highlightObjects(
            @WikiParameter(description = "true to enable object highlighting.") boolean enabled) {
        if (highlightObjects != enabled) {
            highlightObjects = enabled;
            hoverSampleValid = false;
            refreshHover();
        }
        return this;
    }

    /**
     * Reads the object highlighting setting.
     *
     * @return true if hovered objects are highlighted.
     */
    @WikiMethod(section = "Picking and highlighting", sectionOrder = 20, order = 20,
            summary = "Reads the object highlighting setting.")
    @WikiReturn("true if hovered objects are highlighted")
    @AiDoc(section = "Picking and highlighting", sectionOrder = 20, order = 20,
            summary = "Reads the object highlighting setting.")
    public boolean highlightObjects() {
        return highlightObjects;
    }

    /**
     * Turns the scene's lights on or off for this viewport only, like the lighting toggle of an editor's scene view.
     * With lighting off, lit meshes show their base color and Minecraft content uses its own lighting. The scene
     * itself and other viewports are unchanged.
     *
     * @param enabled false to render this viewport without scene lights
     * @return this viewport
     */
    public Scene3D sceneLighting(boolean enabled) {
        if (sceneLighting != enabled) {
            sceneLighting = enabled;
            requestRender();
        }
        return this;
    }

    /**
     * @return whether this viewport applies the scene's lights
     */
    public boolean sceneLighting() {
        return sceneLighting;
    }

    /**
     * Shows or hides the scene geometry in this viewport. With geometry hidden the 3D pass only clears the target,
     * so the painter can draw a wireframe or other overlay instead. Picking still uses the scene.
     *
     * @param enabled false to clear the target without drawing the scene
     * @return this viewport
     */
    public Scene3D sceneGeometry(boolean enabled) {
        if (sceneGeometry != enabled) {
            sceneGeometry = enabled;
            requestRender();
        }
        return this;
    }

    /**
     * @return whether this viewport draws the scene geometry
     */
    public boolean sceneGeometry() {
        return sceneGeometry;
    }

    /**
     * Draws this viewport as a wireframe. The renderer draws the edges of everything it renders, Minecraft content
     * included; other viewports of the same scene are not affected. See {@link Scene#renderWireframe}.
     *
     * @param settings edge colors and whether surfaces stay visible, or null to draw filled surfaces only
     * @return this viewport
     */
    public Scene3D wireframe(SceneWireframe settings) {
        if (!java.util.Objects.equals(wireframe, settings)) {
            wireframe = settings;
            requestRender();
        }
        return this;
    }

    /**
     * @return wireframe settings of this viewport, or null
     */
    public SceneWireframe wireframe() {
        return wireframe;
    }

    /**
     * Adds a node supplied by an XAML declaration.
     *
     * @param content declaration that supplies a built node.
     */
    @WikiMethod(section = "XAML integration", sectionOrder = 40, order = 10,
            summary = "Adds a node supplied by an XAML declaration.")
    @AiDoc(section = "XAML integration", sectionOrder = 40, order = 10,
            summary = "Adds a node supplied by an XAML declaration.")
    public void addNodeContent(
            @WikiParameter(description = "declaration that supplies a built node.") NodeContent content) {
        scene.addRoot(content.sceneNode());
        nodeContent.add(content);
        content.onAddedToScene(this);
    }

    /**
     * Reads the logical children for XAML export.
     *
     * @return the scene declarations and overlay content.
     */
    @WikiMethod(section = "XAML integration", sectionOrder = 40, order = 20,
            summary = "Reads the logical children for XAML export.")
    @WikiReturn("the scene declarations and overlay content")
    @AiDoc(section = "XAML integration", sectionOrder = 40, order = 20,
            summary = "Reads the logical children for XAML export.")
    public List<Widget> xamlChildren() {
        List<Widget> result = new ArrayList<>();
        for (NodeContent entry : nodeContent) {
            SceneNode node = entry.sceneNode();
            if (node.scene() == scene && node.parent() == null) result.add(entry);
        }
        if (content() != null) result.add(content());
        return result;
    }

    /**
     * Nonvisual XAML child that creates a scene node.
     */
    public interface NodeContent extends Widget {
        /**
         * Reads the node built during loading.
         *
         * @return node for insertion into the scene.
         */
        SceneNode sceneNode();

        /**
         * Runs after this declaration's node has been added.
         *
         * @param viewport viewport containing the node.
         */
        default void onAddedToScene(Scene3D viewport) {
        }
    }

    /**
     * Preserves depth and compositing settings when configuring the target.
     *
     * @param options options supplying a debug name, or null for the default name.
     * @return this viewport.
     */
    @Override
    public Scene3D targetOptions(RenderTargetOptions options) {
        super.targetOptions(options == null ? SCENE_TARGET
                : new RenderTargetOptions(true, false, options.debugName()));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @WikiMethod(section = "UI composition", sectionOrder = 30, order = 10,
            summary = "Draws a single UI child above the 3D pass and painter. Pass null to clear it.")
    @AiDoc(section = "UI composition", sectionOrder = 30, order = 10,
            summary = "Draws a single UI child above the 3D pass and painter. Pass null to clear it.")
    @Override
    public Scene3D content(
            @WikiParameter(description = "UI overlay widget, or null to clear it.") Widget content) {
        if (content == content()) return this;
        if (content != null) requireFreeWidget(content);
        super.content(content);
        rebuildChildren();
        recomputeSubtreeInvalidation();
        return this;
    }

    /**
     * Anchors a detached UI widget to a world-space point.
     *
     * @param widget detached child widget.
     * @param point finite world-space point.
     * @return anchor for adjusting the point and UI pivot.
     */
    @WikiMethod(section = "UI composition", sectionOrder = 30, order = 20,
            summary = "Anchors a detached UI widget to a world-space point.")
    @WikiReturn("anchor for adjusting the point and UI pivot")
    @AiDoc(section = "UI composition", sectionOrder = 30, order = 20,
            summary = "Anchors a detached UI widget to a world-space point.")
    public SceneAnchor3D addAnchor(
            @WikiParameter(description = "detached child widget.") Widget widget,
            @WikiParameter(description = "finite world-space point.") Vec3View point) {
        return addAnchor(widget, null, point);
    }

    /**
     * Anchors a detached widget using coordinates on a node or in the world.
     *
     * @param widget detached child widget
     * @param node anchoring node, or null for world space
     * @param x finite X in node-local or world units
     * @param y finite Y in node-local or world units
     * @param z finite Z in node-local or world units
     * @return anchor for adjusting the point and UI pivot
     * @throws IllegalArgumentException if a coordinate is nonfinite or the widget cannot be attached
     */
    public SceneAnchor3D addAnchor(Widget widget, SceneNode node, float x, float y, float z) {
        return addAnchor(widget, node, new MutableVec3(x, y, z));
    }

    /**
     * Anchors a detached UI widget to a point on a node or in the world.
     *
     * @param widget detached child widget.
     * @param node anchoring node, or null for a world-space point.
     * @param point finite point in node-local or scene coordinates.
     * @return anchor for adjusting the point and UI pivot.
     * @throws IllegalArgumentException if the widget is already attached or would create a cycle.
     */
    @WikiMethod(section = "UI composition", sectionOrder = 30, order = 20,
            summary = "Anchors a detached UI widget to a point on a node or in the world.")
    @WikiReturn("anchor for adjusting the point and UI pivot")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the widget is already attached or would create a cycle.")
    @AiDoc(section = "UI composition", sectionOrder = 30, order = 20,
            summary = "Anchors a detached UI widget to a point on a node or in the world.")
    public SceneAnchor3D addAnchor(
            @WikiParameter(description = "detached child widget.") Widget widget,
            @WikiParameter(description = "anchoring node, or null for a world-space point.") SceneNode node,
            @WikiParameter(description = "finite point in node-local or scene coordinates.") Vec3View point) {
        requireFreeWidget(widget);
        SceneAnchor3D anchor = new SceneAnchor3D(widget, node, point);
        anchors.add(anchor);
        rebuildChildren();
        anchor.setParentInternal(this);
        anchor.setUiContextInternal(uiContext());
        invalidate(InvalidationFlags.VISUAL);
        return anchor;
    }

    /**
     * Removes an anchor and detaches its widget for reuse.
     *
     * @param anchor anchor to remove.
     * @return true if this viewport owned the anchor.
     */
    @WikiMethod(section = "UI composition", sectionOrder = 30, order = 30,
            summary = "Removes an anchor and detaches its widget for reuse.")
    @WikiReturn("true if this viewport owned the anchor")
    @AiDoc(section = "UI composition", sectionOrder = 30, order = 30,
            summary = "Removes an anchor and detaches its widget for reuse.")
    public boolean removeAnchor(
            @WikiParameter(description = "anchor to remove.") SceneAnchor3D anchor) {
        if (!anchors.remove(anchor)) return false;
        rebuildChildren();
        anchor.hideProjection();
        anchor.setParentInternal(null);
        anchor.setUiContextInternal(null);
        anchor.releaseWidget();
        recomputeSubtreeInvalidation();
        invalidate(InvalidationFlags.VISUAL);
        return true;
    }

    /**
     * Picks within the displayed portion of the canvas.
     *
     * @param x horizontal coordinate relative to the widget in UI units.
     * @param y vertical coordinate relative to the widget in UI units.
     * @return closest visible node, or null.
     */
    @WikiMethod(section = "Picking and highlighting", sectionOrder = 20, order = 30,
            summary = "Picks within the displayed portion of the canvas.")
    @WikiReturn("closest visible node, or null")
    @AiDoc(section = "Picking and highlighting", sectionOrder = 20, order = 30,
            summary = "Picks within the displayed portion of the canvas.")
    public SceneNode pickNode(
            @WikiParameter(description = "horizontal coordinate relative to the widget in UI units.") float x,
            @WikiParameter(description = "vertical coordinate relative to the widget in UI units.") float y) {
        prepareViewport();
        return pickPrepared(x, y);
    }

    /**
     * Picks using the current viewport mapping.
     *
     * @param x horizontal coordinate relative to the widget.
     * @param y vertical coordinate relative to the widget.
     * @return closest node, or null.
     */
    private SceneNode pickPrepared(float x, float y) {
        return pickPrepared(x, y, null);
    }

    /**
     * Picks a node and copies the cell coordinates on a cell hit.
     *
     * @param x horizontal coordinate relative to the widget.
     * @param y vertical coordinate relative to the widget.
     * @param cell optional output for the lower cell corner.
     * @return closest visible node, or null.
     */
    private SceneNode pickPrepared(float x, float y, MutableVec3 cell) {
        if (!visible() || !displayed() || !viewport.toCanvas(x, y, mapped)) return null;
        return picker.pick(scene, camera, mapped.x(), mapped.y(), cell);
    }

    /**
     * Reads the current hover target.
     *
     * @return node under the pointer, or null.
     */
    @WikiMethod(section = "Picking and highlighting", sectionOrder = 20, order = 40,
            summary = "Reads the current hover target.")
    @WikiReturn("node under the pointer, or null")
    @AiDoc(section = "Picking and highlighting", sectionOrder = 20, order = 40,
            summary = "Reads the current hover target.")
    public SceneNode hoveredNode() {
        return hoveredNode;
    }

    /**
     * Subscribes to hover transitions between nodes and empty space.
     *
     * @param listener hover-change callback.
     * @return the event subscription.
     */
    @WikiMethod(section = "Picking and highlighting", sectionOrder = 20, order = 50,
            summary = "Subscribes to hover transitions between nodes and empty space.")
    @WikiReturn("the event subscription")
    @AiDoc(section = "Picking and highlighting", sectionOrder = 20, order = 50,
            summary = "Subscribes to hover transitions between nodes and empty space.")
    public EventSubscription onNodeHover(
            @WikiParameter(description = "hover-change callback.") EventListener<? super NodeHoverEvent> listener) {
        return on(NodeHoverEvent.TYPE, listener);
    }

    /**
     * Subscribes to primary-button clicks on nodes.
     *
     * @param listener click callback.
     * @return the event subscription.
     */
    @WikiMethod(section = "Picking and highlighting", sectionOrder = 20, order = 60,
            summary = "Subscribes to primary-button clicks on nodes.")
    @WikiReturn("the event subscription")
    @AiDoc(section = "Picking and highlighting", sectionOrder = 20, order = 60,
            summary = "Subscribes to primary-button clicks on nodes.")
    public EventSubscription onNodeClick(
            @WikiParameter(description = "click callback.") EventListener<? super NodeClickEvent> listener) {
        return on(NodeClickEvent.TYPE, listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Widget> children() {
        return childrenView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUiContextInternal(UIContext context) {
        boolean changed = uiContext() != context;
        if (changed) releaseGesture();
        super.setUiContextInternal(context);
        for (int i = 0; i < anchors.size(); i++) anchors.get(i).setUiContextInternal(context);
        if (changed) clearHover();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scene3D enabled(boolean enabled) {
        super.enabled(enabled);
        if (!enabled) {
            resetPointer();
            for (int i = 0; i < anchors.size(); i++) anchors.get(i).clearInteraction();
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scene3D visibility(Visibility visibility) {
        super.visibility(visibility);
        if (!visible()) {
            resetPointer();
            projectAnchors();
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scene3D displayed(boolean displayed) {
        super.displayed(displayed);
        if (!displayed) {
            resetPointer();
            projectAnchors();
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        prepareViewport();
        projectAnchors();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void tick(FrameContext frame) {
        if (!visible()) return;
        super.tick(frame);
        if (frame != null) partialTick = frame.partialTick();
        if (captured && uiContext() != null && uiContext().capturedPointer(gesturePointer) != this) {
            releaseGesture();
        }
        prepareViewport();
        synchronizeTexture();
        projectAnchors();
        refreshHover();
        for (int i = 0; i < anchors.size(); i++) {
            SceneAnchor3D anchor = anchors.get(i);
            if (anchor.visible()) anchor.tick(frame);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(RenderContext context) {
        if (!visible() || context == null || !(layoutBounds().width() > 0 && layoutBounds().height() > 0)) return;
        pixelScale = context.pixelScale();
        prepareViewport();
        synchronizeTexture();
        refreshHover();
        super.render(context);
        projectAnchors();
        renderAnchors(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasRawPass() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderRawPass(RenderBackend backend, RenderTarget target, float logicalWidth,
                                 float logicalHeight, float coordinateScale) {
        camera.setViewport(logicalWidth, logicalHeight);
        long sceneRevision = scene.revision();
        long cameraRevision = camera.revision();
        float width = Math.min(target.width(), logicalWidth * coordinateScale);
        float height = Math.min(target.height(), logicalHeight * coordinateScale);
        if (!sceneGeometry) {
            backend.scene3D().render(target, EMPTY_SCENE, camera, width, height, clearColor(), partialTick, null);
        } else {
            Runnable pass = () -> backend.scene3D().render(target, scene, camera, width, height, clearColor(),
                    partialTick, highlight == null ? null : highlight.renderable());
            if (!sceneLighting) {
                Runnable lit = pass;
                pass = () -> scene.renderUnlit(lit);
            }
            if (wireframe != null) scene.renderWireframe(wireframe, pass);
            else pass.run();
        }
        renderedSceneRevision = sceneRevision;
        renderedCameraRevision = cameraRevision;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(Event event) {
        if (!visible() || !enabled() || !displayed()) return;
        super.handle(event);
        if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.TARGET
                && pointer.target() == this) {
            if (pointer instanceof PointerReleasedEvent released && matchesGesture(released)) {
                finishGesture(released);
            } else if (!event.isCancelled() || pointer instanceof PointerExitedEvent) {
                handlePointer(pointer);
            }
        } else if (event instanceof ScrollEvent scroll && scroll.phase() == EventPhase.TARGET
                && scroll.target() == this && !event.isCancelled()) {
            zoom(scroll);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MouseCursor mouseCursorAt(float localX, float localY) {
        if (!enabled() || !displayed()) return super.mouseCursorAt(localX, localY);
        if (dragging && cameraControls) return MouseCursor.CROSSHAIR;
        SceneNode hit = pointerOnScene && localX == pointerX && localY == pointerY
                ? hoveredNode : pickNode(localX, localY);
        return hit == null ? super.mouseCursorAt(localX, localY) : MouseCursor.POINTER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        releaseGesture();
        pointerOnScene = false;
        hoveredNode = null;
        while (!anchors.isEmpty()) {
            SceneAnchor3D anchor = anchors.get(anchors.size() - 1);
            removeAnchor(anchor);
            anchor.widget().dispose();
            anchor.dispose();
        }
        super.dispose();
        rebuildChildren();
    }

    /**
     * Updates camera dimensions and the mapping between canvas and widget.
     */
    private void prepareViewport() {
        TargetSize size = targetSize(pixelScale);
        camera.setViewport(size.logicalWidth(), size.logicalHeight());
        viewport.update(layoutBounds(), fit(), size);
    }

    /**
     * Requests a texture redraw in ON_CHANGE mode when the scene or camera changes.
     */
    private void synchronizeTexture() {
        if (updateMode() == UpdateMode.ON_CHANGE && (renderedCameraRevision != camera.revision()
                || renderedSceneRevision != scene.revision())) {
            requestRender();
        }
    }

    /**
     * Projects anchors using node visibility, clipping and texture placement.
     */
    private void projectAnchors() {
        for (int i = 0; i < anchors.size(); i++) {
            SceneAnchor3D anchor = anchors.get(i);
            if (displayed() && visible() && worldAnchorPoint(anchor) && camera.project(anchorPoint, anchorPoint)
                    && anchorPoint.z() >= -DEPTH_ROUNDOFF && anchorPoint.z() <= 1
                    && viewport.fromCanvas(anchorPoint.x(), anchorPoint.y(), mapped)) {
                anchor.projectAt(mapped.x(), mapped.y());
            } else {
                anchor.hideProjection();
            }
        }
    }

    /**
     * Maps an anchor to world space after checking its node and ancestors.
     *
     * @param anchor scene anchor.
     * @return true if the point is defined and the node is visible.
     */
    private boolean worldAnchorPoint(SceneAnchor3D anchor) {
        anchor.position(anchorPoint);
        SceneNode node = anchor.node();
        if (node == null) return true;
        if (node.scene() != scene) return false;
        for (SceneNode current = node; current != null; current = current.parent()) {
            if (!current.visible()) return false;
        }
        return node.worldMatrix(matrix).transformPoint(anchorPoint, anchorPoint);
    }

    /**
     * Draws UI anchors above the texture with the widget transform and clipping.
     *
     * @param context screen render context.
     */
    private void renderAnchors(RenderContext context) {
        if (anchors.isEmpty() || !displayed()) return;
        RectView bounds = layoutBounds();
        pushOpacity(context);
        context.pushTransform(bounds, transform());
        try {
            context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
            try {
                List<Widget> order = ZOrder.paintOrder(childrenView);
                for (int i = 0; i < order.size(); i++) {
                    Widget child = order.get(i);
                    if (child != content() && child.visible()) child.renderCached(context);
                }
            } finally {
                context.popClip();
            }
        } finally {
            context.popTransform();
            popOpacity(context);
        }
    }

    /**
     * Synchronizes the widget tree with the texture content and anchor order.
     */
    private void rebuildChildren() {
        children.clear();
        if (content() != null) children.add(content());
        children.addAll(anchors);
    }

    /**
     * Checks that the widget can attach without reparenting or creating a cycle.
     *
     * @param widget widget to attach.
     * @throws NullPointerException if widget is null.
     * @throws IllegalArgumentException if widget already has a parent or is an ancestor of this viewport.
     */
    private void requireFreeWidget(Widget widget) {
        Objects.requireNonNull(widget, "widget");
        if (widget.parent() != null) throw new IllegalArgumentException("Widget already has a parent");
        for (Widget ancestor = this; ancestor != null; ancestor = ancestor.parent()) {
            if (widget == ancestor) throw new IllegalArgumentException("Scene cannot contain an ancestor");
        }
    }

    /**
     * Handles input for the viewport itself. Child widgets handle their own events.
     *
     * @param pointer pointer event in viewport-local coordinates.
     */
    private void handlePointer(PointerEvent pointer) {
        if (pointer instanceof PointerExitedEvent) {
            if (!captured) releaseGesture();
            clearHover();
        } else if (pointer instanceof PointerPressedEvent pressed) {
            beginGesture(pressed);
        } else if (pointer instanceof PointerMovedEvent || pointer instanceof PointerEnteredEvent) {
            pointerOnScene = true;
            pointerX = pointer.localX();
            pointerY = pointer.localY();
            if (pointer instanceof PointerMovedEvent && gestureButton != null && gesturePointer == pointer.pointerId()) {
                moveGesture(pointerX, pointerY);
                if (dragging && cameraControls) pointer.cancel();
            }
            refreshHover();
        }
    }

    /**
     * Starts capture when the press controls the camera or hits a node.
     *
     * @param pointer press event in widget-local coordinates.
     */
    private void beginGesture(PointerPressedEvent pointer) {
        if (gestureButton != null) return;
        PointerButton button = pointer.button();
        boolean primary = button == PointerButton.PRIMARY;
        if (!primary && (!cameraControls || (button != PointerButton.SECONDARY && button != PointerButton.MIDDLE)))
            return;
        prepareViewport();
        if (!viewport.toCanvas(pointer.localX(), pointer.localY(), mapped)) return;
        pressedNode = primary ? picker.pick(scene, camera, mapped.x(), mapped.y()) : null;
        if (!cameraControls && pressedNode == null) return;
        gestureButton = button;
        gesturePointer = pointer.pointerId();
        startX = dragX = pointer.localX();
        startY = dragY = pointer.localY();
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(gesturePointer, this);
            captured = context.capturedPointer(gesturePointer) == this;
        }
        pointer.cancel();
    }

    /**
     * Checks whether a release matches the active gesture.
     *
     * @param pointer release event.
     * @return true for the same pointer and button.
     */
    private boolean matchesGesture(PointerReleasedEvent pointer) {
        return gestureButton == pointer.button() && gesturePointer == pointer.pointerId();
    }

    /**
     * Ends the gesture and emits a click if the release counts as a click on the same node.
     *
     * @param pointer release event.
     */
    private void finishGesture(PointerReleasedEvent pointer) {
        SceneNode pressed = pressedNode;
        boolean click = pressed != null && !dragging && !pointer.isCancelled()
                && withinClickThreshold(pointer.localX(), pointer.localY());
        releaseGesture();
        if (click && pickNode(pointer.localX(), pointer.localY()) == pressed) {
            emit(new NodeClickEvent(pressed, pointer));
        }
        pointer.cancel();
        refreshHover();
    }

    /**
     * Releases capture and resets the active gesture.
     */
    private void releaseGesture() {
        if (captured && uiContext() != null) uiContext().releasePointer(gesturePointer, this);
        captured = false;
        gestureButton = null;
        pressedNode = null;
        dragging = false;
    }

    /**
     * Tests the squared distance from the press against the click threshold.
     *
     * @param x local pointer X.
     * @param y local pointer Y.
     * @return true within the radius defined by DRAG_THRESHOLD.
     */
    private boolean withinClickThreshold(float x, float y) {
        float dx = x - startX, dy = y - startY;
        return dx * dx + dy * dy < DRAG_THRESHOLD * DRAG_THRESHOLD;
    }

    /**
     * Orbits or pans while the captured pointer moves. For perspective panning, the visible height
     * at orbit distance d is {@code 2*d / projection[1,1]}. Dividing by the canvas height converts
     * a logical canvas delta to world units.
     *
     * @param x local pointer X.
     * @param y local pointer Y.
     */
    private void moveGesture(float x, float y) {
        float dx = x - dragX, dy = y - dragY;
        if (!Float.isFinite(dx) || !Float.isFinite(dy) || (!dragging && withinClickThreshold(x, y))) return;
        dragging = true;
        if (!cameraControls) return;
        dragX = x;
        dragY = y;
        if (gestureButton == PointerButton.PRIMARY) {
            orbit.rotate(-dx * ROTATION_PER_PIXEL, dy * ROTATION_PER_PIXEL);
        } else {
            double height = camera.orthographic() ? camera.orthographicHeight()
                    : 2.0 * orbit.distance() / camera.projection(matrix).get(1, 1);
            double scale = height / viewport.canvasHeight();
            float right = (float) (-viewport.canvasDeltaX(dx) * scale);
            float up = (float) (viewport.canvasDeltaY(dy) * scale);
            if (Float.isFinite(right) && Float.isFinite(up)) orbit.pan(right, up);
        }
        synchronizeTexture();
        projectAnchors();
    }

    /**
     * Applies {@code factor = WHEEL_SCALE^delta}, where delta is the vertical wheel delta.
     * Clipping planes and {@link #maxZoomDistance()} constrain the perspective orbit radius.
     *
     * @param scroll wheel event in viewport-local coordinates.
     */
    private void zoom(ScrollEvent scroll) {
        float delta = scroll.deltaY();
        if (!cameraControls || delta == 0 || !Float.isFinite(delta)) return;
        prepareViewport();
        if (!viewport.toCanvas(scroll.localX(), scroll.localY(), mapped)) return;
        double factor = Math.pow(WHEEL_SCALE, Math.max(-10, Math.min(10, delta)));
        if (camera.orthographic()) {
            float zoom = (float) factor;
            float height = camera.orthographicHeight() * zoom;
            if (!(height > 0) || !Float.isFinite(height)) return;
            orbit.zoom(zoom);
        } else {
            float limit = maxZoomDistance > 0 ? Math.min(camera.far(), maxZoomDistance) : camera.far();
            float distance = (float) Math.max(camera.near(), Math.min(limit, orbit.distance() * factor));
            orbit.setDistance(distance);
        }
        scroll.cancel();
        synchronizeTexture();
        projectAnchors();
        refreshHover();
    }

    /**
     * Refreshes hover from the last pointer position, including after a camera change.
     */
    private void refreshHover() {
        if (!pointerOnScene || dragging || !enabled()) {
            hoverSampleValid = false;
            updateHighlight(null);
            setHoveredNode(null);
            return;
        }
        prepareViewport();
        long sceneRevision = scene.revision();
        long cameraRevision = camera.revision();
        long viewportRevision = viewport.revision();
        if (hoverSampleValid && sampledX == pointerX && sampledY == pointerY
                && sampledSceneRevision == sceneRevision && sampledCameraRevision == cameraRevision
                && sampledViewportRevision == viewportRevision) {
            return;
        }
        sampledX = pointerX;
        sampledY = pointerY;
        sampledSceneRevision = sceneRevision;
        sampledCameraRevision = cameraRevision;
        sampledViewportRevision = viewportRevision;
        hoverSampleValid = true;
        boolean highlights = highlightBlocks || highlightObjects;
        SceneNode hit = pickPrepared(pointerX, pointerY, highlights ? hoverCell : null);
        updateHighlight(highlights ? hit : null);
        setHoveredNode(hit);
    }

    /**
     * Clears the pointer position and hovered node.
     */
    private void clearHover() {
        pointerOnScene = false;
        hoverSampleValid = false;
        updateHighlight(null);
        setHoveredNode(null);
    }

    /**
     * Resets all input state.
     */
    private void resetPointer() {
        releaseGesture();
        clearHover();
    }

    /**
     * Emits a hover transition if the target changed.
     *
     * @param node new hovered node, or null.
     */
    private void setHoveredNode(SceneNode node) {
        if (hoveredNode != node) {
            SceneNode previous = hoveredNode;
            hoveredNode = node;
            emit(new NodeHoverEvent(previous, node));
        }
    }

    /**
     * Updates the highlighted object or cell.
     *
     * @param node hovered node, or null to clear the highlight.
     */
    private void updateHighlight(SceneNode node) {
        boolean cellHit = node != null && Float.isFinite(hoverCell.x());
        boolean showCell = cellHit && highlightBlocks;
        boolean showObject = node != null && !cellHit && highlightObjects;
        SceneNode.HoverHighlight custom = showCell || showObject
                ? node.hoverHighlight(showCell ? hoverCell : null) : null;
        Aabb bounds = showObject && custom == null ? node.bounds() : null;
        if (!showCell && bounds == null && custom == null) {
            if (highlight == null || highlight.node == null) return;
            highlight.node = null;
            highlight.bounds = null;
            highlight.setCustom(null);
        } else {
            if (highlight == null) highlight = new HoverHighlight();
            boolean customChanged = highlight.setCustom(custom);
            float x = showCell ? hoverCell.x() : 0;
            float y = showCell ? hoverCell.y() : 0;
            float z = showCell ? hoverCell.z() : 0;
            if (!customChanged && highlight.node == node && Objects.equals(highlight.bounds, bounds)
                    && highlight.origin.x() == x && highlight.origin.y() == y && highlight.origin.z() == z) return;
            highlight.node = node;
            highlight.bounds = bounds;
            highlight.origin.set(x, y, z);
        }
        if (updateMode() == UpdateMode.ON_CHANGE) requestRender();
    }

    /**
     * Places default or custom hover geometry in world space.
     */
    private static final class HoverHighlight {
        /**
         * Cell highlight offset in scene units to avoid depth conflicts.
         */
        private static final float OFFSET = 0.003f;
        /**
         * Translucent shell around a unit cell.
         */
        private static final Mesh3D BLOCK_MESH = new Mesh3D(new float[]{
                -OFFSET, -OFFSET, -OFFSET, 1 + OFFSET, -OFFSET, -OFFSET,
                1 + OFFSET, 1 + OFFSET, -OFFSET, -OFFSET, 1 + OFFSET, -OFFSET,
                -OFFSET, -OFFSET, 1 + OFFSET, 1 + OFFSET, -OFFSET, 1 + OFFSET,
                1 + OFFSET, 1 + OFFSET, 1 + OFFSET, -OFFSET, 1 + OFFSET, 1 + OFFSET
        }, null, null, null, new int[]{
                0, 3, 2, 0, 2, 1, 4, 5, 6, 4, 6, 7,
                0, 4, 7, 0, 7, 3, 1, 2, 6, 1, 6, 5,
                0, 1, 5, 0, 5, 4, 3, 7, 6, 3, 6, 2
        });
        /**
         * Warm translucent tint for a highlighted cell.
         */
        private static final Material BLOCK_MATERIAL = new Material(0x73FFE18C, null, Material.Shading.UNLIT,
                Material.AlphaMode.TRANSLUCENT, 0, Material.Culling.BACK);
        /**
         * Padding on each side of an object highlight in node-local units.
         */
        private static final float FRAME_PADDING = 0.04f;
        /**
         * Frame strip width as a fraction of the corresponding frame side.
         */
        private static final float FRAME_EDGE = 0.04f;
        /**
         * Unit cube faces with open centers.
         */
        private static final Mesh3D FRAME_MESH = frameMesh();
        /**
         * Light blue object frame depth-tested against the scene.
         */
        private static final Material FRAME_MATERIAL = new Material(0xE095DEFF, null, Material.Shading.UNLIT,
                Material.AlphaMode.TRANSLUCENT, 0, Material.Culling.BACK);
        /**
         * Lower cell corner or node origin in local units.
         */
        private final MutableVec3 origin = new MutableVec3();
        /**
         * Identity rotation.
         */
        private final MutableQuat rotation = new MutableQuat();
        /**
         * Frame scale along the local axes. Unit scale for a cell.
         */
        private final MutableVec3 scale = new MutableVec3(1, 1, 1);
        /**
         * Frame's first corner in node-local units.
         */
        private final MutableVec3 frameOrigin = new MutableVec3();
        /**
         * Transform from highlight space to node space.
         */
        private final MutableMat4 local = new MutableMat4();
        /**
         * Transform from highlight space to world space.
         */
        private final MutableMat4 world = new MutableMat4();
        /**
         * Reusable cell highlight data.
         */
        private final Scene3DRenderer.Highlight block = new Scene3DRenderer.Highlight(BLOCK_MESH, BLOCK_MATERIAL, world);
        /**
         * Reusable object frame data.
         */
        private final Scene3DRenderer.Highlight frame = new Scene3DRenderer.Highlight(FRAME_MESH, FRAME_MATERIAL, world);
        /**
         * Highlighted node, or null.
         */
        private SceneNode node;
        /**
         * Local object bounds for the default frame, or null.
         */
        private Aabb bounds;
        /**
         * Custom node geometry and material, or null for the default highlight.
         */
        private SceneNode.HoverHighlight custom;
        /**
         * Cached custom draw data for the current mesh and material.
         */
        private Scene3DRenderer.Highlight customRenderable;

        /**
         * Updates the custom draw data when its geometry or material changes.
         *
         * @param next custom node highlight, or null for the default.
         * @return true if the draw data changed.
         */
        private boolean setCustom(SceneNode.HoverHighlight next) {
            if (Objects.equals(custom, next)) return false;
            custom = next;
            customRenderable = next == null ? null : new Scene3DRenderer.Highlight(next.mesh(), next.material(), world);
            return true;
        }

        /**
         * Combines the node's world transform with the highlight's local transform.
         *
         * @return the viewport highlight, or null.
         */
        private Scene3DRenderer.Highlight renderable() {
            if (node == null) return null;
            if (bounds == null) {
                local.setTransform(origin, rotation, scale.set(1, 1, 1));
            } else {
                float width = bounds.maxX() - bounds.minX() + 2 * FRAME_PADDING;
                float height = bounds.maxY() - bounds.minY() + 2 * FRAME_PADDING;
                float depth = bounds.maxZ() - bounds.minZ() + 2 * FRAME_PADDING;
                if (!Float.isFinite(width) || !Float.isFinite(height) || !Float.isFinite(depth)) return null;
                frameOrigin.set(bounds.minX() - FRAME_PADDING, bounds.minY() - FRAME_PADDING,
                        bounds.minZ() - FRAME_PADDING);
                local.setTransform(frameOrigin, rotation, scale.set(width, height, depth));
            }
            world.setProduct(node.worldMatrix(world), local);
            if (customRenderable != null) return customRenderable;
            return bounds == null ? block : frame;
        }

        /**
         * Builds four strips on each face of a unit cube. Each inner corner is
         * {@code outer + FRAME_EDGE * (opposite - outer)}, where opposite is the diagonally opposed corner on that face.
         *
         * @return immutable frame mesh with outward-facing triangles.
         */
        private static Mesh3D frameMesh() {
            float[] corners = {0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1};
            int[][] faces = {{0, 3, 2, 1}, {4, 5, 6, 7}, {0, 4, 7, 3}, {1, 2, 6, 5}, {0, 1, 5, 4}, {3, 7, 6, 2}};
            int outerVertices = corners.length / 3;
            float[] positions = java.util.Arrays.copyOf(corners, corners.length + faces.length * 4 * 3);
            int[] indices = new int[faces.length * 4 * 6];
            int index = 0;
            for (int face = 0; face < faces.length; face++) {
                int[] outer = faces[face];
                int inner = outerVertices + face * 4;
                for (int corner = 0; corner < outer.length; corner++) {
                    int next = (corner + 1) % outer.length;
                    int opposite = outer[(corner + 2) % outer.length];
                    for (int axis = 0; axis < 3; axis++) {
                        float from = corners[outer[corner] * 3 + axis];
                        float to = corners[opposite * 3 + axis];
                        positions[(inner + corner) * 3 + axis] = from + FRAME_EDGE * (to - from);
                    }
                    indices[index++] = outer[corner];
                    indices[index++] = outer[next];
                    indices[index++] = inner + next;
                    indices[index++] = outer[corner];
                    indices[index++] = inner + next;
                    indices[index++] = inner + corner;
                }
            }
            return new Mesh3D(positions, null, null, null, indices);
        }
    }

    /**
     * Change of the node under the pointer.
     */
    public static final class NodeHoverEvent extends BaseEvent {
        /**
         * Local hover subscription type.
         */
        public static final EventType<NodeHoverEvent> TYPE = EventType.create("scene3d.node.hover");
        /**
         * Previously hovered node, or null.
         */
        private final SceneNode previous;
        /**
         * Currently hovered node, or null.
         */
        private final SceneNode node;

        /**
         * Creates a hover transition.
         *
         * @param previous previously hovered node, or null.
         * @param node currently hovered node, or null.
         */
        private NodeHoverEvent(SceneNode previous, SceneNode node) {
            this.previous = previous;
            this.node = node;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public EventType<NodeHoverEvent> type() {
            return TYPE;
        }

        /**
         * Reads the previous hover target.
         *
         * @return node the pointer left, or null.
         */
        public SceneNode previous() {
            return previous;
        }

        /**
         * Reads the current hover target.
         *
         * @return node the pointer entered, or null.
         */
        public SceneNode node() {
            return node;
        }
    }

    /**
     * Primary-button click on a node without a drag.
     */
    public static final class NodeClickEvent extends BaseEvent {
        /**
         * Local node-click subscription type.
         */
        public static final EventType<NodeClickEvent> TYPE = EventType.create("scene3d.node.click");
        /**
         * Clicked node.
         */
        private final SceneNode node;
        /**
         * Pointer release with root and widget-local coordinates.
         */
        private final PointerReleasedEvent pointer;

        /**
         * Creates a node click event.
         *
         * @param node clicked node.
         * @param pointer pointer release event.
         */
        private NodeClickEvent(SceneNode node, PointerReleasedEvent pointer) {
            this.node = node;
            this.pointer = pointer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public EventType<NodeClickEvent> type() {
            return TYPE;
        }

        /**
         * Reads the clicked node.
         *
         * @return clicked node.
         */
        public SceneNode node() {
            return node;
        }

        /**
         * Reads the underlying pointer event.
         *
         * @return primary-button release.
         */
        public PointerReleasedEvent pointer() {
            return pointer;
        }
    }
}
