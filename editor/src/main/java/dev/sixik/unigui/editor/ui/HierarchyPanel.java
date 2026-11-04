package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
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

    private void refresh() {
        rows.clearChildren();
        EditorSession session = session().orElse(null);
        empty.text(session == null ? "No document" : "");
        if (session == null) {
            rows.applyQueuedMutations();
            return;
        }
        UiNode selected = session.selected().orElse(null);
        List<Hierarchy.Row> list = Hierarchy.filter(Hierarchy.rows(session.document(), session.schema()), filter.text());
        for (Hierarchy.Row row : list) {
            Button button = new Button("  ".repeat(row.depth()) + row.label());
            if (row.node() == selected) button.textColor().set(SELECTED);
            else if (!row.known()) button.textColor().set(UNKNOWN);
            button.onClick(event -> session.select(row.node()));
            rows.addChild(button);
        }
        rows.applyQueuedMutations();
    }
}
