package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.input.InputHandle;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.demo.snapshot.SnapshotRenderer;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.interaction.Button;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders the Unity-like scene editor to {@code demo/build/snapshots} with several objects selected, light gizmos and
 * each draw mode, so the 2D overlays and the top bar can be checked without the game. The Java2D renderer has no 3D
 * pass, so shading and the lighting toggle themselves are not visible here.
 */
class UnitySceneEditorSnapshotsTest {
    /** Logical window width. */
    private static final float WIDTH = 1100.0f;
    /** Logical window height. */
    private static final float HEIGHT = 620.0f;
    /** Image pixels per logical pixel. */
    private static final float SCALE = 2.0f;
    /** Output folder. */
    private static final Path OUT = Path.of("build", "snapshots");

    /** @throws IOException if a file was not written */
    @Test
    void sceneEditorModes() throws IOException {
        for (UnitySceneViewOptions.DrawMode mode : UnitySceneViewOptions.DrawMode.values()) {
            UniScreen screen = UnityInspectorScreen.createScreen();
            Widget root = screen.root();
            SnapshotRenderer renderer = new SnapshotRenderer(root, WIDTH, HEIGHT, SCALE);
            for (Scene3D scene3D : descendants(root, Scene3D.class)) scene3D.visible(false);
            renderer.settle(3);
            DockingRoot dock = descendants(root, DockingRoot.class).get(0);
            Widget hierarchyPanel = dock.manager().findPane("hierarchy").content();
            UnityEditorHierarchyView hierarchy = descendants(hierarchyPanel, UnityEditorHierarchyView.class).get(0);
            UnityEditorSceneView sceneView = descendants(dock.manager().findPane("scene").content(),
                    UnityEditorSceneView.class).get(0);

            choose(root, hierarchyPanel, "Light > Spot Light");
            choose(root, hierarchyPanel, "Light > Directional Light");
            choose(root, hierarchyPanel, "Light > Point Light");
            UnitySceneObject point = hierarchy.selectedObject();
            InputHandle input = new InputHandle(root, renderer.context());
            List<UnitySceneObject> shapes = objects(sceneView.viewport().scene(), "Cube", "Sphere");
            hierarchy.selectFromViewport(shapes.get(0));
            input.keyPressed(KeyCodes.LEFT_CONTROL, 0, 0);
            hierarchy.selectFromViewport(shapes.get(1));
            hierarchy.selectFromViewport(point);
            input.keyReleased(KeyCodes.LEFT_CONTROL, 0, 0);
            sceneView.updateOptions(options -> {
                options.drawMode = mode;
                options.snap = true;
                options.lighting = mode != UnitySceneViewOptions.DrawMode.WIREFRAME;
            });
            // The Java2D backend cannot create render targets, so the viewport is hidden (it keeps its layout space)
            // and its painter, which draws the wireframe, light gizmos and handles, is rendered separately on top.
            Scene3D viewport = sceneView.viewport();
            renderer.settle(3);
            String name = "unity-scene-" + mode.name().toLowerCase().replace('_', '-');
            Path file = OUT.resolve(name + ".png");
            renderer.write(file);
            RectView bounds = viewport.layoutBounds();
            Box overlay = new Box() {
                @Override
                public void render(RenderContext context) {
                    super.render(context);
                    viewport.painter().paint(context, bounds.width(), bounds.height());
                }
            };
            overlay.themeEnabled(false);
            overlay.backgroundVisible(true);
            overlay.background().set(MutableColor.rgba255(74, 78, 86, 255));
            Path overlayFile = OUT.resolve(name + "-viewport.png");
            new SnapshotRenderer(overlay, bounds.width(), bounds.height(), SCALE).write(overlayFile);
            BufferedImage image = ImageIO.read(file.toFile());
            Graphics2D graphics = image.createGraphics();
            graphics.drawImage(ImageIO.read(overlayFile.toFile()), Math.round(bounds.x() * SCALE),
                    Math.round(bounds.y() * SCALE), null);
            graphics.dispose();
            ImageIO.write(image, "png", file.toFile());
            Files.delete(overlayFile);
            assertTrue(Files.size(file) > 0);
        }
    }

    /**
     * The Inspector of a cube with its real components and the Add Component popup filtered by a search.
     *
     * @throws IOException if the file was not written
     */
    @Test
    void addComponentPopup() throws IOException {
        UniScreen screen = UnityInspectorScreen.createScreen();
        Widget root = screen.root();
        SnapshotRenderer renderer = new SnapshotRenderer(root, WIDTH, HEIGHT, SCALE);
        for (Scene3D scene3D : descendants(root, Scene3D.class)) scene3D.visible(false);
        renderer.settle(3);
        DockingRoot dock = descendants(root, DockingRoot.class).get(0);
        UnityEditorHierarchyView hierarchy = descendants(dock.manager().findPane("hierarchy").content(),
                UnityEditorHierarchyView.class).get(0);
        UnityEditorSceneView sceneView = descendants(dock.manager().findPane("scene").content(),
                UnityEditorSceneView.class).get(0);
        hierarchy.selectFromViewport(objects(sceneView.viewport().scene(), "Cube").get(0));
        renderer.settle(2);
        UnityInspectorPanel inspector = descendants(dock.manager().findPane("inspector").content(),
                UnityInspectorPanel.class).get(0);
        UnityAddComponentWindow popup = inspector.openComponentPicker();
        renderer.settle(2);
        popup.search().text("r");
        for (Scene3D scene3D : descendants(root, Scene3D.class)) scene3D.visible(false);
        renderer.settle(3);
        Path file = OUT.resolve("unity-add-component.png");
        renderer.write(file);
        assertTrue(Files.size(file) > 0);
    }

