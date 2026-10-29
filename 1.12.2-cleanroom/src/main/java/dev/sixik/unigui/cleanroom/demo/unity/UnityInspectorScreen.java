package dev.sixik.unigui.cleanroom.demo.unity;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.input.MemoryClipboardService;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.docking.*;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Демонстрационный экран полнофункционального редактора Unity с системой Docking:
 * перемещаемые и отделяемые панели Hierarchy, Scene, Game, Inspector, Project и Console,
 * переключение раскладок окон (Default, 2 by 3, Reset), управление Play/Pause
 * и меню Window для восстановления панелей.
 */
public final class UnityInspectorScreen {
    private UnityInspectorScreen() {
    }

    public static void open() {
        Minecraft.getMinecraft().addScheduledTask(UnityInspectorScreen::openClient);
    }

    private static void openClient() {

        DefaultUIContext context = new DefaultUIContext(new MemoryClipboardService());
        context.scaleProvider(new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 6.0f)
                .userScale(1.8f));
        context.debugFlags(DebugFlags.PROFILER_OVERLAY);

        Runnable[] close = new Runnable[1];
        Widget root = buildRoot(() -> close[0].run());

        UniScreen screen = new UniScreen(
                "Unity Inspector Demo", root, context);
        screen.onClose(close[0]);

        screen.scaleWithGameGui(false);
        screen.defaultFont(Fonts.defaultFace());

