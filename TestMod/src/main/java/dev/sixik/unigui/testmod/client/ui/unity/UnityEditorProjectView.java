package dev.sixik.unigui.testmod.client.ui.unity;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.TreeDisclosureStyle;
import dev.sixik.unigui.api.style.TreeRowStyle;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.SplitPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.ScrollBar;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.widgets.navigation.TreeView;
import dev.sixik.unigui.widgets.navigation.TreeViewNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Панель Project в стиле редактора Unity:
 * двухколоночный браузер ассетов с компонентом TreeView слева,
 * интерактивным SplitPanel разделителем для изменения ширины колонок,
 * сеткой файлов справа, хлебными крошками, строкой поиска и строкой состояния.
 */
public class UnityEditorProjectView extends LinearBox {
    private final Label breadcrumbLabel = new Label("Assets > Scenes >");
    private final Label statusLabel = new Label("2 items");
    private final TreeView folderTreeView = new TreeView();
    /** Содержимое правой панели: строки списка или обёртывающая сетка карточек. */
    private final VBox assetContent = new VBox();
    /** Слайдер размера иконок слева от которого (до порога) панель превращается в список. */
    private static final float LIST_THRESHOLD = 0.04f;
    private static final float MIN_ICON_SIZE = 32.0f;
    private static final float MAX_ICON_SIZE = 96.0f;
    private static final float LIST_ROW_HEIGHT = 18.0f;
    private float iconScale = 0.45f;
    private String searchQuery = "";
    private final Map<String, List<AssetItem>> folderContents = new HashMap<>();
    private final Map<String, TreeViewNode> treeNodes = new HashMap<>();
    private final Tooltip assetTooltip = new Tooltip();
    private OverlayLayer gridOverlay;

    private String currentFolder = "Scenes";
    private String selectedAsset = "fwf";

    public record AssetItem(String name, String type, String icon) {
        public AssetItem(String name, String type) {
            this(name, type, "");
        }
    }

