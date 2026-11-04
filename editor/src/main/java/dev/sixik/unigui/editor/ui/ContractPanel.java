package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.math.MutableColor;
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
import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.component.Contract;
import dev.sixik.unigui.xaml.component.ContractEdits;
import dev.sixik.unigui.xaml.edit.Edit;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.ElementListEdit;
import dev.sixik.unigui.xaml.edit.SessionChange;
import dev.sixik.unigui.xaml.preview.ComponentPreview;

import java.util.List;
import java.util.Optional;

/**
 * Контракт компонента: параметры, переменные и события, которые автор объявляет в {@code Component.Contract}, плюс
 * примерные значения параметров, отправка входящих событий и журнал исходящих для предпросмотра.
 *
 * <p>Любая правка идёт через {@link ContractEdits}: плохое имя, повтор или несовместимое значение по умолчанию
 * отклоняются с причиной под таблицей, а документ остаётся прежним. Поля не пересобираются от своих же правок, чтобы
 * ввод не терял фокус; структурные правки (добавить, удалить) и правки из других панелей пересобирают панель.</p>
 *
 * <p>Идентификаторы виджетов {@code param.Имя.Атрибут}, {@code var.…}, {@code event.…}, {@code add.Param},
 * {@code remove.Param.Имя}, {@code sample.Имя}, {@code send.Имя} стабильны и нужны тестам и автоматизации.</p>
 */
final class ContractPanel extends SessionPanel {
    /** Идентификатор панели в каркасе. */
    static final String ID = "contract";

    private static final MutableColor PROBLEM = MutableColor.rgba255(235, 100, 100, 255);
    private static final List<String> DIRECTIONS = List.of("Out", "In");
    private static final List<String> BOOLEANS = List.of("false", "true");

    private final ComponentView componentView;
    private final VBox content = new VBox();
    private final Label problem = new Label("");
    private final VBox log = new VBox();
    private boolean syncing;
    private boolean applying;

