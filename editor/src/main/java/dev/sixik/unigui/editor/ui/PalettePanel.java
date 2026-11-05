package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.dnd.DragController;
import dev.sixik.unigui.widgets.dnd.DragGhost;
import dev.sixik.unigui.widgets.dnd.DragPayload;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.widgets.navigation.TreeView;
import dev.sixik.unigui.widgets.navigation.TreeViewNode;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.schema.TypeInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Типы виджетов схемы списком по группам, как меню Create в Unity: группа раскрывается и сворачивается, внутри неё
 * типы по алфавиту. Щелчок по типу добавляет его в выбранный контейнер, перетаскивание бросает его на предпросмотр или
 * на строку иерархии. Поиск оставляет подходящие типы и раскрывает их группы.
 */
final class PalettePanel extends SessionPanel {
    /** Тип данных перетаскивания: в {@link DragPayload#data} лежит {@link TypeInfo}. */
    static final String PAYLOAD_TYPE = "unigui.widget";

    private final TreeView tree = new TreeView();
    private final TextField filter = EditorStyles.searchField();
    /** Свёрнутые группы запоминаются между обновлениями. */
    private final Set<String> collapsedGroups = new HashSet<>();

    @Override
    public String id() {
        return UiEditor.PALETTE;
    }

    @Override
    public String title() {
        return "Palette";
    }

    @Override
    public dev.sixik.unigui.api.render.UiIcon icon() {
        return dev.sixik.unigui.editor.style.EditorIcons.PALETTE;
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
        filter.id("palette.search");
        filter.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));
        filter.onTextChanged(event -> refresh());

        EditorStyles.styleTreeView(tree);
        tree.iconMode(TreeView.TreeIconMode.NONE);
        tree.layout(style -> style.widthPercent(100.0f));
        tree.onRowCreated(this::rowCreated);
        ScrollView scroll = new ScrollView(tree);
        EditorStyles.styleScroll(scroll);
        scroll.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));
        root.addChild(EditorStyles.searchBar(filter));
        root.addChild(scroll);
        root.applyQueuedMutations();
        return root;
    }

    @Override
    protected void bound(EditorSession next) {
        refresh();
    }

    private void refresh() {
        tree.beginBatch();
        tree.clearRoots();
        EditorSession session = session().orElse(null);
        if (session != null) {
            String query = filter.text().trim().toLowerCase(Locale.ROOT);
            List<TypeInfo<?>> types = new ArrayList<>(session.schema().types());
            types.sort(Comparator.comparing((TypeInfo<?> type) -> type.category().toLowerCase(Locale.ROOT))
                    .thenComparing(type -> type.name().local().toLowerCase(Locale.ROOT)));
            TreeViewNode group = null;
            String category = null;
            for (TypeInfo<?> type : types) {
                String name = type.name().local();
                if (!query.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(query)) continue;
                if (!type.category().equals(category)) {
                    category = type.category();
                    group = new TreeViewNode(category);
                    group.folder(true);
                    group.noIcon();
                    group.userData(category);
                    tree.addRoot(group);
                }
                TreeViewNode entry = new TreeViewNode(name);
                entry.userData(type);
                UiIcon icon = EditorIcons.forWidget(name);
                entry.iconDraw((draw, owner, x, y, width, height) -> icon.draw(draw, x + 2.0f, y + 2.0f, width - 4.0f,
                        EditorStyles.TEXT_LABEL));
                group.addChild(entry);
            }
            for (TreeViewNode root : tree.roots()) {
                // Поиск раскрывает все группы с совпадениями: короткий список не должен прятать найденное.
                root.silentExpanded(!query.isEmpty() || !collapsedGroups.contains((String) root.userData()));
            }
        }
        tree.endBatch();
    }

    private void rowCreated(TreeView.TreeRowButton row) {
        TreeViewNode node = row.node();
        if (!(node.userData() instanceof TypeInfo<?> type)) return;
        String name = type.name().local();
        row.id("palette." + name);
        row.onClick(click -> session().ifPresent(session -> UiEdits.insert(session, type)));
        DragController.attach(row).dragged(row).threshold(4.0f)
                .payload(() -> DragPayload.of(name, PAYLOAD_TYPE, name, type))
                .ghost(DragGhost.text());
    }

    /** Сворачивает или разворачивает группу по имени; нужно тестам и командам. */
    void toggleGroup(String category) {
        for (TreeViewNode root : tree.roots()) {
            if (category.equals(root.userData())) {
                boolean expand = !root.expanded();
                if (expand) collapsedGroups.remove(category);
                else collapsedGroups.add(category);
                root.expanded(expand);
            }
        }
    }

    /** @return группы палитры в порядке показа; нужно тестам */
    List<String> groups() {
        List<String> names = new ArrayList<>();
        for (TreeViewNode root : tree.roots()) names.add((String) root.userData());
        return names;
    }

    /** @return число видимых строк (группы и развёрнутые типы); нужно тестам */
    int visibleRows() {
        return tree.visibleNodes().size();
    }
}
