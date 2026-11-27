package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraft;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraftHost.SceneElementKind;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.dnd.DragController;
import dev.sixik.unigui.widgets.dnd.DragDropEvent;
import dev.sixik.unigui.widgets.dnd.DragGhost;
import dev.sixik.unigui.widgets.dnd.DragLeaveEvent;
import dev.sixik.unigui.widgets.dnd.DragOverEvent;
import dev.sixik.unigui.widgets.dnd.DragSession;
import dev.sixik.unigui.widgets.dnd.DragPayload;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.widgets.navigation.TreeView;
import dev.sixik.unigui.widgets.navigation.TreeViewNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Панель Hierarchy в стиле редактора Unity:
 * <ul>
 *   <li>Полноценный компонент {@link TreeView} со стилями Unity;</li>
 *   <li>Кастомный рендеринг строк и процедурные векторные иконки для каждого типа GameObject;</li>
 *   <li>Кнопка «+ v» (Create) с выпадающим меню создания объектов сцены;</li>
 *   <li>Drag-and-Drop перетаскивание компонентов (reorder выше/ниже, reparenting внутрь объекта);</li>
 *   <li>Строка поиска с фильтрацией объектов сцены.</li>
 * </ul>
 */
public class UnityEditorHierarchyView extends LinearBox {
    /**
     * Identifies hierarchy row drags.
     */
    private static final String PAYLOAD_TYPE = "unity.gameobject";
    /**
     * Ширина колонки глаза слева от дерева.
     */
    private static final float EYE_GUTTER_WIDTH = 16.0f;
    /**
     * Color of the insertion line between siblings.
     */
    private static final MutableColor INDICATOR_LINE = MutableColor.rgba(0.24f, 0.51f, 0.97f, 1.0f);
    /**
     * Color of the drop highlight over a parent row.
     */
    private static final MutableColor HOVER_DROP = MutableColor.rgba(0.24f, 0.51f, 0.97f, 0.35f);

    public enum GameObjectType {
        SCENE,
        EMPTY,
        CAMERA,
        LIGHT,
        PLAYER,
        CUBE,
        SPHERE,
        CAPSULE,
        PLANE,
        AUDIO,
        CANVAS,
        UI_PANEL,
        UI_BUTTON,
        UI_TEXT,
        EVENT_SYSTEM,
        /**
         * Native Minecraft block.
         */
        MINECRAFT_BLOCK,
        /**
         * Native Minecraft item.
         */
        MINECRAFT_ITEM,
        /**
         * Native Minecraft entity.
         */
        MINECRAFT_ENTITY;

        /**
         * @return whether this object type carries a Minecraft registry ID
         */
        boolean isMinecraft() {
            return this == MINECRAFT_BLOCK || this == MINECRAFT_ITEM || this == MINECRAFT_ENTITY;
        }
    }

    private enum DropAction {
        BEFORE,
        AFTER,
        INTO,
        END_OF_SCENE
    }

    private record DropIntent(DropAction action, TreeViewNode target) {
    }

    /**
     * Widget tree projected from the scene.
     */
    private final TreeView treeView = new TreeView();
    /**
     * Input that filters the displayed hierarchy.
     */
    private final TextInput searchInput = new TextInput("");
    /**
     * Opens the scene mutation menu when edits are allowed.
     */
    private final UnityCreateButton createButton = new UnityCreateButton();
    /**
     * Scene shared with the viewport.
     */
    private final Scene scene;
    /**
     * Layer hosting the create menu.
     */
    private final OverlayLayer overlay;
    /**
     * Current tree rows keyed by node identity.
     */
    private final Map<SceneNode, TreeViewNode> rows = new IdentityHashMap<>();
    /**
     * Expanded nodes in the unfiltered tree.
     */
    private final Set<SceneNode> expanded = new HashSet<>();
    /**
     * Synthetic row containing the scene roots.
     */
    private TreeViewNode sceneNode;
    /**
     * Selected scene object, or null for the scene row.
     */
    private UnitySceneObject selected;
    /**
     * Detached snapshot used for scene-object paste, or null until copied.
     */
    private UnitySceneObject clipboard;
    /**
     * Suppresses transient selection events during tree replacement.
     */
    private boolean rebuilding;
    /**
     * Normalized search text, or empty when unfiltered.
     */
    private String query = "";
    /**
     * Most recently opened create menu.
     */
    private ContextMenu activeCreateMenu;
    /**
     * Registry dialog, or null when closed.
     */
    private WindowWidget minecraftPicker;
    /**
     * Receives changes to the actual scene selection.
     */
    private Consumer<UnitySceneObject> selectionListener;
    /**
     * Receives changes made through the visibility column.
     */
    private Consumer<UnitySceneObject> visibilityListener;
    /**
     * Insertion target for the current drag, or null.
     */
    private DropIntent currentDropIntent;
    /**
     * Row alongside the pointer when it is over the visibility gutter.
     */
    private TreeView.TreeRowButton eyeGutterRow;
    /**
     * Next placement slot for a newly created mesh.
     */
    private int nextMeshSlot;
    /**
     * Whether the hierarchy may modify the scene.
     */
    private boolean editable = true;
    /**
     * Writes a selected object to the Project assets.
     */
    private Consumer<UnitySceneObject> savePrefab;
    /**
     * Checks whether the current editor mode allows writing assets.
     */
    private BooleanSupplier canSavePrefab;

