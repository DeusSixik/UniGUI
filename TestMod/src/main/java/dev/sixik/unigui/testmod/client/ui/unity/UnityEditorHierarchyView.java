package dev.sixik.unigui.testmod.client.ui.unity;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Widget;
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
import dev.sixik.unigui.widgets.editor.DragPayload;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.widgets.navigation.TreeView;
import dev.sixik.unigui.widgets.navigation.TreeViewNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
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
    private static final String PAYLOAD_TYPE = "unity.gameobject";
    /**
     * Ширина колонки глаза слева от дерева.
     */
    private static final float EYE_GUTTER_WIDTH = 16.0f;
    private static final MutableColor INDICATOR_LINE = MutableColor.rgba(0.24f, 0.51f, 0.97f, 1.0f);
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
        EVENT_SYSTEM
    }

    private enum DropAction {
        BEFORE,
        AFTER,
        INTO,
        END_OF_SCENE
    }

    private record DropIntent(DropAction action, TreeViewNode target) {
    }

    private final TreeView treeView = new TreeView();
    private TreeViewNode sceneNode;
    private String selectedId = "Main Camera";
    private ContextMenu activeCreateMenu;
    private Consumer<String> selectionListener;
    private BiConsumer<String, GameObjectType> objectSelectionListener;
    private DropIntent currentDropIntent;
    private TreeView.TreeRowButton eyeGutterRow;

    public UnityEditorHierarchyView() {
        super(Orientation.VERTICAL);
        spacing(0.0f);
        layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));

        // 1. Верхняя панель: кнопка создания и строка поиска
        HBox topBar = new HBox();
        topBar.spacing(4.0f);
        topBar.layout(style -> style.widthPercent(100.0f).height(24.0f)
                .padding(4.0f, 2.0f, 4.0f, 2.0f).alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        UnityCreateButton createBtn = new UnityCreateButton();
        createBtn.onClick(e -> openCreateMenu(createBtn));
        topBar.addChild(createBtn);

        TextInput searchInput = new TextInput("");
        searchInput.placeholder("Search...");
        UnityStyles.styleTextInput(searchInput);
        searchInput.layout(style -> style.flexGrow(1.0f).height(20.0f));
        searchInput.onTextChanged(e -> filterItems(e.newText()));
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
                if (row.hovered() || row == eyeGutterRow) {
                    RectView b = row.layoutBounds();
                    float eyeSize = 10.0f;
                    float eyeX = b.x() - EYE_GUTTER_WIDTH + (EYE_GUTTER_WIDTH - eyeSize) * 0.5f;
                    float eyeY = b.y() + (b.height() - eyeSize) * 0.5f;
                    UnityIcons.drawEyeIcon(draw, eyeX, eyeY, eyeSize, UnityStyles.TEXT_MUTED);
                }
            }
        });

        // Настройка Drag-and-Drop на каждой создаваемой строке
        treeView.onRowCreated(row -> {
            TreeViewNode node = row.node();
            if (node == sceneNode) return; // Корень сцены не перетаскивается

            DragController.attach(row)
                    .dragged(row)
                    .threshold(4.0f)
                    .payload(() -> DragPayload.of(node.text(), PAYLOAD_TYPE, node.text(), node))
                    .ghost(DragGhost.text());
        });

        // Подписка на Drag-события для интерактивного перемещения элементов в иерархии
        treeView.on(DragOverEvent.TYPE, this::handleDragOver);
        treeView.on(DragLeaveEvent.TYPE, this::handleDragLeave);
        treeView.on(DragDropEvent.TYPE, this::handleDragDrop);

        // Отслеживание клика и выделения узлов
        treeView.onSelectionChanged(e -> {
            TreeViewNode selected = treeView.selectedNode();
            if (selected != null && selected != sceneNode) {
                selectedId = selected.text();
                notifySelection(selected);
            }
        });

        // Инициализация стандартных объектов сцены
        initSceneObjects();

        // Колонка глаза слева от дерева - левый padding содержимого: колонка и дерево прокручиваются вместе,
        // а указатель над колонкой напротив строки тоже показывает глаз этой строки.
        HBox scrollContent = new HBox();
        scrollContent.spacing(0.0f);
        scrollContent.layout(style -> style.padding(EYE_GUTTER_WIDTH, 0.0f, 0.0f, 0.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        scrollContent.on(PointerMovedEvent.TYPE, e -> {
            if (e.rootX() < treeView.layoutBounds().x()) {
                updateEyeGutterRow(e.rootY());
            } else {
                setEyeGutterRow(null);
            }
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

        // Выделение Main Camera по умолчанию
        selectItem(selectedId);
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

    private void initSceneObjects() {
        treeView.clearRoots();

        sceneNode = treeView.addRoot("SampleScene");
        sceneNode.userData(GameObjectType.SCENE);
        sceneNode.iconDraw((draw, node, x, y, w, h) -> UnityIcons.drawSceneIcon(draw, x, y, w));
        sceneNode.expanded(true);

        createNode(sceneNode, "Main Camera", GameObjectType.CAMERA);
        createNode(sceneNode, "Directional Light", GameObjectType.LIGHT);

        TreeViewNode player = createNode(sceneNode, "Player", GameObjectType.PLAYER);
        createNode(player, "Weapon", GameObjectType.CUBE);
        createNode(player, "CameraTarget", GameObjectType.EMPTY);
        player.expanded(true);

        createNode(sceneNode, "Cube", GameObjectType.CUBE);

        TreeViewNode canvas = createNode(sceneNode, "Canvas", GameObjectType.CANVAS);
        createNode(canvas, "Panel", GameObjectType.UI_PANEL);
        createNode(canvas, "Button", GameObjectType.UI_BUTTON);
        createNode(canvas, "Text (TMP)", GameObjectType.UI_TEXT);
        canvas.expanded(true);

        createNode(sceneNode, "EventSystem", GameObjectType.EVENT_SYSTEM);
    }

    private TreeViewNode createNode(TreeViewNode parent, String name, GameObjectType type) {
        TreeViewNode node = parent.addChild(name);
        node.userData(type);
        configureNodeIcon(node, type);
        return node;
    }

    private void configureNodeIcon(TreeViewNode node, GameObjectType type) {
        if (type == null) type = GameObjectType.EMPTY;
        final GameObjectType resolvedType = type;
        node.iconDraw((draw, n, x, y, w, h) -> UnityIcons.drawGameObjectIcon(draw, resolvedType, x, y, w));
    }

    /**
     * Обработка DragOver: вычисление позиции вставки (BEFORE, AFTER, INTO, END_OF_SCENE)
     * и отрисовка аутентичного индикатора Unity в OverlayLayer.
     */
    private void handleDragOver(DragOverEvent event) {
        DragPayload payload = event.payload();
        if (payload == null || !PAYLOAD_TYPE.equals(payload.type())) return;

        TreeViewNode draggedNode = payload.data(TreeViewNode.class);
        if (draggedNode == null) return;

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
            if (hoveredNode == draggedNode || draggedNode.isAncestor(hoveredNode)) {
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
                event.accept(treeView);
            } else if (relY < h * 0.28f) {
                // Вставка выше узла (BEFORE)
                session.showIndicator(treeX + 2.0f, bounds.y() - 1.0f, treeW - 4.0f, 2.0f, INDICATOR_LINE);
                currentDropIntent = new DropIntent(DropAction.BEFORE, hoveredNode);
                event.accept(treeView);
            } else if (relY > h * 0.72f) {
                // Вставка ниже узла (AFTER)
                session.showIndicator(treeX + 2.0f, bounds.y() + h - 1.0f, treeW - 4.0f, 2.0f, INDICATOR_LINE);
                currentDropIntent = new DropIntent(DropAction.AFTER, hoveredNode);
                event.accept(treeView);
            } else {
                // Бросок внутрь узла (INTO / reparenting)
                session.showIndicator(treeX + 2.0f, bounds.y() + 1.0f, treeW - 4.0f, h - 2.0f, HOVER_DROP);
                currentDropIntent = new DropIntent(DropAction.INTO, hoveredNode);
                event.accept(treeView);
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
            event.accept(treeView);
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
        DragPayload payload = event.payload();
        if (payload == null || !PAYLOAD_TYPE.equals(payload.type())) return;

        TreeViewNode draggedNode = payload.data(TreeViewNode.class);
        if (draggedNode == null || currentDropIntent == null) return;

        TreeViewNode target = currentDropIntent.target();
        DropAction action = currentDropIntent.action();

        switch (action) {
            case INTO -> {
                target.addChild(draggedNode);
                target.expanded(true);
            }
            case BEFORE -> {
                if (target.parent() != null) {
                    int idx = target.parent().children().indexOf(target);
                    target.parent().insertChild(idx, draggedNode);
                } else {
                    int idx = treeView.roots().indexOf(target);
                    treeView.insertRoot(idx, draggedNode);
                }
            }
            case AFTER -> {
                if (target.parent() != null) {
                    int idx = target.parent().children().indexOf(target);
                    target.parent().insertChild(idx + 1, draggedNode);
                } else {
                    int idx = treeView.roots().indexOf(target);
                    treeView.insertRoot(idx + 1, draggedNode);
                }
            }
            case END_OF_SCENE -> {
                sceneNode.addChild(draggedNode);
                sceneNode.expanded(true);
            }
        }

        treeView.select(draggedNode);
        selectItem(draggedNode.text());

        currentDropIntent = null;
        event.cancel();
    }

    /**
     * Открытие меню создания нового GameObject в стиле Unity.
     */
    private void openCreateMenu(Button triggerBtn) {
        OverlayLayer overlay = findOverlayLayer(this);
        if (overlay == null) return;

        if (activeCreateMenu != null) {
            activeCreateMenu.close();
            activeCreateMenu = null;
        }

        RectView b = triggerBtn.layoutBounds();
        float menuX = b.x();
        float menuY = b.y() + b.height() + 2.0f;
        float menuWidth = 160.0f;

        ContextMenu menu = new ContextMenu();
        UnityStyles.styleContextMenu(menu);

        UnityStyles.addContextMenuItem(menu, "Create Empty", true, menuWidth, () -> {
            addNewGameObject("GameObject", GameObjectType.EMPTY, false);
        });
        UnityStyles.addContextMenuItem(menu, "Create Empty Child", true, menuWidth, () -> {
            addNewGameObject("GameObject", GameObjectType.EMPTY, true);
        });
        UnityStyles.addContextMenuSeparator(menu, menuWidth);

        UnityStyles.addContextMenuItem(menu, "3D Object > Cube", true, menuWidth, () -> {
            addNewGameObject("Cube", GameObjectType.CUBE, false);
        });
        UnityStyles.addContextMenuItem(menu, "3D Object > Sphere", true, menuWidth, () -> {
            addNewGameObject("Sphere", GameObjectType.SPHERE, false);
        });
        UnityStyles.addContextMenuItem(menu, "3D Object > Capsule", true, menuWidth, () -> {
            addNewGameObject("Capsule", GameObjectType.CAPSULE, false);
        });
        UnityStyles.addContextMenuItem(menu, "3D Object > Plane", true, menuWidth, () -> {
            addNewGameObject("Plane", GameObjectType.PLANE, false);
        });
        UnityStyles.addContextMenuSeparator(menu, menuWidth);

        UnityStyles.addContextMenuItem(menu, "Light > Directional Light", true, menuWidth, () -> {
            addNewGameObject("Directional Light", GameObjectType.LIGHT, false);
        });
        UnityStyles.addContextMenuItem(menu, "Light > Point Light", true, menuWidth, () -> {
            addNewGameObject("Point Light", GameObjectType.LIGHT, false);
        });
        UnityStyles.addContextMenuItem(menu, "Audio > Audio Source", true, menuWidth, () -> {
            addNewGameObject("Audio Source", GameObjectType.AUDIO, false);
        });
        UnityStyles.addContextMenuSeparator(menu, menuWidth);

        UnityStyles.addContextMenuItem(menu, "UI > Canvas", true, menuWidth, () -> {
            addNewGameObject("Canvas", GameObjectType.CANVAS, false);
        });
        UnityStyles.addContextMenuItem(menu, "UI > Panel", true, menuWidth, () -> {
            addNewGameObject("Panel", GameObjectType.UI_PANEL, false);
        });
        UnityStyles.addContextMenuItem(menu, "UI > Button", true, menuWidth, () -> {
            addNewGameObject("Button", GameObjectType.UI_BUTTON, false);
        });
        UnityStyles.addContextMenuItem(menu, "UI > Text (TMP)", true, menuWidth, () -> {
            addNewGameObject("Text (TMP)", GameObjectType.UI_TEXT, false);
        });
        UnityStyles.addContextMenuItem(menu, "UI > EventSystem", true, menuWidth, () -> {
            addNewGameObject("EventSystem", GameObjectType.EVENT_SYSTEM, false);
        });
        UnityStyles.addContextMenuSeparator(menu, menuWidth);

        UnityStyles.addContextMenuItem(menu, "Camera", true, menuWidth, () -> {
            addNewGameObject("Camera", GameObjectType.CAMERA, false);
        });

        activeCreateMenu = menu;
        menu.openAt(menuX, menuY);
        overlay.addOverlay(menu);
    }

    /**
     * Создание нового GameObject и интеграция его в дерево Hierarchy.
     */
    private void addNewGameObject(String baseName, GameObjectType type, boolean asChild) {
        String uniqueName = generateUniqueName(baseName);
        TreeViewNode selected = treeView.selectedNode();

        TreeViewNode newNode = new TreeViewNode(uniqueName);
        newNode.userData(type);
        configureNodeIcon(newNode, type);

        if (asChild && selected != null && selected != sceneNode) {
            selected.addChild(newNode);
            selected.expanded(true);
        } else if (selected != null && selected != sceneNode && selected.parent() != null) {
            int idx = selected.parent().children().indexOf(selected);
            selected.parent().insertChild(idx + 1, newNode);
        } else {
            sceneNode.addChild(newNode);
            sceneNode.expanded(true);
        }

        treeView.select(newNode);
        selectItem(uniqueName);
    }

    private String generateUniqueName(String baseName) {
        Set<String> existingNames = new HashSet<>();
        collectAllNames(sceneNode, existingNames);

        if (!existingNames.contains(baseName)) {
            return baseName;
        }

        int index = 1;
        while (true) {
            String candidate = baseName + " (" + index + ")";
            if (!existingNames.contains(candidate)) {
                return candidate;
            }
            index++;
        }
    }

    private void collectAllNames(TreeViewNode node, Set<String> names) {
        if (node == null) return;
        names.add(node.text());
        for (TreeViewNode child : node.children()) {
            collectAllNames(child, names);
        }
    }

    private void filterItems(String query) {
        String cleanQuery = query == null ? "" : query.trim().toLowerCase();
        if (cleanQuery.isEmpty()) {
            restoreVisibility(sceneNode);
            return;
        }
        applyFilter(sceneNode, cleanQuery);
    }

    private boolean applyFilter(TreeViewNode node, String query) {
        boolean selfMatches = node.text().toLowerCase().contains(query);
        boolean childMatches = false;

        for (TreeViewNode child : node.children()) {
            if (applyFilter(child, query)) {
                childMatches = true;
            }
        }

        boolean visible = selfMatches || childMatches;
        if (visible && childMatches) {
            node.expanded(true);
        }
        return visible;
    }

    private void restoreVisibility(TreeViewNode node) {
        if (node == null) return;
        for (TreeViewNode child : node.children()) {
            restoreVisibility(child);
        }
    }

    public void onSelectionChanged(Consumer<String> listener) {
        this.selectionListener = listener;
    }

    public void selectItem(String name) {
        this.selectedId = name;
        TreeViewNode target = findNodeByName(sceneNode, name);
        if (target != null) {
            treeView.select(target);
        }
        notifySelection(target);
        if (target == null && selectionListener != null) {
            selectionListener.accept(name);
        }
    }

    /**
     * Подписка на выбор объекта вместе с его типом (например, чтобы Inspector показывал нужную иконку).
     * Слушатель вызывается сразу для текущего выделения.
     */
    public void onObjectSelected(BiConsumer<String, GameObjectType> listener) {
        this.objectSelectionListener = listener;
        TreeViewNode current = findNodeByName(sceneNode, selectedId);
        if (listener != null && current != null) {
            listener.accept(current.text(), typeOf(current));
        }
    }

    private void notifySelection(TreeViewNode node) {
        if (node == null) return;
        if (selectionListener != null) {
            selectionListener.accept(node.text());
        }
        if (objectSelectionListener != null) {
            objectSelectionListener.accept(node.text(), typeOf(node));
        }
    }

    private static GameObjectType typeOf(TreeViewNode node) {
        GameObjectType type = node.userData(GameObjectType.class);
        return type != null ? type : GameObjectType.EMPTY;
    }

    private TreeViewNode findNodeByName(TreeViewNode current, String name) {
        if (current == null) return null;
        if (current.text().equals(name)) return current;
        for (TreeViewNode child : current.children()) {
            TreeViewNode found = findNodeByName(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private static OverlayLayer findOverlayLayer(Widget start) {
        Widget current = start;
        while (current != null) {
            if (current instanceof OverlayLayer layer) return layer;
            current = current.parent();
        }
        return null;
    }
}
