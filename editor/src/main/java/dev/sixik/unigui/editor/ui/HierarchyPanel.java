package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.dnd.DragController;
import dev.sixik.unigui.widgets.dnd.DragGhost;
import dev.sixik.unigui.widgets.dnd.DragPayload;
import dev.sixik.unigui.widgets.dnd.DropController;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.Hierarchy;
import dev.sixik.unigui.xaml.edit.SessionChange;

import java.util.List;

/**
 * Дерево узлов документа. Щелчок по строке выбирает узел; фильтр оставляет строки, в которых есть введённый текст.
 */
final class HierarchyPanel extends SessionPanel {
    private static final MutableColor SELECTED = MutableColor.rgba255(90, 160, 255, 255);
    private static final MutableColor HOVER = MutableColor.rgba255(170, 210, 255, 255);
    private static final MutableColor NORMAL = MutableColor.rgba255(220, 220, 220, 255);
    private static final MutableColor UNKNOWN = MutableColor.rgba255(235, 100, 100, 255);

    private final VBox rows = new VBox();
    private final TextField filter = new TextField();
    private final Label empty = new Label("No document");

    @Override
    public String id() {
        return UiEditor.HIERARCHY;
    }

    @Override
    public String title() {
        return "Hierarchy";
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
        root.spacing(4.0f);
        root.layout(style -> style.sizePercent(100.0f, 100.0f));
        filter.placeholder("Filter");
        filter.onTextChanged(event -> refresh());
        rows.spacing(1.0f);
        ScrollView scroll = new ScrollView(rows);
        scroll.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));
        root.addChild(filter);
        root.addChild(scroll);
        root.addChild(empty);
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
            case DOCUMENT_CHANGED, SELECTION_CHANGED, REVERTED -> refresh();
            default -> { }
        }
    }

    /** Узлы, свёрнутые автором; сравниваются по идентичности, потому что правка документа не создаёт узлы заново. */
    private final java.util.Set<UiNode> collapsed = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private UiNode revealed;

    private final java.util.Map<UiNode, Button> buttons = new java.util.IdentityHashMap<>();
    private boolean hoverBound;

    /** Подсвечивает строку узла, над которым указатель в другой панели, не перестраивая список. */
    private void paintHover() {
        UiNode hovered = hover().node();
        EditorSession session = session().orElse(null);
        UiNode selected = session == null ? null : session.selected().orElse(null);
        buttons.forEach((node, button) -> {
            if (node == selected) button.textColor().set(SELECTED);
            else if (node == hovered) button.textColor().set(HOVER);
            else button.textColor().set(NORMAL);
        });
    }

    private void refresh() {
        if (!hoverBound) {
            hoverBound = true;
            hover().subscribe(this::paintHover);
        }
        buttons.clear();
        rows.clearChildren();
        EditorSession session = session().orElse(null);
        empty.text(session == null ? "No document" : "");
        if (session == null) {
            rows.applyQueuedMutations();
            return;
        }
        UiNode selected = session.selected().orElse(null);
        if (selected != revealed) {
            // Выбор из другой панели (щелчок в Viewport) раскрывает путь к узлу, чтобы строка была видна.
            revealed = selected;
            for (UiNode up = selected; up != null; up = Hierarchy.parentOf(session.document(), up).orElse(null)) {
                if (up != selected) collapsed.remove(up);
            }
        }
        boolean filtering = !filter.text().isBlank();
        List<Hierarchy.Row> list = Hierarchy.filter(Hierarchy.rows(session.document(), session.schema()), filter.text());
        int hiddenBelow = Integer.MAX_VALUE; // глубина свёрнутого узла: всё глубже скрыто
        for (Hierarchy.Row row : list) {
            if (!filtering) {
                if (row.depth() > hiddenBelow) continue;
                hiddenBelow = Integer.MAX_VALUE;
            }
            boolean hasChildren = row.childCount() > 0;
            boolean isCollapsed = hasChildren && collapsed.contains(row.node());
            if (!filtering && isCollapsed) hiddenBelow = row.depth();

            HBox line = new HBox();
            line.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));
            Label twisty = new Label(hasChildren ? (isCollapsed ? "▸" : "▾") : " ");
            twisty.layout(style -> style.width(14.0f).flexShrink(0.0f));
            if (hasChildren && !filtering) {
                twisty.on(PointerPressedEvent.TYPE, event -> {
                    if (!collapsed.remove(row.node())) collapsed.add(row.node());
                    refresh();
                    event.cancel();
                });
            }
            Button button = new Button("  ".repeat(row.depth()) + row.label());
            button.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
            if (row.node() == selected) button.textColor().set(SELECTED);
            else if (!row.known()) button.textColor().set(UNKNOWN);
            button.onClick(event -> session.select(row.node()));
            button.on(dev.sixik.unigui.api.event.PointerEnteredEvent.TYPE, event -> hover().set(row.node()));
            button.on(dev.sixik.unigui.api.event.PointerExitedEvent.TYPE, event -> {
                if (hover().node() == row.node()) hover().set(null);
            });
            buttons.put(row.node(), button);
            DropController.attach(button).accepts(DropActions.ACCEPTED)
                    .onDragEnter(event -> button.textColor().set(HOVER))
                    .onDragLeave(event -> paintHover())
                    .onDrop(event -> {
                        DropActions.drop(session, event.payload(), row.node());
                        paintHover();
                    });
            if (row.node() != session.document().root()) {
                DragController.attach(button).dragged(button).threshold(4.0f)
                        .payload(() -> DragPayload.of(row.label(), DropActions.NODE_TYPE, row.label(), row.node()))
                        .ghost(DragGhost.text());
            }
            line.addChild(twisty);
            line.addChild(button);
            line.applyQueuedMutations();
            rows.addChild(line);
        }
        rows.applyQueuedMutations();
    }

    /** Сворачивает или разворачивает узел; нужно тестам и командам. */
    void toggle(UiNode node) {
        if (!collapsed.remove(node)) collapsed.add(node);
        refresh();
    }
}