    /**
     * Creates the hierarchy projection for the editor's scene.
     *
     * @param scene   editable scene graph
     * @param overlay layer hosting the create menu
     */
    UnityEditorHierarchyView(Scene scene, OverlayLayer overlay) {
        super(Orientation.VERTICAL);
        this.scene = scene;
        this.overlay = overlay;
        spacing(0.0f);
        layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));

        // 1. Верхняя панель: кнопка создания и строка поиска
        HBox topBar = new HBox();
        topBar.spacing(4.0f);
        topBar.layout(style -> style.widthPercent(100.0f).height(24.0f)
                .padding(4.0f, 2.0f, 4.0f, 2.0f).alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        createButton.onClick(e -> openCreateMenu(createButton));
        topBar.addChild(createButton);

        searchInput.placeholder("Search...");
        UnityStyles.styleTextInput(searchInput);
        searchInput.layout(style -> style.flexGrow(1.0f).height(20.0f));
        searchInput.onTextChanged(e -> {
            saveExpansion();
            query = e.newText().strip().toLowerCase(Locale.ROOT);
            rebuildTree();
        });
        topBar.addChild(searchInput);

        addChild(topBar);
        addChild(UnityStyles.separatorLine());

        // 2. Настройка TreeView со стилями Unity
        UnityStyles.styleTreeView(treeView);
        // Дерево занимает всю ширину прокручиваемой области, но не сжимается: узкая панель прокручивается по X.
        // Именно widthPercent, а не flexGrow: растущий ребёнок при измерении без ограничения ширины
        // давал бы 0 в ширину содержимого, и прокручиваться было бы нечему.
        treeView.layout(style -> style.widthPercent(100.0f).flexGrow(0.0f).flexShrink(0.0f));

        // Кастомный рендерер строк TreeView: базовый визуал строки + иконка видимости слева, в отдельной
        // колонке перед деревом, пока на строку наведён указатель (или он над колонкой напротив строки).
        treeView.renderer((draw, widget) -> {
            if (widget instanceof TreeView.TreeRowButton row) {
                row.renderRowVisual(draw);
                UnitySceneObject object = row.node().userData(UnitySceneObject.class);
                if (object != null && (!object.visible() || row.hovered() || row == eyeGutterRow)) {
                    RectView b = row.layoutBounds();
                    float eyeSize = 10.0f;
                    float eyeX = b.x() - EYE_GUTTER_WIDTH + (EYE_GUTTER_WIDTH - eyeSize) * 0.5f;
                    float eyeY = b.y() + (b.height() - eyeSize) * 0.5f;
                    UnityIcons.drawEyeIcon(draw, eyeX, eyeY, eyeSize,
                            object.visible() ? UnityStyles.TEXT_PRIMARY : UnityStyles.TEXT_MUTED);
                }
            }
        });

        // Настройка Drag-and-Drop на каждой создаваемой строке
        treeView.onRowCreated(row -> {
            TreeViewNode node = row.node();
            if (node == sceneNode) return; // Корень сцены не перетаскивается

            DragController.attach(row)
                    .dragged(row)
                    .filter(event -> editable)
                    .threshold(4.0f)
                    .payload(() -> DragPayload.of(node.text(), PAYLOAD_TYPE, node.text(), node))
                    .ghost(DragGhost.text());
        });

        // Подписка на Drag-события для интерактивного перемещения элементов в иерархии
        treeView.onSelectionChanged(e -> {
            if (rebuilding) return;
            TreeViewNode row = treeView.selectedNode();
            setSelected(row == null ? null : row.userData(UnitySceneObject.class));
        });
        for (SceneNode root : scene.roots()) {
            if (!root.children().isEmpty()) expanded.add(root);
        }
        rebuildTree();

        // Колонка глаза слева от дерева - левый padding содержимого: колонка и дерево прокручиваются вместе,
        // а указатель над колонкой напротив строки тоже показывает глаз этой строки.
        HBox scrollContent = new HBox();
        scrollContent.spacing(0.0f);
        scrollContent.layout(style -> style.widthPercent(100.0f).minHeightPercent(100.0f)
                .padding(EYE_GUTTER_WIDTH, 0.0f, 0.0f, 0.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        scrollContent.on(DragOverEvent.TYPE, this::handleDragOver);
        scrollContent.on(DragLeaveEvent.TYPE, this::handleDragLeave);
        scrollContent.on(DragDropEvent.TYPE, this::handleDragDrop);
        scrollContent.on(PointerMovedEvent.TYPE, e -> {
            if (e.rootX() < treeView.layoutBounds().x()) {
                updateEyeGutterRow(e.rootY());
            } else {
                setEyeGutterRow(null);
            }
        });
        scrollContent.on(PointerPressedEvent.TYPE, e -> {
            if (!editable) return;
            if (e.button() == PointerButton.SECONDARY && !e.isCancelled()) {
                openHierarchyMenu(e);
                return;
            }
            if (e.button() != PointerButton.PRIMARY || e.rootX() >= treeView.layoutBounds().x()) return;
            updateEyeGutterRow(e.rootY());
            if (eyeGutterRow == null) return;
            UnitySceneObject object = eyeGutterRow.node().userData(UnitySceneObject.class);
            if (object == null) return;
            object.setVisible(!object.visible());
            refreshObject(object);
            if (visibilityListener != null) visibilityListener.accept(object);
            e.cancel();
        });
        scrollContent.on(PointerExitedEvent.TYPE, e -> setEyeGutterRow(null));
        scrollContent.addChild(treeView);

        ScrollView scroll = new ScrollView(scrollContent);
        scroll.overlayScrollBars(false);
        scroll.scrollStep(18.0f);
        scroll.scrollbarGap(2.0f);
        scroll.scrollbarTrackColor().set(UnityStyles.BORDER_DEFAULT);
        scroll.scrollbarThumbColor().set(UnityStyles.TEXT_MUTED);
        scroll.verticalScrollBar().thumbHoverColor().set(UnityStyles.TEXT_PRIMARY);
        scroll.horizontalScrollBar().thumbHoverColor().set(UnityStyles.TEXT_PRIMARY);
        scroll.layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f)
                .overflowX(Overflow.AUTO).overflowY(Overflow.AUTO));
        addChild(scroll);
    }

    /**
     * Строка напротив указателя, когда он над колонкой глаза: для неё тоже показывается глаз.
     */
    private void updateEyeGutterRow(float rootY) {
        TreeView.TreeRowButton found = null;
        for (TreeViewNode node : treeView.visibleNodes()) {
            if (node.rowButton() instanceof TreeView.TreeRowButton row) {
                RectView b = row.layoutBounds();
                if (rootY >= b.y() && rootY < b.y() + b.height()) {
                    found = row;
                    break;
                }
            }
        }
        setEyeGutterRow(found);
    }

    private void setEyeGutterRow(TreeView.TreeRowButton row) {
        if (eyeGutterRow == row) return;
        TreeView.TreeRowButton previous = eyeGutterRow;
        eyeGutterRow = row;
        if (previous != null) previous.invalidate(InvalidationFlags.VISUAL);
        if (row != null) row.invalidate(InvalidationFlags.VISUAL);
    }

    /**
     * Saves expansion state before replacing the unfiltered projection.
     */
    private void saveExpansion() {
        if (!query.isEmpty()) return;
        expanded.clear();
        for (Map.Entry<SceneNode, TreeViewNode> entry : rows.entrySet()) {
            if (entry.getValue().expanded()) expanded.add(entry.getKey());
        }
    }

    /**
     * Rebuilds the tree from the scene graph and preserves selection by node identity.
     */
    private void rebuildTree() {
        setEyeGutterRow(null);
        boolean sceneSelected = sceneNode != null && treeView.selectedNode() == sceneNode;
        rebuilding = true;
        treeView.beginBatch();
        try {
            rows.clear();
            treeView.clearRoots();
            sceneNode = treeView.addRoot("SampleScene").expanded(true);
            sceneNode.iconDraw((draw, node, x, y, w, h) -> UnityIcons.drawSceneIcon(draw, x, y, w));
            for (SceneNode root : scene.roots()) {
                TreeViewNode row = buildRow((UnitySceneObject) root);
                if (row != null) sceneNode.addChild(row);
            }
        } finally {
            treeView.endBatch();
            rebuilding = false;
        }
        treeView.silentSelect(selected != null ? rows.get(selected) : sceneSelected ? sceneNode : null);
    }

    /**
     * Projects one subtree and retains ancestors of matches while searching.
     *
     * @param object node to project
     * @return the row, or null if the subtree has no match
     */
    private TreeViewNode buildRow(UnitySceneObject object) {
        TreeViewNode row = new TreeViewNode(object.name()).userData(object);
        for (SceneNode child : object.children()) {
            if (!(child instanceof UnitySceneObject childObject)) continue;
            TreeViewNode childRow = buildRow(childObject);
            if (childRow != null) row.addChild(childRow);
        }
        if (!row.hasChildren() && !object.name().toLowerCase(Locale.ROOT).contains(query)) return null;
        row.iconDraw((draw, n, x, y, w, h) -> UnityIcons.drawGameObjectIcon(draw, object.type(), x, y, w));
        row.silentExpanded(expanded.contains(object) || (!query.isEmpty() && row.hasChildren()));
        rows.put(object, row);
        return row;
    }

    /**
     * Обработка DragOver: вычисление позиции вставки (BEFORE, AFTER, INTO, END_OF_SCENE)
     * и отрисовка аутентичного индикатора Unity в OverlayLayer.
     */
    private void handleDragOver(DragOverEvent event) {
        if (!editable) return;
        DragPayload payload = event.payload();
        TreeViewNode draggedNode = draggedRow(payload);
        UnityPrefab prefab = draggedPrefab(payload);
        if (prefab == null && draggedNode == null) return;

        DragSession session = event.session();
        float py = session.pointerY();

        TreeViewNode hoveredNode = null;
        TreeView.TreeRowButton hoveredRow = null;

        for (TreeViewNode node : treeView.visibleNodes()) {
            if (node.rowButton() instanceof TreeView.TreeRowButton row) {
                RectView b = row.layoutBounds();
                if (py >= b.y() && py <= b.y() + b.height()) {
                    hoveredNode = node;
                    hoveredRow = row;
                    break;
                }
            }
        }

        float treeX = treeView.layoutBounds().x();
        float treeW = treeView.layoutBounds().width();

        if (hoveredNode != null && hoveredRow != null) {
            // Запрет бросать узел в самого себя или в своих потомков
            if (draggedNode != null && hoveredNode.isAncestor(draggedNode)) {
                currentDropIntent = null;
                session.clearIndicator();
                return;
            }

            RectView bounds = hoveredRow.layoutBounds();
            float relY = py - bounds.y();
            float h = bounds.height();

            if (hoveredNode == sceneNode) {
                // Корень сцены: бросок внутрь сцены
                session.showIndicator(treeX + 2.0f, bounds.y() + 1.0f, treeW - 4.0f, h - 2.0f, HOVER_DROP);
                currentDropIntent = new DropIntent(DropAction.INTO, sceneNode);
                event.accept(event.currentTarget());
            } else if (relY < h * 0.28f) {
                // Вставка выше узла (BEFORE)
                session.showIndicator(treeX + 2.0f, bounds.y() - 1.0f, treeW - 4.0f, 2.0f, INDICATOR_LINE);
                currentDropIntent = new DropIntent(DropAction.BEFORE, hoveredNode);
                event.accept(event.currentTarget());
            } else if (relY > h * 0.72f) {
                // Вставка ниже узла (AFTER)
                session.showIndicator(treeX + 2.0f, bounds.y() + h - 1.0f, treeW - 4.0f, 2.0f, INDICATOR_LINE);
                currentDropIntent = new DropIntent(DropAction.AFTER, hoveredNode);
                event.accept(event.currentTarget());
            } else {
                // Бросок внутрь узла (INTO / reparenting)
                session.showIndicator(treeX + 2.0f, bounds.y() + 1.0f, treeW - 4.0f, h - 2.0f, HOVER_DROP);
                currentDropIntent = new DropIntent(DropAction.INTO, hoveredNode);
                event.accept(event.currentTarget());
            }
        } else {
            // Указатель ниже всех строк: добавление в конец сцены
            List<TreeViewNode> visible = treeView.visibleNodes();
            float lineY = treeView.layoutBounds().y() + 4.0f;
            if (!visible.isEmpty()) {
                TreeViewNode last = visible.get(visible.size() - 1);
                if (last.rowButton() != null) {
                    lineY = last.rowButton().layoutBounds().y() + last.rowButton().layoutBounds().height();
                }
            }
            session.showIndicator(treeX + 2.0f, lineY - 1.0f, treeW - 4.0f, 2.0f, INDICATOR_LINE);
            currentDropIntent = new DropIntent(DropAction.END_OF_SCENE, sceneNode);
            event.accept(event.currentTarget());
        }
    }

    private void handleDragLeave(DragLeaveEvent event) {
        currentDropIntent = null;
        if (event.session() != null) {
            event.session().clearIndicator();
        }
    }

    /**
     * Обработка DragDrop: применение перестановки или смены родителя (reparenting)
     * в структуре TreeView.
     */
    private void handleDragDrop(DragDropEvent event) {
        if (!editable || currentDropIntent == null) return;
        DragPayload payload = event.payload();
        TreeViewNode row = draggedRow(payload);
        UnityPrefab prefab = draggedPrefab(payload);
        if (row == null && prefab == null) return;
        UnitySceneObject target = currentDropIntent.target().userData(UnitySceneObject.class);
        DropAction action = currentDropIntent.action();
        currentDropIntent = null;
        boolean betweenRows = target != null && (action == DropAction.BEFORE || action == DropAction.AFTER);
        SceneNode parent = betweenRows ? target.parent() : target;
        SceneNode sibling = betweenRows ? target : null;
        boolean after = action == DropAction.AFTER;
        if (prefab != null) placePrefab(prefab, null, parent, sibling, after);
        else moveObject(row.userData(UnitySceneObject.class), parent, sibling, after);
        event.cancel();
    }

    /**
     * Validates that the payload carries a local row and still belongs to this hierarchy.
     *
     * @param payload drag data, possibly null
     * @return the row being moved, or null
     */
    private TreeViewNode draggedRow(DragPayload payload) {
        if (payload == null || !PAYLOAD_TYPE.equals(payload.type())) return null;
        TreeViewNode row = payload.data(TreeViewNode.class);
        return row != null && row.owner() == treeView && row.userData(UnitySceneObject.class) != null ? row : null;
    }

    /**
     * Gets the object being dragged to Project.
     *
     * @param payload drag data, possibly null
     * @return a live scene object, or null
     */
    UnitySceneObject draggedObject(DragPayload payload) {
        TreeViewNode row = draggedRow(payload);
        UnitySceneObject object = row == null ? null : row.userData(UnitySceneObject.class);
        return object != null && object.scene() == scene ? object : null;
    }

    /**
     * Checks whether the gesture carries a prefab from Project.
     *
     * @param payload drag data, possibly null
     * @return the asset, or null
     */
    private static UnityPrefab draggedPrefab(DragPayload payload) {
        return payload != null && UnityPrefab.DRAG_TYPE.equals(payload.type()) ? payload.data(UnityPrefab.class) : null;
    }

    /**
     * Places a new or existing node, preserving its local transform and ordering it relative to a sibling.
     *
     * @param object  node to move
     * @param parent  destination parent, or null for a scene root
     * @param sibling insertion reference, or null to append
     * @param after   whether to place the node after the sibling
     */
    private void moveObject(UnitySceneObject object, SceneNode parent, SceneNode sibling, boolean after) {
        if ((object.scene() != null && object.scene() != scene) || (parent != null && parent.scene() != scene)) return;
        for (SceneNode ancestor = parent; ancestor != null; ancestor = ancestor.parent()) {
            if (ancestor == object) return;
        }
        List<SceneNode> siblings = new ArrayList<>(parent == null ? scene.roots() : parent.children());
        siblings.remove(object);
        int index = sibling == null ? siblings.size() : siblings.indexOf(sibling);
        if (index < 0) return;
        if (sibling != null && after) index++;

        saveExpansion();
        // Scene adds at the end. Reattach the suffix so tree order matches scene traversal order.
        List<SceneNode> suffix = siblings.subList(index, siblings.size());
        object.detach();
        for (SceneNode node : suffix) node.detach();
        attach(object, parent);
        for (SceneNode node : suffix) attach(node, parent);
        if (parent != null) expanded.add(parent);
        rebuildTree();
        selectObject(object);
    }

    /**
     * Adds a prefab as a scene root.
     *
     * @param prefab      template to instantiate
     * @param groundPoint drop position on the work plane, or null to use the captured position
     */
    void placePrefab(UnityPrefab prefab, MutableVec3 groundPoint) {
        placePrefab(prefab, groundPoint, null, null, false);
    }

    /**
     * Instantiates a fresh subtree at the requested position in the scene and hierarchy.
     *
     * @param prefab      template to instantiate
     * @param groundPoint drop point in world units, or null to use the captured local position
     * @param parent      destination node, or null for a root
     * @param sibling     optional row used as an insertion reference
     * @param after       whether to insert after the reference row
     */
    private void placePrefab(UnityPrefab prefab, MutableVec3 groundPoint, SceneNode parent, SceneNode sibling, boolean after) {
        if (!editable) return;
        UnitySceneObject object = prefab.instantiate();
        object.rename(uniqueObjectName(object.name()));
        if (groundPoint != null) {
            object.setPosition(groundPoint.x(), groundPoint.y(), groundPoint.z());
            var bounds = UnityScenePreview.subtreeBounds(object);
            if (bounds != null) {
                float y = (float) (2.0 * groundPoint.y() - bounds.minY());
                if (Float.isFinite(y)) object.setPosition(groundPoint.x(), y, groundPoint.z());
            }
        }
        moveObject(object, parent, sibling, after);
    }

    /**
     * Connects the Hierarchy's save action to the Project assets.
     *
     * @param canSave whether asset writes are currently allowed
     * @param save    receives the selected scene object
     */
    void onSavePrefab(BooleanSupplier canSave, Consumer<UnitySceneObject> save) {
        canSavePrefab = canSave;
        savePrefab = save;
    }

    /**
     * Appends a node to the graph.
     *
     * @param object node to attach
     * @param parent destination parent, or null for a scene root
     */
    private void attach(SceneNode object, SceneNode parent) {
        if (parent == null) scene.addRoot(object);
        else parent.addChild(object);
    }

    /**
     * Opens the creation menu below the plus button.
     *
     * @param triggerBtn button anchoring the menu
     */
    private void openCreateMenu(Button triggerBtn) {
        RectView b = triggerBtn.layoutBounds();
        openObjectMenu(b.x(), b.y() + b.height() + 2.0f);
    }

    /**
     * Selects the row under the pointer before opening its actions. Empty space targets the scene root.
     *
     * @param event secondary press in the tree
     */
    private void openHierarchyMenu(PointerPressedEvent event) {
        Widget target = event.target();
        while (target != null && !(target instanceof TreeView.TreeRowButton)) target = target.parent();
        TreeView.TreeRowButton row = (TreeView.TreeRowButton) target;
        selectObject(row == null ? null : row.node().userData(UnitySceneObject.class));
        openObjectMenu(event.rootX(), event.rootY());
        event.cancel();
    }

    /**
     * Shows the common creation and removal menu.
     *
     * @param x horizontal position in root UI units
     * @param y vertical position in root UI units
     */
    private void openObjectMenu(float x, float y) {
        if (!editable) return;
        if (activeCreateMenu != null) activeCreateMenu.close();

        float menuWidth = 176.0f;
        ContextMenu menu = new ContextMenu();
        UnityStyles.styleContextMenu(menu);

        UnityStyles.addContextMenuItem(menu, "Create Empty", true, menuWidth,
                () -> addObject("GameObject", GameObjectType.EMPTY, false));
        UnityStyles.addContextMenuItem(menu, "Create Empty Child", selected != null, menuWidth,
                () -> addObject("GameObject", GameObjectType.EMPTY, true));
        UnityStyles.addContextMenuItem(menu, "Create Minecraft Element", true, menuWidth, this::openMinecraftPicker);
        UnityStyles.addContextMenuSeparator(menu, menuWidth);

        UnityStyles.addContextMenuItem(menu, "Cube", true, menuWidth, () -> addObject("Cube", GameObjectType.CUBE, false));
        UnityStyles.addContextMenuItem(menu, "Sphere", true, menuWidth, () -> addObject("Sphere", GameObjectType.SPHERE, false));
        UnityStyles.addContextMenuItem(menu, "Capsule", true, menuWidth, () -> addObject("Capsule", GameObjectType.CAPSULE, false));
        UnityStyles.addContextMenuItem(menu, "Plane", true, menuWidth, () -> addObject("Plane", GameObjectType.PLANE, false));
        UnityStyles.addContextMenuSeparator(menu, menuWidth);

        addCreateItem(menu, menuWidth, "Light > Directional Light", "Directional Light", GameObjectType.LIGHT);
        addCreateItem(menu, menuWidth, "Light > Point Light", "Point Light", GameObjectType.LIGHT);
        addCreateItem(menu, menuWidth, "Audio > Audio Source", "Audio Source", GameObjectType.AUDIO);
        addCreateItem(menu, menuWidth, "Camera", "Camera", GameObjectType.CAMERA);
        UnityStyles.addContextMenuSeparator(menu, menuWidth);

        addCreateItem(menu, menuWidth, "UI > Canvas", "Canvas", GameObjectType.CANVAS);
        addCreateItem(menu, menuWidth, "UI > Panel", "Panel", GameObjectType.UI_PANEL);
        addCreateItem(menu, menuWidth, "UI > Button", "Button", GameObjectType.UI_BUTTON);
        addCreateItem(menu, menuWidth, "UI > Text (TMP)", "Text (TMP)", GameObjectType.UI_TEXT);
        addCreateItem(menu, menuWidth, "UI > EventSystem", "EventSystem", GameObjectType.EVENT_SYSTEM);
        UnityStyles.addContextMenuSeparator(menu, menuWidth);
        boolean canSave = editable && selected != null && savePrefab != null && canSavePrefab.getAsBoolean();
        UnityStyles.addContextMenuItem(menu, "Save as Prefab", canSave, menuWidth, () -> {
            if (editable && selected != null && savePrefab != null && canSavePrefab.getAsBoolean())
                savePrefab.accept(selected);
        });
        UnityStyles.addContextMenuItem(menu, "Copy", "Ctrl+C", selected != null, menuWidth, this::copySelected);
        UnityStyles.addContextMenuItem(menu, "Paste", "Ctrl+V", clipboard != null, menuWidth, this::paste);
        UnityStyles.addContextMenuItem(menu, "Duplicate", "Ctrl+D", selected != null, menuWidth, this::duplicateSelected);
        UnityStyles.addContextMenuItem(menu, "Delete Selected", "Delete", selected != null, menuWidth, this::deleteSelected);

        activeCreateMenu = menu;
        overlay.addOverlay(menu);
        menu.openAt(x, y);
    }

    /**
     * Opens the registry picker for creating an object in the shared scene.
     */
    private void openMinecraftPicker() {
        if (!editable || minecraftPicker != null) return;
        minecraftPicker = UnityMinecraftPicker.open(overlay, DemoMinecraft.host(), this::addMinecraftElement, () -> {
            minecraftPicker = null;
            if (treeView.uiContext() != null) treeView.uiContext().focusManager().requestFocus(treeView);
        });
    }

    /**
     * Adds a root object backed by the selected Minecraft registry entry.
     *
     * @param kind  registry category
     * @param entry ID and display label selected in the dialog
     */
    private void addMinecraftElement(SceneElementKind kind, UnityMinecraftPicker.Entry entry) {
        if (!editable) return;
        UnitySceneObject object = UnitySceneObject.minecraft(kind, uniqueObjectName(entry.label()), entry.id());
        positionNewRoot(object, kind == SceneElementKind.ENTITY ? 0.002f : 0.502f);
        moveObject(object, null, null, false);
    }

    /**
     * Adds a creation command for a root scene object.
     *
     * @param menu  creation menu
     * @param width menu item width in UI units
     * @param label command text
     * @param name  base name of the new object
     * @param type  type displayed in the hierarchy
     */
    private void addCreateItem(ContextMenu menu, float width, String label, String name, GameObjectType type) {
        UnityStyles.addContextMenuItem(menu, label, true, width, () -> addObject(name, type, false));
    }

    /**
     * Creates and selects an object in the actual scene.
     *
     * @param baseName preferred display label
     * @param type     content to create
     * @param asChild  whether to create under the selected object
     */
    private void addObject(String baseName, GameObjectType type, boolean asChild) {
        if (!editable) return;
        saveExpansion();
        UnitySceneObject object = new UnitySceneObject(uniqueObjectName(baseName), type);
        SceneNode parent = asChild ? selected : null;
        if (parent == null && object.mesh() != null) {
            positionNewRoot(object, -object.bounds().minY() + 0.002f);
        }
        attach(object, parent);
        if (parent != null) expanded.add(parent);
        rebuildTree();
        selectObject(object);
    }

    /**
     * Places a new root in the next slot of the foreground grid.
     *
     * @param object newly created scene node
     * @param y      height of the object's pivot in scene units
     */
    private void positionNewRoot(UnitySceneObject object, float y) {
        int slot = nextMeshSlot++;
        object.setPosition(1.5f * ((slot + 2) % 5 - 2), y, 1.4f - 1.6f * (slot / 5));
    }

    /**
     * Chooses a label that is not already used by an object in the scene.
     *
     * @param baseName requested label
     * @return original label or a suffixed copy
     */
    private String uniqueObjectName(String baseName) {
        Set<String> names = new HashSet<>();
        for (SceneNode root : scene.roots()) collectNames(root, names);
        String name = baseName;
        for (int suffix = 1; names.contains(name); suffix++) name = baseName + " (" + suffix + ")";
        return name;
    }

    /**
     * Adds the names of an entire subtree, including filtered-out rows.
     *
     * @param node  subtree root
     * @param names output collection
     */
    private static void collectNames(SceneNode node, Set<String> names) {
        if (node instanceof UnitySceneObject object) {
            names.add(object.name());
            for (SceneNode child : object.children()) collectNames(child, names);
        }
    }

    /**
     * Removes the selected subtree and selects its former parent.
     *
     * @return whether a scene object was removed
     */
    boolean deleteSelected() {
        if (!editable || selected == null) return false;
        saveExpansion();
        UnitySceneObject parent = (UnitySceneObject) selected.parent();
        selected.detach();
        expanded.removeIf(node -> node.scene() != scene);
        setSelected(parent);
        rebuildTree();
        selectObject(parent);
        return true;
    }

    /**
     * Saves a snapshot of the selected subtree in the editor's object clipboard.
     *
     * @return whether an object was copied
     */
    boolean copySelected() {
        if (selected == null) return false;
        clipboard = selected.copySubtree();
        return true;
    }

    /**
     * Inserts a copy of the selected subtree immediately after the original.
     *
     * @return whether an object was duplicated
     */
    boolean duplicateSelected() {
        return selected != null && insertCopy(selected);
    }

    /**
     * Inserts an independent copy from the clipboard beside the selection, or as a root.
     *
     * @return whether an object was pasted
     */
    boolean paste() {
        return clipboard != null && insertCopy(clipboard);
    }

    /**
     * Inserts and selects a copy using the existing hierarchy placement logic.
     *
     * @param source object or detached snapshot to copy
     * @return whether the copy was added to the scene
     */
    private boolean insertCopy(UnitySceneObject source) {
        if (!editable) return false;
        UnitySceneObject copy = source.copySubtree();
        copy.rename(uniqueObjectName(source.name()));
        moveObject(copy, selected == null ? null : selected.parent(), selected, true);
        return copy.scene() == scene;
    }

    /**
     * Controls graph edits while leaving the tree available for browsing and selection.
     *
     * @param editable whether adding, removing, reparenting and visibility changes are allowed
     */
    void setEditable(boolean editable) {
        this.editable = editable;
        createButton.enabled(editable);
        if (!editable) {
            currentDropIntent = null;
            if (activeCreateMenu != null) activeCreateMenu.close();
            if (minecraftPicker != null) minecraftPicker.close();
        }
    }

    /**
     * Records hierarchy presentation and placement state before entering Play.
     *
     * @return action that rebuilds the original hierarchy after the graph is restored
     */
    Runnable captureEditorState() {
        saveExpansion();
        Set<SceneNode> savedExpansion = new HashSet<>(expanded);
        UnitySceneObject savedSelection = selected;
        boolean sceneSelected = treeView.selectedNode() == sceneNode;
        int savedMeshSlot = nextMeshSlot;
        return () -> {
            nextMeshSlot = savedMeshSlot;
            expanded.clear();
            expanded.addAll(savedExpansion);
            setSelected(savedSelection);
            rebuildTree();
            if (savedSelection == null) treeView.silentSelect(sceneSelected ? sceneNode : null);
        };
    }

    /**
     * @return the selected object, or null when no scene object is selected
     */
    UnitySceneObject selectedObject() {
        return selected;
    }

    /**
     * Synchronizes selection with a viewport hit. A background click clears the tree selection.
     *
     * @param node hit object, or null for a background click
     */
    void selectFromViewport(SceneNode node) {
        while (node != null && !(node instanceof UnitySceneObject)) node = node.parent();
        if (node != null) {
            selectObject(node);
            return;
        }
        treeView.silentSelect(null);
        setSelected(null);
    }

    /**
     * Reveals and selects a node picked in the viewport or created by the editor.
     *
     * @param node scene object to select, or null to select the scene
     */
    void selectObject(SceneNode node) {
        if (node != null && (!(node instanceof UnitySceneObject) || node.scene() != scene)) return;
        if (node != null && !rows.containsKey(node)) searchInput.text("");
        TreeViewNode row = node == null ? sceneNode : rows.get(node);
        if (row == null) return;
        for (TreeViewNode ancestor = row.parent(); ancestor != null; ancestor = ancestor.parent()) {
            ancestor.expanded(true);
        }
        treeView.silentSelect(row);
        setSelected((UnitySceneObject) node);
    }

    /**
     * Updates a node's row after its name or visibility changes in the Inspector.
     *
     * @param object node with changed display properties
     */
    void refreshObject(UnitySceneObject object) {
        TreeViewNode row = rows.get(object);
        if (row == null) return;
        if (!row.text().equals(object.name())) {
            if (!query.isEmpty()) {
                rebuildTree();
                return;
            }
            row.text(object.name());
        }
        if (row.rowButton() != null) row.rowButton().invalidate(InvalidationFlags.VISUAL);
    }

    /**
     * Binds the editor's shared selection.
     *
     * @param listener receives the selected object, or null for the scene
     */
    void onObjectSelected(Consumer<UnitySceneObject> listener) {
        selectionListener = listener;
    }

    /**
     * Binds visibility updates initiated in the hierarchy.
     *
     * @param listener receives the node whose visibility changed
     */
    void onVisibilityChanged(Consumer<UnitySceneObject> listener) {
        visibilityListener = listener;
    }

    /**
     * Changes the selected object and notifies other panels.
     *
     * @param object selected object, or null
     */
    private void setSelected(UnitySceneObject object) {
        if (selected == object) return;
        selected = object;
        if (selectionListener != null) selectionListener.accept(object);
    }
}
