package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.input.InputHandle;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.scene.DirectionalShadows;
import dev.sixik.unigui.api.scene.FogVolume;
import dev.sixik.unigui.api.scene.LightNode;
import dev.sixik.unigui.api.scene.PointLight;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.SpotLight;
import dev.sixik.unigui.api.scene.SpotShadows;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.demo.ui.scene.SceneTransformGizmo;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.api.widget.CheckboxState;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.ColorPicker;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.NumberField;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.widgets.navigation.TreeView;
import dev.sixik.unigui.widgets.navigation.TreeViewNode;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises multi-selection, snapping, draw modes, the lighting toggle and light objects in the scene editor. */
class UnitySceneEditorFeaturesTest {
    /** Tolerance for float rounding across projection and transforms. */
    private static final float EPSILON = 1e-3f;

    /** Ctrl toggles single rows, Shift selects a visible range, and clicking the active row with Ctrl drops it. */
    @Test
    void hierarchy_ctrlAndShiftClicksBuildTheSelection() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.props();
        UnitySceneObject cube = props.child(0).userData(UnitySceneObject.class);
        UnitySceneObject sphere = props.child(1).userData(UnitySceneObject.class);
        UnitySceneObject capsule = props.child(2).userData(UnitySceneObject.class);

        fixture.clickRow(props.child(0));
        fixture.withKey(KeyCodes.LEFT_CONTROL, () -> fixture.clickRow(props.child(2)));
        assertEquals(List.of(cube, capsule), fixture.hierarchy().selectedObjects());
        assertSame(capsule, fixture.hierarchy().selectedObject());

        fixture.clickRow(props.child(0));
        fixture.withKey(KeyCodes.LEFT_SHIFT, () -> fixture.clickRow(props.child(2)));
        assertEquals(List.of(cube, sphere, capsule), fixture.hierarchy().selectedObjects());

        fixture.withKey(KeyCodes.LEFT_CONTROL, () -> fixture.clickRow(props.child(1)));
        assertEquals(List.of(cube, capsule), fixture.hierarchy().selectedObjects());
        fixture.withKey(KeyCodes.LEFT_CONTROL, () -> fixture.clickRow(props.child(2)));
        assertEquals(List.of(cube), fixture.hierarchy().selectedObjects());
        assertSame(cube, fixture.hierarchy().selectedObject());

