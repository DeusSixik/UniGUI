package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.ContextMenus;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.dnd.DragController;
import dev.sixik.unigui.widgets.dnd.DragGhost;
import dev.sixik.unigui.widgets.dnd.DragPayload;
import dev.sixik.unigui.widgets.dnd.DropController;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.widgets.navigation.TreeView;
import dev.sixik.unigui.widgets.navigation.TreeViewNode;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.Hierarchy;
import dev.sixik.unigui.xaml.edit.SessionChange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Дерево узлов документа в виде {@link TreeView}, как иерархия сцены в Unity: строки со стрелкой раскрытия, отступами
 * по глубине и подсветкой выбора. Выбор строки выбирает узел во всех панелях, а выбор в предпросмотре раскрывает путь
 * к узлу и прокручивает дерево к нему. Строки перетаскиваются и принимают перетаскиваемое (палитру и другие строки);
 * правая кнопка открывает меню действий.
 */
final class HierarchyPanel extends SessionPanel {
    private final TreeView tree = new TreeView();
    private final TextField filter = EditorStyles.searchField();
    private final Map<UiNode, TreeViewNode> nodes = new IdentityHashMap<>();
    /** Свёрнутые автором узлы; сравниваются по идентичности, потому что правка документа не создаёт узлы заново. */
    private final Set<UiNode> collapsed = Collections.newSetFromMap(new IdentityHashMap<>());
    private ScrollView scroll;
    private boolean syncing;
    private boolean scrollPending;
    private UiNode revealed;
    private boolean hoverBound;

    @Override
    public String id() {
        return UiEditor.HIERARCHY;
    }

    @Override
    public String title() {
        return "Hierarchy";
    }

    @Override
    public dev.sixik.unigui.api.render.UiIcon icon() {
        return dev.sixik.unigui.editor.style.EditorIcons.HIERARCHY;
    }

    @Override
    public PanelSlot slot() {
        return PanelSlot.LEFT;
    }

    @Override
    public float share() {
        return 0.22f;
    }

