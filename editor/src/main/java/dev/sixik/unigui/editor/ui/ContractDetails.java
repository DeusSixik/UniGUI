package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.event.CheckedChangedEvent;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.widget.CheckboxState;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.editor.style.FlatField;
import dev.sixik.unigui.editor.style.ToolButton;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.component.ContractEdits;
import dev.sixik.unigui.xaml.component.ParamType;
import dev.sixik.unigui.xaml.component.Renames;
import dev.sixik.unigui.xaml.edit.Edit;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.ElementListEdit;
import dev.sixik.unigui.xaml.edit.ValueEditing;
import dev.sixik.unigui.xaml.logic.GraphDefinition;
import dev.sixik.unigui.xaml.logic.GraphEdits;

import java.util.ArrayList;
import java.util.List;

/**
 * Свойства записи контракта в инспекторе: имя, тип, значение по умолчанию, описание, аргументы события. Панель контракта
 * только перечисляет записи, а всё, что у них настраивается, живёт здесь, как детали переменной в Unreal.
 *
 * <p>Идентификаторы виджетов {@code entry.name}, {@code param.Имя.Type}, {@code param.Имя.Default}, {@code var.…},
 * {@code event.Имя.Direction}, {@code event.Имя.Coalesce}, {@code arg.Событие.Аргумент.*}, {@code arg.add.Событие}
 * стабильны и нужны тестам.</p>
 */
final class ContractDetails {
    private static final float LABEL_WIDTH = 100.0f;
    private static final List<String> DIRECTIONS = List.of("Incoming", "Outgoing");

    /** Аргумент события в записи документа. */
    private record Argument(String name, String type) {
    }

    private final EditorSession session;
    private final UiNode root;
    private final ComponentView view;
    private final ComponentView.Entry entry;
    private final Runnable rebuild;
    private final Label problem = new Label("");
    private boolean syncing;
    private boolean applying;

    /**
     * @param session сессия
     * @param view    общий выбор записи
     * @param entry   показанная запись
     * @param rebuild пересобирает инспектор после структурной правки
     */
    ContractDetails(EditorSession session, ComponentView view, ComponentView.Entry entry, Runnable rebuild) {
        this.session = session;
        this.root = session.document().root();
        this.view = view;
        this.entry = entry;
        this.rebuild = rebuild;
    }

    /** @return {@code true}, пока идёт правка из поля: инспектор не должен пересобираться и терять фокус */
    boolean applying() {
        return applying;
    }

    /**
     * @param session сессия
     * @param entry   запись
     * @return {@code true}, если запись ещё есть в документе
     */
    static boolean exists(EditorSession session, ComponentView.Entry entry) {
        UiNode root = session.document().root();
        if (!root.type().local().equals("Component")) return false;
        if (entry.kind().equals(ComponentView.CUSTOM)) return customNode(root, entry.name()) != null;
        return contractNode(root, entry.kind(), entry.name()) != null;
    }

