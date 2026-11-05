package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.event.CheckedChangedEvent;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.CheckboxState;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.ContextMenus;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.editor.style.FlatField;
import dev.sixik.unigui.editor.style.ToolButton;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.component.Contract;
import dev.sixik.unigui.xaml.component.ContractEdits;
import dev.sixik.unigui.xaml.component.ParamType;
import dev.sixik.unigui.xaml.component.Renames;
import dev.sixik.unigui.xaml.edit.Edit;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.ElementListEdit;
import dev.sixik.unigui.xaml.edit.SessionChange;
import dev.sixik.unigui.xaml.edit.ValueEditing;
import dev.sixik.unigui.xaml.logic.GraphDefinition;
import dev.sixik.unigui.xaml.logic.GraphEdits;
import dev.sixik.unigui.xaml.preview.ComponentPreview;
import dev.sixik.unigui.xaml.schema.LoadContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Контракт компонента: параметры, переменные и события, примерные значения для предпросмотра, отправка входящих событий
 * и журнал исходящих.
 *
 * <p>Имена правятся на месте и применяются по Enter, ссылки в теле и графе обновляются вместе с ними ({@link Renames}).
 * Типы выбираются из списка зарегистрированных. События разделены по смыслу: входящие вызывает родитель, исходящие
 * компонент сообщает родителю, пользовательские живут только внутри графа; в графе у каждого есть ноды On и Raise.</p>
 *
 * <p>Отправка событий и журнал работают только с копией, запущенной по Play: предпросмотр остаётся чертежом, и правка
 * документа не смешивается с запуском. Идентификаторы виджетов {@code param.Имя.Атрибут}, {@code var.…},
 * {@code event.…}, {@code name.Вид.Имя}, {@code add.…}, {@code sample.Имя}, {@code send.Имя} стабильны и нужны тестам.</p>
 */
final class ContractPanel extends SessionPanel {
    /** Идентификатор панели в каркасе. */
    static final String ID = "contract";

    private static final String CUSTOM = "Custom";
    private static final float ARGUMENT_INDENT = 14.0f;

    /** Аргумент события в записи документа. */
    private record Argument(String name, String type) {
    }

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
    public dev.sixik.unigui.api.render.UiIcon icon() {
        return EditorIcons.CONTRACT;
    }

    @Override
    public PanelSlot slot() {
        return PanelSlot.LEFT;
    }

