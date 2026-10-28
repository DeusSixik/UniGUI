package dev.sixik.unigui.testmod.client.ui.unity;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.TreeDisclosureStyle;
import dev.sixik.unigui.api.style.TreeRowStyle;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.SplitPanel;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
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
    private final Label breadcrumbLabel = new Label("Assets > Scenes");
    private final Label statusLabel = new Label("1 item");
    private final TreeView folderTreeView = new TreeView();
    private final HBox assetGrid = new HBox();
    private final Map<String, List<AssetItem>> folderContents = new HashMap<>();
    private final Map<String, TreeViewNode> treeNodes = new HashMap<>();

    private String currentFolder = "Scenes";
    private String selectedAsset = "SampleScene.unity";

    public record AssetItem(String name, String type, String icon) {}

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
        assetGrid.spacing(8.0f);
        assetGrid.layout(style -> style.widthPercent(100.0f).padding(8.0f, 8.0f).flexWrap(FlexWrap.WRAP));
        ScrollView gridScroll = new ScrollView(assetGrid);
        gridScroll.overlayScrollBars(false);
        gridScroll.layout(style -> style.sizePercent(100.0f, 100.0f));

        // 4. SplitPanel с перемещаемым разделителем между деревом папок и сеткой ассетов
        SplitPanel splitPanel = new SplitPanel(folderScroll, gridScroll);
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
                new AssetItem("Scenes", "folder", "📁"),
                new AssetItem("Scripts", "folder", "📁"),
                new AssetItem("Materials", "folder", "📁"),
                new AssetItem("Prefabs", "folder", "📁"),
                new AssetItem("Textures", "folder", "📁")
        ));
        folderContents.put("Scenes", List.of(
                new AssetItem("SampleScene.unity", "scene", "🎬")
        ));
        folderContents.put("Scripts", List.of(
                new AssetItem("PlayerController.cs", "script", "📄"),
                new AssetItem("CameraController.cs", "script", "📄"),
                new AssetItem("GameManager.cs", "script", "📄")
        ));
        folderContents.put("Materials", List.of(
                new AssetItem("PlayerMaterial.mat", "material", "🔮"),
                new AssetItem("GroundGrid.mat", "material", "🔮")
        ));
        folderContents.put("Prefabs", List.of(
                new AssetItem("Player.prefab", "prefab", "🧊"),
                new AssetItem("Enemy.prefab", "prefab", "🧊")
        ));
        folderContents.put("Textures", List.of(
                new AssetItem("SkyboxTexture.png", "texture", "🖼"),
                new AssetItem("GridTexture.png", "texture", "🖼")
        ));
        folderContents.put("Packages", List.of(
                new AssetItem("UniGUI.core", "package", "📦"),
                new AssetItem("UniGUI.docking", "package", "📦")
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

        for (String folder : List.of("Scenes", "Scripts", "Materials", "Prefabs", "Textures")) {
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
        breadcrumbLabel.text("Assets > " + folderName);
        assetGrid.clearChildren();

        List<AssetItem> items = folderContents.getOrDefault(folderName, List.of());
        statusLabel.text(items.size() + (items.size() == 1 ? " item" : " items"));

        for (AssetItem item : items) {
            assetGrid.addChild(createAssetCard(item));
        }

        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private LinearBox createAssetCard(AssetItem item) {
        LinearBox card = new LinearBox(Orientation.VERTICAL);
        card.themeEnabled(false);
        card.borderVisible(false);
        card.spacing(2.0f);
        card.layout(style -> style.size(76.0f, 74.0f).padding(4.0f, 4.0f).alignItems(Align.CENTER));

        boolean selected = item.name.equals(selectedAsset);
        card.backgroundVisible(selected);
        if (selected) {
            card.background().set(UnityStyles.SELECTION_BLUE);
        }

        Label iconLbl = new Label(item.icon);
        iconLbl.layout(style -> style.size(36.0f, 36.0f).alignItems(Align.CENTER));
        card.addChild(iconLbl);

        Label nameLbl = new Label(item.name);
        nameLbl.color(selected ? UnityStyles.TEXT_BOLD : UnityStyles.TEXT_PRIMARY);
        nameLbl.layout(style -> style.widthPercent(100.0f).height(24.0f));
        card.addChild(nameLbl);

        card.on(PointerPressedEvent.TYPE, e -> {
            if (e.button() == PointerButton.PRIMARY) {
                if ("folder".equals(item.type())) {
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
                e.cancel();
            }
        });

        return card;
    }
}