        fixture.clickRow(props.child(1));
        assertEquals(List.of(sphere), fixture.hierarchy().selectedObjects());
    }

    /** The Inspector shows shared values, dashes differing axes and edits only the typed axis of every object. */
    @Test
    void inspector_multiSelectionShowsDashesAndEditsEveryObject() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.props();
        UnitySceneObject cube = props.child(0).userData(UnitySceneObject.class);
        UnitySceneObject sphere = props.child(1).userData(UnitySceneObject.class);
        fixture.clickRow(props.child(0));
        fixture.withKey(KeyCodes.LEFT_CONTROL, () -> fixture.clickRow(props.child(1)));

        List<NumberField> fields = descendants(fixture.inspector, NumberField.class);
        assertEquals("", fields.get(0).text());
        assertEquals("—", fields.get(0).placeholder());
        assertEquals(0.5, fields.get(1).value(), EPSILON);
        TextInput name = descendants(fixture.inspector, TextInput.class).get(0);
        assertEquals("", name.text());

        fields.get(0).value(3);
        assertEquals(3, cube.position(new MutableVec3()).x(), EPSILON);
        assertEquals(3, sphere.position(new MutableVec3()).x(), EPSILON);
        assertEquals(0.3f, cube.position(new MutableVec3()).z(), EPSILON);
        assertEquals(0, sphere.position(new MutableVec3()).z(), EPSILON);
        assertEquals(20, cube.angles().y(), EPSILON);
        assertEquals(0, sphere.angles().y(), EPSILON);
    }

    /** Duplicate copies every selected subtree and selects the copies; Delete removes them together. */
    @Test
    void shortcuts_duplicateAndDeleteTheWholeSelection() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.props();
        UnitySceneObject parent = props.userData(UnitySceneObject.class);
        fixture.clickRow(props.child(0));
        fixture.withKey(KeyCodes.LEFT_CONTROL, () -> fixture.clickRow(props.child(1)));

        assertTrue(fixture.pressKey(KeyCodes.D, KeyModifiers.CONTROL));
        List<UnitySceneObject> copies = fixture.hierarchy().selectedObjects();
        assertEquals(2, copies.size());
        assertEquals(5, parent.children().size());
        assertEquals("Cube (1)", copies.get(0).name());
        assertEquals("Sphere (1)", copies.get(1).name());

        assertTrue(fixture.pressKey(KeyCodes.DELETE, 0));
        assertEquals(3, parent.children().size());
        for (UnitySceneObject copy : copies) assertNull(copy.scene());
    }

    /** One drag of the X handle moves every selected object by the same world offset around their center. */
    @Test
    void gizmo_movesSeveralObjectsTogether() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.props();
        UnitySceneObject cube = props.child(0).userData(UnitySceneObject.class);
        UnitySceneObject capsule = props.child(2).userData(UnitySceneObject.class);
        fixture.clickRow(props.child(0));
        fixture.withKey(KeyCodes.LEFT_CONTROL, () -> fixture.clickRow(props.child(2)));
        MutableVec3 cubeBefore = worldPivot(cube), capsuleBefore = worldPivot(capsule);
        MutableVec3 center = new MutableVec3((cubeBefore.x() + capsuleBefore.x()) / 2,
                (cubeBefore.y() + capsuleBefore.y()) / 2, (cubeBefore.z() + capsuleBefore.z()) / 2);
        fixture.viewport.orbit().setOrbit(center, 9.0f, 0.45, 0.3);

        float radius = fixture.dragAxis(center, new MutableVec3(1, 0, 0), 0.5f);

        MutableVec3 cubeAfter = worldPivot(cube), capsuleAfter = worldPivot(capsule);
        assertEquals(cubeBefore.x() + radius * 0.5f, cubeAfter.x(), EPSILON);
        assertEquals(capsuleBefore.x() + radius * 0.5f, capsuleAfter.x(), EPSILON);
        assertEquals(cubeBefore.y(), cubeAfter.y(), EPSILON);
        assertEquals(capsuleBefore.z(), capsuleAfter.z(), EPSILON);
    }

    /** With snapping on, or while Ctrl is held, a global move lands the pivot on the grid step. */
    @Test
    void gizmo_snapsTranslationToTheGrid() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.props();
        UnitySceneObject cube = props.child(0).userData(UnitySceneObject.class);
        fixture.clickRow(props.child(0));
        MutableVec3 before = worldPivot(cube);
        fixture.viewport.orbit().setOrbit(before, 7.0f, 0.45, 0.3);

        fixture.sceneView().updateOptions(options -> {
            options.snap = true;
            options.moveStep = 0.5f;
        });
        fixture.dragAxis(before, new MutableVec3(1, 0, 0), 0.83f);
        float snapped = worldPivot(cube).x();
        assertEquals(Math.round(snapped / 0.5f) * 0.5f, snapped, EPSILON);
        assertTrue(snapped != before.x());
        assertEquals(before.z(), worldPivot(cube).z(), EPSILON);

        fixture.sceneView().updateOptions(options -> options.snap = false);
        MutableVec3 start = worldPivot(cube);
        fixture.withKey(KeyCodes.LEFT_CONTROL, () -> fixture.dragAxis(start, new MutableVec3(0, 0, 1), 0.61f));
        float z = worldPivot(cube).z();
        assertEquals(Math.round(z / 0.5f) * 0.5f, z, EPSILON);

        MutableVec3 free = worldPivot(cube);
        fixture.dragAxis(free, new MutableVec3(0, 0, 1), 0.37f);
        float unsnapped = worldPivot(cube).z();
        assertTrue(Math.abs(Math.round(unsnapped / 0.5f) * 0.5f - unsnapped) > EPSILON);
    }

    /** Draw modes and the lighting toggle change only the Scene viewport, not the scene or the Game view. */
    @Test
    void toolbar_drawModeAndLightingAffectOnlyTheSceneView() {
        Fixture fixture = layout();
        var directional = fixture.viewport.scene().directionalLight();
        assertNotNull(directional);
        assertNull(fixture.viewport.wireframe());
        fixture.chooseToolbarOption("Shaded", "Wireframe");
        // The renderer draws the edges, so Minecraft content gets them too; surfaces are hidden by the settings.
        assertNotNull(fixture.viewport.wireframe());
        assertFalse(fixture.viewport.wireframe().shaded());
        fixture.chooseToolbarOption("Wireframe", "Shaded Wireframe");
        assertTrue(fixture.viewport.wireframe().shaded());
        assertTrue(fixture.viewport.sceneGeometry());
        assertEquals(UnitySceneViewOptions.DrawMode.SHADED_WIREFRAME, fixture.sceneView().options().drawMode);

        // Edges of a selected object and of its Minecraft content use the selection color.
        UnitySceneObject cube = fixture.viewport.scene().roots().stream()
                .filter(UnitySceneObject.class::isInstance).map(UnitySceneObject.class::cast).findFirst().orElseThrow();
        fixture.sceneView().selectObject(cube);
        var settings = fixture.viewport.wireframe();
        assertEquals(settings.highlightArgb(), settings.colorFor(cube));
        for (var child : cube.children()) assertEquals(settings.highlightArgb(), settings.colorFor(child));
        fixture.sceneView().selectObject(null);
        assertEquals(settings.colorArgb(), settings.colorFor(cube));

        fixture.sceneView().updateOptions(options -> options.lighting = false);
        assertFalse(fixture.viewport.sceneLighting());
        assertSame(directional, fixture.viewport.scene().directionalLight());
        Scene3D game = descendants(fixture.dock().manager().findPane("game").content(), Scene3D.class).get(0);
        assertTrue(game.sceneLighting());
        assertTrue(game.sceneGeometry());
        assertNull(game.wireframe());
    }

    /** Lights are created from the menu, edited in the Inspector and drive their render node. */
    @Test
    void lights_createEditAndRender() {
        Fixture fixture = layout();
        fixture.choose("Light > Point Light");
        UnitySceneObject point = fixture.hierarchy().selectedObject();
        assertEquals(UnityLight.Kind.POINT, point.light().kind());
        LightNode node = lightNode(point);
        assertInstanceOf(PointLight.class, node.light());

        UnityComponentSection section = descendants(fixture.inspector, UnityComponentSection.class).stream()
                .filter(candidate -> "Light".equals(candidate.title())).findFirst().orElseThrow();
        assertEquals(Visibility.VISIBLE, section.visibility());
        List<NumberField> lightFields = descendants(section, NumberField.class);
        // Intensity, Range, Spot Angle, Inner Spot Angle; the color is a picker.
        lightFields.get(0).value(2.5);
        lightFields.get(1).value(7);
        assertEquals(2.5f, node.light().intensity(), EPSILON);
        assertEquals(7, node.light().range(), EPSILON);
        ColorPicker color = descendants(section, ColorPicker.class).get(0);
        pickColor(color, "#00FF80");
        assertEquals(0x00FF80, point.light().colorRgb());
        assertEquals(0x00FF80, lightNode(point).light().colorRgb());
        // The color row stays inside the Inspector: the picker ends before the panel's right edge.
        fixture.relayout();
        RectView picker = color.layoutBounds();
        RectView panel = fixture.inspector().layoutBounds();
        assertTrue(picker.width() > 20, "picker width " + picker.width());
        assertTrue(picker.x() + picker.width() <= panel.x() + panel.width() + EPSILON,
                picker.x() + picker.width() + " > " + (panel.x() + panel.width()));

        fixture.choose("Light > Spot Light");
        UnitySceneObject spot = fixture.hierarchy().selectedObject();
        SpotLight spotLight = assertInstanceOf(SpotLight.class, lightNode(spot).light());
        assertEquals(Math.toRadians(22.5), spotLight.outerAngle(), EPSILON);
        MutableVec3 down = UnitySceneOverlay.lightDirection(spot, new MutableVec3());
        assertEquals(-1, down.y(), EPSILON);

        fixture.choose("Light > Directional Light");
        UnitySceneObject sun = fixture.hierarchy().selectedObject();
        assertNull(sun.children().stream().filter(LightNode.class::isInstance).findFirst().orElse(null));
        UnitySceneOverlay.syncSceneLighting(fixture.viewport.scene());
        MutableVec3 direction = UnitySceneOverlay.lightDirection(sun, new MutableVec3());
        assertEquals(direction.y(), fixture.viewport.scene().directionalLight().y(), EPSILON);
        assertSame(UnitySceneOverlay.SKY_AMBIENT, fixture.viewport.scene().ambientLight());
        // The sample scene's own Directional Light still lights it; hiding every sun leaves the scene dark.
        for (UnitySceneObject light : UnitySceneOverlay.lights(fixture.viewport.scene())) {
            if (light.light().kind() == UnityLight.Kind.DIRECTIONAL) light.setVisible(false);
        }
        UnitySceneOverlay.syncSceneLighting(fixture.viewport.scene());
        assertNull(fixture.viewport.scene().directionalLight());
        assertSame(UnitySceneOverlay.DARK_AMBIENT, fixture.viewport.scene().ambientLight());
    }

    /** The sample scene has a Directional Light object; deleting it from the Hierarchy darkens the scene. */
    @Test
    void lights_sampleSunIsAHierarchyObject() {
        Fixture fixture = layout();
        var scene = fixture.viewport.scene();
        UnitySceneObject sun = (UnitySceneObject) scene.roots().get(scene.roots().size() - 1);
        assertEquals("Directional Light", sun.name());
        assertEquals(UnityLight.Kind.DIRECTIONAL, sun.light().kind());
        assertNotNull(scene.directionalLight());
        assertSame(UnitySceneOverlay.SKY_AMBIENT, scene.ambientLight());

        fixture.clickRow(fixture.tree().roots().get(0).child(2));
        assertSame(sun, fixture.hierarchy().selectedObject());
        assertTrue(fixture.pressKey(KeyCodes.DELETE, 0));
        fixture.sceneView().tick(new dev.sixik.unigui.api.core.FrameContext(1, 1 / 60f, 0,
                dev.sixik.unigui.api.core.FramePhase.ANIMATION));
        assertNull(scene.directionalLight());
        assertSame(UnitySceneOverlay.DARK_AMBIENT, scene.ambientLight());
    }

    /** A click on a light icon selects the light even though it has no mesh. */
    @Test
    void lights_iconClickSelectsTheLight() {
        Fixture fixture = layout();
        fixture.choose("Light > Point Light");
        UnitySceneObject light = fixture.hierarchy().selectedObject();
        fixture.clickRow(fixture.props());
        assertSame(fixture.props().userData(UnitySceneObject.class), fixture.hierarchy().selectedObject());

        MutableVec3 projected = new MutableVec3();
        assertTrue(fixture.viewport.camera().project(worldPivot(light), projected));
        RectView bounds = fixture.viewport.layoutBounds();
        fixture.click(bounds.x() + projected.x(), bounds.y() + projected.y());
        assertSame(light, fixture.hierarchy().selectedObject());
    }

    /** Light settings survive prefabs, copies and the Play snapshot. */
    @Test
    void lights_persistThroughPrefabCopyAndSnapshot() throws IOException {
        UnitySceneObject spot = UnitySceneObject.light("Spot", UnityLight.Kind.SPOT);
        UnityLight settings = new UnityLight(UnityLight.Kind.SPOT, 0x12AB34, 3, 11, 60, 20,
                true, 2048, 0.002f, 0.75f, 16);
        spot.setLight(settings);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        UnityPrefab.capture("Spot", spot).write(new DataOutputStream(bytes));
        UnityPrefab read = UnityPrefab.read(UUID.randomUUID(),
                new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        UnitySceneObject instance = read.instantiate();
        assertEquals(settings, instance.light());
        assertEquals(11, lightNode(instance).light().range(), EPSILON);
        assertEquals(new SpotShadows(2048, 0.002f, 0.75f),
                assertInstanceOf(SpotLight.class, lightNode(instance).light()).shadows());
        assertEquals(settings, spot.copySubtree().light());

        var scene = new dev.sixik.unigui.api.scene.Scene();
        scene.addRoot(spot);
        UnitySceneSnapshot snapshot = new UnitySceneSnapshot(scene);
        spot.setLight(settings.withKind(UnityLight.Kind.DIRECTIONAL));
        assertNull(spot.children().stream().filter(LightNode.class::isInstance).findFirst().orElse(null));
        snapshot.restore();
        assertEquals(settings, spot.light());
        assertNotNull(lightNode(spot));
        assertNotNull(assertInstanceOf(SpotLight.class, lightNode(spot).light()).shadows());
    }

    /** Spot and directional lights toggle shadows from the Inspector, while point lights hide shadow controls. */
    @Test
    void lights_toggleAndConfigureShadows() {
        Fixture fixture = layout();
        var scene = fixture.viewport.scene();

        // Sample directional light starts with shadows enabled; unchecking Shadows disables DirectionalShadows.
        TreeViewNode sunRow = fixture.tree().roots().get(0).child(2);
        fixture.clickRow(sunRow);
        UnityComponentSection section = section(fixture, "Light");
        List<Checkbox> checkboxes = descendants(section.contentHost(), Checkbox.class);
        assertEquals(1, checkboxes.size());
        Checkbox shadowsToggle = checkboxes.get(0);
        assertEquals(CheckboxState.CHECKED, shadowsToggle.state());
        assertNotNull(scene.directionalShadows());

        List<NumberField> sunFields = descendants(section, NumberField.class);
        // Intensity, Range (collapsed), SpotAngle (collapsed), InnerAngle (collapsed), Strength, Bias, ShadowSize.
        assertEquals(7, sunFields.size());
        sunFields.get(4).value(0.6);
        sunFields.get(5).value(0.0025);
        sunFields.get(6).value(16);
        List<ComboBox> sunCombos = descendants(section, ComboBox.class);
        sunCombos.get(1).selectedIndex(2); // 512
        UnitySceneOverlay.syncSceneLighting(scene);
        DirectionalShadows dirShadows = scene.directionalShadows();
        assertNotNull(dirShadows);
        assertEquals(512, dirShadows.resolution());
        assertEquals(0.6f, dirShadows.strength(), EPSILON);
        assertEquals(0.0025f, dirShadows.bias(), EPSILON);
        assertEquals(16f, dirShadows.size(), EPSILON);

        shadowsToggle.state(CheckboxState.UNCHECKED);
        UnitySceneOverlay.syncSceneLighting(scene);
        assertNull(scene.directionalShadows());
        assertEquals(Visibility.COLLAPSED, sunFields.get(4).parent().visibility());

        // Point lights do not support shadow maps, so the Shadows row is hidden.
        fixture.choose("Light > Point Light");
        assertEquals(Visibility.COLLAPSED, shadowsToggle.parent().visibility());

        // Spot lights default to shadows off and attach SpotShadows when enabled.
        fixture.choose("Light > Spot Light");
        UnitySceneObject spot = fixture.hierarchy().selectedObject();
        assertNull(assertInstanceOf(SpotLight.class, lightNode(spot).light()).shadows());
        assertEquals(Visibility.VISIBLE, shadowsToggle.parent().visibility());
        assertEquals(CheckboxState.UNCHECKED, shadowsToggle.state());

        shadowsToggle.state(CheckboxState.CHECKED);
        SpotShadows spotShadows = assertInstanceOf(SpotLight.class, lightNode(spot).light()).shadows();
        assertNotNull(spotShadows);
        assertEquals(1024, spotShadows.resolution());
        assertEquals(0.0012f, spotShadows.bias(), EPSILON);
        assertEquals(0.95f, spotShadows.strength(), EPSILON);

        sunCombos.get(1).selectedIndex(4); // 2048
        sunFields.get(4).value(0.8);
        sunFields.get(5).value(0.003);
        spotShadows = assertInstanceOf(SpotLight.class, lightNode(spot).light()).shadows();
        assertEquals(new SpotShadows(2048, 0.003f, 0.8f), spotShadows);

        shadowsToggle.state(CheckboxState.UNCHECKED);
        assertNull(assertInstanceOf(SpotLight.class, lightNode(spot).light()).shadows());
    }

    /** Components live on any object, survive prefabs and Play, and the Rotator only turns while Play runs. */
    @Test
    void components_meshLightAndRotator() throws IOException {
        UnitySceneObject empty = new UnitySceneObject("Holder", UnityEditorHierarchyView.GameObjectType.EMPTY);
        assertNull(empty.meshShape());
        empty.setMeshShape(UnityEditorHierarchyView.GameObjectType.SPHERE);
        empty.setLight(UnityLight.defaults(UnityLight.Kind.SPOT));
        empty.setRotatorSpeed(45);
        assertNotNull(empty.mesh());
        assertNotNull(lightNode(empty));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        UnityPrefab.capture("Holder", empty).write(new DataOutputStream(bytes));
        UnitySceneObject loaded = UnityPrefab.read(UUID.randomUUID(),
                new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))).instantiate();
        assertEquals(UnityEditorHierarchyView.GameObjectType.SPHERE, loaded.meshShape());
        assertEquals(UnityLight.Kind.SPOT, loaded.light().kind());
        assertEquals(45, loaded.rotatorSpeed(), EPSILON);

        UnitySceneObject cube = new UnitySceneObject("Cube", UnityEditorHierarchyView.GameObjectType.CUBE);
        cube.setMeshShape(null);
        bytes.reset();
        UnityPrefab.capture("Cube", cube).write(new DataOutputStream(bytes));
        assertNull(UnityPrefab.read(UUID.randomUUID(), new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())))
                .instantiate().mesh());

        var scene = new dev.sixik.unigui.api.scene.Scene();
        scene.addRoot(empty);
        UnitySceneSnapshot snapshot = new UnitySceneSnapshot(scene);
        UnityPlayMode.tick(scene, new dev.sixik.unigui.api.core.FrameContext(1, 1, 0,
                dev.sixik.unigui.api.core.FramePhase.ANIMATION));
        assertEquals(0, empty.angles().y(), EPSILON);
        UnityPlayMode.setRunning(scene, true);
        UnityPlayMode.tick(scene, new dev.sixik.unigui.api.core.FrameContext(2, 0.1f, 0,
                dev.sixik.unigui.api.core.FramePhase.ANIMATION));
        UnityPlayMode.tick(scene, new dev.sixik.unigui.api.core.FrameContext(2, 0.1f, 0,
                dev.sixik.unigui.api.core.FramePhase.ANIMATION));
        assertEquals(4.5f, empty.angles().y(), EPSILON);
        empty.setLight(null);
        empty.setMeshShape(null);
        UnityPlayMode.setRunning(scene, false);
        snapshot.restore();
        assertEquals(0, empty.angles().y(), EPSILON);
        assertNotNull(empty.light());
        assertEquals(UnityEditorHierarchyView.GameObjectType.SPHERE, empty.meshShape());
    }

    /** The Mesh dropdown swaps the primitive of every selected Mesh Renderer. */
    @Test
    void components_inspectorChangesTheMesh() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.props();
        UnitySceneObject cube = props.child(0).userData(UnitySceneObject.class);
        UnitySceneObject sphere = props.child(1).userData(UnitySceneObject.class);
        fixture.clickRow(props.child(0));
        fixture.withKey(KeyCodes.LEFT_CONTROL, () -> fixture.clickRow(props.child(1)));
        dev.sixik.unigui.widgets.interaction.ComboBox mesh = descendants(fixture.inspector, UnityComponentSection.class)
                .stream().filter(section -> "Mesh Renderer".equals(section.title())).findFirst().orElseThrow()
                .contentHost().children().stream().flatMap(row -> descendants(row,
                        dev.sixik.unigui.widgets.interaction.ComboBox.class).stream()).findFirst().orElseThrow();
        mesh.selectedIndex(UnitySceneObject.MESH_SHAPES.indexOf(UnityEditorHierarchyView.GameObjectType.CAPSULE));
        assertEquals(UnityEditorHierarchyView.GameObjectType.CAPSULE, cube.meshShape());
        assertEquals(UnityEditorHierarchyView.GameObjectType.CAPSULE, sphere.meshShape());
    }

    /** Header checkboxes switch components off without losing their settings, and show the selection's state. */
    @Test
    void components_checkboxesSwitchComponentsOff() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.props();
        UnitySceneObject cube = props.child(0).userData(UnitySceneObject.class);
        UnitySceneObject sphere = props.child(1).userData(UnitySceneObject.class);
        fixture.clickRow(props.child(0));
        UnityComponentSection mesh = section(fixture, "Mesh Renderer");
        assertNotNull(mesh.activeCheckbox());
        assertEquals(CheckboxState.CHECKED, mesh.activeCheckbox().state());
        mesh.activeCheckbox().state(CheckboxState.UNCHECKED);
        assertFalse(cube.componentEnabled(UnitySceneObject.Component.MESH_RENDERER));
        assertNull(cube.mesh());
        assertEquals(UnityEditorHierarchyView.GameObjectType.CUBE, cube.meshShape());
        // A mixed selection shows the indeterminate state; checking it switches the mesh on for both.
        fixture.withKey(KeyCodes.LEFT_CONTROL, () -> fixture.clickRow(props.child(1)));
        assertEquals(CheckboxState.INDETERMINATE, mesh.activeCheckbox().state());
        mesh.activeCheckbox().state(CheckboxState.CHECKED);
        assertNotNull(cube.mesh());
        assertNotNull(sphere.mesh());

        // A switched-off sun no longer lights the scene.
        var scene = fixture.viewport.scene();
        UnitySceneObject sun = (UnitySceneObject) scene.roots().get(scene.roots().size() - 1);
        fixture.clickRow(fixture.tree().roots().get(0).child(2));
        assertSame(sun, fixture.hierarchy().selectedObject());
        section(fixture, "Light").activeCheckbox().state(CheckboxState.UNCHECKED);
        UnitySceneOverlay.syncSceneLighting(scene);
        assertNull(scene.directionalLight());
        assertSame(UnitySceneOverlay.DARK_AMBIENT, scene.ambientLight());
        assertEquals(0.5f, section(fixture, "Light").contentHost().opacity(), EPSILON);

        // A switched-off point light loses its render node and gets it back with the same settings.
        fixture.choose("Light > Point Light");
        UnitySceneObject point = fixture.hierarchy().selectedObject();
        UnityLight settings = point.light();
        assertEquals(CheckboxState.CHECKED, section(fixture, "Light").activeCheckbox().state());
        section(fixture, "Light").activeCheckbox().state(CheckboxState.UNCHECKED);
        assertTrue(point.children().stream().noneMatch(LightNode.class::isInstance));
        section(fixture, "Light").activeCheckbox().state(CheckboxState.CHECKED);
        assertEquals(settings, point.light());
        assertNotNull(lightNode(point));

        // A switched-off Rotator stays still in Play.
        UnitySceneObject spinner = new UnitySceneObject("Spinner", UnityEditorHierarchyView.GameObjectType.EMPTY);
        spinner.setRotatorSpeed(90);
        spinner.setComponentEnabled(UnitySceneObject.Component.ROTATOR, false);
        var playScene = new dev.sixik.unigui.api.scene.Scene();
        playScene.addRoot(spinner);
        UnityPlayMode.setRunning(playScene, true);
        UnityPlayMode.tick(playScene, new dev.sixik.unigui.api.core.FrameContext(1, 0.1f, 0,
                dev.sixik.unigui.api.core.FramePhase.ANIMATION));
        assertEquals(0, spinner.angles().y(), EPSILON);
        spinner.setComponentEnabled(UnitySceneObject.Component.ROTATOR, true);
        UnityPlayMode.tick(playScene, new dev.sixik.unigui.api.core.FrameContext(2, 0.1f, 0,
                dev.sixik.unigui.api.core.FramePhase.ANIMATION));
        assertEquals(9, spinner.angles().y(), EPSILON);
    }

    /** Add Component offers Material Effect; its shader, color, strength and loop reach the node's shader effect. */
    @Test
    void components_materialEffectFromTheInspector() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.props();
        UnitySceneObject cube = props.child(0).userData(UnitySceneObject.class);
        fixture.clickRow(props.child(0));
        assertEquals(Visibility.COLLAPSED, section(fixture, "Material Effect").visibility());
        fixture.addComponent("Material Effect");
        UnityComponentSection effect = section(fixture, "Material Effect");
        assertEquals(Visibility.VISIBLE, effect.visibility());
        assertEquals(UnityMaterialEffect.Preset.SCANLINE, cube.materialEffectSettings().preset());
        assertNotNull(cube.materialEffect());
        assertSame(UnityMaterialEffect.Preset.SCANLINE.source().shader(), cube.materialEffect().shader());

        descendants(effect, dev.sixik.unigui.widgets.interaction.ComboBox.class).get(0)
                .selectedIndex(UnityMaterialEffect.Preset.HOLOGRAM.ordinal());
        assertSame(UnityMaterialEffect.Preset.HOLOGRAM.source().shader(), cube.materialEffect().shader());
        pickColor(descendants(effect, ColorPicker.class).get(0), "#FF0000");
        List<NumberField> fields = descendants(effect, NumberField.class);
        fields.get(0).value(3);
        fields.get(1).value(4);
        assertEquals(new UnityMaterialEffect(UnityMaterialEffect.Preset.HOLOGRAM, 0xFF0000, 3, 4),
                cube.materialEffectSettings());
        assertEquals(1, cube.materialEffect().x(), EPSILON);
        assertEquals(0, cube.materialEffect().y(), EPSILON);
        assertEquals(3, cube.materialEffect().w(), EPSILON);
        assertEquals(4, cube.materialEffect().timing().durationSeconds(), EPSILON);

        // The scene clock runs while an effect exists, and each loop restarts the effect timing.
        var scene = fixture.viewport.scene();
        float before = scene.materialTime();
        for (int frame = 1; frame <= 50; frame++) {
            UnityPlayMode.tick(scene, new dev.sixik.unigui.api.core.FrameContext(1000 + frame, 0.1f, 0,
                    dev.sixik.unigui.api.core.FramePhase.ANIMATION));
        }
        assertEquals(before + 5, scene.materialTime(), 1e-2f);
        float start = cube.materialEffect().timing().startSeconds();
        assertEquals((float) Math.floor(scene.materialTime() / 4) * 4, start, EPSILON);

        effect.activeCheckbox().state(CheckboxState.UNCHECKED);
        assertNull(cube.materialEffect());
        effect.activeCheckbox().state(CheckboxState.CHECKED);
        assertNotNull(cube.materialEffect());
        assertFalse(fixture.hasComponentEntry("Material Effect"));
    }

    /** Switched-off components and the Material Effect survive prefabs, copies and the Play snapshot. */
    @Test
    void components_disabledStateAndEffectPersist() throws IOException {
        UnitySceneObject cube = new UnitySceneObject("Cube", UnityEditorHierarchyView.GameObjectType.CUBE);
        cube.setLight(UnityLight.defaults(UnityLight.Kind.POINT));
        cube.setRotatorSpeed(30);
        UnityMaterialEffect settings = new UnityMaterialEffect(UnityMaterialEffect.Preset.PULSE, 0x123456, 2, 3);
        cube.setMaterialEffectSettings(settings);
        cube.setComponentEnabled(UnitySceneObject.Component.MESH_RENDERER, false);
        cube.setComponentEnabled(UnitySceneObject.Component.LIGHT, false);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        UnityPrefab.capture("Cube", cube).write(new DataOutputStream(bytes));
        UnitySceneObject loaded = UnityPrefab.read(UUID.randomUUID(),
                new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))).instantiate();
        assertEquals(cube.disabledComponents(), loaded.disabledComponents());
        assertNull(loaded.mesh());
        assertTrue(loaded.children().stream().noneMatch(LightNode.class::isInstance));
        assertEquals(settings, loaded.materialEffectSettings());
        assertNotNull(loaded.materialEffect());

        UnitySceneObject copy = cube.copySubtree();
        assertEquals(cube.disabledComponents(), copy.disabledComponents());
        assertEquals(settings, copy.materialEffectSettings());

        var scene = new dev.sixik.unigui.api.scene.Scene();
        scene.addRoot(cube);
        UnitySceneSnapshot snapshot = new UnitySceneSnapshot(scene);
        cube.setComponentEnabled(UnitySceneObject.Component.MESH_RENDERER, true);
        cube.setMaterialEffectSettings(null);
        snapshot.restore();
        assertNull(cube.mesh());
        assertEquals(settings, cube.materialEffectSettings());
        assertNotNull(cube.materialEffect());
    }

    /**
     * The create menu adds a fog volume; the Inspector edits its shape, color, density and drift on the render node,
     * the checkbox detaches the node, its icon selects it and its drifting noise keeps the scene clock running.
     */
    @Test
    void fog_createEditAndPick() {
        Fixture fixture = layout();
        fixture.choose("Effects > Fog Volume (Box)");
        UnitySceneObject object = fixture.hierarchy().selectedObject();
        assertEquals(UnityEditorHierarchyView.GameObjectType.FOG_VOLUME, object.type());
        FogVolume node = fogNode(object);
        assertEquals(FogVolume.Shape.BOX, node.shape());
        assertNull(object.mesh());

        UnityComponentSection section = section(fixture, "Fog Volume");
        assertEquals(Visibility.VISIBLE, section.visibility());
        List<NumberField> fields = descendants(section, NumberField.class);
        // Density, Edge Softness, Noise Strength, Noise Scale, Noise Drift X/Y/Z, Light Scattering.
        assertEquals(8, fields.size());
        fields.get(0).value(1.25);
        fields.get(1).value(0.8);
        fields.get(4).value(0.5);
        fields.get(7).value(2);
        assertEquals(1.25f, fogNode(object).density(), EPSILON);
        assertEquals(0.8f, fogNode(object).edgeSoftness(), EPSILON);
        assertEquals(0.5f, fogNode(object).noise().velocityX(), EPSILON);
        assertEquals(2, fogNode(object).lightScattering(), EPSILON);
        pickColor(descendants(section, ColorPicker.class).get(0), "#336699");
        assertEquals(0x336699, fogNode(object).colorRgb());

        // A new shape replaces the node and keeps the other settings.
        descendants(section, dev.sixik.unigui.widgets.interaction.ComboBox.class).get(0).selectedIndex(1);
        assertEquals(FogVolume.Shape.SPHERE, fogNode(object).shape());
        assertEquals(1, object.children().stream().filter(FogVolume.class::isInstance).count());
        assertEquals(1.25f, fogNode(object).density(), EPSILON);

        section.activeCheckbox().state(CheckboxState.UNCHECKED);
        assertTrue(object.children().stream().noneMatch(FogVolume.class::isInstance));
        section.activeCheckbox().state(CheckboxState.CHECKED);
        assertEquals(0x336699, fogNode(object).colorRgb());

        var scene = fixture.viewport.scene();
        float before = scene.materialTime();
        UnityPlayMode.tick(scene, new dev.sixik.unigui.api.core.FrameContext(5000, 0.1f, 0,
                dev.sixik.unigui.api.core.FramePhase.ANIMATION));
        assertEquals(before + 0.1f, scene.materialTime(), EPSILON);

        fixture.clickRow(fixture.props());
        assertNotSame(object, fixture.hierarchy().selectedObject());
        MutableVec3 projected = new MutableVec3();
        assertTrue(fixture.viewport.camera().project(worldPivot(object), projected));
        RectView bounds = fixture.viewport.layoutBounds();
        fixture.click(bounds.x() + projected.x(), bounds.y() + projected.y());
        assertSame(object, fixture.hierarchy().selectedObject());
    }

    /** Add Component puts a Fog Volume on any object; it survives prefabs, copies and the Play snapshot. */
    @Test
    void fog_addComponentAndPersist() throws IOException {
        Fixture fixture = layout();
        TreeViewNode props = fixture.props();
        UnitySceneObject cube = props.child(0).userData(UnitySceneObject.class);
        fixture.clickRow(props.child(0));
        assertEquals(Visibility.COLLAPSED, section(fixture, "Fog Volume").visibility());
        fixture.addComponent("Fog Volume");
        assertEquals(Visibility.VISIBLE, section(fixture, "Fog Volume").visibility());
        assertNotNull(fogNode(cube));
        assertFalse(fixture.hasComponentEntry("Fog Volume"));

        UnitySceneObject fog = UnitySceneObject.fog("Haze", FogVolume.Shape.SPHERE);
        UnityFog settings = new UnityFog(FogVolume.Shape.SPHERE, 0x204060, 0.7f, 0.2f, 0.4f, 3, 0.1f, -0.2f, 0.3f, 1.5f);
        fog.setFog(settings);
        fog.setComponentEnabled(UnitySceneObject.Component.FOG_VOLUME, false);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        UnityPrefab.capture("Haze", fog).write(new DataOutputStream(bytes));
        UnitySceneObject loaded = UnityPrefab.read(UUID.randomUUID(),
                new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))).instantiate();
        assertEquals(settings, loaded.fog());
        assertFalse(loaded.componentEnabled(UnitySceneObject.Component.FOG_VOLUME));
        assertTrue(loaded.children().stream().noneMatch(FogVolume.class::isInstance));
        loaded.setComponentEnabled(UnitySceneObject.Component.FOG_VOLUME, true);
        assertEquals(-0.2f, fogNode(loaded).noise().velocityY(), EPSILON);
        assertEquals(settings, fog.copySubtree().fog());

        var scene = new dev.sixik.unigui.api.scene.Scene();
        scene.addRoot(fog);
        fog.setComponentEnabled(UnitySceneObject.Component.FOG_VOLUME, true);
        UnitySceneSnapshot snapshot = new UnitySceneSnapshot(scene);
        fog.setFog(null);
        assertTrue(fog.children().stream().noneMatch(FogVolume.class::isInstance));
        snapshot.restore();
        assertEquals(settings, fog.fog());
        assertEquals(0x204060, fogNode(fog).colorRgb());
    }

    /**
     * @param object editor object with an enabled Fog Volume
     * @return its fog render node
     */
    private static FogVolume fogNode(UnitySceneObject object) {
        return object.children().stream().filter(FogVolume.class::isInstance).map(FogVolume.class::cast)
                .findFirst().orElseThrow();
    }

    /**
     * Types a color into the picker's HEX field, the way a user edits it in the popup.
     *
     * @param picker color picker
     * @param hex    color as #RRGGBB
     */
    private static void pickColor(ColorPicker picker, String hex) {
        try {
            java.lang.reflect.Field field = ColorPicker.class.getDeclaredField("hexField");
            field.setAccessible(true);
            ((dev.sixik.unigui.widgets.interaction.TextField) field.get(picker)).text(hex);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * @param fixture editor
     * @param title   component title
     * @return the Inspector section of that component
     */
    private static UnityComponentSection section(Fixture fixture, String title) {
        return descendants(fixture.inspector(), UnityComponentSection.class).stream()
                .filter(candidate -> title.equals(candidate.title())).findFirst().orElseThrow();
    }

    /** The gizmo rotates and scales several nodes around the selection center and snaps the angle. */
    @Test
    void gizmo_pivotModesAndSnapSteps() {
        Fixture fixture = layout();
        fixture.sceneView().updateOptions(options -> options.pivot = SceneTransformGizmo.Pivot.PIVOT);
        TreeViewNode props = fixture.props();
        UnitySceneObject cube = props.child(0).userData(UnitySceneObject.class);
        UnitySceneObject sphere = props.child(1).userData(UnitySceneObject.class);
        fixture.clickRow(props.child(1));
        fixture.withKey(KeyCodes.LEFT_CONTROL, () -> fixture.clickRow(props.child(0)));
        // In Pivot mode the handles stand on the active object, the cube.
        MutableVec3 cubePivot = worldPivot(cube);
        MutableVec3 spherePivot = worldPivot(sphere);
        fixture.viewport.orbit().setOrbit(cubePivot, 8.0f, 0.45, 0.3);
        float radius = fixture.dragAxis(cubePivot, new MutableVec3(0, 1, 0), 0.5f);
        assertEquals(cubePivot.y() + radius * 0.5f, worldPivot(cube).y(), EPSILON);
        assertEquals(spherePivot.y() + radius * 0.5f, worldPivot(sphere).y(), EPSILON);
    }

    /** Planar Move quads translate within the plane while keeping the normal axis fixed, and center Scale scales uniformly. */
    @Test
    void gizmo_planarMoveAndCenterUniformScale() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.props();
        UnitySceneObject cube = props.child(0).userData(UnitySceneObject.class);
        fixture.clickRow(props.child(0));
        MutableVec3 pivot = worldPivot(cube);
        fixture.viewport.orbit().setOrbit(pivot, 8.0f, 0.55, 0.45);
        fixture.relayout();

        MutableVec3 centerScreen = new MutableVec3(), sideScreen = new MutableVec3(), planeScreen = new MutableVec3();
        assertTrue(fixture.viewport.camera().project(pivot, centerScreen));
        assertTrue(fixture.viewport.camera().unproject(centerScreen.x() + 62, centerScreen.y(), centerScreen.z(), sideScreen));
        float worldRadius = (float) Math.hypot(Math.hypot(sideScreen.x() - pivot.x(), sideScreen.y() - pivot.y()),
                sideScreen.z() - pivot.z());
        // XZ plane handle sits around (0.29 * worldRadius, 0, 0.29 * worldRadius) from pivot.
        assertTrue(fixture.viewport.camera().project(new MutableVec3().set(pivot)
                .add(0.29f * worldRadius, 0, 0.29f * worldRadius), planeScreen));
        RectView bounds = fixture.viewport.layoutBounds();
        float px = bounds.x() + planeScreen.x(), py = bounds.y() + planeScreen.y();
        fixture.input.pointerPressed(px, py, 0, PointerButton.PRIMARY);
        fixture.input.pointerMoved(px + 24, py + 12, 0);
        fixture.input.pointerReleased(px + 24, py + 12, 0, PointerButton.PRIMARY);
        MutableVec3 afterPlane = worldPivot(cube);
        assertEquals(pivot.y(), afterPlane.y(), EPSILON);
        assertTrue(Math.hypot(afterPlane.x() - pivot.x(), afterPlane.z() - pivot.z()) > 0.05f);

        assertTrue(fixture.pressKey('S', 0));
        assertTrue(fixture.viewport.camera().project(afterPlane, centerScreen));
        float cx = bounds.x() + centerScreen.x(), cy = bounds.y() + centerScreen.y();
        fixture.input.pointerPressed(cx, cy, 0, PointerButton.PRIMARY);
        fixture.input.pointerMoved(cx + 31, cy - 31, 0);
        fixture.input.pointerReleased(cx + 31, cy - 31, 0, PointerButton.PRIMARY);
        MutableVec3 scaled = cube.scale(new MutableVec3());
        assertEquals(2.0f, scaled.x(), EPSILON);
        assertEquals(2.0f, scaled.y(), EPSILON);
        assertEquals(2.0f, scaled.z(), EPSILON);
    }

    /**
     * @param object light object
     * @return its render node
     */
    private static LightNode lightNode(UnitySceneObject object) {
        for (SceneNode child : object.children()) {
            if (child instanceof LightNode node) return node;
        }
        throw new AssertionError("light node missing");
    }

    /**
     * @param object scene node
     * @return pivot in world space
     */
    private static MutableVec3 worldPivot(UnitySceneObject object) {
        MutableVec3 pivot = object.origin(new MutableVec3());
        assertTrue(object.worldMatrix(new MutableMat4()).transformPoint(pivot, pivot));
        return pivot;
    }

    /** @return a laid-out editor */
    private static Fixture layout() {
        UniScreen screen = UnityInspectorScreen.createScreen();
        Widget root = screen.root();
        ((WidgetBase) root).setUiContextInternal(screen.context());
        root.measure(new LayoutContext(1100, 700));
        root.arrange(new MutableRect(0, 0, 1100, 700));
        DockingRoot dock = descendants(root, DockingRoot.class).get(0);
        Widget hierarchy = dock.manager().findPane("hierarchy").content();
        Widget inspector = dock.manager().findPane("inspector").content();
        Widget scene = dock.manager().findPane("scene").content();
        Scene3D viewport = descendants(scene, Scene3D.class).get(0);
        return new Fixture(root, hierarchy, inspector, scene, viewport, descendants(hierarchy, TreeView.class).get(0),
                new InputHandle(root, screen.context()));
    }

    /**
     * @param widget subtree
     * @param type   wanted class
     * @param <T>    wanted type
     * @return matching widgets in tree order after queued mutations
     */
    private static <T extends Widget> List<T> descendants(Widget widget, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(widget)) result.add(type.cast(widget));
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) result.addAll(descendants(child, type));
        return result;
    }

    /** Widgets and input of one editor window. */
    private record Fixture(Widget root, Widget hierarchyPanel, Widget inspector, Widget scenePanel, Scene3D viewport,
                           TreeView tree, InputHandle input) {
        /** @return the hierarchy view */
        UnityEditorHierarchyView hierarchy() {
            return descendants(hierarchyPanel, UnityEditorHierarchyView.class).get(0);
        }

        /** @return the Scene view */
        UnityEditorSceneView sceneView() {
            return descendants(scenePanel, UnityEditorSceneView.class).get(0);
        }

        /** @return the docking root */
        DockingRoot dock() {
            return descendants(root, DockingRoot.class).get(0);
        }

        /** @return the Props row with its children shown */
        TreeViewNode props() {
            TreeViewNode props = tree.roots().get(0).child(1);
            props.expanded(true);
            relayout();
            return props;
        }

        /**
         * Runs an action while a key is held.
         *
         * @param key    key code
         * @param action input to send meanwhile
         */
        void withKey(int key, Runnable action) {
            input.keyPressed(key, 0, 0);
            try {
                action.run();
            } finally {
                input.keyReleased(key, 0, 0);
            }
        }

        /**
         * @param code      key code
         * @param modifiers modifier mask
         * @return whether the press was handled
         */
        boolean pressKey(int code, int modifiers) {
            boolean handled = input.keyPressed(code, 0, modifiers);
            input.keyReleased(code, 0, modifiers);
            return handled;
        }

        /** @param row hierarchy row to click */
        void clickRow(TreeViewNode row) {
            relayout();
            RectView bounds = row.rowButton().layoutBounds();
            click(bounds.x() + bounds.width() / 2, bounds.y() + bounds.height() / 2);
        }

        /**
         * @param x root X
         * @param y root Y
         */
        void click(float x, float y) {
            input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
            input.pointerReleased(x, y, 0, PointerButton.PRIMARY);
        }

        /**
         * Opens a top bar dropdown by its current label and picks an option.
         *
         * @param current dropdown label
         * @param option  option to choose
         */
        void chooseToolbarOption(String current, String option) {
            Button dropdown = descendants(scenePanel, Button.class).stream()
                    .filter(button -> current.equals(button.text())).findFirst().orElseThrow();
            dropdown.click();
            ContextMenu menu = descendants(root, ContextMenu.class).stream().filter(ContextMenu::opened)
                    .findFirst().orElseThrow();
            menu.itemWidgets().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(button -> option.equals(button.text())).findFirst().orElseThrow().click();
        }

        /** @param name component to add from the Add Component popup */
        void addComponent(String name) {
            openComponentPopup();
            WindowWidget popup = componentPopup();
            descendants(popup, Button.class).stream().filter(button -> button.text().endsWith(" / " + name))
                    .findFirst().orElseThrow().click();
            relayout();
        }

        /**
         * @param name component name
         * @return whether the Add Component popup lists it for the selection
         */
        boolean hasComponentEntry(String name) {
            openComponentPopup();
            WindowWidget popup = componentPopup();
            boolean found = descendants(popup, Button.class).stream()
                    .anyMatch(button -> button.text().endsWith(" / " + name));
            popup.close();
            return found;
        }

        /** Clicks the Inspector's Add Component button. */
        private void openComponentPopup() {
            descendants(inspector, Button.class).stream().filter(button -> "Add Component".equals(button.text()))
                    .findFirst().orElseThrow().click();
        }

        /** @return the open Add Component popup */
        private WindowWidget componentPopup() {
            return descendants(root, WindowWidget.class).stream()
                    .filter(window -> "Add Component".equals(window.title()) && window.opened()).findFirst()
                    .orElseThrow();
        }

        /** @param label create menu command */
        void choose(String label) {
            descendants(hierarchyPanel, UnityCreateButton.class).get(0).click();
            ContextMenu menu = descendants(root, ContextMenu.class).stream().filter(ContextMenu::opened)
                    .findFirst().orElseThrow();
            menu.itemWidgets().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(button -> label.equals(button.text())).findFirst().orElseThrow().click();
            relayout();
        }

        /**
         * Drags along a projected gizmo axis.
         *
         * @param pivot  world-space handle center
         * @param axis   unit world direction
         * @param amount drag in projected handle lengths
         * @return handle length in scene units
         */
        float dragAxis(MutableVec3 pivot, MutableVec3 axis, float amount) {
            relayout();
            MutableVec3 center = new MutableVec3(), side = new MutableVec3(), handle = new MutableVec3();
            assertTrue(viewport.camera().project(pivot, center));
            assertTrue(viewport.camera().unproject(center.x() + 62, center.y(), center.z(), side));
            float radius = (float) Math.hypot(Math.hypot(side.x() - pivot.x(), side.y() - pivot.y()),
                    side.z() - pivot.z());
            assertTrue(viewport.camera().project(new MutableVec3().set(pivot)
                    .add(axis.x() * radius, axis.y() * radius, axis.z() * radius), handle));
            RectView bounds = viewport.layoutBounds();
            float x = bounds.x() + handle.x(), y = bounds.y() + handle.y();
            float dx = amount * (handle.x() - center.x()), dy = amount * (handle.y() - center.y());
            input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
            input.pointerMoved(x + dx, y + dy, 0);
            input.pointerReleased(x + dx, y + dy, 0, PointerButton.PRIMARY);
            return radius;
        }

        /** Updates bounds and applies queued mutations. */
        void relayout() {
            root.measure(new LayoutContext(1100, 700));
            root.arrange(new MutableRect(0, 0, 1100, 700));
        }
    }
}