    ContractPanel(ComponentView componentView) {
        this.componentView = componentView;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String title() {
        return "Contract";
    }

    @Override
    public PanelSlot slot() {
        return PanelSlot.LEFT;
    }

    @Override
    protected Widget build(EditorContext editorContext) {
        content.spacing(2.0f);
        problem.color(PROBLEM);
        ScrollView scroll = new ScrollView(content);
        scroll.layout(style -> style.sizePercent(100.0f, 100.0f));
        componentView.subscribe(this::refreshLog);
        return scroll;
    }

    @Override
    protected void bound(EditorSession next) {
        rebuild();
    }

    @Override
    protected void changed(EditorSession current, SessionChange change) {
        switch (change.kind()) {
            case DOCUMENT_CHANGED -> {
                if (!applying) rebuild();
            }
            case REVERTED -> rebuild();
            default -> { }
        }
    }

    // ------------------------------------------------------------------ построение

    private void rebuild() {
        content.clearChildren();
        EditorSession session = session().orElse(null);
        UiNode root = session == null ? null : session.document().root();
        if (root == null || !root.type().local().equals("Component")) {
            content.addChild(new Label(session == null ? "No document" : "Not a component document"));
            content.applyQueuedMutations();
            return;
        }
        syncing = true;
        try {
            for (String kind : List.of("Param", "Var")) {
                section(session, root, kind, kind.equals("Param") ? "Parameters" : "Variables");
            }
            eventSection(session, root);
            content.addChild(problem);
            previewSection(session, root);
        } finally {
            syncing = false;
        }
        content.applyQueuedMutations();
    }

    private void section(EditorSession session, UiNode root, String kind, String title) {
        content.addChild(header(title, "add." + kind, () -> {
            String name = ContractEdits.freshName(root, kind);
            structural(session, kind.equals("Param")
                    ? ContractEdits.addParam(root, name, "string", null)
                    : ContractEdits.addVar(root, name, "string", null));
        }));
        for (UiNode node : contractNodes(root, kind)) {
            String name = node.attributeText("Name", "");
            VBox card = card(session, root, kind, name);
            HBox fields = row();
            fields.addChild(field(session, root, kind, name, "Type", node.attributeText("Type", ""), "type"));
            fields.addChild(field(session, root, kind, name, "Default", node.attributeText("Default", ""), "default"));
            fields.applyQueuedMutations();
            card.addChild(fields);
            card.applyQueuedMutations();
            content.addChild(card);
        }
    }

    private void eventSection(EditorSession session, UiNode root) {
        content.addChild(header("Events", "add.Event", () ->
                structural(session, ContractEdits.addEvent(root, ContractEdits.freshName(root, "Event"), "Out", null, false))));
        for (UiNode node : contractNodes(root, "Event")) {
            String name = node.attributeText("Name", "");
            VBox card = card(session, root, "Event", name);
            HBox options = row();
            options.addChild(combo(session, root, "Event", name, "Direction", DIRECTIONS,
                    node.attributeText("Direction", "Out"), false));
            options.addChild(combo(session, root, "Event", name, "Coalesce", BOOLEANS,
                    node.attributeText("Coalesce", "false"), true));
            options.applyQueuedMutations();
            card.addChild(options);
            card.addChild(field(session, root, "Event", name, "Args", node.attributeText("Args", ""),
                    "args: name:type, name:type"));
            card.applyQueuedMutations();
            content.addChild(card);
        }
    }

    /**
     * Карточка записи контракта: имя и меню действий в первой строке, поля ниже. Всё тянется по ширине панели, поэтому
     * при любой ширине подписи и поля остаются целиком видимыми.
     */
    private VBox card(EditorSession session, UiNode root, String kind, String name) {
        VBox card = new VBox();
        card.spacing(2.0f);
        card.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));
        HBox head = row();
        Label title = nameLabel(name);
        head.addChild(title);
        Button menu = ContextMenus.button(context(), () -> List.of(
                ContextMenus.Entry.of("Delete", () -> structural(session, ContractEdits.remove(root, kind, name))),
                ContextMenus.Entry.of("Clear default", !kind.equals("Event"),
                        () -> commit(session, ContractEdits.setAttribute(root, kind, name, "Default", null))),
                ContextMenus.Entry.of("Clear description",
                        () -> commit(session, ContractEdits.setAttribute(root, kind, name, "Description", null)))));
        menu.id("menu." + kind + "." + name);
        head.addChild(menu);
        head.applyQueuedMutations();
        card.addChild(head);
        // Правая кнопка по заголовку открывает то же меню.
        ContextMenus.onSecondaryClick(head, context(), () -> List.of(
                ContextMenus.Entry.of("Delete", () -> structural(session, ContractEdits.remove(root, kind, name)))));
        return card;
    }

    private void previewSection(EditorSession session, UiNode root) {
        Optional<ComponentPreview> preview = componentView.preview();
        content.addChild(new Label("Preview"));
        ComponentDefinition definition = preview.flatMap(ComponentPreview::definition).orElse(null);
        if (preview.isEmpty() || definition == null) {
            content.addChild(new Label("The component does not build yet"));
        } else {
            for (Contract.Param param : definition.contract().params()) {
                if (!param.type().textual()) continue;
                HBox row = row();
                Label sampleName = new Label(param.name());
                sampleName.layout(style -> style.width(70.0f).flexShrink(0.0f));
                row.addChild(sampleName);
                Object current = preview.get().host().orElseThrow().instance().get(param.name());
                TextField sample = new TextField(current == null ? "" : String.valueOf(current));
                sample.id("sample." + param.name());
                sample.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
                sample.onTextChanged(event -> {
                    if (!syncing) setSample(preview.get(), param, sample.text());
                });
                row.addChild(sample);
                row.applyQueuedMutations();
                content.addChild(row);
            }
            for (Contract.Event event : definition.contract().events()) {
                if (event.direction() != Contract.Direction.IN || !event.args().isEmpty()) continue;
                Button send = new Button("Send " + event.name());
                send.id("send." + event.name());
                send.onClick(click -> {
                    try {
                        preview.get().send(event.name(), Args.empty());
                        problem.text("");
                    } catch (RuntimeException failure) {
                        problem.text(failure.getMessage());
                    }
                });
                content.addChild(send);
            }
        }
        content.addChild(new Label("Events"));
        log.layout(style -> style.flexShrink(0.0f));
        content.addChild(log);
        refreshLog();
    }

    private void setSample(ComponentPreview preview, Contract.Param param, String text) {
        try {
            preview.sample(param.name(), param.type().parse(text, dev.sixik.unigui.xaml.schema.LoadContext.DEFAULT));
            problem.text("");
        } catch (RuntimeException failure) {
            problem.text("Sample " + param.name() + ": " + failure.getMessage());
        }
    }

    private void refreshLog() {
        log.clearChildren();
        for (String line : componentView.preview().map(ComponentPreview::events).orElse(List.of())) {
            log.addChild(new Label(line));
        }
        log.applyQueuedMutations();
    }

    // ------------------------------------------------------------------ части строк

    private Widget header(String title, String addId, Runnable add) {
        HBox row = row();
        Label label = new Label(title);
        label.layout(style -> style.flexGrow(1.0f));
        row.addChild(label);
        Button button = new Button("+");
        button.id(addId);
        button.onClick(click -> add.run());
        row.addChild(button);
        row.applyQueuedMutations();
        return row;
    }

    private static HBox row() {
        HBox row = new HBox();
        row.spacing(4.0f);
        row.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));
        return row;
    }

    private static Label nameLabel(String name) {
        Label label = new Label(name);
        label.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        return label;
    }

    private TextField field(EditorSession session, UiNode root, String kind, String name, String attribute,
                            String value, String placeholder) {
        TextField field = new TextField(value);
        field.id(kind.toLowerCase() + "." + name + "." + attribute);
        field.placeholder(placeholder);
        field.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        field.onTextChanged(event -> {
            if (!syncing) commit(session, ContractEdits.setAttribute(root, kind, name, attribute, field.text()));
        });
        return field;
    }

    private ComboBox combo(EditorSession session, UiNode root, String kind, String name, String attribute,
                           List<String> items, String value, boolean clearWhenFirst) {
        ComboBox combo = new ComboBox();
        combo.id(kind.toLowerCase() + "." + name + "." + attribute);
        combo.items(items);
        combo.silentSelectedIndex(Math.max(0, items.indexOf(value)));
        combo.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        combo.onSelectionChanged(event -> {
            int index = combo.selectedIndex();
            if (syncing || index < 0) return;
            String chosen = items.get(index);
            commit(session, ContractEdits.setAttribute(root, kind, name, attribute,
                    clearWhenFirst && index == 0 ? null : chosen));
        });
        return combo;
    }

    // ------------------------------------------------------------------ правки

    /** Правка поля: панель не пересобирается, чтобы ввод не терял фокус. */
    private void commit(EditorSession session, Edit edit) {
        applying = true;
        try {
            report(session, session.apply(edit));
        } finally {
            applying = false;
        }
    }

    /** Структурная правка: меняет набор строк, поэтому панель пересобирается. */
    private void structural(EditorSession session, Edit edit) {
        boolean done = session.apply(edit);
        report(session, done);
        if (done) rebuild();
    }

    private void report(EditorSession session, boolean done) {
        if (done) {
            problem.text("");
            return;
        }
        List<dev.sixik.unigui.xaml.UiDiagnostic> found = session.diagnostics(EditorSession.Channel.EDIT);
        problem.text(found.isEmpty() ? "The edit was rejected" : found.get(0).message());
    }

    private static List<UiNode> contractNodes(UiNode root, String kind) {
        return ElementListEdit.items(root, "Component", "Contract").stream()
                .filter(UiNode.class::isInstance).map(UiNode.class::cast)
                .filter(node -> node.type().local().equals(kind)).toList();
    }
}