    @Override
    protected Widget build(EditorContext editorContext) {
        VBox root = new VBox();
        root.layout(style -> style.sizePercent(100.0f, 100.0f));
        filter.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));
        filter.onTextChanged(event -> refresh());

        EditorStyles.styleTreeView(tree);
        tree.iconMode(TreeView.TreeIconMode.NONE);
        tree.layout(style -> style.widthPercent(100.0f));
        tree.onRowCreated(this::rowCreated);
        tree.onSelectionChanged(event -> {
            if (syncing) return;
            TreeViewNode selected = tree.selectedNode();
            EditorSession session = session().orElse(null);
            if (session != null && selected != null && selected.userData() instanceof UiNode node) session.select(node);
        });
        scroll = new ScrollView(tree);
        EditorStyles.styleScroll(scroll);
        scroll.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));

        // Раскладка строк известна только при отрисовке: прокрутка к выбранной строке откладывается до неё.
        CanvasWidget probe = new CanvasWidget();
        probe.layout(style -> style.size(0.0f, 0.0f).flexShrink(0.0f));
        probe.onDraw(draw -> revealSelected());
        root.addChild(EditorStyles.searchBar(filter));
        root.addChild(scroll);
        root.addChild(probe);
        root.applyQueuedMutations();
        return root;
    }

    @Override
    protected void bound(EditorSession next) {
        refresh();
    }

    @Override
    protected void changed(EditorSession current, SessionChange change) {
        switch (change.kind()) {
            case DOCUMENT_CHANGED, REVERTED -> refresh();
            case SELECTION_CHANGED -> showSelection();
            default -> { }
        }
    }

    // ------------------------------------------------------------------ построение дерева

    private void refresh() {
        if (!hoverBound) hoverBound = true;
        syncing = true;
        try {
            tree.beginBatch();
            tree.clearRoots();
            nodes.clear();
            EditorSession session = session().orElse(null);
            if (session != null) build(session);
            tree.endBatch();
        } finally {
            syncing = false;
        }
        showSelection();
    }

    private void build(EditorSession session) {
        boolean filtering = !filter.text().isBlank();
        List<Hierarchy.Row> rows = Hierarchy.filter(Hierarchy.rows(session.document(), session.schema()), filter.text());
        List<TreeViewNode> stack = new ArrayList<>(); // последний узел на каждой глубине
        for (Hierarchy.Row row : rows) {
            TreeViewNode node = new TreeViewNode(rowText(row.label()));
            node.userData(row.node());
            UiIcon icon = EditorIcons.forWidget(row.node().type().local());
            node.iconDraw((draw, owner, x, y, width, height) -> icon.draw(draw, x + 2.0f, y + 2.0f, width - 4.0f,
                    row.node() == session.document().root() ? EditorStyles.INFO_BLUE : EditorStyles.TEXT_LABEL));
            nodes.put(row.node(), node);
            int depth = filtering ? 0 : row.depth();
            while (stack.size() > depth) stack.remove(stack.size() - 1);
            if (depth == 0 || stack.isEmpty()) {
                tree.addRoot(node);
                if (!filtering) stack.clear();
            } else {
                stack.get(stack.size() - 1).addChild(node);
            }
            stack.add(node);
        }
        for (Map.Entry<UiNode, TreeViewNode> entry : nodes.entrySet()) {
            entry.getValue().silentExpanded(!collapsed.contains(entry.getKey()));
        }
    }

    /** Подпись строки: тип обычным цветом, идентификатор приглушённым, потому что читается вторым. */
    private static RichText rowText(String label) {
        int mark = label.indexOf(" #");
        if (mark < 0) return RichText.plain(label);
        return RichText.builder().append(label.substring(0, mark)).color(EditorStyles.TEXT_SECONDARY)
                .append(label.substring(mark)).build();
    }

    /** Подсвечивает выбранный узел сессии; путь к нему раскрывается, чтобы строка была видна. */
    private void showSelection() {
        EditorSession session = session().orElse(null);
        UiNode selected = session == null ? null : session.selected().orElse(null);
        syncing = true;
        try {
            if (selected == null) {
                tree.clearSelection();
            } else {
                TreeViewNode node = nodes.get(selected);
                if (node != null) {
                    if (selected != revealed) {
                        for (TreeViewNode up = node.parent(); up != null; up = up.parent()) {
                            if (up.userData() instanceof UiNode parentNode) collapsed.remove(parentNode);
                            up.expanded(true);
                        }
                        scrollPending = true;
                    }
                    tree.silentSelect(node);
                }
            }
            revealed = selected;
        } finally {
            syncing = false;
        }
    }

    // ------------------------------------------------------------------ строки

    private void rowCreated(TreeView.TreeRowButton row) {
        TreeViewNode treeNode = row.node();
        if (!(treeNode.userData() instanceof UiNode uiNode)) return;
        EditorSession session = session().orElse(null);
        if (session == null) return;

        row.on(PointerEnteredEvent.TYPE, event -> hover().set(uiNode));
        row.on(PointerExitedEvent.TYPE, event -> {
            if (hover().node() == uiNode) hover().set(null);
        });
        DropController.attach(row).accepts(DropActions.ACCEPTED)
                .onDrop(event -> DropActions.drop(session, event.payload(), uiNode));
        if (uiNode != session.document().root()) {
            String label = treeNode.text();
            DragController.attach(row).dragged(row).threshold(4.0f)
                    .payload(() -> DragPayload.of(label, DropActions.NODE_TYPE, label, uiNode))
                    .ghost(DragGhost.text());
        }
        ContextMenus.onSecondaryClick(row, context(), () -> {
            session.select(uiNode);
            boolean movable = UiEdits.hasMovableSelection(session);
            return List.of(
                    ContextMenus.Entry.of("Duplicate", movable, () -> UiEdits.duplicateSelected(session)),
                    ContextMenus.Entry.of("Delete", movable, () -> UiEdits.deleteSelected(session)),
                    ContextMenus.Entry.separator(),
                    ContextMenus.Entry.of("Move Up", movable, () -> UiEdits.moveSelected(session, -1)),
                    ContextMenus.Entry.of("Move Down", movable, () -> UiEdits.moveSelected(session, 1)));
        });
    }

    // ------------------------------------------------------------------ прокрутка

    /**
     * Прокручивает дерево так, чтобы выбранная строка была видна: строка уже в видимой части остаётся на месте, иначе
     * ставится в середину. Вызывается при отрисовке, когда раскладка строк известна.
     *
     * @return {@code true}, если прокрутка выполнялась
     */
    boolean revealSelected() {
        if (!scrollPending || scroll == null) return false;
        TreeViewNode selected = tree.selectedNode();
        if (selected == null || selected.rowButton() == null) return false;
        var row = selected.rowButton().layoutBounds();
        var content = tree.layoutBounds();
        var view = scroll.layoutBounds();
        if (view.height() <= 0.0f || row.height() <= 0.0f) return false; // раскладки ещё нет
        scrollPending = false;
        float top = row.y() - content.y();
        float visibleTop = scroll.scrollY();
        if (top >= visibleTop && top + row.height() <= visibleTop + view.height()) return true;
        scroll.scrollTo(0.0f, Math.max(0.0f, top - view.height() / 2.0f));
        return true;
    }

    /** Сворачивает или разворачивает узел; нужно тестам и командам. */
    void toggle(UiNode node) {
        TreeViewNode treeNode = nodes.get(node);
        if (treeNode == null) return;
        boolean expand = !treeNode.expanded();
        if (expand) collapsed.remove(node);
        else collapsed.add(node);
        treeNode.expanded(expand);
    }

    /** @return число строк, которые сейчас видны (развёрнутые ветки считаются, свёрнутые нет) */
    int visibleRows() {
        return tree.visibleNodes().size();
    }

    /** @return дерево; нужно тестам */
    TreeView tree() {
        return tree;
    }

    /** @return узел дерева для узла документа; нужно тестам */
    TreeViewNode treeNodeOf(UiNode node) {
        return nodes.get(node);
    }
}
