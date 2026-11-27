package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.input.InputHandle;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraft;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraftHost;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.data.VirtualListView;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.NumberField;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.widgets.navigation.TreeView;
import dev.sixik.unigui.widgets.navigation.TreeViewNode;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the scene editor through its widgets and pointer routing. */
class UnitySceneEditorTest {
    /** Tolerance for float rounding across camera projection and parent transforms. */
    private static final float EPSILON = 1e-4f;

    /** Verifies the create menu, inspector edits, and subtree removal operate on the shared scene. */
    @Test
    void createAndEdit_updateSceneAndHierarchy() {
        Fixture fixture = layout();
        fixture.choose("Cube");
        TreeViewNode row = fixture.tree.selectedNode();
        UnitySceneObject cube = row.userData(UnitySceneObject.class);
        assertEquals("Cube (1)", row.text());
        assertSame(cube, fixture.viewport.scene().roots().get(2));
        assertNotNull(cube.mesh());

        descendants(fixture.inspector, TextInput.class).get(0).text("Edited Cube");
        assertEquals("Edited Cube", row.text());
        descendants(fixture.inspector, NumberField.class).get(0).value(2.5);
        assertEquals(2.5f, cube.position(new MutableVec3()).x(), Math.ulp(2.5f));
        descendants(fixture.inspector, Checkbox.class).get(0).checked(false);
        assertFalse(cube.visible());

        fixture.choose("Create Empty Child");
        UnitySceneObject child = fixture.tree.selectedNode().userData(UnitySceneObject.class);
        assertSame(cube, child.parent());
        assertSame(cube, fixture.tree.selectedNode().parent().userData(UnitySceneObject.class));
        fixture.tree.select(fixture.tree.selectedNode().parent());
        fixture.choose("Delete Selected");
        assertNull(cube.scene());
        assertNull(child.scene());
        assertSame(fixture.tree.roots().get(0), fixture.tree.selectedNode());
    }

    /** Verifies a viewport click finds a real mesh and opens its row in the hierarchy. */
    @Test
    void pick_revealsNestedObjectAndSelectsItsInspector() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.tree.roots().get(0).child(1);
        UnitySceneObject cube = props.child(0).userData(UnitySceneObject.class);
        props.expanded(false);
        MutableVec3 projected = new MutableVec3();
        assertTrue(fixture.viewport.camera().project(cube.position(new MutableVec3()), projected));

        RectView bounds = fixture.viewport.layoutBounds();
        fixture.click(bounds.x() + projected.x(), bounds.y() + projected.y());

