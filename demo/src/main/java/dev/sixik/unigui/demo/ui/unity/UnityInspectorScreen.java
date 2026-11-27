package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.scene.AmbientLight;
import dev.sixik.unigui.api.scene.DirectionalLight;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.EditorLayout;
import dev.sixik.unigui.editor.EditorPanel;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.editor.ToolbarSection;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.docking.DockLayoutSnapshot;
import dev.sixik.unigui.widgets.docking.DockNode;
import dev.sixik.unigui.widgets.docking.DockSplitOrientation;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.Popup;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextArea;
import dev.sixik.unigui.widgets.interaction.TextInput;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static dev.sixik.unigui.demo.ui.unity.UnityEditorHierarchyView.GameObjectType;

/**
 * Демонстрационный экран полнофункционального редактора Unity с системой Docking:
 * перемещаемые и отделяемые панели Hierarchy, Scene, Game, Inspector, Project и Console,
 * переключение раскладок окон (Default, 2 by 3, Reset), управление Play/Pause
 * и меню Window для восстановления панелей.
 *
 * <p>Окно собирает общий каркас {@link EditorShell}: меню, панель инструментов, докинг и меню Window. Здесь остаётся
 * только то, что относится к Unity: сами панели, тема {@link UnityEditorTheme}, кнопки Play/Layers/Layout.</p>
 */
public final class UnityInspectorScreen {
    /**
     * Modifiers matched by the editor's object and tool shortcuts.
     */
    private static final int SHORTCUT_MODIFIERS = KeyModifiers.CONTROL | KeyModifiers.SHIFT
            | KeyModifiers.ALT | KeyModifiers.SUPER;

    private UnityInspectorScreen() {
    }

    public static void open() {
        createScreen().openGui();
    }

    public static UniScreen createScreen() {
        return createScreen(UnityPrefabStore.DEFAULT_DIRECTORY);
    }

    /**
     * Builds a demo editor using the given project asset directory.
     *
     * @param prefabDirectory backing folder for Assets/Prefabs
     * @return demo screen with its own graph and asset browser
     */
    static UniScreen createScreen(Path prefabDirectory) {
        DefaultUIContext context = new DefaultUIContext();
        context.scaleProvider(new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 6.0f)
                .userScale(1.8f));