    /**
     * The Inspector of a cube carrying every optional component: header checkboxes (Rotator switched off), the light
     * color picker filling the row, and the Material Effect section. A tall window keeps all sections on screen.
     *
     * @throws IOException if the file was not written
     */
    @Test
    void componentsInspector() throws IOException {
        UniScreen screen = UnityInspectorScreen.createScreen();
        Widget root = screen.root();
        SnapshotRenderer renderer = new SnapshotRenderer(root, WIDTH, 1100, SCALE);
        for (Scene3D scene3D : descendants(root, Scene3D.class)) scene3D.visible(false);
        renderer.settle(3);
        DockingRoot dock = descendants(root, DockingRoot.class).get(0);
        UnityEditorHierarchyView hierarchy = descendants(dock.manager().findPane("hierarchy").content(),
                UnityEditorHierarchyView.class).get(0);
        UnityEditorSceneView sceneView = descendants(dock.manager().findPane("scene").content(),
                UnityEditorSceneView.class).get(0);
        UnitySceneObject cube = objects(sceneView.viewport().scene(), "Cube").get(0);
        cube.setLight(new UnityLight(UnityLight.Kind.POINT, 0xFFB040, 1.5f, 8, 30, 21.8f));
        cube.setRotatorSpeed(45);
        cube.setComponentEnabled(UnitySceneObject.Component.ROTATOR, false);
        cube.setMaterialEffectSettings(UnityMaterialEffect.defaults(UnityMaterialEffect.Preset.HOLOGRAM));
        hierarchy.selectFromViewport(cube);
        for (Scene3D scene3D : descendants(root, Scene3D.class)) scene3D.visible(false);
        renderer.settle(3);
        Path file = OUT.resolve("unity-components.png");
        renderer.write(file);
        assertTrue(Files.size(file) > 0);
    }

    /**
     * A selected box fog volume beside an unselected fog sphere: the Fog Volume Inspector section, the bright box
     * bounds, the faint sphere ellipses and both cloud icons drawn by the viewport painter.
     *
     * @throws IOException if the file was not written
     */
    @Test
    void fogVolume() throws IOException {
        UniScreen screen = UnityInspectorScreen.createScreen();
        Widget root = screen.root();
        SnapshotRenderer renderer = new SnapshotRenderer(root, WIDTH, 900, SCALE);
        for (Scene3D scene3D : descendants(root, Scene3D.class)) scene3D.visible(false);
        renderer.settle(3);
        DockingRoot dock = descendants(root, DockingRoot.class).get(0);
        Widget hierarchyPanel = dock.manager().findPane("hierarchy").content();
        UnityEditorSceneView sceneView = descendants(dock.manager().findPane("scene").content(),
                UnityEditorSceneView.class).get(0);
        choose(root, hierarchyPanel, "Effects > Fog Volume (Sphere)");
        choose(root, hierarchyPanel, "Effects > Fog Volume (Box)");
        for (Scene3D scene3D : descendants(root, Scene3D.class)) scene3D.visible(false);
        renderer.settle(3);
        Path file = OUT.resolve("unity-fog-volume.png");
        renderer.write(file);
        Scene3D viewport = sceneView.viewport();
        RectView bounds = viewport.layoutBounds();
        Box overlay = new Box() {
            @Override
            public void render(RenderContext context) {
                super.render(context);
                viewport.painter().paint(context, bounds.width(), bounds.height());
            }
        };
        overlay.themeEnabled(false);
        overlay.backgroundVisible(true);
        overlay.background().set(MutableColor.rgba255(74, 78, 86, 255));
        Path overlayFile = OUT.resolve("unity-fog-volume-viewport.png");
        new SnapshotRenderer(overlay, bounds.width(), bounds.height(), SCALE).write(overlayFile);
        BufferedImage image = ImageIO.read(file.toFile());
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(ImageIO.read(overlayFile.toFile()), Math.round(bounds.x() * SCALE),
                Math.round(bounds.y() * SCALE), null);
        graphics.dispose();
        ImageIO.write(image, "png", file.toFile());
        Files.delete(overlayFile);
        assertTrue(Files.size(file) > 0);
    }

    /**
     * @param scene scene graph
     * @param names object names
     * @return the first object with each name
     */
    private static List<UnitySceneObject> objects(Scene scene, String... names) {
        List<UnitySceneObject> all = new ArrayList<>();
        for (SceneNode root : scene.roots()) collect(root, all);
        List<UnitySceneObject> result = new ArrayList<>();
        for (String name : names) {
            all.stream().filter(object -> name.equals(object.name())).findFirst().ifPresent(result::add);
        }
        return result;
    }

    /**
     * @param node subtree
     * @param out  receives editor objects
     */
    private static void collect(SceneNode node, List<UnitySceneObject> out) {
        if (node instanceof UnitySceneObject object) out.add(object);
        for (SceneNode child : node.children()) collect(child, out);
    }

    /**
     * Picks a command from the hierarchy create menu.
     *
     * @param root           screen root
     * @param hierarchyPanel hierarchy pane content
     * @param label          menu item
     */
    private static void choose(Widget root, Widget hierarchyPanel, String label) {
        descendants(hierarchyPanel, UnityCreateButton.class).get(0).click();
        ContextMenu menu = descendants(root, ContextMenu.class).stream().filter(ContextMenu::opened)
                .findFirst().orElseThrow();
        menu.itemWidgets().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> label.equals(button.text())).findFirst().orElseThrow().click();
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
}
