package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.schema.TypeInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Список всех типов виджетов схемы по категориям. Щелчок добавляет новый виджет в выбранный контейнер.
 */
final class PalettePanel extends SessionPanel {
    private final VBox entries = new VBox();
    private final TextField filter = new TextField();

    @Override
    public String id() {
        return UiEditor.PALETTE;
    }

    @Override
    public String title() {
        return "Palette";
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
        filter.placeholder("Search widgets");
        filter.onTextChanged(event -> refresh());
        entries.spacing(1.0f);
        ScrollView scroll = new ScrollView(entries);
        scroll.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));
        root.addChild(filter);
        root.addChild(scroll);
        root.applyQueuedMutations();
        return root;
    }

    @Override
    protected void bound(EditorSession next) {
        refresh();
    }

    private void refresh() {
        entries.clearChildren();
        EditorSession session = session().orElse(null);
        if (session == null) {
            entries.applyQueuedMutations();
            return;
        }
        String query = filter.text().trim().toLowerCase(Locale.ROOT);
        List<TypeInfo<?>> types = new ArrayList<>(session.schema().types());
        types.sort(Comparator.comparing((TypeInfo<?> type) -> type.category().toLowerCase(Locale.ROOT))
                .thenComparing(type -> type.name().local().toLowerCase(Locale.ROOT)));
        String category = null;
        for (TypeInfo<?> type : types) {
            String name = type.name().local();
            if (!query.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(query)) continue;
            if (!type.category().equals(category)) {
                category = type.category();
                entries.addChild(new Label(category));
            }
            Button button = new Button("  " + name);
            button.onClick(event -> UiEdits.insert(session, type));
            entries.addChild(button);
        }
        entries.applyQueuedMutations();
    }
}
