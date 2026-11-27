package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.FocusLostEvent;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.TreeDisclosureStyle;
import dev.sixik.unigui.api.style.TreeRowStyle;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.SplitPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.dnd.DragController;
import dev.sixik.unigui.widgets.dnd.DragDropEvent;
import dev.sixik.unigui.widgets.dnd.DragGhost;
import dev.sixik.unigui.widgets.dnd.DragOverEvent;
import dev.sixik.unigui.widgets.dnd.DragPayload;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.ScrollBar;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.widgets.navigation.TreeView;
import dev.sixik.unigui.widgets.navigation.TreeViewNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Панель Project в стиле редактора Unity:
 * <ul>
 *   <li>Папки всегда отображаются в начале и отсортированы по алфавиту;</li>
 *   <li>Двухколоночный браузер ассетов с компонентом TreeView слева и разделителем SplitPanel;</li>
 *   <li>Полнофункциональное контекстное меню (Screenshot 1) для левой и правой панели;</li>
 *   <li>Горячая клавиша F2 для быстрого переименования элементов как в дереве, так и в сетке;</li>
 *   <li>Сетка и список файлов, хлебные крошки, поиск и тултипы при переполнении текста.</li>
 * </ul>
 */
public class UnityEditorProjectView extends LinearBox {
    /**
     * Project folder containing persisted prefab assets.
     */
    private static final String PREFABS = "Prefabs";
    private final HBox breadcrumbsBar = new HBox();
    private final Label statusLabel = new Label("2 items");
    private final TreeView folderTreeView = new TreeView();
    private final VBox assetContent = new VBox();
    /**
     * Search field reset when saving and revealing a new prefab.
     */
    private final TextInput searchInput = new TextInput("");

    private static final float LIST_THRESHOLD = 0.04f;
    private static final float MIN_ICON_SIZE = 32.0f;
    private static final float MAX_ICON_SIZE = 96.0f;
    private static final float LIST_ROW_HEIGHT = 18.0f;
    private float iconScale = 0.45f;
    private String searchQuery = "";
    private final Map<String, List<AssetItem>> folderContents = new HashMap<>();
    private final Map<String, TreeViewNode> treeNodes = new HashMap<>();
    private final Tooltip assetTooltip = new Tooltip();
    private OverlayLayer rootOverlay;

    private TreeViewNode assetsRootNode;
    private TreeViewNode packagesRootNode;

    private String currentFolder = "Scenes";
    private String selectedAsset = "fwf";
    private String renamingAsset = null;
    private ContextMenu activeContextMenu = null;
    /**
     * Backing store for the managed prefab folder.
     */
    private final UnityPrefabStore prefabStore;
    /**
     * Templates keyed by their labels in the prefab folder.
     */
    private final Map<String, UnityPrefab> prefabs = new HashMap<>();
    /**
     * Last selected prefab sent to the Inspector, or null.
     */
    private UnityPrefab previewedPrefab;
    /**
     * Receives changes to the asset preview.
     */
    private Consumer<UnityPrefab> onPrefabSelected = prefab -> {
    };
    /**
     * Places the asset at its saved position in the shared graph.
     */
    private Consumer<UnityPrefab> onPlacePrefab = prefab -> {
    };
    /**
     * Resolves the current scene selection when using Create Prefab.
     */
    private Supplier<UnitySceneObject> selectedObject = () -> null;
    /**
     * Resolves a drag from the editor's Hierarchy.
     */
    private Function<DragPayload, UnitySceneObject> sourceObject = payload -> null;
    /**
     * Whether the editor permits saving, renaming or deleting prefab assets.
     */
    private boolean assetWrites = true;
    /**
     * Whether the scene accepts new prefab instances.
     */
    private boolean sceneEditable = true;
    /**
     * Error message shown in the Project status bar, or null.
     */
    private String prefabError;

    public record AssetItem(String name, String type, String icon) {
        public AssetItem(String name, String type) {
            this(name, type, "");
        }
    }

    public UnityEditorProjectView() {
        this(null);
    }

    public UnityEditorProjectView(OverlayLayer rootOverlay) {
        this(rootOverlay, new UnityPrefabStore(UnityPrefabStore.DEFAULT_DIRECTORY));
    }

