package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.ContextMenus;
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
    private final java.util.Set<String> collapsed = new java.util.HashSet<>();
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
                addGroup(session, category.getKey(), category.getValue());
            }
        } finally {
            syncing = false;
        }
        if (Anchors.supported(session, shown)) {
            Button header = new Button("Anchor");
            header.id("inspector.group.Anchor");
            content.addChild(header);
            content.addChild(Anchors.picker(session, shown, this::rebuild));
        }
        for (UiProperty unknown : inspection.unknown()) {
            content.addChild(new Label(unknown.displayName() + " (not in schema)"));
        }
        content.applyQueuedMutations();
    }

    /** Группа свойств с заголовком-переключателем; свёрнутые группы запоминаются между перестроениями. */
    private void addGroup(EditorSession session, String category, List<Inspector.PropertyView> properties) {
        VBox body = new VBox();
        body.spacing(2.0f);
        Button header = new Button((collapsed.contains(category) ? "▸ " : "▾ ") + category);
        header.id("inspector.group." + category);
        header.onClick(click -> {
            boolean now = collapsed.add(category);
            if (!now) collapsed.remove(category);
            header.text((now ? "▸ " : "▾ ") + category);
            body.visibility(now ? Visibility.COLLAPSED : Visibility.VISIBLE);
        });
        content.addChild(header);
        for (Inspector.PropertyView property : properties) addRow(session, property, body);
        body.visibility(collapsed.contains(category) ? Visibility.COLLAPSED : Visibility.VISIBLE);
        body.applyQueuedMutations();
        content.addChild(body);
    }

    private void addRow(EditorSession session, Inspector.PropertyView property, VBox into) {
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

        // «Очистить» живёт в контекстном меню строки (правая кнопка), а не в кнопке на каждой строке.
        ContextMenus.onSecondaryClick(row, context(), () -> List.of(
                ContextMenus.Entry.of("Clear", shown != null && shown.attributeText(property.name()).isPresent(),
                        clear)));
        row.applyQueuedMutations();
        Label problem = new Label(property.problem() == null ? "" : property.problem());
        problem.color(PROBLEM);
        problems.put(property.name(), problem);
        into.addChild(row);
        into.addChild(problem);
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