    @Override
    protected Widget build(EditorContext editorContext) {
        content.spacing(3.0f);
        content.layout(style -> style.widthPercent(100.0f).padding(6.0f, 4.0f, 8.0f, 10.0f));
        problem.color(EditorStyles.ERROR_RED);
        ScrollView scroll = new ScrollView(content);
        EditorStyles.styleScroll(scroll);
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
            content.addChild(EditorStyles.caption(session == null ? "No document" : "Not a component document"));
            content.applyQueuedMutations();
            EditorStyles.apply(content);
            return;
        }
        syncing = true;
        try {
            valueSection(session, root, "Param", "Parameters", "Set from outside, like tag properties.");
            valueSection(session, root, "Var", "Variables", "Internal state of this component.");
            eventSection(session, root, "In", "Incoming events", "Called from outside. Handled by On nodes.");
            eventSection(session, root, "Out", "Outgoing events", "Sent to the parent. Fired by Raise nodes.");
            customSection(session, root);
            content.addChild(problem);
            previewSection();
        } finally {
            syncing = false;
        }
        content.applyQueuedMutations();
        EditorStyles.apply(content);
    }

    private void valueSection(EditorSession session, UiNode root, String kind, String title, String hint) {
        content.addChild(header(title, hint, "add." + kind, () -> {
            String name = ContractEdits.freshName(root, kind);
            structural(session, kind.equals("Param")
                    ? ContractEdits.addParam(root, name, "string", null)
                    : ContractEdits.addVar(root, name, "string", null));
        }));
        for (UiNode node : contractNodes(root, kind)) {
            String name = node.attributeText("Name", "");
            String type = node.attributeText("Type", "");
            VBox card = card(kind, name, pin(kind, type), renamer(session, kind, name), entryMenu(session, root, kind, name));
            HBox fields = row();
            fields.addChild(typeChoice(session, root, kind, name, type));
            fields.addChild(defaultEditor(session, root, kind, name, type, node.attributeText("Default", "")));
            fields.applyQueuedMutations();
            card.addChild(fields);
            card.applyQueuedMutations();
            content.addChild(card);
        }
    }

    private void eventSection(EditorSession session, UiNode root, String direction, String title, String hint) {
        content.addChild(header(title, hint, "add.Event." + direction, () ->
                structural(session, ContractEdits.addEvent(root, ContractEdits.freshName(root, "Event"), direction, null, false))));
        for (UiNode node : contractNodes(root, "Event")) {
            if (!direction.equals(node.attributeText("Direction", "Out"))) continue;
            String name = node.attributeText("Name", "");
            List<ContextMenus.Entry> menu = new ArrayList<>(entryMenu(session, root, "Event", name));
            String other = direction.equals("In") ? "Out" : "In";
            menu.add(0, ContextMenus.Entry.of(other.equals("In") ? "Make incoming" : "Make outgoing",
                    () -> structural(session, ContractEdits.setAttribute(root, "Event", name, "Direction", other))));
            VBox card = card("Event", name, pin("Event", ""), renamer(session, "Event", name), menu);
            arguments(session, card, name, node.attributeText("Args", ""));
            if (direction.equals("In")) card.addChild(coalesce(session, root, name, node));
            card.applyQueuedMutations();
            content.addChild(card);
        }
    }

    /** Пользовательские события объявляются нодами графа: они не видны снаружи, но работают как обычные. */
    private void customSection(EditorSession session, UiNode root) {
        content.addChild(header("Custom events", "Internal to this graph.", "add.Custom", () -> addCustom(session, root)));
        for (GraphDefinition.Node node : GraphEdits.read(root).nodes()) {
            if (!node.type().equals("Event.Custom")) continue;
            String name = node.attributes().getOrDefault("Name", "");
            List<ContextMenus.Entry> menu = List.of(ContextMenus.Entry.of("Delete",
                    () -> structural(session, GraphEdits.removeNode(root, node.id()))));
            VBox card = card(CUSTOM, name, pin("Event", ""), renamer(session, CUSTOM, name), menu);
            arguments(session, card, name, node.attributes().getOrDefault("Args", ""));
            card.applyQueuedMutations();
            content.addChild(card);
        }
    }

    private void addCustom(EditorSession session, UiNode root) {
        GraphDefinition graph = GraphEdits.read(root);
        List<String> used = new ArrayList<>();
        float bottom = 0.0f;
        for (GraphDefinition.Node node : graph.nodes()) {
            bottom = Math.max(bottom, node.y() + 120.0f);
            if (node.type().equals("Event.Custom")) used.add(node.attributes().getOrDefault("Name", ""));
        }
        int number = 1;
        while (used.contains("CustomEvent" + number)) number++;
        structural(session, GraphEdits.addNode(root, GraphEdits.freshId(root, "Event.Custom"), "Event.Custom",
                Map.of("Name", "CustomEvent" + number), 0.0f, bottom));
    }

    /** Карточка записи: значок типа, имя с правкой на месте и меню действий. */
    private VBox card(String kind, String name, Widget marker, java.util.function.Consumer<String> rename,
                      List<ContextMenus.Entry> entries) {
        VBox card = new VBox();
        card.spacing(3.0f);
        card.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f).padding(0.0f, 0.0f, 0.0f, 5.0f));
        HBox head = row();
        head.layout(style -> style.alignItems(Align.CENTER));
        head.addChild(marker);
        FlatField title = new FlatField(name, rename);
        title.id("name." + kind + "." + name);
        head.addChild(title);
        Button menu = ContextMenus.button(context(), () -> entries);
        menu.id("menu." + kind + "." + name);
        head.addChild(menu);
        head.applyQueuedMutations();
        card.addChild(head);
        ContextMenus.onSecondaryClick(head, context(), () -> entries);
        return card;
    }

    private List<ContextMenus.Entry> entryMenu(EditorSession session, UiNode root, String kind, String name) {
        List<ContextMenus.Entry> entries = new ArrayList<>();
        entries.add(ContextMenus.Entry.of("Delete", () -> structural(session, ContractEdits.remove(root, kind, name))));
        if (!kind.equals("Event")) {
            entries.add(ContextMenus.Entry.of("Clear default",
                    () -> structural(session, ContractEdits.setAttribute(root, kind, name, "Default", null))));
        }
        return entries;
    }

    /** Переименование идёт через рефакторинг: контракт, тело и граф меняются одним шагом отмены. */
    private java.util.function.Consumer<String> renamer(EditorSession session, String kind, String oldName) {
        return next -> {
            boolean done = switch (kind) {
                case "Param" -> Renames.param(session, oldName, next);
                case "Var" -> Renames.variable(session, oldName, next);
                case "Event" -> Renames.event(session, oldName, next);
                default -> Renames.customEvent(session, oldName, next);
            };
            report(session, done);
            if (!done) rebuildKeepingProblem();
        };
    }

    /** Отклонённая правка возвращает прежние подписи, а причина остаётся видна. */
    private void rebuildKeepingProblem() {
        String reason = problem.text();
        rebuild();
        problem.text(reason);
    }

    // ------------------------------------------------------------------ типы и значения

    private ComboBox typeChoice(EditorSession session, UiNode root, String kind, String name, String type) {
        List<String> items = typeItems(type);
        ComboBox combo = new ComboBox();
        combo.id(kind.toLowerCase() + "." + name + ".Type");
        combo.items(items);
        combo.silentSelectedIndex(Math.max(0, items.indexOf(type)));
        combo.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
        combo.onSelectionChanged(event -> {
            int index = combo.selectedIndex();
            if (syncing || index < 0) return;
            structural(session, ContractEdits.setType(root, kind, name, items.get(index)));
        });
        return combo;
    }

    /** Зарегистрированные типы; значение, которого среди них нет, остаётся видимым. */
    private static List<String> typeItems(String current) {
        List<String> items = new ArrayList<>(ParamType.names());
        if (!current.isEmpty() && !items.contains(current)) items.add(0, current);
        return items;
    }

    /** Значение по умолчанию: флажок для логического типа, иначе поле, приводящее запись к типу. */
    private Widget defaultEditor(EditorSession session, UiNode root, String kind, String name, String type, String value) {
        String id = kind.toLowerCase() + "." + name + ".Default";
        if (type.equals("bool")) {
            Checkbox box = new Checkbox();
            box.id(id);
            box.silentState(Boolean.parseBoolean(value) ? CheckboxState.CHECKED : CheckboxState.UNCHECKED);
            box.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).height(EditorStyles.ROW_HEIGHT));
            box.on(CheckedChangedEvent.TYPE, event -> {
                if (!syncing) commit(session, ContractEdits.setAttribute(root, kind, name, "Default", event.newValue() ? "true" : null));
            });
            return box;
        }
        TextField field = new TextField(value);
        field.id(id);
        field.placeholder("default");
        field.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
        field.onTextChanged(event -> {
            if (syncing) return;
            String text = field.text();
            if (!text.isEmpty()) {
                // Значение по умолчанию приводится к записи по типу параметра: 1.50 → 1.5.
                try {
                    text = ValueEditing.normalize(ValueEditing.editorForType(type, null), text);
                } catch (IllegalArgumentException invalid) {
                    problem.text(invalid.getMessage());
                    return;
                }
            }
            commit(session, ContractEdits.setAttribute(root, kind, name, "Default", text));
        });
        return field;
    }

    // ------------------------------------------------------------------ события

    private Widget coalesce(EditorSession session, UiNode root, String name, UiNode node) {
        Checkbox box = new Checkbox();
        box.id("event." + name + ".Coalesce");
        box.text("Last call per frame only");
        box.silentState(Boolean.parseBoolean(node.attributeText("Coalesce", "false"))
                ? CheckboxState.CHECKED : CheckboxState.UNCHECKED);
        box.layout(style -> style.height(EditorStyles.ROW_HEIGHT).margin(ARGUMENT_INDENT, 0.0f, 0.0f, 0.0f));
        box.on(CheckedChangedEvent.TYPE, event -> {
            if (!syncing) commit(session, ContractEdits.setAttribute(root, "Event", name, "Coalesce", event.newValue() ? "true" : null));
        });
        return box;
    }

    /** Аргументы списком: у каждого имя, тип и кнопка удаления, как входы функции в Blueprint. */
    private void arguments(EditorSession session, VBox card, String event, String text) {
        List<Argument> arguments = parse(text);
        for (int i = 0; i < arguments.size(); i++) {
            int index = i;
            Argument argument = arguments.get(i);
            HBox line = row();
            line.layout(style -> style.alignItems(Align.CENTER).padding(ARGUMENT_INDENT, 0.0f, 0.0f, 0.0f));
            FlatField name = new FlatField(argument.name(), next -> {
                List<Argument> changed = new ArrayList<>(arguments);
                changed.set(index, new Argument(next, argument.type()));
                finish(session, Renames.argument(session, event, write(changed), argument.name(), next));
            });
            name.id("arg." + event + "." + argument.name() + ".name");
            line.addChild(name);

            List<String> items = typeItems(argument.type());
            ComboBox type = new ComboBox();
            type.id("arg." + event + "." + argument.name() + ".type");
            type.items(items);
            type.silentSelectedIndex(Math.max(0, items.indexOf(argument.type())));
            type.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
            type.onSelectionChanged(change -> {
                int selected = type.selectedIndex();
                if (syncing || selected < 0) return;
                List<Argument> changed = new ArrayList<>(arguments);
                changed.set(index, new Argument(argument.name(), items.get(selected)));
                finish(session, Renames.arguments(session, event, write(changed)));
            });
            line.addChild(type);

            ToolButton remove = new ToolButton(EditorIcons.CLOSE, "Remove argument");
            remove.id("arg." + event + "." + argument.name() + ".remove");
            remove.onClick(click -> {
                List<Argument> changed = new ArrayList<>(arguments);
                changed.remove(index);
                finish(session, Renames.arguments(session, event, write(changed)));
            });
            line.addChild(remove);
            line.applyQueuedMutations();
            card.addChild(line);
        }
        ToolButton add = new ToolButton(EditorIcons.PLUS, "Add argument", ToolButton.Variant.GHOST).alignStart();
        add.id("arg.add." + event);
        add.layout(style -> style.margin(ARGUMENT_INDENT - 5.0f, 0.0f, 0.0f, 0.0f));
        add.onClick(click -> {
            List<Argument> changed = new ArrayList<>(arguments);
            changed.add(new Argument(freshArgument(arguments), "string"));
            finish(session, Renames.arguments(session, event, write(changed)));
        });
        card.addChild(add);
    }

    private void finish(EditorSession session, boolean done) {
        report(session, done);
        if (done) rebuild();
        else rebuildKeepingProblem();
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

    // ------------------------------------------------------------------ предпросмотр и запуск

    private void previewSection() {
        Optional<ComponentPreview> preview = componentView.preview();
        ComponentDefinition definition = preview.flatMap(ComponentPreview::definition).orElse(null);
        content.addChild(sectionTitle("Preview values"));
        if (preview.isEmpty() || definition == null) {
            content.addChild(EditorStyles.caption("The component does not build yet"));
        } else {
            for (Contract.Param param : definition.contract().params()) {
                if (!param.type().textual()) continue;
                HBox row = row();
                Label sampleName = new Label(param.name());
                sampleName.color(EditorStyles.TEXT_LABEL);
                sampleName.layout(style -> style.width(60.0f).height(EditorStyles.ROW_HEIGHT).flexShrink(0.0f));
                row.addChild(sampleName);
                Object current = preview.get().host().orElseThrow().instance().get(param.name());
                TextField sample = new TextField(current == null ? "" : String.valueOf(current));
                sample.id("sample." + param.name());
                sample.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
                sample.onTextChanged(event -> {
                    if (!syncing) setSample(preview.get(), param, sample.text());
                });
                row.addChild(sample);
                row.applyQueuedMutations();
                content.addChild(row);
            }
            playSection(definition);
        }
        content.addChild(sectionTitle("Event log"));
        log.layout(style -> style.flexShrink(0.0f));
        content.addChild(log);
        refreshLog();
    }

    /** События уходят только в запущенную копию: без Play кнопки недоступны, а предпросмотр не меняется. */
    private void playSection(ComponentDefinition definition) {
        List<Contract.Event> incoming = definition.contract().events().stream()
                .filter(event -> event.direction() == Contract.Direction.IN).toList();
        if (incoming.isEmpty()) return;
        content.addChild(sectionTitle("Send events"));
        content.addChild(EditorStyles.caption("Available in Play mode."));
        for (Contract.Event event : incoming) {
            Map<String, TextField> values = new LinkedHashMap<>();
            for (Contract.Arg arg : event.args()) {
                if (!arg.type().textual()) continue;
                HBox row = row();
                Label label = new Label(arg.name());
                label.color(EditorStyles.TEXT_LABEL);
                label.layout(style -> style.width(60.0f).height(EditorStyles.ROW_HEIGHT).flexShrink(0.0f));
                TextField value = new TextField("");
                value.id("send." + event.name() + "." + arg.name());
                value.placeholder(arg.type().name());
                value.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
                values.put(arg.name(), value);
                row.addChild(label);
                row.addChild(value);
                row.applyQueuedMutations();
                content.addChild(row);
            }
            ToolButton send = new ToolButton(EditorIcons.PLAY, "Send " + event.name(), ToolButton.Variant.SOLID);
            send.id("send." + event.name());
            send.layout(style -> style.widthPercent(100.0f));
            send.availableWhen(() -> componentView.running().isPresent());
            send.onClick(click -> send(event, values));
            content.addChild(send);
        }
    }

    private void send(Contract.Event event, Map<String, TextField> values) {
        ComponentPreview running = componentView.running().orElse(null);
        if (running == null) return;
        try {
            Map<String, Object> arguments = new LinkedHashMap<>();
            for (Contract.Arg arg : event.args()) {
                TextField value = values.get(arg.name());
                if (value != null && !value.text().isEmpty()) {
                    arguments.put(arg.name(), arg.type().parse(value.text(), LoadContext.DEFAULT));
                }
            }
            running.send(event.name(), Args.of(arguments));
            problem.text("");
        } catch (RuntimeException failure) {
            problem.text(failure.getMessage());
        }
    }

    private void setSample(ComponentPreview preview, Contract.Param param, String text) {
        try {
            preview.sample(param.name(), param.type().parse(text, LoadContext.DEFAULT));
            problem.text("");
        } catch (RuntimeException failure) {
            problem.text("Sample " + param.name() + ": " + failure.getMessage());
        }
    }

    private void refreshLog() {
        log.clearChildren();
        ComponentPreview running = componentView.running().orElse(null);
        List<String> lines = running == null ? List.of() : running.events();
        if (lines.isEmpty()) log.addChild(EditorStyles.caption(running == null ? "Press Play to see events" : "No events yet"));
        for (String line : lines) {
            log.addChild(new Label(line));
        }
        log.applyQueuedMutations();
    }

    // ------------------------------------------------------------------ части строк

    private Widget header(String title, String hint, String addId, Runnable add) {
        VBox block = new VBox();
        block.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f).margin(0.0f, 4.0f, 0.0f, 2.0f));
        HBox row = row();
        row.layout(style -> style.alignItems(Align.CENTER));
        Label label = new Label(title);
        label.color(EditorStyles.TEXT_BOLD);
        label.layout(style -> style.flexGrow(1.0f));
        row.addChild(label);
        ToolButton button = new ToolButton(EditorIcons.PLUS, "Add");
        button.id(addId);
        button.onClick(click -> add.run());
        row.addChild(button);
        row.applyQueuedMutations();
        block.addChild(row);
        block.addChild(EditorStyles.caption(hint));
        block.applyQueuedMutations();
        return block;
    }

    private static Label sectionTitle(String title) {
        Label label = new Label(title);
        label.color(EditorStyles.TEXT_BOLD);
        label.layout(style -> style.margin(0.0f, 6.0f, 0.0f, 2.0f));
        return label;
    }

    /** Значок записи: кружок цвета типа, как пин ноды; у событий — ромб цвета потока. */
    private static Widget pin(String kind, String type) {
        CanvasWidget pin = new CanvasWidget();
        pin.layout(style -> style.size(8.0f, 8.0f).flexShrink(0.0f));
        MutableColor color = kind.equals("Event") ? EditorStyles.TEXT_LABEL : argb(EditorStyles.pinColor(type));
        pin.onDraw(draw -> {
            var b = pin.layoutBounds();
            if (kind.equals("Event")) {
                draw.pathClear();
                draw.pathLineTo(b.x() + 4.0f, b.y());
                draw.pathLineTo(b.x() + 8.0f, b.y() + 4.0f);
                draw.pathLineTo(b.x() + 4.0f, b.y() + 8.0f);
                draw.pathLineTo(b.x(), b.y() + 4.0f);
                draw.pathFillConvex(color);
            } else {
                draw.circleFilled(b.x(), b.y(), 8.0f, 8.0f, color);
            }
        });
        return pin;
    }

    private static MutableColor argb(int value) {
        return MutableColor.rgba255((value >> 16) & 255, (value >> 8) & 255, value & 255, (value >>> 24) & 255);
    }

    private static HBox row() {
        HBox row = new HBox();
        row.spacing(4.0f);
        row.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));
        return row;
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