        screen.renderPolicy(UiRenderPolicy.vsync());
//        minecraft.setScreen(screen);
        screen.openGui();
    }

    private static Widget buildRoot(Runnable closeAction) {
        StackPanel viewport = new StackPanel();
        viewport.layout(style -> style.sizePercent(100.0f, 100.0f).overflow(Overflow.HIDDEN));

        // Фоновый слой
        Box bg = new Box();
        bg.themeEnabled(false);
        bg.backgroundVisible(true);
        bg.background().set(UnityStyles.BG_WORKSPACE);
        bg.borderVisible(false);
        bg.layout(style -> style.sizePercent(100.0f, 100.0f));
        viewport.addChild(bg);

        // Оверлей для меню, выпадающих списков и плавающих окон докинга
        OverlayLayer overlayLayer = new OverlayLayer();
        overlayLayer.layout(style -> style.sizePercent(100.0f, 100.0f));

        VBox editorRoot = new VBox();
        editorRoot.spacing(0.0f);
        editorRoot.layout(style -> style.sizePercent(100.0f, 100.0f));

        // 1. Создание всех представлений редактора
        UnityEditorSceneView sceneView = new UnityEditorSceneView();
        UnityEditorGameView gameView = new UnityEditorGameView();
        UnityEditorHierarchyView hierarchyView = new UnityEditorHierarchyView();
        UnityEditorProjectView projectView = new UnityEditorProjectView(overlayLayer);
        UnityEditorConsoleView consoleView = new UnityEditorConsoleView();
        UnityInspectorPanel inspectorPanel = new UnityInspectorPanel(overlayLayer);

        // Связывание Inspector со Scene View
        inspectorPanel.onFovChanged(sceneView::updateCameraFov);
        inspectorPanel.onPositionChanged(pos -> sceneView.updateCameraPosition(pos[0], pos[1], pos[2]));

        // Связывание Hierarchy с Inspector
        hierarchyView.onObjectSelected(inspectorPanel::setTargetObject);

        // 2. Создание DockingRoot и настройка визуальной темы Unity
        DockingRoot dockingRoot = new DockingRoot();
        dockingRoot.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f)
                .flexGrow(1.0f).flexShrink(1.0f));

        DockTheme theme = DockTheme.editor();
        theme.color(DockColor.STRIP_BACKGROUND).set(UnityStyles.BG_WORKSPACE);
        theme.color(DockColor.STRIP_BORDER).set(UnityStyles.BORDER_DEFAULT);
        theme.color(DockColor.TAB_BACKGROUND).set(UnityStyles.BG_TAB_INACTIVE);
        theme.color(DockColor.TAB_HOVER).set(UnityStyles.BG_TAB_ACTIVE);
        theme.color(DockColor.TAB_SELECTED).set(UnityStyles.BG_TAB_ACTIVE);
        theme.color(DockColor.TAB_SELECTED_INACTIVE).set(UnityStyles.BG_TAB_INACTIVE);
        theme.color(DockColor.TAB_ACCENT).set(UnityStyles.TAB_ACCENT);
        theme.color(DockColor.TAB_ACCENT_INACTIVE).set(UnityStyles.TAB_ACCENT);
        theme.color(DockColor.TAB_TEXT).set(UnityStyles.TEXT_PRIMARY);
        theme.color(DockColor.TAB_TEXT_MUTED).set(UnityStyles.TEXT_MUTED);
        theme.color(DockColor.CONTENT_BACKGROUND).set(UnityStyles.BG_WINDOW);
        theme.color(DockColor.PANE_BORDER).set(UnityStyles.BORDER_DEFAULT);
        theme.color(DockColor.SPLIT).set(UnityStyles.BORDER_DEFAULT);
        theme.color(DockColor.SPLIT_HOVER).set(UnityStyles.SELECTION_BLUE);
        theme.color(DockColor.SPLIT_PRESSED).set(UnityStyles.SELECTION_BLUE);
        theme.splitStyle(DockingRoot.SplitHandleStyle.SEAM);
        theme.splitThickness(3.0f);
        theme.tabHeight(22.0f);

        dockingRoot.theme(theme);
        dockingRoot.tabHeight(22.0f);
        dockingRoot.splitHandleThickness(3.0f);
        dockingRoot.splitHandleStyle(DockingRoot.SplitHandleStyle.SEAM);
        dockingRoot.tearOffOnDragOut(true);

        // Создание панелей с иконками вкладок
        DockPane scenePane = DockPane.document("scene", "Scene", sceneView).icon(UnityIcons.SCENE_TAB_ICON);
        DockPane gamePane = DockPane.document("game", "Game", gameView).icon(UnityIcons.GAME_TAB_ICON);
        DockPane hierarchyPane = DockPane.tool("hierarchy", "Hierarchy", hierarchyView).icon(UnityIcons.HIERARCHY_TAB_ICON);
        DockPane inspectorPane = DockPane.tool("inspector", "Inspector", inspectorPanel).icon(UnityIcons.INSPECTOR_TAB_ICON);
        DockPane projectPane = DockPane.tool("project", "Project", projectView).icon(UnityIcons.PROJECT_TAB_ICON);
        DockPane consolePane = DockPane.tool("console", "Console", consoleView).icon(UnityIcons.CONSOLE_TAB_ICON);

        Map<String, DockPane> paneRegistry = new HashMap<>();
        paneRegistry.put("scene", scenePane);
        paneRegistry.put("game", gamePane);
        paneRegistry.put("hierarchy", hierarchyPane);
        paneRegistry.put("inspector", inspectorPane);
        paneRegistry.put("project", projectPane);
        paneRegistry.put("console", consolePane);

        // Загрузка начальной раскладки Default
        dockingRoot.restoreLayout(buildDefaultLayoutSnapshot(), paneRegistry);

        // 3. Верхнее меню Unity (File, Edit, Assets, GameObject, Window, Help)
        editorRoot.addChild(buildTopMenuBar(closeAction, dockingRoot, paneRegistry, overlayLayer));
        editorRoot.addChild(UnityStyles.separatorLine());

        // 4. Тулбар Unity (Инструменты, Play/Pause/Step, Layers, Layout)
        editorRoot.addChild(buildToolbar(dockingRoot, paneRegistry, overlayLayer, gameView, consoleView));
        editorRoot.addChild(UnityStyles.separatorLine());

        // 5. Область докинга
        editorRoot.addChild(dockingRoot);

        overlayLayer.content(editorRoot);
        viewport.addChild(overlayLayer);

        return viewport;
    }

    private static DockLayoutSnapshot buildDefaultLayoutSnapshot() {
        DockLayoutSnapshot.Node sceneGameLeaf = new DockLayoutSnapshot.Node(
                DockNode.Kind.LEAF, null, 1.0f, List.of("scene", "game"), "scene", null, null);
        DockLayoutSnapshot.Node projectConsoleLeaf = new DockLayoutSnapshot.Node(
                DockNode.Kind.LEAF, null, 1.0f, List.of("project", "console"), "project", null, null);
        DockLayoutSnapshot.Node centerSplit = new DockLayoutSnapshot.Node(
                DockNode.Kind.SPLIT, DockSplitOrientation.VERTICAL, 0.65f, List.of(), "", sceneGameLeaf, projectConsoleLeaf);
        DockLayoutSnapshot.Node inspectorLeaf = new DockLayoutSnapshot.Node(
                DockNode.Kind.LEAF, null, 1.0f, List.of("inspector"), "inspector", null, null);
        DockLayoutSnapshot.Node centerRightSplit = new DockLayoutSnapshot.Node(
                DockNode.Kind.SPLIT, DockSplitOrientation.HORIZONTAL, 0.72f, List.of(), "", centerSplit, inspectorLeaf);
        DockLayoutSnapshot.Node hierarchyLeaf = new DockLayoutSnapshot.Node(
                DockNode.Kind.LEAF, null, 1.0f, List.of("hierarchy"), "hierarchy", null, null);
        DockLayoutSnapshot.Node rootNode = new DockLayoutSnapshot.Node(
                DockNode.Kind.SPLIT, DockSplitOrientation.HORIZONTAL, 0.18f, List.of(), "", hierarchyLeaf, centerRightSplit);
        return new DockLayoutSnapshot(rootNode, "scene");
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

    private static void openOrFocusPane(DockingRoot dockingRoot, String id, Map<String, DockPane> registry) {
        if (dockingRoot.findPane(id) != null) {
            dockingRoot.selectPane(id);
            return;
        }
        DockPane pane = registry.get(id);
        if (pane == null) return;
        switch (id) {
            case "scene" -> {
                if (dockingRoot.findPane("game") != null) dockingRoot.tabPane("game", pane);
                else dockingRoot.addPane(pane);
            }
            case "game" -> {
                if (dockingRoot.findPane("scene") != null) dockingRoot.tabPane("scene", pane);
                else dockingRoot.addPane(pane);
            }
            case "hierarchy" -> {
                if (dockingRoot.findPane("scene") != null) dockingRoot.splitPane("scene", DockArea.LEFT, pane, 0.22f);
                else dockingRoot.addPane(pane);
            }
            case "inspector" -> {
                if (dockingRoot.findPane("scene") != null) dockingRoot.splitPane("scene", DockArea.RIGHT, pane, 0.70f);
                else dockingRoot.addPane(pane);
            }
            case "project" -> {
                if (dockingRoot.findPane("console") != null) dockingRoot.tabPane("console", pane);
                else if (dockingRoot.findPane("scene") != null) dockingRoot.splitPane("scene", DockArea.BOTTOM, pane, 0.65f);
                else dockingRoot.addPane(pane);
            }
            case "console" -> {
                if (dockingRoot.findPane("project") != null) dockingRoot.tabPane("project", pane);
                else if (dockingRoot.findPane("scene") != null) dockingRoot.splitPane("scene", DockArea.BOTTOM, pane, 0.65f);
                else dockingRoot.addPane(pane);
            }
            default -> dockingRoot.addPane(pane);
        }
        dockingRoot.selectPane(id);
    }

    private static HBox buildTopMenuBar(Runnable closeAction, DockingRoot dockingRoot,
                                        Map<String, DockPane> paneRegistry, OverlayLayer overlayLayer) {
        HBox menuBar = new HBox();
        menuBar.spacing(8.0f);
        menuBar.layout(style -> style.widthPercent(100.0f).height(22.0f)
                .padding(8.0f, 0.0f, 8.0f, 0.0f)
                .alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        // Иконка куба Unity
        menuBar.addChild(UnityIcons.gameObjectCube(14.0f));

        // Пункты меню
        String[] standardItems = new String[]{"File", "Edit", "Assets", "GameObject", "Component"};
        for (String item : standardItems) {
            Button itemBtn = createMenuItemButton(item);
            menuBar.addChild(itemBtn);
        }

        // Меню Window с выпадающим списком окон
        Button windowBtn = createMenuItemButton("Window");
        windowBtn.onClick(e -> {
            ContextMenu menu = new ContextMenu();
            UnityStyles.styleContextMenu(menu);
            float menuWidth = 145.0f;
            UnityStyles.addContextMenuItem(menu, "Scene", true, menuWidth, () -> openOrFocusPane(dockingRoot, "scene", paneRegistry));
            UnityStyles.addContextMenuItem(menu, "Game", true, menuWidth, () -> openOrFocusPane(dockingRoot, "game", paneRegistry));
            UnityStyles.addContextMenuItem(menu, "Hierarchy", true, menuWidth, () -> openOrFocusPane(dockingRoot, "hierarchy", paneRegistry));
            UnityStyles.addContextMenuItem(menu, "Inspector", true, menuWidth, () -> openOrFocusPane(dockingRoot, "inspector", paneRegistry));
            UnityStyles.addContextMenuItem(menu, "Project", true, menuWidth, () -> openOrFocusPane(dockingRoot, "project", paneRegistry));
            UnityStyles.addContextMenuItem(menu, "Console", true, menuWidth, () -> openOrFocusPane(dockingRoot, "console", paneRegistry));
            overlayLayer.addOverlay(menu);
            RectView b = windowBtn.layoutBounds();
            menu.openAt(b.x(), b.y() + b.height() + 2.0f);
        });
        menuBar.addChild(windowBtn);

        Button helpBtn = createMenuItemButton("Help");
        menuBar.addChild(helpBtn);

        // Заголовок проекта по центру
        Label projectTitle = new Label("Unity 2022.3 - SampleScene - PC, Mac & Linux Standalone <DX11>");
        projectTitle.color(UnityStyles.TEXT_MUTED);
        projectTitle.layout(style -> style.height(20.0f).padding(16.0f, 0.0f));
        menuBar.addChild(projectTitle);

        Box spacer = new Box();
        spacer.themeEnabled(false);
        spacer.layout(style -> style.flexGrow(1.0f).height(1.0f));
        menuBar.addChild(spacer);

        // Кнопка закрытия демо-экрана
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
        menuBar.addChild(closeBtn);

        return menuBar;
    }

    private static Button createMenuItemButton(String text) {
        Button itemBtn = new Button(text);
        itemBtn.themeEnabled(false);
        itemBtn.boxVisualEnabled(false);
        itemBtn.focusRingVisible(false);
        itemBtn.focusable(false);
        itemBtn.textColor().set(UnityStyles.TEXT_PRIMARY);
        itemBtn.layout(style -> style.height(20.0f).padding(5.0f, 0.0f));
        return itemBtn;
    }

    /**
     * Главная панель Unity: Play/Pause/Step по центру, слои и раскладка справа. Инструменты трансформации,
     * Center/Local и сетка живут внутри панели Scene ({@link UnitySceneToolbar}), как в оригинале.
     */
    private static HBox buildToolbar(DockingRoot dockingRoot, Map<String, DockPane> paneRegistry,
                                     OverlayLayer overlayLayer, UnityEditorGameView gameView,
                                     UnityEditorConsoleView consoleView) {
        HBox toolbar = new HBox();
        toolbar.spacing(8.0f);
        toolbar.layout(style -> style.widthPercent(100.0f).height(30.0f)
                .padding(8.0f, 0.0f, 8.0f, 0.0f)
                .alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        Box spacer1 = new Box();
        spacer1.themeEnabled(false);
        spacer1.layout(style -> style.flexGrow(1.0f).height(1.0f));
        toolbar.addChild(spacer1);

        // Центральная секция: Play ▶, Pause ❚❚, Step ⏭
        HBox playGroup = new HBox();
        playGroup.spacing(2.0f);
        playGroup.layout(style -> style.height(22.0f).alignItems(Align.CENTER));

        boolean[] isPlaying = {false};
        boolean[] isPaused = {false};

        Button playBtn = new Button("▶");
        playBtn.themeEnabled(false);
        playBtn.backgroundVisible(true);
        playBtn.background().set(UnityStyles.BG_INPUT);
        playBtn.borderVisible(true);
        playBtn.borderColor().set(UnityStyles.BORDER_INPUT);
        playBtn.radius(2.0f);
        playBtn.focusRingVisible(false);
        playBtn.focusable(false);
        playBtn.textColor().set(UnityStyles.TEXT_PRIMARY);
        playBtn.layout(style -> style.size(28.0f, 22.0f));

        Button pauseBtn = new Button("❚❚");
        pauseBtn.themeEnabled(false);
        pauseBtn.backgroundVisible(true);
        pauseBtn.background().set(UnityStyles.BG_INPUT);
        pauseBtn.borderVisible(true);
        pauseBtn.borderColor().set(UnityStyles.BORDER_INPUT);
        pauseBtn.radius(2.0f);
        pauseBtn.focusRingVisible(false);
        pauseBtn.focusable(false);
        pauseBtn.textColor().set(UnityStyles.TEXT_PRIMARY);
        pauseBtn.layout(style -> style.size(28.0f, 22.0f));

        playBtn.onClick(e -> {
            isPlaying[0] = !isPlaying[0];
            playBtn.background().set(isPlaying[0] ? UnityStyles.PLAY_GREEN : UnityStyles.BG_INPUT);
            if (!isPlaying[0]) {
                isPaused[0] = false;
                pauseBtn.background().set(UnityStyles.BG_INPUT);
            }
            gameView.setPlaying(isPlaying[0]);
            consoleView.onPlayModeChanged(isPlaying[0]);
        });

        pauseBtn.onClick(e -> {
            if (!isPlaying[0]) return;
            isPaused[0] = !isPaused[0];
            pauseBtn.background().set(isPaused[0] ? UnityStyles.BG_BUTTON_ACTIVE : UnityStyles.BG_INPUT);
            gameView.setPlaying(!isPaused[0]);
        });

        Button stepBtn = new Button("⏭");
        stepBtn.themeEnabled(false);
        stepBtn.backgroundVisible(true);
        stepBtn.background().set(UnityStyles.BG_INPUT);
        stepBtn.borderVisible(true);
        stepBtn.borderColor().set(UnityStyles.BORDER_INPUT);
        stepBtn.radius(2.0f);
        stepBtn.focusRingVisible(false);
        stepBtn.focusable(false);
        stepBtn.textColor().set(UnityStyles.TEXT_PRIMARY);
        stepBtn.layout(style -> style.size(28.0f, 22.0f));

        playGroup.addChild(playBtn);
        playGroup.addChild(pauseBtn);
        playGroup.addChild(stepBtn);
        toolbar.addChild(playGroup);

        Box spacer2 = new Box();
        spacer2.themeEnabled(false);
        spacer2.layout(style -> style.flexGrow(1.0f).height(1.0f));
        toolbar.addChild(spacer2);

        // Правая часть: слои и раскладка
        Button layersBtn = new Button("Layers v");
        UnityStyles.styleButton(layersBtn);
        layersBtn.focusRingVisible(false);
        layersBtn.focusable(false);
        layersBtn.layout(style -> style.height(22.0f).padding(8.0f, 0.0f));

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
                dockingRoot.restoreLayout(buildDefaultLayoutSnapshot(), paneRegistry);
                layoutBtn.text("Default v");
            });
            UnityStyles.addContextMenuItem(menu, "2 by 3", true, menuWidth, () -> {
                dockingRoot.restoreLayout(buildTwoByThreeLayoutSnapshot(), paneRegistry);
                layoutBtn.text("2 by 3 v");
            });
            UnityStyles.addContextMenuSeparator(menu, menuWidth);
            UnityStyles.addContextMenuItem(menu, "Reset Layout", true, menuWidth, () -> {
                dockingRoot.restoreLayout(buildDefaultLayoutSnapshot(), paneRegistry);
                layoutBtn.text("Default v");
            });
            overlayLayer.addOverlay(menu);
            RectView b = layoutBtn.layoutBounds();
            menu.openAt(b.x() + b.width() - menuWidth, b.y() + b.height() + 2.0f);
        });

        toolbar.addChild(layersBtn);
        toolbar.addChild(layoutBtn);

        return toolbar;
    }
}