    /** Заполняет контейнер инспектора. */
    void fill(VBox into) {
        syncing = true;
        try {
            into.addChild(heading());
            VBox body = new VBox();
            body.spacing(3.0f);
            body.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f).padding(0.0f, 1.0f, 0.0f, 6.0f));
            body.addChild(row("Name", nameField()));
            switch (entry.kind()) {
                case "Param", "Var" -> value(body);
                case "Event" -> event(body);
                default -> custom();
            }
            body.applyQueuedMutations();
            into.addChild(body);
            if (arguments != null) into.addChild(arguments);
            problem.color(EditorStyles.ERROR_RED);
            problem.visibility(Visibility.COLLAPSED);
            into.addChild(problem);
            Label hint = EditorStyles.caption("Drag it onto the graph or a property.");
            hint.layout(style -> style.widthPercent(100.0f).margin(2.0f, 6.0f, 2.0f, 2.0f));
            into.addChild(hint);
        } finally {
            syncing = false;
        }
    }

    private VBox arguments;

    private Widget heading() {
        String title = switch (entry.kind()) {
            case "Param" -> "Parameter";
            case "Var" -> "Variable";
            case "Event" -> "Event";
            default -> "Custom event";
        };
        Label label = new Label(title);
        label.color(EditorStyles.TEXT_BOLD);
        label.layout(style -> style.margin(2.0f, 0.0f, 0.0f, 6.0f));
        return label;
    }

    private static HBox row(String title, Widget editor) {
        HBox row = new HBox();
        row.spacing(4.0f);
        row.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f).minHeight(EditorStyles.ROW_HEIGHT));
        Label label = new Label(title);
        label.color(EditorStyles.TEXT_LABEL);
        label.layout(style -> style.width(LABEL_WIDTH).height(EditorStyles.ROW_HEIGHT).flexShrink(0.0f));
        row.addChild(label);
        row.addChild(editor);
        row.applyQueuedMutations();
        return row;
    }

    private Widget nameField() {
        FlatField field = new FlatField(entry.name(), this::rename).boxed();
        field.id("entry.name");
        return field;
    }

    /** Новое имя применяется рефакторингом: контракт, тело и граф меняются одним шагом отмены. */
    private void rename(String next) {
        boolean done = switch (entry.kind()) {
            case "Param" -> Renames.param(session, entry.name(), next);
            case "Var" -> Renames.variable(session, entry.name(), next);
            case "Event" -> Renames.event(session, entry.name(), next);
            default -> Renames.customEvent(session, entry.name(), next);
        };
        if (done) {
            view.entry(new ComponentView.Entry(entry.kind(), next));
        } else {
            String reason = reason();
            rebuild.run();
            view.problem(reason);
        }
    }

    // ------------------------------------------------------------------ параметры и переменные

    private void value(VBox body) {
        UiNode node = contractNode(root, entry.kind(), entry.name());
        String prefix = entry.kind().toLowerCase() + "." + entry.name() + ".";
        String type = node.attributeText("Type", "");
        body.addChild(row("Type", typeChoice(prefix + "Type", type, chosen ->
                structural(ContractEdits.setType(root, entry.kind(), entry.name(), chosen)))));
        body.addChild(row("Default", defaultEditor(prefix + "Default", type, node.attributeText("Default", ""))));
        TextField description = new TextField(node.attributeText("Description", ""));
        description.id(prefix + "Description");
        description.placeholder("shown as a hint");
        grow(description);
        description.onTextChanged(event -> {
            if (!syncing) commit(ContractEdits.setAttribute(root, entry.kind(), entry.name(), "Description", description.text()));
        });
        body.addChild(row("Description", description));
    }

    private static void grow(dev.sixik.unigui.impl.widget.WidgetBase widget) {
        widget.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
    }

    private ComboBox typeChoice(String id, String type, java.util.function.Consumer<String> chosen) {
        List<String> items = new ArrayList<>(ParamType.names());
        if (!type.isEmpty() && !items.contains(type)) items.add(0, type); // неизвестный тип остаётся видимым
        ComboBox combo = new ComboBox();
        combo.id(id);
        combo.items(items);
        combo.silentSelectedIndex(Math.max(0, items.indexOf(type)));
        grow(combo);
        combo.onSelectionChanged(event -> {
            int index = combo.selectedIndex();
            if (!syncing && index >= 0) chosen.accept(items.get(index));
        });
        return combo;
    }

    /** Значение по умолчанию: флажок для логического типа, иначе поле, приводящее запись к типу. */
    private Widget defaultEditor(String id, String type, String value) {
        if (type.equals("bool")) {
            Checkbox box = new Checkbox();
            box.id(id);
            box.silentState(Boolean.parseBoolean(value) ? CheckboxState.CHECKED : CheckboxState.UNCHECKED);
            box.layout(style -> style.height(EditorStyles.ROW_HEIGHT));
            box.on(CheckedChangedEvent.TYPE, event -> {
                if (!syncing) commit(ContractEdits.setAttribute(root, entry.kind(), entry.name(), "Default",
                        event.newValue() ? "true" : null));
            });
            return box;
        }
        TextField field = new TextField(value);
        field.id(id);
        field.placeholder("default");
        grow(field);
        field.onTextChanged(event -> {
            if (syncing) return;
            String text = field.text();
            if (!text.isEmpty()) {
                // Значение по умолчанию приводится к записи по типу параметра: 1.50 → 1.5.
                try {
                    text = ValueEditing.normalize(ValueEditing.editorForType(type, null), text);
                } catch (IllegalArgumentException invalid) {
                    show(invalid.getMessage());
                    return;
                }
            }
            commit(ContractEdits.setAttribute(root, entry.kind(), entry.name(), "Default", text));
        });
        return field;
    }

    // ------------------------------------------------------------------ события

    private void event(VBox body) {
        UiNode node = contractNode(root, "Event", entry.name());
        String direction = node.attributeText("Direction", "Out");
        ComboBox kind = new ComboBox();
        kind.id("event." + entry.name() + ".Direction");
        kind.items(DIRECTIONS);
        kind.silentSelectedIndex(direction.equals("In") ? 0 : 1);
        grow(kind);
        kind.onSelectionChanged(change -> {
            if (syncing || kind.selectedIndex() < 0) return;
            structural(ContractEdits.setAttribute(root, "Event", entry.name(), "Direction",
                    kind.selectedIndex() == 0 ? "In" : "Out"));
        });
        body.addChild(row("Direction", kind));
        if (direction.equals("In")) {
            Checkbox box = new Checkbox();
            box.id("event." + entry.name() + ".Coalesce");
            box.text("Last call per frame only");
            box.silentState(Boolean.parseBoolean(node.attributeText("Coalesce", "false"))
                    ? CheckboxState.CHECKED : CheckboxState.UNCHECKED);
            box.layout(style -> style.height(EditorStyles.ROW_HEIGHT));
            box.on(CheckedChangedEvent.TYPE, change -> {
                if (!syncing) commit(ContractEdits.setAttribute(root, "Event", entry.name(), "Coalesce",
                        change.newValue() ? "true" : null));
            });
            body.addChild(row("Merge", box));
        }
        arguments = argumentList(node.attributeText("Args", ""));
    }

    private void custom() {
        GraphDefinition.Node node = customNode(root, entry.name());
        arguments = argumentList(node == null ? "" : node.attributes().getOrDefault("Args", ""));
    }

    /** Аргументы списком: у каждого имя, тип и кнопка удаления, как входы функции в Blueprint. */
    private VBox argumentList(String text) {
        String event = entry.name();
        List<Argument> list = parse(text);
        VBox block = new VBox();
        block.spacing(3.0f);
        block.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f).padding(0.0f, 1.0f, 0.0f, 6.0f));
        Label title = new Label("Arguments");
        title.color(EditorStyles.TEXT_BOLD);
        title.layout(style -> style.margin(0.0f, 6.0f, 0.0f, 2.0f));
        block.addChild(title);
        for (int i = 0; i < list.size(); i++) {
            int index = i;
            Argument argument = list.get(i);
            HBox line = new HBox();
            line.spacing(4.0f);
            line.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f).alignItems(Align.CENTER));
            FlatField name = new FlatField(argument.name(), next -> {
                List<Argument> changed = new ArrayList<>(list);
                changed.set(index, new Argument(next, argument.type()));
                finish(Renames.argument(session, event, write(changed), argument.name(), next));
            }).boxed();
            name.id("arg." + event + "." + argument.name() + ".name");
            line.addChild(name);
            line.addChild(typeChoice("arg." + event + "." + argument.name() + ".type", argument.type(), chosen -> {
                List<Argument> changed = new ArrayList<>(list);
                changed.set(index, new Argument(argument.name(), chosen));
                finish(Renames.arguments(session, event, write(changed)));
            }));
            ToolButton remove = new ToolButton(EditorIcons.CLOSE, "Remove argument");
            remove.id("arg." + event + "." + argument.name() + ".remove");
            remove.onClick(click -> {
                List<Argument> changed = new ArrayList<>(list);
                changed.remove(index);
                finish(Renames.arguments(session, event, write(changed)));
            });
            line.addChild(remove);
            line.applyQueuedMutations();
            block.addChild(line);
        }
        ToolButton add = new ToolButton(EditorIcons.PLUS, "Add argument", ToolButton.Variant.GHOST).alignStart();
        add.id("arg.add." + event);
        add.onClick(click -> {
            List<Argument> changed = new ArrayList<>(list);
            changed.add(new Argument(freshArgument(list), "string"));
            finish(Renames.arguments(session, event, write(changed)));
        });
        block.addChild(add);
        block.applyQueuedMutations();
        return block;
    }

    private static String freshArgument(List<Argument> arguments) {
        for (int i = 1; ; i++) {
            String candidate = "arg" + i;
            if (arguments.stream().noneMatch(argument -> argument.name().equals(candidate))) return candidate;
        }
    }

    private static List<Argument> parse(String text) {
        List<Argument> arguments = new ArrayList<>();
        for (String part : text.split(",")) {
            int colon = part.indexOf(':');
            if (colon > 0) arguments.add(new Argument(part.substring(0, colon).trim(), part.substring(colon + 1).trim()));
        }
        return arguments;
    }

    private static String write(List<Argument> arguments) {
        StringBuilder text = new StringBuilder();
        for (Argument argument : arguments) {
            if (text.length() > 0) text.append(", ");
            text.append(argument.name()).append(':').append(argument.type());
        }
        return text.toString();
    }

    // ------------------------------------------------------------------ правки

    /** Правка поля: инспектор не пересобирается, чтобы ввод не терял фокус. */
    private void commit(Edit edit) {
        applying = true;
        try {
            boolean done = session.apply(edit);
            show(done ? "" : reason());
        } finally {
            applying = false;
        }
    }

    /** Структурная правка меняет набор полей; сессия сама сообщает об изменении, и инспектор пересобирается. */
    private void structural(Edit edit) {
        if (!session.apply(edit)) show(reason());
    }

    private void finish(boolean done) {
        if (!done) {
            String reason = reason();
            rebuild.run();
            view.problem(reason);
        }
    }

    private String reason() {
        var found = session.diagnostics(EditorSession.Channel.EDIT);
        return found.isEmpty() ? "The edit was rejected" : found.get(0).message();
    }

    private void show(String text) {
        problem.text(text == null ? "" : text);
        problem.visibility(text == null || text.isEmpty() ? Visibility.COLLAPSED : Visibility.VISIBLE);
    }

    private static UiNode contractNode(UiNode root, String kind, String name) {
        return ElementListEdit.items(root, "Component", "Contract").stream()
                .filter(UiNode.class::isInstance).map(UiNode.class::cast)
                .filter(node -> node.type().local().equals(kind) && name.equals(node.attributeText("Name", "")))
                .findFirst().orElse(null);
    }

    private static GraphDefinition.Node customNode(UiNode root, String name) {
        for (GraphDefinition.Node node : GraphEdits.read(root).nodes()) {
            if (node.type().equals("Event.Custom") && name.equals(node.attributes().get("Name"))) return node;
        }
        return null;
    }
}