    public UnityEditorProjectView() {
        super(Orientation.VERTICAL);
        spacing(0.0f);
        layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));

        initSampleData();

        // 1. Верхняя панель: Хлебные крошки и поиск
        HBox topBar = new HBox();
        topBar.spacing(6.0f);
        topBar.layout(style -> style.widthPercent(100.0f).height(24.0f)
                .padding(6.0f, 2.0f, 6.0f, 2.0f).alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        Button createBtn = new Button("+ v");
        UnityStyles.styleButton(createBtn);
        createBtn.focusRingVisible(false);
        createBtn.focusable(false);
        createBtn.layout(style -> style.height(20.0f).padding(4.0f, 0.0f));
        topBar.addChild(createBtn);

        breadcrumbLabel.color(UnityStyles.TEXT_PRIMARY);
        breadcrumbLabel.layout(style -> style.height(20.0f).padding(4.0f, 0.0f));
        topBar.addChild(breadcrumbLabel);

        Box spacer = new Box();
        spacer.themeEnabled(false);
        spacer.layout(style -> style.flexGrow(1.0f).height(1.0f));
        topBar.addChild(spacer);

        TextInput searchInput = new TextInput("");
        searchInput.placeholder("Search...");
        UnityStyles.styleTextInput(searchInput);
        searchInput.layout(style -> style.width(140.0f).height(20.0f));
        searchInput.onTextChanged(e -> {
            searchQuery = e.newText() == null ? "" : e.newText().trim().toLowerCase();
            loadFolder(currentFolder);
        });
        topBar.addChild(searchInput);

        addChild(topBar);
        addChild(UnityStyles.separatorLine());

        // 2. Настройка TreeView для дерева папок
        folderTreeView.style(style -> style
                .put(StyleKeys.TREE_ROW_HEIGHT, UnityStyles.ROW_HEIGHT)
                .put(StyleKeys.TREE_ROW_STYLE, TreeRowStyle.FULL_WIDTH)
                .put(StyleKeys.TREE_DISCLOSURE_STYLE, TreeDisclosureStyle.TRIANGLE)
                .put(StyleKeys.TREE_ROW_RADIUS, 0.0f)
                .put(StyleKeys.TREE_ROW_INSET_X, 0.0f)
                .put(StyleKeys.TREE_ROW_INSET_Y, 0.0f)
                .put(StyleKeys.TREE_INDENT_WIDTH, 14.0f)
                .put(StyleKeys.TREE_FOLDER_FILL_ENABLED, true)
                .put(StyleKeys.TREE_SELECTED_COLOR, UnityStyles.SELECTION_BLUE)
                .put(StyleKeys.TREE_HOVER_COLOR, UnityStyles.BG_HEADER_HOVER)
                .put(StyleKeys.TREE_TEXT_COLOR, UnityStyles.TEXT_PRIMARY)
                .put(StyleKeys.TREE_SELECTED_TEXT_COLOR, UnityStyles.TEXT_BOLD)
                .put(StyleKeys.TREE_DISABLED_TEXT_COLOR, UnityStyles.TEXT_MUTED)
                .put(StyleKeys.TREE_DISCLOSURE_COLOR, UnityStyles.TEXT_MUTED)
                .put(StyleKeys.TREE_ICON_COLOR, UnityStyles.TEXT_PRIMARY));
        folderTreeView.layout(style -> style.widthPercent(100.0f));
        buildFolderTree();

        folderTreeView.onSelectionChanged(e -> {
            TreeViewNode selected = folderTreeView.selectedNode();
            if (selected != null) {
                loadFolder(selected.text());
            }
        });

        ScrollView folderScroll = new ScrollView(folderTreeView);
        folderScroll.overlayScrollBars(false);
        folderScroll.layout(style -> style.sizePercent(100.0f, 100.0f));

        // 3. Сетка ассетов справа
        assetContent.spacing(0.0f);
        assetContent.layout(style -> style.widthPercent(100.0f).flexGrow(0.0f).flexShrink(0.0f));
        ScrollView gridScroll = new ScrollView(assetContent);
        gridScroll.overlayScrollBars(false);
        gridScroll.scrollStep(24.0f);
        gridScroll.scrollbarGap(2.0f);
        gridScroll.scrollbarTrackColor().set(UnityStyles.BORDER_DEFAULT);
        gridScroll.scrollbarThumbColor().set(UnityStyles.TEXT_MUTED);
        gridScroll.verticalScrollBar().thumbHoverColor().set(UnityStyles.TEXT_PRIMARY);
        gridScroll.layout(style -> style.sizePercent(100.0f, 100.0f));
        // Клик по пустому месту панели снимает выделение; карточки и строки гасят своё нажатие сами.
        gridScroll.on(PointerPressedEvent.TYPE, e -> {
            if (e.button() != PointerButton.PRIMARY || e.isCancelled() || selectedAsset == null) return;
            for (Widget w = e.target(); w != null; w = w.parent()) {
                if (w instanceof ScrollBar) return;
            }
            selectedAsset = null;
            loadFolder(currentFolder);
        });

        gridOverlay = new OverlayLayer(gridScroll);
        gridOverlay.layout(style -> style.sizePercent(100.0f, 100.0f));
        UnityStyles.styleTooltip(assetTooltip);
        assetTooltip.offset(0.0f, 4.0f);
        gridOverlay.addOverlay(assetTooltip);

        // 4. SplitPanel с перемещаемым разделителем между деревом папок и сеткой ассетов
        SplitPanel splitPanel = new SplitPanel(folderScroll, gridOverlay);
        splitPanel.orientation(Orientation.HORIZONTAL);
        splitPanel.splitRatio(0.24f);
        splitPanel.minFirstSize(80.0f);
        splitPanel.minSecondSize(120.0f);
        splitPanel.splitterThickness(4.0f);
        splitPanel.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));

        // Стилизация разделителя под тёмную тему Unity
        splitPanel.splitter().backgroundVisible(true);
        splitPanel.splitter().background().set(UnityStyles.BORDER_DEFAULT);
        splitPanel.splitter().handleColor().set(UnityStyles.BORDER_SUBTLE);

        addChild(splitPanel);
        addChild(UnityStyles.separatorLine());

        // 5. Нижняя строка состояния
        HBox statusBar = new HBox();
        statusBar.spacing(8.0f);
        statusBar.layout(style -> style.widthPercent(100.0f).height(18.0f)
                .padding(6.0f, 0.0f, 6.0f, 0.0f).alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        statusLabel.color(UnityStyles.TEXT_MUTED);
        statusLabel.layout(style -> style.height(18.0f));
        statusBar.addChild(statusLabel);

        Box statusSpacer = new Box();
        statusSpacer.themeEnabled(false);
        statusSpacer.layout(style -> style.flexGrow(1.0f).height(1.0f));
        statusBar.addChild(statusSpacer);

        // Слайдер размера иконок: в крайнем левом положении правая панель становится списком.
        Slider sizeSlider = new Slider();
        sizeSlider.themeEnabled(false);
        sizeSlider.min(0.0f);
        sizeSlider.max(1.0f);
        sizeSlider.value(iconScale);
        sizeSlider.trackColor().set(UnityStyles.BORDER_SUBTLE);
        sizeSlider.fillColor().set(UnityStyles.BORDER_SUBTLE);
        sizeSlider.knobColor().set(UnityStyles.TEXT_MUTED);
        sizeSlider.layout(style -> style.width(72.0f).height(14.0f).flexGrow(0.0f).flexShrink(0.0f));
        sizeSlider.onValueChanged(e -> {
            iconScale = e.newValue();
            loadFolder(currentFolder);
        });
        statusBar.addChild(sizeSlider);

        addChild(statusBar);

        // Выбрать "Scenes" по умолчанию
        TreeViewNode scenesNode = treeNodes.get("Scenes");
        if (scenesNode != null) {
            folderTreeView.select(scenesNode);
        } else {
            loadFolder("Scenes");
        }
    }

    private void initSampleData() {
        folderContents.put("Assets", List.of(
                new AssetItem("Scenes", "folder"),
                new AssetItem("Settings", "folder"),
                new AssetItem("TutorialInfo", "folder"),
                new AssetItem("Scripts", "folder"),
                new AssetItem("Materials", "folder"),
                new AssetItem("Prefabs", "folder"),
                new AssetItem("Textures", "folder")
        ));
        folderContents.put("Scenes", List.of(
                new AssetItem("fwf", "folder"),
                new AssetItem("SampleScene", "scene")
        ));
        folderContents.put("fwf", List.of());
        folderContents.put("Settings", List.of(
                new AssetItem("ProjectSettings.asset", "script")
        ));
        folderContents.put("TutorialInfo", List.of(
                new AssetItem("Readme.asset", "script")
        ));
        folderContents.put("Scripts", List.of(
                new AssetItem("PlayerController.cs", "script"),
                new AssetItem("CameraController.cs", "script"),
                new AssetItem("GameManager.cs", "script")
        ));
        folderContents.put("Materials", List.of(
                new AssetItem("PlayerMaterial.mat", "material"),
                new AssetItem("GroundGrid.mat", "material")
        ));
        folderContents.put("Prefabs", List.of(
                new AssetItem("Player.prefab", "prefab"),
                new AssetItem("Enemy.prefab", "prefab")
        ));
        folderContents.put("Textures", List.of(
                new AssetItem("SkyboxTexture.png", "texture"),
                new AssetItem("GridTexture.png", "texture")
        ));
        folderContents.put("Packages", List.of(
                new AssetItem("UniGUI.core", "package"),
                new AssetItem("UniGUI.docking", "package")
        ));
    }

    private void buildFolderTree() {
        folderTreeView.clearRoots();
        treeNodes.clear();

        // Assets root
        TreeViewNode assetsRoot = folderTreeView.addRoot("Assets");
        assetsRoot.expanded(true);
        assetsRoot.folder(true);
        treeNodes.put("Assets", assetsRoot);

        // Scenes has a child 'fwf', so it's a non-empty folder (filled icon)
        TreeViewNode scenesNode = assetsRoot.addChild("Scenes");
        scenesNode.expanded(true);
        scenesNode.folder(true);
        treeNodes.put("Scenes", scenesNode);

        // fwf is an empty leaf folder, so emptyFolder(true) makes it render as outline
        TreeViewNode fwfNode = scenesNode.addChild("fwf");
        fwfNode.folder(true);
        fwfNode.emptyFolder(true);
        treeNodes.put("fwf", fwfNode);

        TreeViewNode settingsNode = assetsRoot.addChild("Settings");
        settingsNode.folder(true);
        settingsNode.emptyFolder(false);
        treeNodes.put("Settings", settingsNode);

        TreeViewNode tutorialNode = assetsRoot.addChild("TutorialInfo");
        tutorialNode.folder(true);
        tutorialNode.emptyFolder(false);
        treeNodes.put("TutorialInfo", tutorialNode);

        for (String folder : List.of("Scripts", "Materials", "Prefabs", "Textures")) {
            TreeViewNode child = assetsRoot.addChild(folder);
            child.folder(true);
            treeNodes.put(folder, child);
        }

        // Packages root
        TreeViewNode packagesRoot = folderTreeView.addRoot("Packages");
        packagesRoot.expanded(true);
        packagesRoot.folder(true);
        treeNodes.put("Packages", packagesRoot);
    }

    private void loadFolder(String folderName) {
        this.currentFolder = folderName;
        breadcrumbLabel.text("Assets > " + folderName + " >");
        assetContent.clearChildren();

        List<AssetItem> items = new ArrayList<>();
        for (AssetItem item : folderContents.getOrDefault(folderName, List.of())) {
            if (searchQuery.isEmpty() || item.name().toLowerCase().contains(searchQuery)) {
                items.add(item);
            }
        }
        statusLabel.text(items.size() + (items.size() == 1 ? " item" : " items"));

        if (iconScale <= LIST_THRESHOLD) {
            for (AssetItem item : items) {
                assetContent.addChild(createListRow(item));
            }
        } else {
            float iconSize = MIN_ICON_SIZE + (MAX_ICON_SIZE - MIN_ICON_SIZE)
                    * (iconScale - LIST_THRESHOLD) / (1.0f - LIST_THRESHOLD);
            HBox grid = new HBox();
            grid.spacing(8.0f);
            grid.layout(style -> style.widthPercent(100.0f).padding(8.0f, 8.0f).flexWrap(FlexWrap.WRAP));
            for (AssetItem item : items) {
                grid.addChild(createAssetCard(item, iconSize));
            }
            assetContent.addChild(grid);
        }

        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    /** Клик по ассету: выбор, а повторный клик по выбранной папке открывает её. */
    private void onAssetPressed(AssetItem item, boolean selected) {
        if ("folder".equals(item.type()) && selected) {
            TreeViewNode targetNode = treeNodes.get(item.name());
            if (targetNode != null) {
                folderTreeView.select(targetNode);
            } else {
                loadFolder(item.name());
            }
        } else {
            selectedAsset = item.name();
            loadFolder(currentFolder);
        }
    }

    /** Строка списка: маленькая иконка и имя, выделение на всю ширину. */
    private HBox createListRow(AssetItem item) {
        boolean selected = item.name().equals(selectedAsset);
        HBox row = new HBox();
        row.themeEnabled(false);
        row.borderVisible(false);
        row.spacing(4.0f);
        // Сначала цвет: background(color) сам включает видимость фона, поэтому флаг ставится после него.
        row.background(UnityStyles.SELECTION_BLUE);
        row.backgroundVisible(selected);
        row.layout(style -> style.widthPercent(100.0f).height(LIST_ROW_HEIGHT)
                .padding(8.0f, 0.0f, 8.0f, 0.0f).alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        boolean emptyFolder = "folder".equals(item.type()) && isFolderEmpty(item.name());
        CanvasWidget icon = new CanvasWidget();
        icon.layout(style -> style.size(16.0f, 16.0f).flexGrow(0.0f).flexShrink(0.0f));
        icon.onDraw(context -> {
            DrawScope draw = new DrawScope(context, icon.transform(), icon.layoutBounds());
            UnityIcons.drawProjectItemIcon(draw, item.type(), emptyFolder,
                    icon.layoutBounds().x(), icon.layoutBounds().y(),
                    icon.layoutBounds().width(), icon.layoutBounds().height());
        });
        row.addChild(icon);

        Label nameLbl = new Label(item.name());
        nameLbl.color(selected ? UnityStyles.TEXT_BOLD : UnityStyles.TEXT_PRIMARY);
        nameLbl.overflowMode(TextOverflowMode.ELLIPSIS);
        nameLbl.layout(style -> style.height(LIST_ROW_HEIGHT).flexGrow(1.0f).flexShrink(1.0f));
        row.addChild(nameLbl);

        row.on(PointerPressedEvent.TYPE, e -> {
            if (e.button() == PointerButton.PRIMARY) {
                onAssetPressed(item, selected);
                e.cancel();
            }
        });
        return row;
    }
    private boolean isFolderEmpty(String folderName) {
        List<AssetItem> items = folderContents.get(folderName);
        return items == null || items.isEmpty();
    }

    private LinearBox createAssetCard(AssetItem item, float iconSize) {
        float cardWidth = Math.max(56.0f, iconSize + 12.0f);
        LinearBox card = new LinearBox(Orientation.VERTICAL);
        card.themeEnabled(false);
        card.borderVisible(false);
        card.spacing(4.0f);
        card.layout(style -> style.size(cardWidth, iconSize + 32.0f).padding(4.0f, 4.0f)
                .alignItems(Align.CENTER).flexGrow(0.0f).flexShrink(0.0f));

        boolean selected = item.name().equals(selectedAsset);
        card.backgroundVisible(false);

        boolean emptyFolder = "folder".equals(item.type()) && isFolderEmpty(item.name());
        CanvasWidget iconCanvas = new CanvasWidget();
        iconCanvas.layout(style -> style.size(iconSize, iconSize).flexGrow(0.0f).flexShrink(0.0f));
        iconCanvas.onDraw(context -> {
            DrawScope draw = new DrawScope(context, iconCanvas.transform(), iconCanvas.layoutBounds());
            UnityIcons.drawProjectItemIcon(draw, item.type(), emptyFolder,
                    iconCanvas.layoutBounds().x(), iconCanvas.layoutBounds().y(),
                    iconCanvas.layoutBounds().width(), iconCanvas.layoutBounds().height());
        });
        card.addChild(iconCanvas);

        Label nameLbl = new Label(item.name());
        nameLbl.textAlignment(Alignment.CENTER);
        nameLbl.overflowMode(TextOverflowMode.ELLIPSIS);
        float labelMax = cardWidth - 4.0f;
        nameLbl.backgroundVisible(selected);
        if (selected) {
            nameLbl.background().set(UnityStyles.SELECTION_BLUE);
            nameLbl.radius(3.0f);
            nameLbl.color(UnityStyles.TEXT_BOLD);
        } else {
            nameLbl.color(UnityStyles.TEXT_PRIMARY);
        }
        nameLbl.layout(style -> style.padding(5.0f, 1.0f).maxWidth(labelMax).alignSelf(Align.CENTER));
        card.addChild(nameLbl);

        card.on(PointerPressedEvent.TYPE, e -> {
            if (e.button() == PointerButton.PRIMARY) {
                onAssetPressed(item, selected);
                e.cancel();
            }
        });

        return card;
    }
}