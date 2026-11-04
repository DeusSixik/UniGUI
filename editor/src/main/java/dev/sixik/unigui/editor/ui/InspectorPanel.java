package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.edit.Edit;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.Edits;
import dev.sixik.unigui.xaml.edit.Inspector;
import dev.sixik.unigui.xaml.edit.SessionChange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Свойства выбранного узла. Пустое поле означает «не задано» (действует значение по умолчанию), любое другое
 * значение записывается в документ правкой с историей отмены. Некорректное значение подсвечивается под полем.
 */
final class InspectorPanel extends SessionPanel {
    private static final MutableColor PROBLEM = MutableColor.rgba255(235, 100, 100, 255);

    private static final String NOT_SET = "(default)";

    private final VBox content = new VBox();
    private final Map<String, TextField> fields = new LinkedHashMap<>();
    private final Map<String, ComboBox> combos = new LinkedHashMap<>();
    private final Map<String, Label> problems = new LinkedHashMap<>();
    private UiNode shown;
    private boolean syncing;
    private boolean applying;

    @Override
    public String id() {
        return UiEditor.INSPECTOR;
    }

    @Override
    public String title() {
        return "Inspector";
    }

    @Override
    public PanelSlot slot() {
        return PanelSlot.RIGHT;
    }

    @Override
    public float share() {
        return 0.28f;
    }

    @Override
    protected Widget build(EditorContext editorContext) {
        content.spacing(2.0f);
        ScrollView scroll = new ScrollView(content);
        scroll.layout(style -> style.sizePercent(100.0f, 100.0f));
        return scroll;
    }

    @Override
    protected void bound(EditorSession next) {
        rebuild();
    }

    @Override
    protected void changed(EditorSession current, SessionChange change) {
        switch (change.kind()) {
            case SELECTION_CHANGED, REVERTED -> rebuild();
            case DOCUMENT_CHANGED -> {
                // Свои же правки поля не пересобирают: иначе поле теряло бы фокус на каждой букве.
                if (applying) refreshProblems();
                else rebuild();
            }
            default -> { }
        }
    }

    private void rebuild() {
        content.clearChildren();
        fields.clear();
        combos.clear();
        problems.clear();
        EditorSession session = session().orElse(null);
        shown = session == null ? null : session.selected().orElse(null);
        if (shown == null) {
            content.addChild(new Label(session == null ? "No document" : "Select a widget"));
            content.applyQueuedMutations();
            return;
        }

        Inspector.Inspection inspection = Inspector.inspect(session.schema(), shown);
        content.addChild(new Label(inspection.typeName() + (inspection.known() ? "" : " (unknown type)")));
        if (!inspection.description().isBlank()) content.addChild(new Label(inspection.description()));

        syncing = true;
        try {
            for (Map.Entry<String, List<Inspector.PropertyView>> category : inspection.byCategory().entrySet()) {
                content.addChild(new Label(category.getKey()));
                for (Inspector.PropertyView property : category.getValue()) addRow(session, property);
            }
        } finally {
            syncing = false;
        }
        for (UiProperty unknown : inspection.unknown()) {
            content.addChild(new Label(unknown.displayName() + " (not in schema)"));
        }
        content.applyQueuedMutations();
    }

    private void addRow(EditorSession session, Inspector.PropertyView property) {
        HBox row = new HBox();
        row.spacing(4.0f);
        Label name = new Label(property.displayName());
        name.layout(style -> style.width(110.0f).flexShrink(0.0f));
        row.addChild(name);

        Widget editor;
        Runnable clear;
        if (property.options().isEmpty()) {
            TextField field = new TextField(property.valueText() == null ? "" : property.valueText());
            field.placeholder(property.defaultValue());
            field.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
            field.onTextChanged(event -> {
                if (!syncing) commit(session, property.name(), field.text());
            });
            fields.put(property.name(), field);
            editor = field;
            clear = () -> field.text("");
        } else {
            // Перечисления и логические свойства выбираются из списка, а не печатаются: опечатка невозможна.
            // У списка всегда есть выбранный пункт, поэтому «не задано» — отдельный первый пункт.
            ComboBox combo = new ComboBox();
            List<String> items = new ArrayList<>();
            items.add(NOT_SET);
            items.addAll(property.options());
            combo.items(items);
            combo.silentSelectedIndex(property.valueText() == null ? 0 : items.indexOf(property.valueText()));
            combo.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
            combo.onSelectionChanged(event -> {
                int index = combo.selectedIndex();
                if (syncing || index < 0) return;
                commit(session, property.name(), index == 0 ? "" : items.get(index));
            });
            combos.put(property.name(), combo);
            editor = combo;
            clear = () -> {
                combo.silentSelectedIndex(0);
                commit(session, property.name(), "");
            };
        }
        row.addChild(editor);

        Button reset = new Button("×");
        reset.onClick(event -> clear.run());
        row.addChild(reset);
        row.applyQueuedMutations();
        Label problem = new Label(property.problem() == null ? "" : property.problem());
        problem.color(PROBLEM);
        problems.put(property.name(), problem);
        content.addChild(row);
        content.addChild(problem);
    }

    private void commit(EditorSession session, String name, String text) {
        UiNode node = shown;
        if (node == null) return;
        Edit edit;
        if (text.isBlank()) {
            if (node.attributeText(name).isEmpty()) return;
            edit = Edits.removeProperty(node, name);
        } else {
            edit = Edits.setProperty(node, name, text);
        }
        applying = true;
        try {
            session.apply(edit);
        } finally {
            applying = false;
        }
        refreshProblems();
    }

    /** Обновляет подписи ошибок под полями, не пересоздавая сами поля. */
    private void refreshProblems() {
        EditorSession session = session().orElse(null);
        if (session == null || shown == null) return;
        Inspector.Inspection inspection = Inspector.inspect(session.schema(), shown);
        for (Inspector.PropertyView property : inspection.properties()) {
            Label label = problems.get(property.name());
            if (label != null) label.text(property.problem() == null ? "" : property.problem());
        }
    }
}