        assertTrue(props.expanded());
        assertSame(cube, fixture.tree.selectedNode().userData(UnitySceneObject.class));
        assertEquals("Cube", descendants(fixture.inspector, TextInput.class).get(0).text());
    }

    /** Verifies a tree drop reparents the scene node and updates the displayed tree. */
    @Test
    void dragToObject_reparentsTheSceneNode() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.tree.roots().get(0).child(1);
        TreeViewNode cubeRow = props.child(0);
        TreeViewNode sphereRow = props.child(1);
        UnitySceneObject cube = cubeRow.userData(UnitySceneObject.class);
        UnitySceneObject sphere = sphereRow.userData(UnitySceneObject.class);
        fixture.drag(cubeRow, sphereRow, 0.5f);
        assertSame(sphere, cube.parent());
        assertSame(cube, fixture.tree.selectedNode().userData(UnitySceneObject.class));
        assertSame(sphere, fixture.tree.selectedNode().parent().userData(UnitySceneObject.class));

        TreeViewNode scene = fixture.tree.roots().get(0);
        fixture.drag(fixture.tree.selectedNode(), scene, 0.5f);
        assertNull(cube.parent());
        assertSame(cube, fixture.viewport.scene().roots().get(2));
    }

    /** Verifies sibling order in the tree matches the graph after a drop. */
    @Test
    void dragAboveSibling_reordersSceneTraversal() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.tree.roots().get(0).child(1);
        TreeViewNode cube = props.child(0);
        TreeViewNode capsule = props.child(2);
        UnitySceneObject parent = props.userData(UnitySceneObject.class);
        UnitySceneObject moved = capsule.userData(UnitySceneObject.class);

        fixture.drag(capsule, cube, 0.15f);

        assertSame(moved, parent.children().get(0));
        assertSame(moved, fixture.tree.roots().get(0).child(1).child(0).userData(UnitySceneObject.class));
        assertSame(parent, moved.parent());
    }

    /** Verifies searching keeps matching branches and mesh selection reveals filtered objects. */
    @Test
    void search_keepsAncestorsAndRevealsPickedObject() {
        Fixture fixture = layout();
        TextInput search = descendants(fixture.hierarchy, TextInput.class).get(0);
        search.text("capsule");
        assertEquals(List.of("SampleScene", "Props", "Capsule"),
                fixture.tree.visibleNodes().stream().map(TreeViewNode::text).toList());

        UnitySceneObject cube = (UnitySceneObject) fixture.viewport.scene().roots().get(1).children().get(0);
        MutableVec3 projected = new MutableVec3();
        assertTrue(fixture.viewport.camera().project(cube.position(new MutableVec3()), projected));
        RectView bounds = fixture.viewport.layoutBounds();
        fixture.click(bounds.x() + projected.x(), bounds.y() + projected.y());

        assertEquals("", search.text());
        assertSame(cube, fixture.tree.selectedNode().userData(UnitySceneObject.class));
    }

    /** An orbit drag keeps the selection. A stationary click on the empty viewport clears it. */
    @Test
    void backgroundClick_clearsSelectionWithoutBreakingOrbit() {
        Fixture fixture = layout();
        TreeViewNode cube = fixture.tree.roots().get(0).child(1).child(0);
        fixture.tree.select(cube);
        RectView bounds = fixture.viewport.layoutBounds();
        float x = bounds.width() - 12.0f, y = 12.0f;
        assertNull(fixture.viewport.pickNode(x, y));
        double yaw = fixture.viewport.orbit().yaw();
        fixture.input.pointerPressed(bounds.x() + x, bounds.y() + y, 0, PointerButton.PRIMARY);
        fixture.input.pointerMoved(bounds.x() + x - 40, bounds.y() + y, 0);
        fixture.input.pointerReleased(bounds.x() + x - 40, bounds.y() + y, 0, PointerButton.PRIMARY);
        assertSame(cube, fixture.tree.selectedNode());
        assertTrue(fixture.viewport.orbit().yaw() != yaw);
        assertNull(fixture.viewport.pickNode(x, y));
        fixture.click(bounds.x() + x, bounds.y() + y);
        assertNull(fixture.tree.selectedNode());

        fixture.tree.select(cube);
        MutableVec3 floorPoint = new MutableVec3();
        assertTrue(fixture.viewport.camera().project(new MutableVec3(-2, 0, 2), floorPoint));
        assertSame(fixture.viewport.scene().roots().get(0),
                fixture.viewport.pickNode(floorPoint.x(), floorPoint.y()));
        fixture.click(bounds.x() + floorPoint.x(), bounds.y() + floorPoint.y());
        assertNull(fixture.tree.selectedNode());
    }

    /** A world-X handle translates a child through its rotated and scaled parent. */
    @Test
    void moveHandle_updatesNestedObjectAndInspector() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.tree.roots().get(0).child(1);
        UnitySceneObject parent = props.userData(UnitySceneObject.class);
        TreeViewNode cubeRow = props.child(0);
        UnitySceneObject cube = cubeRow.userData(UnitySceneObject.class);
        parent.setAngles(0, 90, 0);
        parent.setScale(1, 1, 2);
        fixture.tree.select(cubeRow);
        MutableVec3 before = worldPivot(cube);
        var camera = fixture.viewport.camera();
        fixture.viewport.orbit().setOrbit(before, 7.0f, 0.45, 0.3);
        MutableVec3 center = new MutableVec3();
        MutableVec3 side = new MutableVec3();
        assertTrue(camera.project(before, center));
        assertTrue(camera.unproject(center.x() + 62, center.y(), center.z(), side));
        float radius = (float) Math.hypot(Math.hypot(side.x() - before.x(), side.y() - before.y()),
                side.z() - before.z());
        MutableVec3 handle = new MutableVec3();
        assertTrue(camera.project(new MutableVec3().set(before).add(radius, 0, 0), handle));
        RectView bounds = fixture.viewport.layoutBounds();
        float x = bounds.x() + handle.x(), y = bounds.y() + handle.y();
        float dx = (handle.x() - center.x()) * 0.5f, dy = (handle.y() - center.y()) * 0.5f;
        fixture.input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
        fixture.input.pointerMoved(x + dx, y + dy, 0);
        fixture.input.pointerReleased(x + dx, y + dy, 0, PointerButton.PRIMARY);
        MutableVec3 after = worldPivot(cube);
        assertEquals(before.x() + radius * 0.5f, after.x(), EPSILON);
        assertEquals(before.y(), after.y(), EPSILON);
        assertEquals(before.z(), after.z(), EPSILON);
        List<NumberField> fields = descendants(fixture.inspector, NumberField.class);
        assertEquals(cube.position(new MutableVec3()).z(), fields.get(2).value(), EPSILON);
    }

    /** @param object scene node whose local pivot is projected into world space
     * @return the world-space pivot
     */
    private static MutableVec3 worldPivot(UnitySceneObject object) {
        MutableVec3 pivot = object.origin(new MutableVec3());
        assertTrue(object.worldMatrix(new MutableMat4()).transformPoint(pivot, pivot));
        return pivot;
    }

    /** Secondary clicks target their own rows for creating and deleting scene objects. */
    @Test
    void hierarchyContextMenu_createsChildAndDeletesIt() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.tree.roots().get(0).child(1);
        UnitySceneObject parent = props.userData(UnitySceneObject.class);
        fixture.rightClick(props);
        fixture.chooseOpenMenuItem("Create Empty Child");
        TreeViewNode childRow = fixture.tree.selectedNode();
        UnitySceneObject child = childRow.userData(UnitySceneObject.class);
        assertSame(parent, child.parent());
        fixture.tree.select(fixture.tree.roots().get(0));
        fixture.rightClick(childRow);
        fixture.chooseOpenMenuItem("Delete Selected");
        assertNull(child.scene());
        assertSame(parent, fixture.tree.selectedNode().userData(UnitySceneObject.class));
    }

    /** Verifies restored inspector components, their removal menu, and usable coordinate widths. */
    @Test
    void inspector_showsComponentsAndKeepsWideFields() {
        Fixture fixture = layout();
        fixture.tree.select(fixture.tree.roots().get(0).child(1).child(0));
        fixture.root.measure(new LayoutContext(760, 670));
        fixture.root.arrange(new MutableRect(0, 0, 760, 670));
        List<NumberField> fields = descendants(fixture.inspector, NumberField.class);
        for (NumberField field : fields.subList(0, 9)) {
            RectView bounds = field.layoutBounds();
            assertTrue(bounds.width() >= 46.0f);
            assertTrue(bounds.x() + bounds.width() <= fixture.inspector.layoutBounds().x()
                    + fixture.inspector.layoutBounds().width());
        }
        assertEquals(List.of("Transform", "Camera", "Audio Listener"),
                descendants(fixture.inspector, UnityComponentSection.class).stream().map(UnityComponentSection::title).toList());
        descendants(fixture.inspector, Button.class).stream()
                .filter(button -> "Add Component".equals(button.text())).findFirst().orElseThrow().click();
        UnityComponentSection added = descendants(fixture.inspector, UnityComponentSection.class).get(3);
        assertEquals("Box Collider", added.title());
        added.menuButton().click();
        fixture.chooseOpenMenuItem("Remove Component");
        assertFalse(descendants(fixture.inspector, UnityComponentSection.class).contains(added));
    }

    /** Verifies the screenshot layout at normal widths, with the label and all axes on a single line. */
    @Test
    void inspector_widePanelAlignsTransformLabelsAndAxes() {
        Fixture fixture = layout();
        fixture.tree.select(fixture.tree.roots().get(0).child(1).child(0));
        fixture.relayout();
        List<NumberField> fields = descendants(fixture.inspector, NumberField.class);
        for (int index = 0; index < 9; index += 3) {
            RectView x = fields.get(index).layoutBounds();
            RectView y = fields.get(index + 1).layoutBounds();
            RectView z = fields.get(index + 2).layoutBounds();
            assertEquals(x.y(), y.y(), EPSILON);
            assertEquals(x.y(), z.y(), EPSILON);
            assertTrue(x.x() < y.x() && y.x() < z.x());
        }
        assertEquals(UnityStyles.ROW_HEIGHT + 3, fields.get(3).layoutBounds().y() - fields.get(0).layoutBounds().y(),
                EPSILON, () -> "Transform row width: " + fields.get(0).parent().parent().parent().layoutBounds().width());
    }

    /** Local translation follows an object's orientation, including its rotated parent. */
    @Test
    void moveHandle_localSpaceFollowsAncestorRotation() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.tree.roots().get(0).child(1);
        props.userData(UnitySceneObject.class).setAngles(0, 90, 0);
        TreeViewNode cubeRow = props.child(0);
        UnitySceneObject cube = cubeRow.userData(UnitySceneObject.class);
        cube.setAngles(0, 0, 0);
        fixture.tree.select(cubeRow);
        fixture.button("Global").click();
        fixture.chooseOpenMenuItem("Local");
        fixture.clickAt(fixture.button("SCALE"), PointerButton.PRIMARY);
        assertTrue(fixture.pressKey('T', 0));
        MutableVec3 pivot = worldPivot(cube);
        fixture.viewport.orbit().setOrbit(pivot, 7, 0.45, 0.3);

        float radius = fixture.dragAxis(pivot, new MutableVec3(0, 0, -1), 0.5f);

        MutableVec3 moved = worldPivot(cube);
        assertEquals(pivot.x(), moved.x(), EPSILON);
        assertEquals(pivot.y(), moved.y(), EPSILON);
        assertEquals(pivot.z() - radius * 0.5f, moved.z(), EPSILON);
    }

    /** A global Z ring uses world axes even under a rotated parent and refreshes the Inspector's local angles. */
    @Test
    void rotateHandle_updatesWorldOrientationAndLocalAngles() {
        Fixture fixture = layout();
        TreeViewNode props = fixture.tree.roots().get(0).child(1);
        UnitySceneObject parent = props.userData(UnitySceneObject.class);
        parent.setAngles(0, 90, 0);
        TreeViewNode cubeRow = props.child(0);
        UnitySceneObject cube = cubeRow.userData(UnitySceneObject.class);
        cube.setAngles(0, 0, 0);
        fixture.clickAt(cubeRow.rowButton(), PointerButton.PRIMARY);
        assertTrue(fixture.pressKey(KeyCodes.R, 0));
        MutableVec3 pivot = worldPivot(cube);
        fixture.viewport.orbit().setOrbit(pivot, 7, 0, 0);
        float radius = fixture.handleRadius(pivot) * 1.24f;
        float diagonal = radius * (float) Math.sqrt(0.5);
        MutableVec3 start = fixture.project(new MutableVec3().set(pivot).add(diagonal, diagonal, 0));
        double nextAngle = Math.PI * 0.3;
        MutableVec3 end = fixture.project(new MutableVec3().set(pivot)
                .add(radius * (float) Math.cos(nextAngle), radius * (float) Math.sin(nextAngle), 0));
        float dx = end.x() - start.x(), dy = end.y() - start.y();
        float fraction = 31 / (float) Math.hypot(dx, dy);

        fixture.dragViewport((start.x() + end.x()) * 0.5f, (start.y() + end.y()) * 0.5f,
                dx * fraction, dy * fraction);

        MutableQuat world = new MutableQuat().setProduct(parent.rotation(new MutableQuat()), cube.rotation(new MutableQuat()));
        MutableVec3 y = world.rotate(0, 1, 0, new MutableVec3());
        assertEquals(-Math.sin(0.5), y.x(), EPSILON);
        assertEquals(Math.cos(0.5), y.y(), EPSILON);
        assertEquals(Math.toDegrees(-0.5), descendants(fixture.inspector, NumberField.class).get(3).value(), EPSILON);
    }

    /** Global scaling distributes a world-axis stretch across the rotated object's local axes. */
    @Test
    void scaleHandle_supportsBothFramesAndRefreshesInspector() {
        Fixture fixture = layout();
        TreeViewNode cubeRow = fixture.tree.roots().get(0).child(1).child(0);
        UnitySceneObject cube = cubeRow.userData(UnitySceneObject.class);
        cube.setAngles(0, 45, 0);
        fixture.clickAt(cubeRow.rowButton(), PointerButton.PRIMARY);
        assertTrue(fixture.pressKey(KeyCodes.S, 0));
        MutableVec3 pivot = worldPivot(cube);
        fixture.viewport.orbit().setOrbit(pivot, 7, 0.45, 0.3);
        fixture.dragAxis(pivot, new MutableVec3(1, 0, 0), 1);
        List<NumberField> fields = descendants(fixture.inspector, NumberField.class);
        assertEquals(Math.sqrt(2.5), fields.get(6).value(), EPSILON);
        assertEquals(1, fields.get(7).value(), EPSILON);
        assertEquals(Math.sqrt(2.5), fields.get(8).value(), EPSILON);

        cube.setScale(1, 1, 1);
        fixture.button("Global").click();
        fixture.chooseOpenMenuItem("Local");
        MutableVec3 localX = cube.rotation(new MutableQuat()).rotate(1, 0, 0, new MutableVec3());
        fixture.dragAxis(pivot, localX, 1);
        assertEquals(2, fields.get(6).value(), EPSILON);
        assertEquals(1, fields.get(7).value(), EPSILON);
        assertEquals(1, fields.get(8).value(), EPSILON);
    }

    /** Checks that keyboard copy captures a snapshot and that duplicate and paste select independent subtrees. */
    @Test
    void objectShortcuts_copyDuplicatePasteAndDeletePreserveHierarchy() {
        Fixture fixture = layout();
        TreeViewNode propsRow = fixture.tree.roots().get(0).child(1);
        UnitySceneObject props = propsRow.userData(UnitySceneObject.class);
        UnitySceneObject cube = (UnitySceneObject) props.children().get(0);
        cube.setAngles(0, 380, 0);
        cube.setVisible(false);
        fixture.clickAt(propsRow.rowButton(), PointerButton.PRIMARY);
        assertTrue(fixture.pressKey(KeyCodes.C, KeyModifiers.CONTROL));
        cube.setAngles(0, 45, 0);
        cube.setVisible(true);

        assertTrue(fixture.pressKey(KeyCodes.D, KeyModifiers.CONTROL));
        UnitySceneObject duplicate = fixture.tree.selectedNode().userData(UnitySceneObject.class);
        assertEquals(List.of(props, duplicate), fixture.viewport.scene().roots().subList(1, 3));
        assertEquals("Props (1)", duplicate.name());
        assertEquals(45, ((UnitySceneObject) duplicate.children().get(0)).angles().y(), EPSILON);
        assertTrue(fixture.pressKey(KeyCodes.DELETE, 0));
        assertNull(duplicate.scene());
        assertFalse(fixture.pressKey(KeyCodes.DELETE, 0));

        assertTrue(fixture.pressKey(KeyCodes.V, KeyModifiers.CONTROL));
        UnitySceneObject pasted = fixture.tree.selectedNode().userData(UnitySceneObject.class);
        UnitySceneObject pastedCube = (UnitySceneObject) pasted.children().get(0);
        assertEquals(3, pasted.children().size());
        assertNotSame(cube, pastedCube);
        assertEquals(380, pastedCube.angles().y(), EPSILON);
        assertFalse(pastedCube.visible());
        pastedCube.setAngles(0, 15, 0);
        assertTrue(fixture.pressKey(KeyCodes.V, KeyModifiers.CONTROL));
        UnitySceneObject second = fixture.tree.selectedNode().userData(UnitySceneObject.class);
        assertEquals(380, ((UnitySceneObject) second.children().get(0)).angles().y(), EPSILON);
    }

    /** Text fields retain their own Delete, Ctrl+C, and Ctrl+V behavior. */
    @Test
    void objectShortcuts_leaveTextEditingInControl() {
        Fixture fixture = layout();
        TreeViewNode cube = fixture.tree.roots().get(0).child(1).child(0);
        fixture.clickAt(cube.rowButton(), PointerButton.PRIMARY);
        fixture.relayout();
        TextInput name = descendants(fixture.inspector, TextInput.class).get(0);
        fixture.clickAt(name, PointerButton.PRIMARY);
        name.selectAll();
        assertTrue(fixture.pressKey(KeyCodes.C, KeyModifiers.CONTROL));
        assertTrue(fixture.pressKey(KeyCodes.DELETE, 0));
        assertEquals("", name.text());
        assertTrue(fixture.pressKey(KeyCodes.V, KeyModifiers.CONTROL));
        assertEquals("Cube", name.text());
        fixture.pressKey(KeyCodes.D, KeyModifiers.CONTROL);
        assertSame(cube, fixture.tree.selectedNode());
        assertEquals(3, cube.parent().children().size());
        fixture.pressKey(KeyCodes.R, 0);
        assertTrue(fixture.input.textInput('r', 0));
        assertEquals("Cuber", name.text());
    }

    /**
     * Checks that Project Delete removes the asset and leaves the scene selection intact.
     * @param directory isolated asset storage
     */
    @Test
    void objectShortcuts_projectDeleteTargetsTheAsset(@TempDir Path directory) {
        Fixture fixture = layout(UnityInspectorScreen.createScreen(directory));
        TreeViewNode props = fixture.tree.roots().get(0).child(1);
        fixture.rightClick(props);
        fixture.chooseOpenMenuItem("Save as Prefab");
        fixture.relayout();
        fixture.clickAt(fixture.assetLabel("Props.prefab"), PointerButton.PRIMARY);
        assertTrue(fixture.pressKey(KeyCodes.DELETE, 0));
        assertEquals(Visibility.COLLAPSED, fixture.preview().visibility());
        assertSame(props, fixture.tree.selectedNode());
        assertEquals(2, fixture.viewport.scene().roots().size());
    }

    /** Pausing Play blocks keyboard mutations and stopping restores the pre-Play scene. */
    @Test
    void objectShortcuts_pauseBlocksEditsAndPlayRestoresThem() {
        Fixture fixture = layout();
        TreeViewNode row = fixture.tree.roots().get(0).child(1).child(0);
        UnitySceneObject cube = row.userData(UnitySceneObject.class);
        fixture.clickAt(row.rowButton(), PointerButton.PRIMARY);
        assertTrue(fixture.pressKey(KeyCodes.C, KeyModifiers.CONTROL));
        fixture.button("▶").click();
        fixture.button("❚❚").click();
        focus(fixture, fixture.tree);
        assertFalse(fixture.pressKey(KeyCodes.DELETE, 0));
        assertFalse(fixture.pressKey(KeyCodes.D, KeyModifiers.CONTROL));
        assertFalse(fixture.pressKey(KeyCodes.V, KeyModifiers.CONTROL));
        assertEquals(3, cube.parent().children().size());
        fixture.button("❚❚").click();
        focus(fixture, fixture.tree);
        assertTrue(fixture.pressKey(KeyCodes.V, KeyModifiers.CONTROL));
        assertEquals(4, cube.parent().children().size());
        assertSame(cube.parent().children().get(1), fixture.tree.selectedNode().userData(UnitySceneObject.class));
        fixture.button("▶").click();
        assertEquals(3, fixture.tree.roots().get(0).child(1).children().size());
    }

    /** Previews every registry category and adds independent native nodes only on confirmation. */
    @Test
    void minecraftPicker_previewsRegistriesAndSelectsCreatedObjects() {
        DemoMinecraftHost previous = DemoMinecraft.host();
        DemoMinecraft.host(new DemoMinecraftHost() {
            /** {@inheritDoc} */
            @Override
            public String platform() {
                return "Test Minecraft";
            }

            /** {@inheritDoc} */
            @Override
            public List<String> sceneElementIds(SceneElementKind kind) {
                return switch (kind) {
                    case BLOCK -> List.of("minecraft:stone", "minecraft:glass");
                    case ITEM -> List.of("minecraft:diamond_sword", "example:hammer");
                    case ENTITY -> List.of("minecraft:zombie", "CaveSpider");
                };
            }
        });
        try {
            Fixture fixture = layout();
            WindowWidget picker = openMinecraftPicker(fixture);
            Scene3D pickerViewport = descendants(picker, Scene3D.class).get(0);
            Button addToScene = descendants(picker, Button.class).stream()
                    .filter(button -> "Add to Scene".equals(button.text())).findFirst().orElseThrow();
            assertTrue(pickerViewport.layoutBounds().width() > 0 && pickerViewport.layoutBounds().height() > 0);
            assertFalse(addToScene.enabled());
            VirtualListView results = descendants(picker, VirtualListView.class).get(0);
            assertEquals(2, results.itemCount());
            TextInput search = descendants(picker, TextInput.class).get(0);
            search.text("missing");
            assertEquals(0, results.itemCount());
            search.text("minecraft:stone");
            fixture.relayout();
            assertEquals(1, results.itemCount());
            clickPickerButton(fixture, picker, "Stone  ·  minecraft:stone");
            assertTrue(picker.opened());
            assertTrue(addToScene.enabled());
            assertEquals(2, fixture.viewport.scene().roots().size());
            var previewStone = pickerViewport.scene().roots().get(0);
            assertTrue(previewStone.children().get(0) instanceof BlockNode);

            search.text("stone");
            fixture.relayout();
            assertSame(previewStone, pickerViewport.scene().roots().get(0));
            RectView previewArea = pickerViewport.layoutBounds();
            double yaw = pickerViewport.orbit().yaw();
            fixture.dragTo(pickerViewport,
                    new MutableRect(previewArea.x() + previewArea.width() * 0.75f, previewArea.y() + 20, 1, 1),
                    PointerButton.PRIMARY);
            assertTrue(Math.abs(yaw - pickerViewport.orbit().yaw()) > EPSILON);
            search.text("glass");
            fixture.relayout();
            assertTrue(pickerViewport.scene().roots().isEmpty());
            assertFalse(addToScene.enabled());
            clickPickerButton(fixture, picker, "Glass  ·  minecraft:glass");
            clickPickerButton(fixture, picker, "Items");
            assertTrue(pickerViewport.scene().roots().isEmpty());
            assertFalse(addToScene.enabled());

            clickPickerButton(fixture, picker, "Blocks");
            search.text("stone");
            fixture.relayout();
            clickPickerButton(fixture, picker, "Stone  ·  minecraft:stone");
            previewStone = pickerViewport.scene().roots().get(0);
            clickPickerButton(fixture, picker, "Add to Scene");
            assertFalse(picker.opened());
            ((WidgetBase) fixture.root).uiContext().dispatcher().drain();
            assertTrue(picker.children().isEmpty());
            UnitySceneObject stone = fixture.tree.selectedNode().userData(UnitySceneObject.class);
            assertSame(stone, fixture.viewport.scene().roots().get(2));
            assertNotSame(previewStone, stone);
            assertTrue(stone.children().get(0) instanceof BlockNode);
            assertEquals("minecraft:stone", stone.minecraftId());
            assertTrue(fixture.tree.selectedNode().children().isEmpty());
            assertEquals(Visibility.VISIBLE, fixture.preview().visibility());
            Scene3D inspectorViewport = descendants(fixture.preview(), Scene3D.class).get(0);
            var inspectorStone = (UnitySceneObject) inspectorViewport.scene().roots().get(0);
            assertEquals(stone.minecraftId(), inspectorStone.minecraftId());
            assertNotSame(previewStone, inspectorStone);
            assertNotSame(stone.children().get(0), inspectorStone.children().get(0));
            assertTrue(fixture.pressKey(KeyCodes.D, KeyModifiers.CONTROL));
            assertTrue(fixture.pressKey(KeyCodes.DELETE, 0));
            assertEquals(3, fixture.viewport.scene().roots().size());

            picker = openMinecraftPicker(fixture);
            clickPickerButton(fixture, picker, "Items");
            descendants(picker, TextInput.class).get(0).text("diamond sword");
            fixture.relayout();
            clickPickerButton(fixture, picker, "Diamond sword  ·  minecraft:diamond_sword");
            assertTrue(descendants(picker, Scene3D.class).get(0).scene().roots().get(0).children().get(0) instanceof ItemNode);
            clickPickerButton(fixture, picker, "Add to Scene");
            UnitySceneObject sword = fixture.tree.selectedNode().userData(UnitySceneObject.class);
            assertTrue(sword.children().get(0) instanceof ItemNode);
            assertEquals("minecraft:diamond_sword", sword.minecraftId());

            picker = openMinecraftPicker(fixture);
            clickPickerButton(fixture, picker, "Entities");
            descendants(picker, TextInput.class).get(0).text("cave spider");
            fixture.relayout();
            clickPickerButton(fixture, picker, "Cave Spider  ·  CaveSpider");
            assertTrue(descendants(picker, Scene3D.class).get(0).scene().roots().get(0).children().get(0) instanceof EntityNode);
            clickPickerButton(fixture, picker, "Add to Scene");
            UnitySceneObject spider = fixture.tree.selectedNode().userData(UnitySceneObject.class);
            assertTrue(spider.children().get(0) instanceof EntityNode);
            assertEquals("CaveSpider", spider.minecraftId());

            fixture.tree.select(fixture.tree.roots().get(0));
            assertEquals(Visibility.COLLAPSED, fixture.preview().visibility());
            assertTrue(inspectorViewport.scene().roots().isEmpty());
            MutableVec3 center = stone.position(new MutableVec3());
            fixture.viewport.orbit().setOrbit(center, 5, 0.55, 0.48);
            MutableVec3 pixel = fixture.project(center);
            RectView area = fixture.viewport.layoutBounds();
            fixture.click(area.x() + pixel.x(), area.y() + pixel.y());
            assertSame(stone, fixture.tree.selectedNode().userData(UnitySceneObject.class));

            picker = openMinecraftPicker(fixture);
            fixture.clickAt(descendants(picker, TextInput.class).get(0), PointerButton.PRIMARY);
            assertTrue(fixture.pressKey(KeyCodes.ESCAPE, 0));
            assertFalse(picker.opened());
            assertEquals(5, fixture.viewport.scene().roots().size());

            fixture.button("▶").click();
            fixture.choose("Delete Selected");
            assertNull(stone.scene());
            fixture.button("▶").click();
            assertSame(stone, fixture.tree.selectedNode().userData(UnitySceneObject.class));
            assertSame(fixture.viewport.scene(), stone.children().get(0).scene());
            assertEquals(1, stone.children().size());
        } finally {
            DemoMinecraft.host(previous);
        }
    }

    /**
     * Opens the picker from the Hierarchy creation menu and lays out the dialog.
     * @param fixture active editor
     * @return the open modal picker
     */
    private static WindowWidget openMinecraftPicker(Fixture fixture) {
        fixture.choose("Create Minecraft Element");
        fixture.relayout();
        return descendants(fixture.root, WindowWidget.class).stream().filter(WindowWidget::opened)
                .filter(window -> "Create Minecraft Element".equals(window.title())).findFirst().orElseThrow();
    }

    /**
     * Activates a picker command through the modal dialog's normal pointer path.
     * @param fixture active editor
     * @param picker open registry dialog
     * @param text button caption
     */
    private static void clickPickerButton(Fixture fixture, WindowWidget picker, String text) {
        Button button = descendants(picker, Button.class).stream().filter(item -> text.equals(item.text()))
                .findFirst().orElseThrow();
        fixture.clickAt(button, PointerButton.PRIMARY);
    }

    /**
     * Focuses the supplied widget for a key dispatched without an intervening pointer press.
     * @param fixture active editor
     * @param target focusable widget in the editor
     */
    private static void focus(Fixture fixture, Widget target) {
        ((WidgetBase) fixture.root).uiContext().focusManager().requestFocus(target);
    }

    /** Stopping Play restores identities, graph order, transforms, properties and Inspector components. */
    @Test
    void playStop_restoresPrePlaySceneAndComponents() {
        Fixture fixture = layout();
        var scene = fixture.viewport.scene();
        var props = fixture.tree.roots().get(0).child(1);
        UnitySceneObject parent = props.userData(UnitySceneObject.class);
        var cubeRow = props.child(0);
        UnitySceneObject cube = cubeRow.userData(UnitySceneObject.class);
        var originalChildren = List.copyOf(parent.children());
        fixture.tree.select(cubeRow);
        fixture.button("Add Component").click();
        UnityComponentSection original = descendants(fixture.inspector, UnityComponentSection.class).get(3);
        List<NumberField> fields = descendants(fixture.inspector, NumberField.class);
        fields.get(0).value(3.0);
        fields.get(4).value(380);
        fields.get(6).value(1.5);

        fixture.button("▶").click();
        fields.get(0).value(6.0);
        fields.get(4).value(60);
        fields.get(6).value(2.5);
        descendants(fixture.inspector, TextInput.class).get(0).text("Temporary");
        descendants(fixture.inspector, Checkbox.class).get(0).checked(false);
        original.menuButton().click();
        fixture.chooseOpenMenuItem("Remove Component");
        fixture.button("Add Component").click();
        fixture.drag(cubeRow, props.child(1), 0.5f);
        fixture.choose("Delete Selected");
        fixture.choose("Sphere");
        UnitySceneObject runtimeObject = fixture.tree.selectedNode().userData(UnitySceneObject.class);
        assertNull(cube.scene());
        assertSame(scene, runtimeObject.scene());

        fixture.button("▶").click();

        assertEquals(2, scene.roots().size());
        assertEquals(originalChildren, parent.children());
        assertNull(runtimeObject.scene());
        assertEquals("Cube", cube.name());
        assertTrue(cube.visible());
        assertSame(cube, fixture.tree.selectedNode().userData(UnitySceneObject.class));
        assertEquals(3, fields.get(0).value(), EPSILON);
        assertEquals(380, fields.get(4).value(), EPSILON);
        assertEquals(1.5, fields.get(6).value(), EPSILON);
        assertSame(original, descendants(fixture.inspector, UnityComponentSection.class).get(3));
    }

    /** Pause blocks editing until resumed and is unavailable outside Play. */
    @Test
    void playPause_blocksSceneEditsAndStopReenablesThem() {
        Fixture fixture = layout();
        Button pause = fixture.button("❚❚");
        assertFalse(pause.enabled());
        var props = fixture.tree.roots().get(0).child(1);
        var cubeRow = props.child(0);
        UnitySceneObject cube = cubeRow.userData(UnitySceneObject.class);
        fixture.tree.select(cubeRow);
        float x = cube.position(new MutableVec3()).x();
        fixture.button("▶").click();
        assertTrue(pause.enabled());
        pause.click();
        fixture.rightClick(cubeRow);
        assertFalse(descendants(fixture.root, ContextMenu.class).stream().anyMatch(ContextMenu::opened));
        assertFalse(descendants(fixture.hierarchy, UnityCreateButton.class).get(0).enabled());
        fixture.drag(cubeRow, props.child(1), 0.5f);
        assertSame(props.userData(UnitySceneObject.class), cube.parent());
        fixture.tree.select(cubeRow);
        List<NumberField> fields = descendants(fixture.inspector, NumberField.class);
        fields.get(0).value(8);
        assertEquals(x, cube.position(new MutableVec3()).x(), EPSILON);
        MutableVec3 pivot = worldPivot(cube);
        fixture.viewport.orbit().setOrbit(pivot, 7, 0.45, 0.3);
        fixture.dragAxis(pivot, new MutableVec3(1, 0, 0), 0.25f);
        assertEquals(x, cube.position(new MutableVec3()).x(), EPSILON);

        pause.click();
        fixture.tree.select(cubeRow);
        fields.get(0).value(5);
        assertEquals(5, cube.position(new MutableVec3()).x(), EPSILON);
        pause.click();
        fixture.button("▶").click();
        assertFalse(pause.enabled());
        assertEquals(x, cube.position(new MutableVec3()).x(), EPSILON);
        fields.get(0).value(4);
        assertEquals(4, cube.position(new MutableVec3()).x(), EPSILON);
    }

    /** Game renders the edited graph with a separate camera that ignores viewport gestures. */
    @Test
    void gameView_sharesEditsAndIgnoresCameraGestures() {
        Fixture fixture = layout();
        DockingRoot dock = descendants(fixture.root, DockingRoot.class).get(0);
        Widget game = dock.manager().findPane("game").content();
        Scene3D gameViewport = descendants(game, Scene3D.class).get(0);
        assertSame(fixture.viewport.scene(), gameViewport.scene());
        assertFalse(gameViewport.cameraControls());
        MutableVec3 initial = gameViewport.camera().position(new MutableVec3());
        fixture.viewport.orbit().setAngles(-1, 0.2);
        fixture.choose("Cube");
        assertSame(fixture.tree.selectedNode().userData(UnitySceneObject.class), gameViewport.scene().roots().get(2));

        dock.manager().selectPane("game");
        fixture.relayout();
        RectView bounds = gameViewport.layoutBounds();
        assertTrue(bounds.width() > 0 && bounds.height() > 0);
        float x = bounds.x() + bounds.width() / 2.0f, y = bounds.y() + bounds.height() / 2.0f;
        fixture.input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
        fixture.input.pointerMoved(x + 40, y - 20, 0);
        fixture.input.pointerReleased(x + 40, y - 20, 0, PointerButton.PRIMARY);
        fixture.input.pointerScrolled(x, y, 0, 1, 0, 0);
        MutableVec3 current = gameViewport.camera().position(new MutableVec3());
        assertEquals(initial.x(), current.x(), EPSILON);
        assertEquals(initial.y(), current.y(), EPSILON);
        assertEquals(initial.z(), current.z(), EPSILON);
    }

    /**
     * Saves and previews a subtree, then places another copy after reopening the editor.
     * @param directory empty project directory supplied by JUnit
     */
    @Test
    void prefab_saveFromHierarchyReopensWithPreviewAndPlacement(@TempDir Path directory) {
        Fixture fixture = layout(UnityInspectorScreen.createScreen(directory));
        TreeViewNode props = fixture.tree.roots().get(0).child(1);
        UnitySceneObject source = props.userData(UnitySceneObject.class);
        fixture.rightClick(props);
        fixture.chooseOpenMenuItem("Save as Prefab");
        fixture.relayout();
        UnityScenePreview preview = fixture.preview();
        Scene3D previewViewport = descendants(preview, Scene3D.class).get(0);
        assertEquals(Visibility.VISIBLE, preview.visibility());
        assertTrue(previewViewport.layoutBounds().width() > 0 && previewViewport.layoutBounds().height() > 0);
        assertTrue(preview.layoutBounds().y() + preview.layoutBounds().height()
                <= fixture.inspector.layoutBounds().y() + fixture.inspector.layoutBounds().height() + EPSILON);
        assertNotSame(source, previewViewport.scene().roots().get(0));
        assertEquals(3, previewViewport.scene().roots().get(0).children().size());
        assertEquals(2, fixture.viewport.scene().roots().size());

        Fixture reopened = layout(UnityInspectorScreen.createScreen(directory));
        reopened.openPrefabs();
        Label prefab = reopened.assetLabel("Props.prefab");
        reopened.clickAt(prefab, PointerButton.PRIMARY);
        assertEquals(Visibility.VISIBLE, reopened.preview().visibility());
        reopened.relayout();
        reopened.clickAt(reopened.assetLabel("Props.prefab"), PointerButton.SECONDARY);
        reopened.chooseOpenMenuItem("Add to Scene");
        assertEquals(3, reopened.viewport.scene().roots().size());
        UnitySceneObject placed = reopened.tree.selectedNode().userData(UnitySceneObject.class);
        assertEquals("Props (1)", placed.name());
        assertEquals(3, placed.children().size());
        assertEquals(Visibility.COLLAPSED, reopened.preview().visibility());
    }

    /**
     * Drags a scene row into Project, then places the saved template through both drop targets.
     * @param directory empty project directory supplied by JUnit
     */
    @Test
    void prefab_dragBetweenHierarchyProjectAndScene_createsCopies(@TempDir Path directory) {
        Fixture fixture = layout(UnityInspectorScreen.createScreen(directory));
        fixture.openPrefabs();
        TreeViewNode cubeRow = fixture.tree.roots().get(0).child(1).child(0);
        UnitySceneObject cube = cubeRow.userData(UnitySceneObject.class);
        fixture.dragTo(cubeRow.rowButton(), fixture.prefabsFolder().rowButton().layoutBounds(), PointerButton.PRIMARY);
        fixture.relayout();
        assertEquals(2, fixture.viewport.scene().roots().size());
        assertEquals(Visibility.VISIBLE, fixture.preview().visibility());

        TreeViewNode props = fixture.tree.roots().get(0).child(1);
        fixture.dragTo(fixture.assetLabel("Cube.prefab"), props.rowButton().layoutBounds(), PointerButton.PRIMARY);
        assertSame(props.userData(UnitySceneObject.class), fixture.tree.selectedNode().userData(UnitySceneObject.class).parent());
        assertEquals(4, cube.parent().children().size());

        fixture.relayout();
        MutableVec3 drop = fixture.project(new MutableVec3(1.5f, 0, 1.0f));
        RectView viewport = fixture.viewport.layoutBounds();
        MutableRect point = new MutableRect(viewport.x() + drop.x(), viewport.y() + drop.y(), 0, 0);
        fixture.dragTo(fixture.assetLabel("Cube.prefab"), point, PointerButton.PRIMARY);
        UnitySceneObject placed = fixture.tree.selectedNode().userData(UnitySceneObject.class);
        assertNotSame(cube, placed);
        assertNull(placed.parent());
        assertEquals(3, fixture.viewport.scene().roots().size());
        assertEquals(1.5, placed.position(new MutableVec3()).x(), EPSILON);
        assertEquals(1.0, placed.position(new MutableVec3()).z(), EPSILON);
        assertEquals(0, UnityScenePreview.subtreeBounds(placed).minY(), EPSILON);
    }

    /**
     * Ensures Play permits temporary prefab instances and Pause blocks placement.
     * @param directory empty project directory supplied by JUnit
     */
    @Test
    void prefab_playPlacesTemporaryInstanceButPauseDisablesEditing(@TempDir Path directory) {
        Fixture fixture = layout(UnityInspectorScreen.createScreen(directory));
        fixture.rightClick(fixture.tree.roots().get(0).child(1));
        fixture.chooseOpenMenuItem("Save as Prefab");
        fixture.button("▶").click();
        fixture.relayout();
        fixture.clickAt(fixture.assetLabel("Props.prefab"), PointerButton.SECONDARY);
        fixture.chooseOpenMenuItem("Add to Scene");
        assertEquals(3, fixture.viewport.scene().roots().size());
        fixture.rightClick(fixture.tree.roots().get(0).child(1));
        assertFalse(fixture.openMenu().itemWidgets().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> "Save as Prefab".equals(button.text())).findFirst().orElseThrow().enabled());

        fixture.button("❚❚").click();
        fixture.relayout();
        fixture.clickAt(fixture.assetLabel("Props.prefab"), PointerButton.SECONDARY);
        assertFalse(fixture.openMenu().itemWidgets().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> "Add to Scene".equals(button.text())).findFirst().orElseThrow().enabled());
        fixture.button("▶").click();
        assertEquals(2, fixture.viewport.scene().roots().size());
    }

    /**
     * Renames and deletes a real asset through the Project browser, then reopens the folder.
     * @param directory empty project directory supplied by JUnit
     */
    @Test
    void prefab_projectRenameAndDeletePersist(@TempDir Path directory) {
        Fixture fixture = layout(UnityInspectorScreen.createScreen(directory));
        fixture.rightClick(fixture.tree.roots().get(0).child(1));
        fixture.chooseOpenMenuItem("Save as Prefab");
        fixture.relayout();
        fixture.clickAt(fixture.assetLabel("Props.prefab"), PointerButton.SECONDARY);
        fixture.chooseOpenMenuItem("Rename");
        ((WidgetBase) fixture.root).uiContext().dispatcher().drain();
        TextInput field = descendants(fixture.projectPanel(), TextInput.class).stream()
                .filter(input -> "Props.prefab".equals(input.text())).findFirst().orElseThrow();
        field.text("Named Prefab");
        fixture.input.keyPressed(KeyCodes.ENTER, 0, 0);
        fixture.relayout();
        assertNotNull(fixture.assetLabel("Named Prefab.prefab"));

        Fixture reopened = layout(UnityInspectorScreen.createScreen(directory));
        reopened.openPrefabs();
        reopened.clickAt(reopened.assetLabel("Named Prefab.prefab"), PointerButton.SECONDARY);
        reopened.chooseOpenMenuItem("Delete");
        assertEquals(Visibility.COLLAPSED, reopened.preview().visibility());
        Fixture afterDelete = layout(UnityInspectorScreen.createScreen(directory));
        afterDelete.openPrefabs();
        assertFalse(descendants(afterDelete.projectPanel(), Label.class).stream()
                .anyMatch(label -> label.text().endsWith(".prefab")));
    }

    /** @return the laid-out screen with a pointer router and handles to its actual panels */
    private static Fixture layout() {
        return layout(UnityInspectorScreen.createScreen());
    }

    /**
     * Lays out the given editor screen with test input routing.
     * @param screen editor instance to arrange
     * @return fixture connected to its scene and UI
     */
    private static Fixture layout(UniScreen screen) {
        Widget root = screen.root();
        ((WidgetBase) root).setUiContextInternal(screen.context());
        root.measure(new LayoutContext(1100, 700));
        root.arrange(new MutableRect(0, 0, 1100, 700));
        DockingRoot dock = descendants(root, DockingRoot.class).get(0);
        Widget hierarchy = dock.manager().findPane("hierarchy").content();
        Widget inspector = dock.manager().findPane("inspector").content();
        Widget scene = dock.manager().findPane("scene").content();
        Scene3D viewport = descendants(scene, Scene3D.class).get(0);
        assertTrue(viewport.layoutBounds().width() > 0 && viewport.layoutBounds().height() > 0);
        return new Fixture(root, hierarchy, inspector, viewport, descendants(hierarchy, TreeView.class).get(0),
                new InputHandle(root, screen.context()));
    }

    /**
     * Finds widgets after applying queued child mutations.
     * @param widget subtree to search
     * @param type desired widget class
     * @param <T> matching widget type
     * @return widgets in tree order
     */
    private static <T extends Widget> List<T> descendants(Widget widget, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(widget)) result.add(type.cast(widget));
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) result.addAll(descendants(child, type));
        return result;
    }

    /** Active widgets and input for one editor window. */
    private record Fixture(Widget root, Widget hierarchy, Widget inspector, Scene3D viewport,
                           TreeView tree, InputHandle input) {
        /**
         * Routes a complete key press through the editor input handler.
         * @param code normalized key code
         * @param modifiers active modifier mask
         * @return whether the key press was handled
         */
        boolean pressKey(int code, int modifiers) {
            boolean handled = input.keyPressed(code, 0, modifiers);
            input.keyReleased(code, 0, modifiers);
            return handled;
        }

        /** @return the Project browser from the bottom dock */
        Widget projectPanel() {
            return descendants(root, DockingRoot.class).get(0).manager().findPane("project").content();
        }

        /** @return the managed Prefabs folder in the Project tree */
        TreeViewNode prefabsFolder() {
            return descendants(projectPanel(), TreeView.class).get(0).roots().get(0).children().stream()
                    .filter(node -> "Prefabs".equals(node.text())).findFirst().orElseThrow();
        }

        /** Opens the prefab folder and arranges newly loaded asset cards. */
        void openPrefabs() {
            descendants(projectPanel(), TreeView.class).get(0).select(prefabsFolder());
            relayout();
        }

        /**
         * Finds the asset's visible caption in the Project browser.
         * @param name expected filename
         * @return matching label
         */
        Label assetLabel(String name) {
            return descendants(projectPanel(), Label.class).stream().filter(label -> name.equals(label.text()))
                    .findFirst().orElseThrow();
        }

        /** @return Inspector's dedicated prefab preview */
        UnityScenePreview preview() {
            return descendants(inspector, UnityScenePreview.class).get(0);
        }

        /**
         * Clicks the center of a widget using the supplied pointer button.
         * @param widget click target
         * @param button pointer button
         */
        void clickAt(Widget widget, PointerButton button) {
            RectView bounds = widget.layoutBounds();
            float x = bounds.x() + bounds.width() / 2, y = bounds.y() + bounds.height() / 2;
            input.pointerPressed(x, y, 0, button);
            input.pointerReleased(x, y, 0, button);
        }

        /**
         * Starts a drag at the source center and drops it in the center of the given rectangle.
         * @param source widget under the starting pointer
         * @param destination drop area in root coordinates
         * @param button pointer button used by the drag
         */
        void dragTo(Widget source, RectView destination, PointerButton button) {
            RectView bounds = source.layoutBounds();
            float x = bounds.x() + bounds.width() / 2, y = bounds.y() + bounds.height() / 2;
            float dx = destination.x() + destination.width() / 2, dy = destination.y() + destination.height() / 2;
            input.pointerPressed(x, y, 0, button);
            input.pointerMoved(dx, dy, 0);
            input.pointerReleased(dx, dy, 0, button);
        }

        /**
         * Finds a toolbar or Inspector button by its caption.
         * @param label caption to match
         * @return matching button in the editor
         */
        Button button(String label) {
            return descendants(root, Button.class).stream().filter(button -> label.equals(button.text()))
                    .findFirst().orElseThrow();
        }

        /**
         * Draws a pointer gesture along a projected gizmo axis.
         * @param pivot world-space gizmo center
         * @param axis unit handle direction in world space
         * @param amount drag measured in projected handle lengths
         * @return world-space length of the gizmo axis
         */
        float dragAxis(MutableVec3 pivot, MutableVec3 axis, float amount) {
            float radius = handleRadius(pivot);
            MutableVec3 center = project(pivot);
            MutableVec3 handle = project(new MutableVec3().set(pivot)
                    .add(axis.x() * radius, axis.y() * radius, axis.z() * radius));
            dragViewport(handle.x(), handle.y(), amount * (handle.x() - center.x()),
                    amount * (handle.y() - center.y()));
            return radius;
        }

        /**
         * Unprojects a point 62 UI units away at the pivot's depth.
         * @param pivot world-space position of the gizmo
         * @return radius in scene units
         */
        float handleRadius(MutableVec3 pivot) {
            MutableVec3 center = project(pivot);
            MutableVec3 side = new MutableVec3();
            assertTrue(viewport.camera().unproject(center.x() + 62, center.y(), center.z(), side));
            return (float) Math.hypot(Math.hypot(side.x() - pivot.x(), side.y() - pivot.y()), side.z() - pivot.z());
        }

        /**
         * Projects a visible world coordinate into the viewport.
         * @param point coordinate in scene units
         * @return viewport position and depth
         */
        MutableVec3 project(MutableVec3 point) {
            MutableVec3 projected = new MutableVec3();
            assertTrue(viewport.camera().project(point, projected));
            return projected;
        }

        /**
         * Sends a drag in viewport coordinates using actual input routing.
         * @param x start X in UI units
         * @param y start Y in UI units
         * @param dx horizontal movement in UI units
         * @param dy vertical movement in UI units
         */
        void dragViewport(float x, float y, float dx, float dy) {
            RectView bounds = viewport.layoutBounds();
            x += bounds.x();
            y += bounds.y();
            input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
            input.pointerMoved(x + dx, y + dy, 0);
            input.pointerReleased(x + dx, y + dy, 0, PointerButton.PRIMARY);
        }

        /**
         * Opens the hierarchy creation menu and activates a command.
         * @param label command label
         */
        void choose(String label) {
            descendants(hierarchy, UnityCreateButton.class).get(0).click();
            chooseOpenMenuItem(label);
        }

        /** @param label command in the visible context menu */
        void chooseOpenMenuItem(String label) {
            ContextMenu menu = openMenu();
            menu.itemWidgets().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(button -> label.equals(button.text())).findFirst().orElseThrow(() -> new AssertionError(
                            label + " absent in " + menu.itemWidgets().stream().filter(Button.class::isInstance)
                                    .map(Button.class::cast).map(Button::text).toList())).click();
        }

        /** @return the currently opened context menu */
        ContextMenu openMenu() {
            return descendants(root, ContextMenu.class).stream().filter(ContextMenu::opened).findFirst().orElseThrow();
        }

        /** @param row hierarchy row that receives a secondary click */
        void rightClick(TreeViewNode row) {
            relayout();
            RectView bounds = row.rowButton().layoutBounds();
            float x = bounds.x() + bounds.width() / 2, y = bounds.y() + bounds.height() / 2;
            input.pointerPressed(x, y, 0, PointerButton.SECONDARY);
            input.pointerReleased(x, y, 0, PointerButton.SECONDARY);
        }

        /** Updates widget bounds and applies queued mutations. */
        void relayout() {
            root.measure(new LayoutContext(1100, 700));
            root.arrange(new MutableRect(0, 0, 1100, 700));
        }

        /**
         * Sends a primary click in root UI coordinates.
         * @param x horizontal position
         * @param y vertical position
         */
        void click(float x, float y) {
            input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
            input.pointerReleased(x, y, 0, PointerButton.PRIMARY);
        }

        /**
         * Drags a row into a position within another row.
         * @param source row to move
         * @param target destination row
         * @param fraction vertical fraction of the destination row
         */
        void drag(TreeViewNode source, TreeViewNode target, float fraction) {
            relayout();
            RectView from = source.rowButton().layoutBounds();
            RectView to = target.rowButton().layoutBounds();
            float x = from.x() + from.width() / 2;
            float y = from.y() + from.height() / 2;
            float dropY = to.y() + to.height() * fraction;
            input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
            input.pointerMoved(x, dropY, 0);
            input.pointerReleased(x, dropY, 0, PointerButton.PRIMARY);
        }
    }
}