        UniScreen[] screen = new UniScreen[1];
        Widget root = buildRoot(prefabDirectory, () -> {
            if (screen[0] != null) screen[0].closeGui();
        });
        screen[0] = new UniScreen("Unity Inspector Demo", root, context)
                .scaleWithGameGui(false)
                .renderPolicy(UiRenderPolicy.vsync()).defaultFont(Fonts.defaultFace());
        return screen[0];
    }

    /**
     * Представления, которые панели создают при сборке каркаса; после сборки они связываются между собой.
     */
    private static final class Views {
        /**
         * Editable graph shared by the Scene and Hierarchy panels.
         */
        final Scene sceneGraph = createSampleScene();
        UnityEditorSceneView scene;
        UnityEditorGameView game;
        UnityEditorHierarchyView hierarchy;
        UnityEditorProjectView project;
        UnityEditorConsoleView console;
        UnityInspectorPanel inspector;
        /**
         * Restores the graph and panels on exiting Play, or null in Edit mode.
         */
        Runnable restorePlayState;
        /**
         * Whether Play is temporarily frozen.
         */
        boolean paused;

        /**
         * Applies the edit lock to every panel that can mutate the shared scene.
         *
         * @param editable whether scene changes are allowed
         */
        void setSceneEditable(boolean editable) {
            scene.setEditable(editable);
            hierarchy.setEditable(editable);
            inspector.setEditable(editable);
            project.setPrefabAccess(editable && restorePlayState == null, editable);
        }

        /**
         * Captures the current scene and editor controls before applying temporary changes.
         *
         * @return action that rolls back Play mode
         */
        Runnable capturePlayState() {
            UnitySceneSnapshot snapshot = new UnitySceneSnapshot(sceneGraph);
            Runnable restoreHierarchy = hierarchy.captureEditorState();
            Runnable restoreComponents = inspector.captureComponentState();
            return () -> {
                snapshot.restore();
                restoreComponents.run();
                restoreHierarchy.run();
                inspector.setTargetObject(hierarchy.selectedObject());
            };
        }
    }

    /**
     * Панель Unity: заголовок, иконка вкладки, область и доля окна при открытии из меню Window.
     */
    private record UnityPanel(String id, String title, PanelSlot slot, float share, UiIcon icon,
                              Function<EditorContext, Widget> factory) implements EditorPanel {
        @Override
        public Widget create(EditorContext context) {
            return factory.apply(context);
        }
    }

    /**
     * Builds the docked editor and connects the scene, hierarchy, assets and Inspector.
     *
     * @param prefabDirectory backing folder for Assets/Prefabs
     * @param closeAction     closes the editor window
     * @return editor shell root
     */
    private static Widget buildRoot(Path prefabDirectory, Runnable closeAction) {
        Views views = new Views();
        EditorShell shell = EditorShell.builder()
                .theme(new UnityEditorTheme())
                .title("Unity 2022.3 - SampleScene - PC, Mac & Linux Standalone <DX11>")
                .menus("File", "Edit", "Assets", "GameObject", "Component", "Window", "Help")
                .panel(new UnityPanel("scene", "Scene", PanelSlot.CENTER, 0.5f, UnityIcons.SCENE_TAB_ICON,
                        context -> views.scene = new UnityEditorSceneView(views.sceneGraph,
                                views.sceneGraph.roots().get(0), node -> {
                            views.project.clearPrefabSelection();
                            views.hierarchy.selectFromViewport(node);
                        },
                                object -> views.inspector.refreshTransform(object))))
                .panel(new UnityPanel("game", "Game", PanelSlot.CENTER, 0.5f, UnityIcons.GAME_TAB_ICON,
                        context -> views.game = new UnityEditorGameView(views.sceneGraph)))
                .panel(new UnityPanel("hierarchy", "Hierarchy", PanelSlot.LEFT, 0.22f, UnityIcons.HIERARCHY_TAB_ICON,
                        context -> views.hierarchy = new UnityEditorHierarchyView(views.sceneGraph,
                                context.require(OverlayLayer.class))))
                .panel(new UnityPanel("inspector", "Inspector", PanelSlot.RIGHT, 0.30f, UnityIcons.INSPECTOR_TAB_ICON,
                        context -> views.inspector = new UnityInspectorPanel(context.require(OverlayLayer.class),
                                object -> views.hierarchy.refreshObject(object))))
                .panel(new UnityPanel("project", "Project", PanelSlot.BOTTOM, 0.35f, UnityIcons.PROJECT_TAB_ICON,
                        context -> views.project = new UnityEditorProjectView(context.require(OverlayLayer.class),
                                new UnityPrefabStore(prefabDirectory))))
                .panel(new UnityPanel("console", "Console", PanelSlot.BOTTOM, 0.35f, UnityIcons.CONSOLE_TAB_ICON,
                        context -> views.console = new UnityEditorConsoleView()))
                .layout(defaultLayout())
                .layout(EditorLayout.custom("2 by 3", UnityInspectorScreen::buildTwoByThreeLayoutSnapshot))
                .menuBarLeading(owner -> UnityIcons.gameObjectCube(14.0f))
                .menuBarTrailing(owner -> closeButton(closeAction))
                .toolbar(ToolbarSection.CENTER, owner -> playGroup(views, owner.overlay()))
                .toolbar(ToolbarSection.RIGHT, owner -> layersButton())
                .toolbar(ToolbarSection.RIGHT, UnityInspectorScreen::layoutButton)
                .build();

        views.hierarchy.onObjectSelected(object -> {
            views.project.clearPrefabSelection();
            views.scene.selectObject(object);
            views.inspector.setTargetObject(object);
        });
        views.hierarchy.onVisibilityChanged(views.inspector::refreshVisibility);
        views.hierarchy.onSavePrefab(views.project::canSavePrefabs, views.project::savePrefab);
        views.scene.onPlacePrefab(views.hierarchy::placePrefab);
        views.project.connectPrefabs(views.hierarchy::selectedObject, views.hierarchy::draggedObject,
                views.inspector::showPrefab, prefab -> views.hierarchy.placePrefab(prefab, null));
        views.inspector.setTargetObject(null);
        Widget root = shell.root();
        if (root instanceof WidgetBase base) base.focusable(true);
        root.on(KeyPressedEvent.TYPE, event -> {
            if (event.phase() != EventPhase.CAPTURE && handleSceneShortcut(event, views)) event.cancel();
        });
        return root;
    }

    /**
     * Dispatches shortcuts after the focused panel has handled the key.
     *
     * @param event routed keyboard input
     * @param views coordinated editor panels
     * @return whether the command executed
     */
    private static boolean handleSceneShortcut(KeyPressedEvent event, Views views) {
        for (Widget target = event.target(); target != null; target = target.parent()) {
            if (target instanceof TextInput || target instanceof TextArea || target instanceof ContextMenu
                    || target instanceof Popup || target instanceof WindowWidget window && window.modal()
                    || target == views.project) return false;
        }
        int modifiers = event.modifiers() & SHORTCUT_MODIFIERS;
        if (modifiers == KeyModifiers.CONTROL) {
            return switch (event.keyCode()) {
                case KeyCodes.C -> views.hierarchy.copySelected();
                case KeyCodes.V -> views.hierarchy.paste();
                case KeyCodes.D -> views.hierarchy.duplicateSelected();
                default -> false;
            };
        }
        if (modifiers != 0) return false;
        if (event.keyCode() == KeyCodes.DELETE) return views.hierarchy.deleteSelected();
        // The normalized key codes for letters match uppercase ASCII.
        UnitySceneToolbar.Tool tool = switch (event.keyCode()) {
            case 'R' -> UnitySceneToolbar.Tool.ROTATE;
            case 'S' -> UnitySceneToolbar.Tool.SCALE;
            case 'T' -> UnitySceneToolbar.Tool.MOVE;
            default -> null;
        };
        if (tool == null) return false;
        views.scene.selectTool(tool);
        return true;
    }

    /**
     * Creates an editable sample with nested props and a ground plane.
     *
     * @return the initial scene shown by a new editor window
     */
    private static Scene createSampleScene() {
        Scene scene = new Scene().setAmbientLight(new AmbientLight(0xDBE8FF, 0.48f))
                .setDirectionalLight(new DirectionalLight(-0.5f, -0.8f, -0.35f, 0xFFF1DB, 0.75f));

        scene.addRoot(new UnitySceneObject("Ground", GameObjectType.PLANE).setScale(10, 1, 9));
        UnitySceneObject props = new UnitySceneObject("Props", GameObjectType.EMPTY);
        UnitySceneObject cube = new UnitySceneObject("Cube", GameObjectType.CUBE);
        cube.setPosition(-1.35f, 0.5f, 0.3f);
        cube.setAngles(0, 20, 0);
        props.addChild(cube);
        props.addChild(new UnitySceneObject("Sphere", GameObjectType.SPHERE).setPosition(0.1f, 0.5f, 0));
        props.addChild(new UnitySceneObject("Capsule", GameObjectType.CAPSULE).setPosition(1.55f, 1, -0.4f));
        scene.addRoot(props);
        return scene;
    }

    private static EditorLayout defaultLayout() {
        return EditorLayout.named("Default")
                .left(0.18f, "hierarchy")
                .right(0.30f, "inspector")
                .bottom(0.35f, "project", "console")
                .center("scene", "game")
                .active("scene")
                .build();
    }

    private static DockLayoutSnapshot buildTwoByThreeLayoutSnapshot() {
        DockLayoutSnapshot.Node col1 = new DockLayoutSnapshot.Node(
                DockNode.Kind.SPLIT, DockSplitOrientation.VERTICAL, 0.50f, List.of(), "",
                new DockLayoutSnapshot.Node(DockNode.Kind.LEAF, null, 1.0f, List.of("scene"), "scene", null, null),
                new DockLayoutSnapshot.Node(DockNode.Kind.LEAF, null, 1.0f, List.of("game"), "game", null, null));
        DockLayoutSnapshot.Node col2 = new DockLayoutSnapshot.Node(
                DockNode.Kind.SPLIT, DockSplitOrientation.VERTICAL, 0.50f, List.of(), "",
                new DockLayoutSnapshot.Node(DockNode.Kind.LEAF, null, 1.0f, List.of("hierarchy"), "hierarchy", null, null),
                new DockLayoutSnapshot.Node(DockNode.Kind.LEAF, null, 1.0f, List.of("project", "console"), "project", null, null));
        DockLayoutSnapshot.Node col1And2 = new DockLayoutSnapshot.Node(
                DockNode.Kind.SPLIT, DockSplitOrientation.HORIZONTAL, 0.52f, List.of(), "", col1, col2);
        DockLayoutSnapshot.Node rootNode = new DockLayoutSnapshot.Node(
                DockNode.Kind.SPLIT, DockSplitOrientation.HORIZONTAL, 0.72f, List.of(), "", col1And2,
                new DockLayoutSnapshot.Node(DockNode.Kind.LEAF, null, 1.0f, List.of("inspector"), "inspector", null, null));
        return new DockLayoutSnapshot(rootNode, "scene");
    }

    private static Button closeButton(Runnable closeAction) {
        Button closeBtn = new Button("✕ Close Demo");
        closeBtn.themeEnabled(false);
        closeBtn.backgroundVisible(true);
        closeBtn.background().set(UnityStyles.BG_BUTTON);
        closeBtn.borderVisible(true);
        closeBtn.borderColor().set(UnityStyles.BORDER_BUTTON);
        closeBtn.radius(2.0f);
        closeBtn.focusRingVisible(false);
        closeBtn.focusable(false);
        closeBtn.textColor().set(UnityStyles.TEXT_BOLD);
        closeBtn.layout(style -> style.size(90.0f, 18.0f));
        closeBtn.onClick(e -> closeAction.run());
        return closeBtn;
    }

    /**
     * Builds Play and Pause controls for the shared scene and its Game preview.
     *
     * @param views   editor panels affected by a play state change
     * @param overlay host for menus that may contain pending scene actions
     * @return transport controls in toolbar order
     */
    private static HBox playGroup(Views views, OverlayLayer overlay) {
        HBox playGroup = new HBox();
        playGroup.spacing(2.0f);
        playGroup.layout(style -> style.height(22.0f).alignItems(Align.CENTER));

        Button playBtn = transportButton("▶");
        Button pauseBtn = transportButton("❚❚");
        pauseBtn.enabled(false);

        playBtn.onClick(e -> {
            finishEditorInput(overlay);
            views.setSceneEditable(false);
            boolean enteringPlay = views.restorePlayState == null;
            if (enteringPlay) {
                views.restorePlayState = views.capturePlayState();
            } else {
                views.restorePlayState.run();
                views.restorePlayState = null;
                views.paused = false;
            }
            views.setSceneEditable(true);
            playBtn.background().set(enteringPlay ? UnityStyles.PLAY_GREEN : UnityStyles.BG_INPUT);
            pauseBtn.enabled(enteringPlay);
            pauseBtn.background().set(UnityStyles.BG_INPUT);
            views.game.setPlaying(enteringPlay);
            views.console.onPlayModeChanged(enteringPlay);
        });

        pauseBtn.onClick(e -> {
            if (views.restorePlayState == null) return;
            finishEditorInput(overlay);
            views.paused = !views.paused;
            views.setSceneEditable(!views.paused);
            pauseBtn.background().set(views.paused ? UnityStyles.BG_BUTTON_ACTIVE : UnityStyles.BG_INPUT);
            views.game.setPaused(views.paused);
        });

        Button stepBtn = transportButton("⏭");

        playGroup.addChild(playBtn);
        playGroup.addChild(pauseBtn);
        playGroup.addChild(stepBtn);
        return playGroup;
    }

    /**
     * Commits focused input and closes popups before the edit state changes.
     *
     * @param overlay host containing the editor input context and open menus
     */
    private static void finishEditorInput(OverlayLayer overlay) {
        if (overlay.uiContext() != null) overlay.uiContext().focusManager().clearFocus();
        overlay.applyQueuedMutations();
        for (Widget child : List.copyOf(overlay.children())) {
            if (child instanceof ContextMenu menu && menu.opened()) menu.close();
            if (child instanceof Popup popup && popup.opened()) popup.close();
        }
    }

    private static Button transportButton(String text) {
        Button button = new Button(text);
        button.themeEnabled(false);
        button.backgroundVisible(true);
        button.background().set(UnityStyles.BG_INPUT);
        button.borderVisible(true);
        button.borderColor().set(UnityStyles.BORDER_INPUT);
        button.radius(2.0f);
        button.focusRingVisible(false);
        button.focusable(false);
        button.textColor().set(UnityStyles.TEXT_PRIMARY);
        button.layout(style -> style.size(28.0f, 22.0f));
        return button;
    }

    private static Button layersButton() {
        Button layersBtn = new Button("Layers v");
        UnityStyles.styleButton(layersBtn);
        layersBtn.focusRingVisible(false);
        layersBtn.focusable(false);
        layersBtn.layout(style -> style.height(22.0f).padding(8.0f, 0.0f));
        return layersBtn;
    }

    /**
     * Кнопка раскладок: Default, 2 by 3 и Reset Layout.
     */
    private static Button layoutButton(EditorShell shell) {
        Button layoutBtn = new Button("Default v");
        UnityStyles.styleButton(layoutBtn);
        layoutBtn.focusRingVisible(false);
        layoutBtn.focusable(false);
        layoutBtn.layout(style -> style.height(22.0f).padding(8.0f, 0.0f));

        layoutBtn.onClick(e -> {
            ContextMenu menu = new ContextMenu();
            UnityStyles.styleContextMenu(menu);
            float menuWidth = 130.0f;
            UnityStyles.addContextMenuItem(menu, "Default", true, menuWidth, () -> {
                shell.applyLayout("Default");
                layoutBtn.text("Default v");
            });
            UnityStyles.addContextMenuItem(menu, "2 by 3", true, menuWidth, () -> {
                shell.applyLayout("2 by 3");
                layoutBtn.text("2 by 3 v");
            });
            UnityStyles.addContextMenuSeparator(menu, menuWidth);
            UnityStyles.addContextMenuItem(menu, "Reset Layout", true, menuWidth, () -> {
                shell.applyLayout("Default");
                layoutBtn.text("Default v");
            });
            shell.overlay().addOverlay(menu);
            RectView b = layoutBtn.layoutBounds();
            menu.openAt(b.x() + b.width() - menuWidth, b.y() + b.height() + 2.0f);
        });
        return layoutBtn;
    }
}