    /**
     * Opens the Project panel backed by the given prefab folder.
     *
     * @param rootOverlay shell overlay for menus and tooltips
     * @param prefabStore persistent storage for managed assets
     */
    UnityEditorProjectView(OverlayLayer rootOverlay, UnityPrefabStore prefabStore) {
        super(Orientation.VERTICAL);
        this.rootOverlay = rootOverlay;
        this.prefabStore = prefabStore;
        spacing(0.0f);
        focusable(true);
        layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));

        UnityStyles.styleTooltip(assetTooltip);
        assetTooltip.offset(0.0f, 4.0f);
        assetTooltip.visible(false);
        if (rootOverlay != null) {
            rootOverlay.addOverlay(assetTooltip);
        }

        initSampleData();
        loadPrefabs();
        on(DragOverEvent.TYPE, event -> {
            if (event.phase() != EventPhase.CAPTURE && assetWrites && sourceObject.apply(event.payload()) != null) {
                event.accept(this);
            }
        });
        on(DragDropEvent.TYPE, event -> {
            UnitySceneObject object = sourceObject.apply(event.payload());
            if (object == null || !assetWrites) return;
            savePrefab(object);
            event.cancel();
        });

        // 1. Верхняя панель: Хлебные крошки и поиск
        HBox topBar = new HBox();
        topBar.spacing(6.0f);
        topBar.layout(style -> style.widthPercent(100.0f).height(24.0f)
                .padding(6.0f, 2.0f, 6.0f, 2.0f).alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        UnityCreateButton createBtn = new UnityCreateButton();
        createBtn.onClick(e -> openCreateMenu(createBtn));
        topBar.addChild(createBtn);

        breadcrumbsBar.spacing(0.0f);
        breadcrumbsBar.layout(style -> style.height(20.0f).alignItems(Align.CENTER).flexGrow(0.0f).flexShrink(0.0f));
        topBar.addChild(breadcrumbsBar);

        Box spacer = new Box();
        spacer.themeEnabled(false);
        spacer.layout(style -> style.flexGrow(1.0f).height(1.0f));
        topBar.addChild(spacer);

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

        // Контекстное меню по ПКМ на строке в TreeView
        folderTreeView.on(PointerPressedEvent.TYPE, e -> {
            if (e.button() == PointerButton.SECONDARY) {
                Widget target = e.target();
                while (target != null && !(target instanceof TreeView.TreeRowButton)) {
                    target = target.parent();
                }
                if (target instanceof TreeView.TreeRowButton row) {
                    folderTreeView.select(row.node());
                    openProjectContextMenu(e.rootX(), e.rootY(), row.node(), null);
                    e.cancel();
                }
            }
        });

        // F2 и Delete для TreeView
        folderTreeView.on(KeyPressedEvent.TYPE, e -> {
            if (e.phase() == EventPhase.CAPTURE || e.target() instanceof TextInput) return;
            if (e.keyCode() == KeyCodes.F2) {
                TreeViewNode selected = folderTreeView.selectedNode();
                if (selected != null) {
                    startRenamingTreeNode(selected);
                    e.cancel();
                }
            } else if (e.keyCode() == KeyCodes.DELETE) {
                TreeViewNode selected = folderTreeView.selectedNode();
                if (selected != null) {
                    deleteFolderNode(selected);
                    e.cancel();
                }
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

        // Клик по пустому месту панели снимает выделение и открывает контекстное меню по ПКМ
        gridScroll.on(PointerPressedEvent.TYPE, e -> {
            if (e.phase() == EventPhase.CAPTURE) return;
            if (e.button() == PointerButton.PRIMARY && !e.isCancelled() && selectedAsset != null) {
                for (Widget w = e.target(); w != null; w = w.parent()) {
                    if (w instanceof ScrollBar) return;
                }
                selectedAsset = null;
                renamingAsset = null;
                loadFolder(currentFolder);
            } else if (e.button() == PointerButton.SECONDARY && !e.isCancelled()) {
                openProjectContextMenu(e.rootX(), e.rootY(), null, null);
                e.cancel();
            }
        });

        // 4. SplitPanel с перемещаемым разделителем между деревом папок и сеткой ассетов
        SplitPanel splitPanel = new SplitPanel(folderScroll, gridScroll);
        splitPanel.orientation(Orientation.HORIZONTAL);
        splitPanel.splitRatio(0.24f);
        splitPanel.minFirstSize(80.0f);
        splitPanel.minSecondSize(120.0f);
        splitPanel.splitterThickness(4.0f);
        splitPanel.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));

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

        // Горячие клавиши на уровне панели Project
        on(KeyPressedEvent.TYPE, e -> {
            if (e.phase() == EventPhase.CAPTURE || e.target() instanceof TextInput) return;
            if (e.keyCode() == KeyCodes.F2) {
                if (selectedAsset != null) {
                    AssetItem item = findAssetByName(selectedAsset);
                    if (item != null) {
                        startRenamingAsset(item);
                        e.cancel();
                    }
                } else if (folderTreeView.selectedNode() != null) {
                    startRenamingTreeNode(folderTreeView.selectedNode());
                    e.cancel();
                }
            } else if (e.keyCode() == KeyCodes.DELETE) {
                if (selectedAsset != null) {
                    AssetItem item = findAssetByName(selectedAsset);
                    if (item != null) {
                        deleteAsset(item);
                        e.cancel();
                    }
                }
            }
        });

        // Выбрать "Scenes" по умолчанию
        TreeViewNode scenesNode = treeNodes.get("Scenes");
        if (scenesNode != null) {
            folderTreeView.select(scenesNode);
        } else {
            loadFolder("Scenes");
        }
    }

    /**
     * Connects Project assets to the scene editor.
     *
     * @param selectedObject   retrieves the selected scene node for the Create command
     * @param sourceObject     validates a Hierarchy drag and retrieves the node
     * @param onPrefabSelected updates the Inspector preview
     * @param onPlacePrefab    adds a new instance to the shared scene
     */
    void connectPrefabs(Supplier<UnitySceneObject> selectedObject, Function<DragPayload, UnitySceneObject> sourceObject,
                        Consumer<UnityPrefab> onPrefabSelected, Consumer<UnityPrefab> onPlacePrefab) {
        this.selectedObject = selectedObject;
        this.sourceObject = sourceObject;
        this.onPrefabSelected = onPrefabSelected;
        this.onPlacePrefab = onPlacePrefab;
    }

    /**
     * Enables asset writes in Edit mode and instance placement when the scene is editable.
     *
     * @param assetWrites   whether saving, renaming and deleting prefabs is allowed
     * @param sceneEditable whether prefab instances can be added
     */
    void setPrefabAccess(boolean assetWrites, boolean sceneEditable) {
        this.assetWrites = assetWrites;
        this.sceneEditable = sceneEditable;
        if (!assetWrites && PREFABS.equals(currentFolder) && renamingAsset != null) {
            renamingAsset = null;
            loadFolder(currentFolder);
        }
    }

    /**
     * @return whether the current mode permits writing asset files
     */
    boolean canSavePrefabs() {
        return assetWrites;
    }

    /**
     * Saves a prefab and reveals it immediately in Assets/Prefabs.
     *
     * @param object scene subtree to capture, or null if nothing is selected
     */
    void savePrefab(UnitySceneObject object) {
        if (!assetWrites || object == null || object.scene() == null) return;
        String filename = generateUniqueAssetName(folderContents.get(PREFABS), object.name() + UnityPrefabStore.EXTENSION);
        try {
            UnityPrefab prefab = UnityPrefab.capture(prefabName(filename), object);
            prefabStore.save(prefab);
            addPrefabEntry(prefab, filename);
            prefabError = null;
            if (!PREFABS.equals(currentFolder)) loadFolder(PREFABS);
            if (!searchQuery.isEmpty()) searchInput.text("");
            selectedAsset = filename;
            loadFolder(PREFABS);
        } catch (IOException failure) {
            reportPrefabError("Could not save prefab", failure);
        }
    }

    /**
     * Clears the Project highlight and preview when the editor selects a scene object.
     */
    void clearPrefabSelection() {
        if (previewedPrefab == null) return;
        selectedAsset = null;
        renamingAsset = null;
        loadFolder(currentFolder);
    }

    /**
     * Loads persisted prefabs before building the folder browser.
     */
    private void loadPrefabs() {
        try {
            for (UnityPrefab prefab : prefabStore.load(file -> prefabError = "Could not read " + file)) {
                String name = generateUniqueAssetName(folderContents.get(PREFABS), prefab.name() + UnityPrefabStore.EXTENSION);
                addPrefabEntry(prefab, name);
            }
        } catch (IOException failure) {
            prefabError = "Could not open Assets/Prefabs: " + failure.getMessage();
        }
    }

    /**
     * Adds an asset and its visible browser entry.
     *
     * @param prefab   persisted template
     * @param filename displayed name with the prefab extension
     */
    private void addPrefabEntry(UnityPrefab prefab, String filename) {
        prefabs.put(filename, prefab);
        folderContents.get(PREFABS).add(new AssetItem(filename, "prefab"));
    }

    /**
     * Resolves a Prefabs folder item to a real template.
     *
     * @param item current asset entry, or null
     * @return the template, or null for another asset kind
     */
    private UnityPrefab prefab(AssetItem item) {
        return item != null && PREFABS.equals(currentFolder) && "prefab".equals(item.type())
                ? prefabs.get(item.name()) : null;
    }

    /**
     * Gets a display name without the optional prefab extension.
     *
     * @param filename entered name, with or without a suffix
     * @return logical name of the asset
     */
    private static String prefabName(String filename) {
        int stemLength = filename.length() - UnityPrefabStore.EXTENSION.length();
        return stemLength >= 0 && filename.regionMatches(true, stemLength, UnityPrefabStore.EXTENSION, 0,
                UnityPrefabStore.EXTENSION.length()) ? filename.substring(0, stemLength).strip() : filename;
    }

    /**
     * Reports the reason a persistence operation could not finish.
     *
     * @param action  short description of the operation
     * @param failure filesystem or serialization error
     */
    private void reportPrefabError(String action, IOException failure) {
        prefabError = action + ": " + failure.getMessage();
        loadFolder(currentFolder);
    }

    /**
     * Сортирует список элементов: папки ВСЕГДА идут в начале, и папки, и файлы
     * упорядочиваются по алфавиту без учёта регистра.
     */
    private void sortItems(List<AssetItem> items) {
        if (items == null) return;
        items.sort((a, b) -> {
            boolean aFolder = "folder".equalsIgnoreCase(a.type());
            boolean bFolder = "folder".equalsIgnoreCase(b.type());
            if (aFolder != bFolder) {
                return aFolder ? -1 : 1;
            }
            return a.name().compareToIgnoreCase(b.name());
        });
    }

    private void initSampleData() {
        folderContents.put("Assets", new ArrayList<>(List.of(
                new AssetItem("Materials", "folder"),
                new AssetItem(PREFABS, "folder"),
                new AssetItem("Scenes", "folder"),
                new AssetItem("Scripts", "folder"),
                new AssetItem("Settings", "folder"),
                new AssetItem("Textures", "folder"),
                new AssetItem("TutorialInfo", "folder")
        )));
        folderContents.put("Scenes", new ArrayList<>(List.of(
                new AssetItem("fwf", "folder"),
                new AssetItem("SampleScene", "scene")
        )));
        folderContents.put("fwf", new ArrayList<>());
        folderContents.put("Settings", new ArrayList<>(List.of(
                new AssetItem("ProjectSettings.asset", "script")
        )));
        folderContents.put("TutorialInfo", new ArrayList<>(List.of(
                new AssetItem("Readme.asset", "script")
        )));
        folderContents.put("Scripts", new ArrayList<>(List.of(
                new AssetItem("CameraController.cs", "script"),
                new AssetItem("GameManager.cs", "script"),
                new AssetItem("PlayerController.cs", "script")
        )));
        folderContents.put("Materials", new ArrayList<>(List.of(
                new AssetItem("GroundGrid.mat", "material"),
                new AssetItem("PlayerMaterial.mat", "material")
        )));
        folderContents.put(PREFABS, new ArrayList<>());
        folderContents.put("Textures", new ArrayList<>(List.of(
                new AssetItem("GridTexture.png", "texture"),
                new AssetItem("SkyboxTexture.png", "texture")
        )));
        folderContents.put("Packages", new ArrayList<>(List.of(
                new AssetItem("UniGUI.core", "package"),
                new AssetItem("UniGUI.docking", "package")
        )));

        for (List<AssetItem> list : folderContents.values()) {
            sortItems(list);
        }
    }

    private void buildFolderTree() {
        folderTreeView.clearRoots();
        treeNodes.clear();

        // Assets root
        assetsRootNode = folderTreeView.addRoot("Assets");
        assetsRootNode.expanded(true);
        assetsRootNode.folder(true);
        treeNodes.put("Assets", assetsRootNode);

        List<String> assetFolders = new ArrayList<>(List.of(
                "Materials", PREFABS, "Scenes", "Scripts", "Settings", "Textures", "TutorialInfo"
        ));
        Collections.sort(assetFolders, String.CASE_INSENSITIVE_ORDER);

        for (String folder : assetFolders) {
            TreeViewNode node = assetsRootNode.addChild(folder);
            node.folder(true);
            treeNodes.put(folder, node);

            if ("Scenes".equals(folder)) {
                node.expanded(true);
                TreeViewNode fwfNode = node.addChild("fwf");
                fwfNode.folder(true);
                fwfNode.emptyFolder(true);
                treeNodes.put("fwf", fwfNode);
            } else if ("Settings".equals(folder) || "TutorialInfo".equals(folder)) {
                node.emptyFolder(false);
            }
        }

        // Packages root
        packagesRootNode = folderTreeView.addRoot("Packages");
        packagesRootNode.expanded(true);
        packagesRootNode.folder(true);
        treeNodes.put("Packages", packagesRootNode);
    }

    /**
     * Обновляет панель хлебных крошек в соответствии с текущей выбранной папкой:
     * - при нажатии на сегмент пути переходим в эту папку;
     * - текущая папка отображается более ярким/жирным шрифтом (TEXT_BOLD);
     * - при нажатии на стрелочку шеврона (>) открывается выпадающий список подпапок;
     * - активная в пути подпапка отмечена галочкой (✓) в голубом квадрате.
     */
    private void updateBreadcrumbs(String folderName) {
        breadcrumbsBar.clearChildren();

        List<String> path = new ArrayList<>();
        TreeViewNode node = treeNodes.get(folderName);
        if (node != null) {
            TreeViewNode cur = node;
            while (cur != null) {
                path.add(0, cur.text());
                cur = cur.parent();
            }
        } else {
            if (!"Assets".equalsIgnoreCase(folderName)) {
                path.add("Assets");
            }
            path.add(folderName);
        }

        for (int i = 0; i < path.size(); i++) {
            String seg = path.get(i);
            boolean isLast = (i == path.size() - 1);
            String nextChild = !isLast ? path.get(i + 1) : null;

            Button segBtn = createBreadcrumbSegmentButton(seg, isLast);
            breadcrumbsBar.addChild(segBtn);

            List<String> subs = getSubfolders(seg);
            if (!isLast || !subs.isEmpty()) {
                Button chevronBtn = createBreadcrumbChevronButton(seg, nextChild);
                breadcrumbsBar.addChild(chevronBtn);
            }
        }
    }

    private Button createBreadcrumbSegmentButton(String folderName, boolean isLast) {
        UnityStyles.BreadcrumbSegmentButton btn = new UnityStyles.BreadcrumbSegmentButton(folderName, isLast);
        btn.renderer(UnityStyles.BREADCRUMB_SEGMENT_RENDERER);
        btn.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
        btn.onClick(e -> {
            loadFolder(folderName);
            TreeViewNode node = treeNodes.get(folderName);
            if (node != null) {
                folderTreeView.silentSelect(node);
            }
        });
        return btn;
    }

    private Button createBreadcrumbChevronButton(String parentFolder, String nextChild) {
        Button btn = new Button();
        btn.themeEnabled(false);
        btn.renderer(UnityStyles.BREADCRUMB_CHEVRON_RENDERER);
        btn.layout(style -> style.size(14.0f, 20.0f).flexGrow(0.0f).flexShrink(0.0f));
        btn.onClick(e -> openBreadcrumbDropdown(btn, parentFolder, nextChild));
        return btn;
    }

    private void openBreadcrumbDropdown(Button anchorBtn, String parentFolder, String currentChild) {
        OverlayLayer overlay = resolveRootOverlay();
        if (overlay == null) return;

        if (activeContextMenu != null) {
            activeContextMenu.close();
            activeContextMenu = null;
        }

        List<String> subfolders = getSubfolders(parentFolder);
        if (subfolders.isEmpty()) return;

        RectView b = anchorBtn.layoutBounds();
        float menuX = b.x();
        float menuY = b.y() + b.height() + 2.0f;

        float maxItemWidth = 120.0f;
        for (String sub : subfolders) {
            float textW = sub.length() * 7.5f + 40.0f;
            if (textW > maxItemWidth) maxItemWidth = textW;
        }
        float menuWidth = Math.max(140.0f, maxItemWidth);

        ContextMenu menu = new ContextMenu();
        UnityStyles.styleContextMenu(menu);

        for (String subfolder : subfolders) {
            boolean isChecked = subfolder.equalsIgnoreCase(currentChild);
            UnityStyles.addBreadcrumbDropdownItem(menu, subfolder, isChecked, menuWidth, () -> {
                loadFolder(subfolder);
                TreeViewNode subNode = treeNodes.get(subfolder);
                if (subNode != null) {
                    folderTreeView.silentSelect(subNode);
                }
            });
        }

        activeContextMenu = menu;
        menu.openAt(menuX, menuY);
        overlay.addOverlay(menu);
    }

    private List<String> getSubfolders(String parentFolder) {
        Set<String> subfolderSet = new LinkedHashSet<>();
        TreeViewNode node = treeNodes.get(parentFolder);
        if (node != null) {
            for (TreeViewNode child : node.children()) {
                if (child.folder()) {
                    subfolderSet.add(child.text());
                }
            }
        }
        List<AssetItem> items = folderContents.get(parentFolder);
        if (items != null) {
            for (AssetItem item : items) {
                if ("folder".equalsIgnoreCase(item.type())) {
                    subfolderSet.add(item.name());
                }
            }
        }
        List<String> list = new ArrayList<>(subfolderSet);
        list.sort(String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    private void loadFolder(String folderName) {
        hideAssetTooltip();
        if (!folderName.equals(currentFolder)) {
            selectedAsset = null;
            renamingAsset = null;
        }
        this.currentFolder = folderName;
        UnityPrefab chosen = prefab(findAssetByName(selectedAsset));
        if (chosen != previewedPrefab) {
            previewedPrefab = chosen;
            onPrefabSelected.accept(chosen);
        }
        updateBreadcrumbs(folderName);
        assetContent.clearChildren();

        TreeViewNode treeNode = treeNodes.get(folderName);
        if (treeNode != null && PREFABS.equals(folderName)) treeNode.emptyFolder(isFolderEmpty(folderName));
        if (treeNode != null && folderTreeView.selectedNode() != treeNode) {
            TreeViewNode cur = treeNode.parent();
            while (cur != null) {
                cur.expanded(true);
                cur = cur.parent();
            }
            folderTreeView.silentSelect(treeNode);
        }

        List<AssetItem> items = new ArrayList<>();
        for (AssetItem item : folderContents.getOrDefault(folderName, List.of())) {
            if (searchQuery.isEmpty() || item.name().toLowerCase().contains(searchQuery)) {
                items.add(item);
            }
        }
        sortItems(items);
        statusLabel.text(prefabError != null ? prefabError : items.size() + (items.size() == 1 ? " item" : " items"));

        TextInput activeRenameInput = null;

        if (iconScale <= LIST_THRESHOLD) {
            for (AssetItem item : items) {
                LinearBox row = createListRow(item);
                assetContent.addChild(row);
                if (item.name().equals(renamingAsset)) {
                    activeRenameInput = findTextInput(row);
                }
            }
        } else {
            HBox flexGrid = new HBox();
            flexGrid.spacing(8.0f);
            flexGrid.layout(style -> style.widthPercent(100.0f).flexWrap(FlexWrap.WRAP).padding(8.0f, 6.0f, 8.0f, 6.0f));

            float iconSize = MIN_ICON_SIZE + (MAX_ICON_SIZE - MIN_ICON_SIZE) * iconScale;
            for (AssetItem item : items) {
                LinearBox card = createAssetCard(item, iconSize);
                flexGrid.addChild(card);
                if (item.name().equals(renamingAsset)) {
                    activeRenameInput = findTextInput(card);
                }
            }
            if (activeRenameInput != null) flexGrid.applyQueuedMutations();
            assetContent.addChild(flexGrid);
        }

        if (activeRenameInput != null && uiContext() != null) {
            assetContent.applyQueuedMutations();
            var context = uiContext();
            TextInput input = activeRenameInput;
            String name = renamingAsset;
            // The context menu selects its command after running the callback.
            context.dispatcher().executeNextFrame(() -> {
                if (input.uiContext() == context && name.equals(renamingAsset)) {
                    context.focusManager().requestFocus(input);
                    input.selectAll();
                }
            });
        }
    }

    private TextInput findTextInput(Widget root) {
        if (root instanceof TextInput input) return input;
        if (root instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : root.children()) {
            TextInput found = findTextInput(child);
            if (found != null) return found;
        }
        return null;
    }

    private AssetItem findAssetByName(String name) {
        if (name == null) return null;
        List<AssetItem> items = folderContents.get(currentFolder);
        if (items != null) {
            for (AssetItem item : items) {
                if (item.name().equalsIgnoreCase(name)) return item;
            }
        }
        return null;
    }

    private void onAssetPressed(AssetItem item, boolean wasSelected) {
        if (wasSelected && "folder".equals(item.type())) {
            loadFolder(item.name());
            TreeViewNode node = treeNodes.get(item.name());
            if (node != null) {
                folderTreeView.select(node);
            }
        } else {
            selectedAsset = item.name();
            loadFolder(currentFolder);
        }
    }

    private LinearBox createListRow(AssetItem item) {
        LinearBox row = new LinearBox(Orientation.HORIZONTAL);
        row.themeEnabled(false);
        row.borderVisible(false);
        row.spacing(6.0f);
        row.layout(style -> style.widthPercent(100.0f).height(LIST_ROW_HEIGHT)
                .padding(6.0f, 1.0f, 6.0f, 1.0f).alignItems(Align.CENTER));

        boolean selected = item.name().equals(selectedAsset);
        row.backgroundVisible(selected);
        if (selected) {
            row.background().set(UnityStyles.SELECTION_BLUE);
            row.radius(0.0f);
        }

        boolean emptyFolder = "folder".equals(item.type()) && isFolderEmpty(item.name());
        CanvasWidget iconCanvas = new CanvasWidget();
        iconCanvas.layout(style -> style.size(14.0f, 14.0f).flexGrow(0.0f).flexShrink(0.0f));
        iconCanvas.onDraw(context -> {
            DrawScope draw = new DrawScope(context, iconCanvas.transform(), iconCanvas.layoutBounds());
            UnityIcons.drawProjectItemIcon(draw, item.type(), emptyFolder,
                    iconCanvas.layoutBounds().x(), iconCanvas.layoutBounds().y(),
                    iconCanvas.layoutBounds().width(), iconCanvas.layoutBounds().height());
        });
        row.addChild(iconCanvas);

        if (item.name().equals(renamingAsset)) {
            TextInput renameInput = new TextInput(item.name());
            UnityStyles.styleTextInput(renameInput);
            renameInput.layout(style -> style.width(180.0f).height(16.0f));
            renameInput.on(KeyPressedEvent.TYPE, e -> {
                if (e.keyCode() == KeyCodes.ENTER || e.keyCode() == KeyCodes.KEYPAD_ENTER) {
                    commitRenameAsset(item, renameInput.text());
                    e.cancel();
                } else if (e.keyCode() == KeyCodes.ESCAPE) {
                    renamingAsset = null;
                    loadFolder(currentFolder);
                    e.cancel();
                }
            });
            renameInput.on(FocusLostEvent.TYPE, e -> {
                if (item.name().equals(renamingAsset)) {
                    commitRenameAsset(item, renameInput.text());
                }
            });
            row.addChild(renameInput);
        } else {
            Label nameLbl = new Label(item.name());
            nameLbl.color(selected ? UnityStyles.TEXT_BOLD : UnityStyles.TEXT_PRIMARY);
            nameLbl.overflowMode(TextOverflowMode.ELLIPSIS);
            nameLbl.layout(style -> style.flexGrow(1.0f).height(LIST_ROW_HEIGHT));
            nameLbl.on(PointerEnteredEvent.TYPE, e -> {
                if (nameLbl.isTextTruncated()) {
                    showAssetTooltip(row, item.name());
                }
            });
            nameLbl.on(PointerExitedEvent.TYPE, e -> hideAssetTooltip());
            row.on(PointerExitedEvent.TYPE, e -> hideAssetTooltip());
            row.addChild(nameLbl);
        }

        installAssetInput(row, item, selected);
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

        if (item.name().equals(renamingAsset)) {
            TextInput renameInput = new TextInput(item.name());
            UnityStyles.styleTextInput(renameInput);
            renameInput.layout(style -> style.size(cardWidth, 18.0f).alignSelf(Align.CENTER));
            renameInput.on(KeyPressedEvent.TYPE, e -> {
                if (e.keyCode() == KeyCodes.ENTER || e.keyCode() == KeyCodes.KEYPAD_ENTER) {
                    commitRenameAsset(item, renameInput.text());
                    e.cancel();
                } else if (e.keyCode() == KeyCodes.ESCAPE) {
                    renamingAsset = null;
                    loadFolder(currentFolder);
                    e.cancel();
                }
            });
            renameInput.on(FocusLostEvent.TYPE, e -> {
                if (item.name().equals(renamingAsset)) {
                    commitRenameAsset(item, renameInput.text());
                }
            });
            card.addChild(renameInput);
        } else {
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

            // Тултип с полным названием, если текст обрезается многоточием
            nameLbl.on(PointerEnteredEvent.TYPE, e -> {
                if (nameLbl.isTextTruncated()) {
                    showAssetTooltip(card, item.name());
                }
            });
            nameLbl.on(PointerExitedEvent.TYPE, e -> hideAssetTooltip());
            card.on(PointerExitedEvent.TYPE, e -> hideAssetTooltip());

            card.addChild(nameLbl);
        }

        installAssetInput(card, item, selected);
        return card;
    }

    /**
     * Installs selection, context actions and prefab dragging on either browser layout.
     *
     * @param itemWidget asset card or list row
     * @param item       represented asset
     * @param selected   whether it was selected when rendered
     */
    private void installAssetInput(LinearBox itemWidget, AssetItem item, boolean selected) {
        UnityPrefab prefab = prefab(item);
        boolean draggable = prefab != null && !item.name().equals(renamingAsset);
        if (draggable) {
            DragController.attach(itemWidget)
                    .threshold(4.0f)
                    .filter(event -> sceneEditable)
                    .payload(() -> DragPayload.of(prefab.id().toString(), UnityPrefab.DRAG_TYPE, prefab.name(), prefab))
                    .ghost(DragGhost.text());
            itemWidget.on(PointerReleasedEvent.TYPE, event -> {
                RectView area = itemWidget.layoutBounds();
                if (event.phase() != EventPhase.CAPTURE && event.button() == PointerButton.PRIMARY && !event.isCancelled()
                        && event.rootX() >= area.x() && event.rootX() < area.x() + area.width()
                        && event.rootY() >= area.y() && event.rootY() < area.y() + area.height()) {
                    onAssetPressed(item, selected);
                    event.cancel();
                }
            });
        }
        itemWidget.on(PointerPressedEvent.TYPE, e -> {
            if (e.phase() == EventPhase.CAPTURE || e.isCancelled() || item.name().equals(renamingAsset)) return;
            if (uiContext() != null) uiContext().focusManager().requestFocus(UnityEditorProjectView.this);
            if (e.button() == PointerButton.PRIMARY) {
                if (!draggable) onAssetPressed(item, selected);
                e.cancel();
            } else if (e.button() == PointerButton.SECONDARY) {
                selectedAsset = item.name();
                loadFolder(currentFolder);
                openProjectContextMenu(e.rootX(), e.rootY(), null, item);
                e.cancel();
            }
        });

    }

    /**
     * Открытие меню кнопки "+" (Create) в верхней панели.
     */
    private void openCreateMenu(Button triggerBtn) {
        OverlayLayer overlay = resolveRootOverlay();
        if (overlay == null) return;

        if (activeContextMenu != null) {
            activeContextMenu.close();
            activeContextMenu = null;
        }

        RectView b = triggerBtn.layoutBounds();
        float menuX = b.x();
        float menuY = b.y() + b.height() + 2.0f;
        float menuWidth = 150.0f;

        ContextMenu menu = new ContextMenu();
        UnityStyles.styleContextMenu(menu);

        UnityStyles.addContextMenuItem(menu, "Folder", true, menuWidth, () -> createNewAsset("New Folder", "folder"));
        UnityStyles.addContextMenuSeparator(menu, menuWidth);

        UnityStyles.addContextMenuItem(menu, "C# Script", true, menuWidth, () -> createNewAsset("NewBehaviourScript.cs", "script"));
        UnityStyles.addContextMenuItem(menu, "Shader", true, menuWidth, () -> createNewAsset("NewShader.shader", "script"));
        UnityStyles.addContextMenuItem(menu, "Material", true, menuWidth, () -> createNewAsset("New Material.mat", "material"));
        UnityStyles.addContextMenuSeparator(menu, menuWidth);

        UnityStyles.addContextMenuItem(menu, "Scene", true, menuWidth, () -> createNewAsset("New Scene.unity", "scene"));
        UnityStyles.addContextMenuItem(menu, "Prefab", assetWrites && selectedObject.get() != null, menuWidth,
                () -> savePrefab(selectedObject.get()));
        UnityStyles.addContextMenuItem(menu, "Audio Mixer", true, menuWidth, () -> createNewAsset("NewAudioMixer.mixer", "package"));

        activeContextMenu = menu;
        menu.openAt(menuX, menuY);
        overlay.addOverlay(menu);
    }

    /**
     * Полноценное контекстное меню окна Project по правому клику мыши (Скриншот 1).
     */
    private void openProjectContextMenu(float menuX, float menuY, TreeViewNode treeNode, AssetItem assetItem) {
        OverlayLayer overlay = resolveRootOverlay();
        if (overlay == null) return;

        if (activeContextMenu != null) {
            activeContextMenu.close();
            activeContextMenu = null;
        }

        float menuWidth = 220.0f;
        ContextMenu menu = new ContextMenu();
        UnityStyles.styleContextMenu(menu);

        // Submenu: Create >
        ContextMenu createSubmenu = new ContextMenu();
        UnityStyles.styleContextMenu(createSubmenu);
        float subWidth = 160.0f;
        UnityStyles.addContextMenuItem(createSubmenu, "Folder", true, subWidth, () -> createNewAsset("New Folder", "folder"));
        UnityStyles.addContextMenuSeparator(createSubmenu, subWidth);
        UnityStyles.addContextMenuItem(createSubmenu, "C# Script", true, subWidth, () -> createNewAsset("NewBehaviourScript.cs", "script"));
        UnityStyles.addContextMenuItem(createSubmenu, "Shader", true, subWidth, () -> createNewAsset("NewShader.shader", "script"));
        UnityStyles.addContextMenuItem(createSubmenu, "Material", true, subWidth, () -> createNewAsset("New Material.mat", "material"));
        UnityStyles.addContextMenuSeparator(createSubmenu, subWidth);
        UnityStyles.addContextMenuItem(createSubmenu, "Scene", true, subWidth, () -> createNewAsset("New Scene.unity", "scene"));
        UnityStyles.addContextMenuItem(createSubmenu, "Prefab", assetWrites && selectedObject.get() != null, subWidth,
                () -> savePrefab(selectedObject.get()));
        UnityStyles.addContextMenuItem(createSubmenu, "Audio Mixer", true, subWidth, () -> createNewAsset("NewAudioMixer.mixer", "package"));

        // 1. Create >
        UnityStyles.addContextMenuSubmenu(menu, "Create", createSubmenu, menuWidth);

        UnityPrefab prefab = prefab(assetItem);
        if (prefab != null) {
            UnityStyles.addContextMenuItem(menu, "Add to Scene", sceneEditable, menuWidth, () -> {
                if (sceneEditable) onPlacePrefab.accept(prefab);
            });
        }

        // 2. Show in Explorer
        UnityStyles.addContextMenuItem(menu, "Show in Explorer", true, menuWidth, () -> copyCurrentPathToClipboard(treeNode, assetItem));

        // 3. Open
        boolean canOpen = (treeNode != null) || (assetItem != null);
        UnityStyles.addContextMenuItem(menu, "Open", canOpen, menuWidth, () -> {
            if (treeNode != null) {
                folderTreeView.select(treeNode);
                loadFolder(treeNode.text());
            } else if (assetItem != null && "folder".equalsIgnoreCase(assetItem.type())) {
                loadFolder(assetItem.name());
            }
        });

        // 4. Open Scene Additive
        boolean isScene = assetItem != null && "scene".equalsIgnoreCase(assetItem.type());
        UnityStyles.addContextMenuItem(menu, "Open Scene Additive", isScene, menuWidth, () -> {
        });

        // 5. Delete
        boolean canDelete = assetItem != null ? canModifyAsset(assetItem) : canModifyFolder(treeNode);
        UnityStyles.addContextMenuItem(menu, "Delete", canDelete, menuWidth, () -> {
            if (assetItem != null) {
                deleteAsset(assetItem);
            } else if (treeNode != null) {
                deleteFolderNode(treeNode);
            }
        });

        // 6. Rename
        boolean canRename = canDelete;
        UnityStyles.addContextMenuItem(menu, "Rename", canRename, menuWidth, () -> {
            if (assetItem != null) {
                startRenamingAsset(assetItem);
            } else if (treeNode != null) {
                startRenamingTreeNode(treeNode);
            }
        });

        // 7. Copy Path Alt+Ctrl+C
        UnityStyles.addContextMenuItem(menu, "Copy Path", "Alt+Ctrl+C", true, menuWidth, () -> copyCurrentPathToClipboard(treeNode, assetItem));

        UnityStyles.addContextMenuSeparator(menu, menuWidth);

        // Group 2: Packages
        UnityStyles.addContextMenuItem(menu, "View in Package Manager", false, menuWidth, () -> {
        });
        UnityStyles.addContextMenuItem(menu, "Create UPM Package...", false, menuWidth, () -> {
        });
        UnityStyles.addContextMenuItem(menu, "Import Asset Package...", true, menuWidth, () -> {
        });
        UnityStyles.addContextMenuItem(menu, "Export Asset Package...", true, menuWidth, () -> {
        });
        UnityStyles.addContextMenuItem(menu, "Export UPM Package...", false, menuWidth, () -> {
        });

        UnityStyles.addContextMenuSeparator(menu, menuWidth);

        // Group 3: Assets
        UnityStyles.addContextMenuItem(menu, "Import New Asset...", true, menuWidth, () -> {
        });
        UnityStyles.addContextMenuItem(menu, "Extract Material SubAsset", false, menuWidth, () -> {
        });

        UnityStyles.addContextMenuSeparator(menu, menuWidth);

        // Group 4: References
        UnityStyles.addContextMenuItem(menu, "Find References In Scene", false, menuWidth, () -> {
        });
        UnityStyles.addContextMenuItem(menu, "Find References In Project", true, menuWidth, () -> {
        });
        UnityStyles.addContextMenuItem(menu, "Select Dependencies", true, menuWidth, () -> {
        });

        UnityStyles.addContextMenuSeparator(menu, menuWidth);

        // Group 5: Reimport & Refresh
        UnityStyles.addContextMenuItem(menu, "Refresh", "Ctrl+R", true, menuWidth, () -> loadFolder(currentFolder));
        UnityStyles.addContextMenuItem(menu, "Reimport", true, menuWidth, () -> loadFolder(currentFolder));
        UnityStyles.addContextMenuItem(menu, "Reimport All", true, menuWidth, () -> loadFolder(currentFolder));

        UnityStyles.addContextMenuSeparator(menu, menuWidth);

        // Group 6: IDE / Schema
        UnityStyles.addContextMenuItem(menu, "Open C# Project", true, menuWidth, () -> {
        });
        UnityStyles.addContextMenuItem(menu, "View in Import Activity Window", true, menuWidth, () -> {
        });
        UnityStyles.addContextMenuItem(menu, "Update UXML Schema", true, menuWidth, () -> {
        });

        UnityStyles.addContextMenuSeparator(menu, menuWidth);

        // Group 7: Properties
        UnityStyles.addContextMenuItem(menu, "Properties...", "Alt+P", true, menuWidth, () -> {
        });

        activeContextMenu = menu;
        menu.openAt(menuX, menuY);
        overlay.addOverlay(menu);
    }

    private void copyCurrentPathToClipboard(TreeViewNode treeNode, AssetItem assetItem) {
        String path;
        UnityPrefab prefab = prefab(assetItem);
        if (prefab != null) {
            path = prefabStore.path(prefab.id()).toString();
        } else if (assetItem != null) {
            path = "Assets/" + currentFolder + "/" + assetItem.name();
        } else if (treeNode != null) {
            path = buildNodePath(treeNode);
        } else {
            path = "Assets/" + currentFolder;
        }
        if (uiContext() != null && uiContext().clipboard() != null) {
            uiContext().clipboard().setText(path);
        }
    }

    private String buildNodePath(TreeViewNode node) {
        StringBuilder sb = new StringBuilder(node.text());
        TreeViewNode cur = node.parent();
        while (cur != null) {
            sb.insert(0, cur.text() + "/");
            cur = cur.parent();
        }
        return sb.toString();
    }

    private void deleteAsset(AssetItem item) {
        if (!canModifyAsset(item)) return;
        UnityPrefab prefab = prefab(item);
        if (prefab != null) {
            try {
                prefabStore.delete(prefab);
                prefabs.remove(item.name());
                prefabError = null;
            } catch (IOException failure) {
                reportPrefabError("Could not delete prefab", failure);
                return;
            }
        }
        List<AssetItem> items = folderContents.get(currentFolder);
        if (items != null) {
            items.removeIf(i -> i.name().equalsIgnoreCase(item.name()));
        }
        if ("folder".equalsIgnoreCase(item.type())) {
            folderContents.remove(item.name());
            TreeViewNode node = treeNodes.remove(item.name());
            if (node != null && node.parent() != null) {
                node.parent().removeChild(node);
            }
        }
        if (item.name().equals(selectedAsset)) {
            selectedAsset = null;
        }
        loadFolder(currentFolder);
    }

    private void deleteFolderNode(TreeViewNode node) {
        if (!canModifyFolder(node)) return;
        String name = node.text();
        folderContents.remove(name);
        treeNodes.remove(name);
        TreeViewNode parent = node.parent();
        if (parent != null) {
            parent.removeChild(node);
            List<AssetItem> parentItems = folderContents.get(parent.text());
            if (parentItems != null) {
                parentItems.removeIf(i -> i.name().equalsIgnoreCase(name));
            }
            if (currentFolder.equals(name)) {
                folderTreeView.select(parent);
                loadFolder(parent.text());
            } else {
                loadFolder(currentFolder);
            }
        }
    }

    private void startRenamingTreeNode(TreeViewNode node) {
        if (!canModifyFolder(node)) return;
        folderTreeView.startEditing(node, UnityStyles::styleTextInput, newName -> {
            renameFolderNode(node, newName);
        });
    }

    private void renameFolderNode(TreeViewNode node, String newName) {
        if (!canModifyFolder(node) || newName == null) return;
        newName = newName.trim();
        String oldName = node.text();
        if (newName.isEmpty() || newName.equals(oldName) || PREFABS.equalsIgnoreCase(newName)) return;

        TreeViewNode parent = node.parent();
        if (parent != null) {
            for (TreeViewNode sibling : parent.children()) {
                if (sibling != node && sibling.text().equalsIgnoreCase(newName)) {
                    return;
                }
            }
            List<AssetItem> parentItems = folderContents.get(parent.text());
            if (parentItems != null) {
                for (int i = 0; i < parentItems.size(); i++) {
                    AssetItem it = parentItems.get(i);
                    if (it.name().equalsIgnoreCase(oldName)) {
                        parentItems.set(i, new AssetItem(newName, it.type(), it.icon()));
                        break;
                    }
                }
                sortItems(parentItems);
            }
        }

        List<AssetItem> contents = folderContents.remove(oldName);
        if (contents != null) {
            folderContents.put(newName, contents);
        }

        treeNodes.remove(oldName);
        node.text(newName);
        treeNodes.put(newName, node);

        if (parent != null) {
            sortTreeChildren(parent);
        }

        if (currentFolder.equals(oldName)) {
            currentFolder = newName;
        }
        if (oldName.equals(selectedAsset)) {
            selectedAsset = newName;
        }

        loadFolder(currentFolder);
    }

    private void sortTreeChildren(TreeViewNode parent) {
        List<TreeViewNode> children = new ArrayList<>(parent.children());
        children.sort((a, b) -> a.text().compareToIgnoreCase(b.text()));
        for (int i = 0; i < children.size(); i++) {
            TreeViewNode child = children.get(i);
            int curIdx = parent.children().indexOf(child);
            if (curIdx != i) {
                parent.removeChild(child);
                parent.insertChild(i, child);
            }
        }
    }

    private void startRenamingAsset(AssetItem item) {
        if (item == null || !canModifyAsset(item)) return;
        renamingAsset = item.name();
        loadFolder(currentFolder);
    }

    private void commitRenameAsset(AssetItem oldItem, String newName) {
        renamingAsset = null;
        if (newName == null || !canModifyAsset(oldItem)) {
            loadFolder(currentFolder);
            return;
        }
        newName = newName.trim();
        String oldName = oldItem.name();
        UnityPrefab prefab = prefab(oldItem);
        if (prefab != null) {
            String displayName = prefabName(newName);
            newName = displayName.isBlank() ? "" : displayName + UnityPrefabStore.EXTENSION;
        }
        if (newName.isEmpty() || newName.equals(oldName)
                || ("folder".equals(oldItem.type()) && PREFABS.equalsIgnoreCase(newName))) {
            loadFolder(currentFolder);
            return;
        }

        List<AssetItem> items = folderContents.get(currentFolder);
        if (items != null) {
            for (AssetItem sibling : items) {
                if (!sibling.name().equalsIgnoreCase(oldName) && sibling.name().equalsIgnoreCase(newName)) {
                    loadFolder(currentFolder);
                    return;
                }
            }

            if (prefab != null) {
                UnityPrefab updated = prefab.renamed(prefabName(newName));
                try {
                    prefabStore.save(updated);
                    prefabs.remove(oldName);
                    prefabs.put(newName, updated);
                    prefabError = null;
                } catch (IOException failure) {
                    reportPrefabError("Could not rename prefab", failure);
                    return;
                }
            }

            for (int i = 0; i < items.size(); i++) {
                AssetItem it = items.get(i);
                if (it.name().equalsIgnoreCase(oldName)) {
                    items.set(i, new AssetItem(newName, it.type(), it.icon()));
                    break;
                }
            }
            sortItems(items);
        }

        if ("folder".equalsIgnoreCase(oldItem.type())) {
            List<AssetItem> contents = folderContents.remove(oldName);
            if (contents != null) {
                folderContents.put(newName, contents);
            }
            TreeViewNode node = treeNodes.remove(oldName);
            if (node != null) {
                node.text(newName);
                treeNodes.put(newName, node);
                if (node.parent() != null) {
                    sortTreeChildren(node.parent());
                }
            }
            if (currentFolder.equals(oldName)) {
                currentFolder = newName;
            }
        }

        selectedAsset = newName;
        loadFolder(currentFolder);
    }

    /**
     * Checks whether a folder can be renamed or deleted in this demo.
     *
     * @param node folder row, or null
     * @return true for folders outside the fixed project and prefab roots
     */
    private boolean canModifyFolder(TreeViewNode node) {
        return node != null && node != assetsRootNode && node != packagesRootNode && node != treeNodes.get(PREFABS);
    }

    /**
     * Checks whether an entry can be renamed or deleted.
     *
     * @param item file or folder entry
     * @return true if this action is available in the current mode
     */
    private boolean canModifyAsset(AssetItem item) {
        if ("folder".equals(item.type()) && PREFABS.equals(item.name())) return false;
        return prefab(item) == null || assetWrites;
    }

    private void createNewAsset(String defaultName, String type) {
        List<AssetItem> items = folderContents.computeIfAbsent(currentFolder, k -> new ArrayList<>());

        String uniqueName = generateUniqueAssetName(items, defaultName);
        AssetItem newItem = new AssetItem(uniqueName, type);
        items.add(newItem);
        sortItems(items);

        if ("folder".equals(type)) {
            folderContents.putIfAbsent(uniqueName, new ArrayList<>());
            TreeViewNode parentNode = treeNodes.get(currentFolder);
            if (parentNode != null) {
                parentNode.emptyFolder(false);
                TreeViewNode childNode = new TreeViewNode(uniqueName);
                childNode.folder(true);
                childNode.emptyFolder(true);

                int insertIdx = 0;
                while (insertIdx < parentNode.children().size() && parentNode.children().get(insertIdx).text().compareToIgnoreCase(uniqueName) < 0) {
                    insertIdx++;
                }
                parentNode.insertChild(insertIdx, childNode);
                treeNodes.put(uniqueName, childNode);
                parentNode.expanded(true);
            }
        }

        selectedAsset = uniqueName;
        renamingAsset = uniqueName;
        loadFolder(currentFolder);
    }

    private String generateUniqueAssetName(List<AssetItem> items, String baseName) {
        boolean exists = false;
        for (AssetItem item : items) {
            if (item.name().equalsIgnoreCase(baseName)) {
                exists = true;
                break;
            }
        }
        if (!exists) return baseName;

        String nameWithoutExt;
        String ext;
        int dot = baseName.lastIndexOf('.');
        if (dot > 0) {
            nameWithoutExt = baseName.substring(0, dot);
            ext = baseName.substring(dot);
        } else {
            nameWithoutExt = baseName;
            ext = "";
        }

        int index = 1;
        while (true) {
            String candidate = nameWithoutExt + " " + index + ext;
            boolean candExists = false;
            for (AssetItem item : items) {
                if (item.name().equalsIgnoreCase(candidate)) {
                    candExists = true;
                    break;
                }
            }
            if (!candExists) return candidate;
            index++;
        }
    }

    private OverlayLayer resolveRootOverlay() {
        if (rootOverlay != null) {
            ensureTooltipAttached(rootOverlay);
            return rootOverlay;
        }
        OverlayLayer found = findRootOverlayLayer(this);
        if (found != null) {
            rootOverlay = found;
            ensureTooltipAttached(rootOverlay);
        }
        return rootOverlay;
    }

    private void ensureTooltipAttached(OverlayLayer overlay) {
        if (overlay != null && assetTooltip.parent() != overlay) {
            if (assetTooltip.parent() instanceof OverlayLayer oldOverlay) {
                oldOverlay.removeOverlay(assetTooltip);
            }
            overlay.addOverlay(assetTooltip);
        }
    }

    private void showAssetTooltip(Widget anchor, String text) {
        OverlayLayer overlay = resolveRootOverlay();
        if (overlay != null) {
            ensureTooltipAttached(overlay);
            assetTooltip.text(text);
            assetTooltip.anchor(anchor);
            assetTooltip.visible(true);
        }
    }

    private void hideAssetTooltip() {
        assetTooltip.visible(false);
    }

    private static OverlayLayer findRootOverlayLayer(Widget start) {
        OverlayLayer root = null;
        Widget current = start;
        while (current != null) {
            if (current instanceof OverlayLayer layer) {
                root = layer;
            }
            current = current.parent();
        }
        return root;
    }

    @Override
    public void dispose() {
        if (rootOverlay != null) {
            rootOverlay.removeOverlay(assetTooltip);
        }
        assetTooltip.dispose();
        super.dispose();
    }
}
