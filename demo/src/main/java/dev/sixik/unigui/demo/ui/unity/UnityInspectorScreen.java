package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.render.UiRenderPolicy;
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
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.docking.DockLayoutSnapshot;
import dev.sixik.unigui.widgets.docking.DockNode;
import dev.sixik.unigui.widgets.docking.DockSplitOrientation;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;

import java.util.List;
import java.util.function.Function;

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
    private UnityInspectorScreen() {
    }

    public static void open() {
        createScreen().openGui();
    }

    public static UniScreen createScreen() {
        DefaultUIContext context = new DefaultUIContext();
        context.scaleProvider(new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 6.0f)
                .userScale(1.8f));

        UniScreen[] screen = new UniScreen[1];
        Widget root = buildRoot(() -> {
            if (screen[0] != null) screen[0].closeGui();
        });
        screen[0] = new UniScreen("Unity Inspector Demo", root, context)
                .scaleWithGameGui(false)
                .renderPolicy(UiRenderPolicy.vsync()).defaultFont(Fonts.defaultFace());
        return screen[0];
    }

    /** Представления, которые панели создают при сборке каркаса; после сборки они связываются между собой. */
    private static final class Views {
        UnityEditorSceneView scene;
        UnityEditorGameView game;
        UnityEditorHierarchyView hierarchy;
        UnityEditorProjectView project;
        UnityEditorConsoleView console;
        UnityInspectorPanel inspector;
    }

    /** Панель Unity: заголовок, иконка вкладки, область и доля окна при открытии из меню Window. */
    private record UnityPanel(String id, String title, PanelSlot slot, float share, UiIcon icon,
                              Function<EditorContext, Widget> factory) implements EditorPanel {
        @Override
        public Widget create(EditorContext context) {
            return factory.apply(context);
        }
    }

    private static Widget buildRoot(Runnable closeAction) {
        Views views = new Views();
        EditorShell shell = EditorShell.builder()
                .theme(new UnityEditorTheme())
                .title("Unity 2022.3 - SampleScene - PC, Mac & Linux Standalone <DX11>")
                .menus("File", "Edit", "Assets", "GameObject", "Component", "Window", "Help")
                .panel(new UnityPanel("scene", "Scene", PanelSlot.CENTER, 0.5f, UnityIcons.SCENE_TAB_ICON,
                        context -> views.scene = new UnityEditorSceneView()))
                .panel(new UnityPanel("game", "Game", PanelSlot.CENTER, 0.5f, UnityIcons.GAME_TAB_ICON,
                        context -> views.game = new UnityEditorGameView()))
                .panel(new UnityPanel("hierarchy", "Hierarchy", PanelSlot.LEFT, 0.22f, UnityIcons.HIERARCHY_TAB_ICON,
                        context -> views.hierarchy = new UnityEditorHierarchyView()))
                .panel(new UnityPanel("inspector", "Inspector", PanelSlot.RIGHT, 0.30f, UnityIcons.INSPECTOR_TAB_ICON,
                        context -> views.inspector = new UnityInspectorPanel(context.require(OverlayLayer.class))))
                .panel(new UnityPanel("project", "Project", PanelSlot.BOTTOM, 0.35f, UnityIcons.PROJECT_TAB_ICON,
                        context -> views.project = new UnityEditorProjectView(context.require(OverlayLayer.class))))
                .panel(new UnityPanel("console", "Console", PanelSlot.BOTTOM, 0.35f, UnityIcons.CONSOLE_TAB_ICON,
                        context -> views.console = new UnityEditorConsoleView()))
                .layout(defaultLayout())
                .layout(EditorLayout.custom("2 by 3", UnityInspectorScreen::buildTwoByThreeLayoutSnapshot))
                .menuBarLeading(owner -> UnityIcons.gameObjectCube(14.0f))
                .menuBarTrailing(owner -> closeButton(closeAction))
                .toolbar(ToolbarSection.CENTER, owner -> playGroup(views))
                .toolbar(ToolbarSection.RIGHT, owner -> layersButton())
                .toolbar(ToolbarSection.RIGHT, UnityInspectorScreen::layoutButton)
                .build();

        // Связывание Inspector со Scene View и Hierarchy с Inspector
        views.inspector.onFovChanged(views.scene::updateCameraFov);
        views.inspector.onPositionChanged(pos -> views.scene.updateCameraPosition(pos[0], pos[1], pos[2]));
        views.hierarchy.onObjectSelected(views.inspector::setTargetObject);
        return shell.root();
    }

    private static EditorLayout defaultLayout() {
        return EditorLayout.named("Default")
                .left(0.18f, "hierarchy")
                .right(0.28f, "inspector")
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

    /** Play ▶, Pause ❚❚, Step ⏭: управляют Game View и Console. */
    private static HBox playGroup(Views views) {
        HBox playGroup = new HBox();
        playGroup.spacing(2.0f);
        playGroup.layout(style -> style.height(22.0f).alignItems(Align.CENTER));

        boolean[] isPlaying = {false};
        boolean[] isPaused = {false};

        Button playBtn = transportButton("▶");
        Button pauseBtn = transportButton("❚❚");

        playBtn.onClick(e -> {
            isPlaying[0] = !isPlaying[0];
            playBtn.background().set(isPlaying[0] ? UnityStyles.PLAY_GREEN : UnityStyles.BG_INPUT);
            if (!isPlaying[0]) {
                isPaused[0] = false;
                pauseBtn.background().set(UnityStyles.BG_INPUT);
            }
            views.game.setPlaying(isPlaying[0]);
            views.console.onPlayModeChanged(isPlaying[0]);
        });

        pauseBtn.onClick(e -> {
            if (!isPlaying[0]) return;
            isPaused[0] = !isPaused[0];
            pauseBtn.background().set(isPaused[0] ? UnityStyles.BG_BUTTON_ACTIVE : UnityStyles.BG_INPUT);
            views.game.setPlaying(!isPaused[0]);
        });

        Button stepBtn = transportButton("⏭");

        playGroup.addChild(playBtn);
        playGroup.addChild(pauseBtn);
        playGroup.addChild(stepBtn);
        return playGroup;
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

    /** Кнопка раскладок: Default, 2 by 3 и Reset Layout. */
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
