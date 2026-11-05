package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.ContextMenus;
import dev.sixik.unigui.editor.EditorCommand;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.editor.style.FlatField;
import dev.sixik.unigui.editor.style.SectionHeader;
import dev.sixik.unigui.editor.style.ToolButton;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.dnd.DragController;
import dev.sixik.unigui.widgets.dnd.DragGhost;
import dev.sixik.unigui.widgets.dnd.DragPayload;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.component.ContractEdits;
import dev.sixik.unigui.xaml.component.Renames;
import dev.sixik.unigui.xaml.edit.Edit;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.ElementListEdit;
import dev.sixik.unigui.xaml.edit.SessionChange;
import dev.sixik.unigui.xaml.logic.GraphDefinition;
import dev.sixik.unigui.xaml.logic.GraphEdits;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Контракт компонента списком, как My Blueprint в Unreal: группы с кнопкой добавления и однострочные записи с именем и
 * типом. Здесь записи только создаются, ищутся, выбираются и перетаскиваются; настраиваются они в инспекторе
 * ({@link ContractDetails}), а запуск живёт в {@link RunPanel}.
 *
 * <p>Запись можно бросить на полотно графа (меню Get/Set или On/Raise) и на строку свойства в инспекторе (привязка).
 * Переименование и удаление — в меню по правой кнопке. Идентификаторы {@code entry.Вид.Имя}, {@code name.Вид.Имя},
 * {@code add.…}, {@code group.…} стабильны и нужны тестам.</p>
 */
final class ContractPanel extends SessionPanel {
    /** Идентификатор панели в каркасе. */
    static final String ID = "contract";
    /** Тип данных перетаскивания: в {@link DragPayload#data} лежит {@link ComponentView.Entry}. */
    static final String PAYLOAD_TYPE = "unigui.contract";

    private final ComponentView componentView;
    private final VBox content = new VBox();
    private final TextField search = EditorStyles.searchField();
    private final Label problem = new Label("");
    private final Set<String> collapsed = new HashSet<>();
    private ComponentView.Entry renaming;

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
    public UiIcon icon() {
        return EditorIcons.CONTRACT;
    }

    @Override
    public PanelSlot slot() {
        return PanelSlot.LEFT;
    }

    @Override
    protected Widget build(EditorContext editorContext) {
        VBox root = new VBox();
        root.layout(style -> style.sizePercent(100.0f, 100.0f));

        HBox top = new HBox();
        top.spacing(4.0f);
        top.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f).padding(4.0f, 4.0f, 4.0f, 3.0f));
        ToolButton add = new ToolButton(EditorIcons.PLUS, "Add", ToolButton.Variant.SOLID);
        add.id("contract.add");
        add.iconColor(EditorStyles.PLAY_GREEN);
        add.onClick(click -> {
            var bounds = add.layoutBounds();
            ContextMenus.open(context(), bounds.x(), bounds.y() + bounds.height() + 2.0f, addMenu());
        });
        search.id("contract.search");
        search.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
        search.onTextChanged(event -> rebuild());
        top.addChild(add);
        top.addChild(search);
        top.applyQueuedMutations();

        content.layout(style -> style.widthPercent(100.0f));
        ScrollView scroll = new ScrollView(content);
        EditorStyles.styleScroll(scroll);
        scroll.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));

        problem.color(EditorStyles.ERROR_RED);
        problem.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f).margin(6.0f, 2.0f));
        problem.visibility(Visibility.COLLAPSED);

        root.addChild(top);
        root.addChild(scroll);
        root.addChild(problem);
        root.applyQueuedMutations();
        componentView.onEntry(this::rebuild);
        return root;
    }

    @Override
    protected void bound(EditorSession next) {
        rebuild();
    }

    @Override
    protected void changed(EditorSession current, SessionChange change) {
        switch (change.kind()) {
            case DOCUMENT_CHANGED, REVERTED -> rebuild();
            default -> { }
        }
    }

    // ------------------------------------------------------------------ список

    private void rebuild() {
        content.clearChildren();
        String reason = componentView.problem();
        problem.text(reason);
        problem.visibility(reason.isEmpty() ? Visibility.COLLAPSED : Visibility.VISIBLE);
        EditorSession session = session().orElse(null);
        UiNode root = session == null ? null : session.document().root();
        if (root == null || !root.type().local().equals("Component")) {
            Label empty = EditorStyles.caption(session == null ? "No document" : "Not a component document");
            empty.layout(style -> style.margin(8.0f, 6.0f));
            content.addChild(empty);
            content.applyQueuedMutations();
            return;
        }
        group("GRAPHS", null, null, List.of(graphRow()));
        group("PARAMETERS", "add.Param", () -> addValue(session, root, "Param"), valueRows(session, root, "Param"));
        group("VARIABLES", "add.Var", () -> addValue(session, root, "Var"), valueRows(session, root, "Var"));
        group("INCOMING EVENTS", "add.Event.In", () -> addEvent(session, root, "In"), eventRows(session, root, "In"));
        group("OUTGOING EVENTS", "add.Event.Out", () -> addEvent(session, root, "Out"), eventRows(session, root, "Out"));
        group("CUSTOM EVENTS", "add.Custom", () -> addCustom(session, root), customRows(session, root));
        content.applyQueuedMutations();
    }

    /** Группа: полоса с шевроном и кнопкой добавления, под ней записи. */
    private void group(String title, String addId, Runnable add, List<Widget> rows) {
        boolean closed = collapsed.contains(title);
        HBox bar = new HBox();
        bar.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f).margin(0.0f, 0.0f, 0.0f, 1.0f));
        SectionHeader header = new SectionHeader((closed ? "▸ " : "▾ ") + title).banner();
        header.id("group." + title);
        header.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
        header.onClick(click -> {
            if (!collapsed.add(title)) collapsed.remove(title);
            rebuild();
        });
        bar.addChild(header);
        if (add != null) {
            ToolButton button = new ToolButton(EditorIcons.PLUS, "Add").variant(ToolButton.Variant.BANNER);
            button.id(addId);
            button.onClick(click -> add.run());
            bar.addChild(button);
        }
        bar.applyQueuedMutations();
        content.addChild(bar);
        if (!closed) for (Widget row : rows) content.addChild(row);
    }

    private Widget graphRow() {
        ContractRow row = new ContractRow("EventGraph", EditorIcons.GRAPH, "", null);
        row.id("entry.graph");
        row.onClick(click -> runCommand("window.open." + GraphPanel.ID));
        return row;
    }

    private void runCommand(String id) {
        for (EditorCommand command : context().commands().all()) {
            if (command.id().equals(id)) command.run(context());
        }
    }

    private boolean matches(String name) {
        String query = search.text().trim().toLowerCase(Locale.ROOT);
        return query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query);
    }

    private List<Widget> valueRows(EditorSession session, UiNode root, String kind) {
        List<Widget> rows = new ArrayList<>();
        for (UiNode node : contractNodes(root, kind)) {
            String name = node.attributeText("Name", "");
            if (!matches(name)) continue;
            String type = node.attributeText("Type", "");
            rows.add(row(session, new ComponentView.Entry(kind, name), null, shortType(type), pill(type),
                    () -> structural(session, ContractEdits.remove(root, kind, name))));
        }
        return rows;
    }

    private List<Widget> eventRows(EditorSession session, UiNode root, String direction) {
        List<Widget> rows = new ArrayList<>();
        for (UiNode node : contractNodes(root, "Event")) {
            if (!direction.equals(node.attributeText("Direction", "Out"))) continue;
            String name = node.attributeText("Name", "");
            if (!matches(name)) continue;
            rows.add(row(session, new ComponentView.Entry("Event", name), EditorIcons.BOLT, arguments(node.attributeText("Args", "")),
                    null, () -> structural(session, ContractEdits.remove(root, "Event", name))));
        }
        return rows;
    }

    private List<Widget> customRows(EditorSession session, UiNode root) {
        List<Widget> rows = new ArrayList<>();
        for (GraphDefinition.Node node : GraphEdits.read(root).nodes()) {
            if (!node.type().equals("Event.Custom")) continue;
            String name = node.attributes().getOrDefault("Name", "");
            if (!matches(name)) continue;
            rows.add(row(session, new ComponentView.Entry(ComponentView.CUSTOM, name), EditorIcons.BOLT,
                    arguments(node.attributes().getOrDefault("Args", "")), null,
                    () -> structural(session, GraphEdits.removeNode(root, node.id()))));
        }
        return rows;
    }

    /** Строка записи; пока запись переименовывается, вместо неё стоит поле ввода. */
    private Widget row(EditorSession session, ComponentView.Entry entry, UiIcon icon, String trailing, MutableColor color,
                       Runnable delete) {
        if (entry.equals(renaming)) {
            FlatField field = new FlatField(entry.name(), next -> rename(session, entry, next)).boxed().reportUnchanged();
            field.id("name." + entry.kind() + "." + entry.name());
            field.layout(style -> style.widthPercent(100.0f).height(ContractRow.HEIGHT).flexShrink(0.0f)
                    .margin(10.0f, 0.0f, 4.0f, 0.0f));
            field.beginEdit();
            return field;
        }
        ContractRow row = new ContractRow(entry.name(), icon, trailing, color);
        row.id("entry." + entry.kind() + "." + entry.name());
        row.selected(entry.equals(componentView.entry().orElse(null)));
        row.onClick(click -> select(session, entry));
        DragController.attach(row).dragged(row).threshold(4.0f)
                .payload(() -> DragPayload.of(entry.name(), PAYLOAD_TYPE, entry.name(), entry))
                .ghost(DragGhost.text());
        ContextMenus.onSecondaryClick(row, context(), () -> {
            select(session, entry);
            return menu(entry, delete);
        });
        menus.put(entry, () -> menu(entry, delete));
        return row;
    }

    private final Map<ComponentView.Entry, java.util.function.Supplier<List<ContextMenus.Entry>>> menus = new java.util.HashMap<>();

    private List<ContextMenus.Entry> menu(ComponentView.Entry entry, Runnable delete) {
        return List.of(ContextMenus.Entry.of("Rename", () -> {
                    renaming = entry;
                    rebuild();
                }),
                ContextMenus.Entry.of("Delete", delete));
    }

    /**
     * @param entry запись
     * @return меню строки; нужно тестам
     */
    List<ContextMenus.Entry> menuOf(ComponentView.Entry entry) {
        return menus.get(entry).get();
    }

    private void select(EditorSession session, ComponentView.Entry entry) {
        // Инспектор показывает либо узел, либо запись: выбор записи снимает выбор узла.
        session.select((UiNode) null);
        componentView.entry(entry);
    }

    private void rename(EditorSession session, ComponentView.Entry entry, String next) {
        renaming = null;
        boolean done = next.equals(entry.name()) || switch (entry.kind()) {
            case "Param" -> Renames.param(session, entry.name(), next);
            case "Var" -> Renames.variable(session, entry.name(), next);
            case "Event" -> Renames.event(session, entry.name(), next);
            default -> Renames.customEvent(session, entry.name(), next);
        };
        if (done) {
            componentView.entry(new ComponentView.Entry(entry.kind(), next));
        } else {
            var found = session.diagnostics(EditorSession.Channel.EDIT);
            componentView.problem(found.isEmpty() ? "The rename was rejected" : found.get(0).message());
        }
    }

    // ------------------------------------------------------------------ добавление

    private List<ContextMenus.Entry> addMenu() {
        EditorSession session = session().orElse(null);
        UiNode root = session == null ? null : session.document().root();
        if (root == null || !root.type().local().equals("Component")) return List.of();
        return List.of(ContextMenus.Entry.of("Parameter", () -> addValue(session, root, "Param")),
                ContextMenus.Entry.of("Variable", () -> addValue(session, root, "Var")),
                ContextMenus.Entry.separator(),
                ContextMenus.Entry.of("Incoming event", () -> addEvent(session, root, "In")),
                ContextMenus.Entry.of("Outgoing event", () -> addEvent(session, root, "Out")),
                ContextMenus.Entry.of("Custom event", () -> addCustom(session, root)));
    }

    private void addValue(EditorSession session, UiNode root, String kind) {
        String name = ContractEdits.freshName(root, kind);
        added(session, new ComponentView.Entry(kind, name), kind.equals("Param")
                ? ContractEdits.addParam(root, name, "string", null)
                : ContractEdits.addVar(root, name, "string", null));
    }

    private void addEvent(EditorSession session, UiNode root, String direction) {
        String name = ContractEdits.freshName(root, "Event");
        added(session, new ComponentView.Entry("Event", name), ContractEdits.addEvent(root, name, direction, null, false));
    }

    private void addCustom(EditorSession session, UiNode root) {
        List<String> used = new ArrayList<>();
        float bottom = 0.0f;
        for (GraphDefinition.Node node : GraphEdits.read(root).nodes()) {
            bottom = Math.max(bottom, node.y() + 120.0f);
            if (node.type().equals("Event.Custom")) used.add(node.attributes().getOrDefault("Name", ""));
        }
        int number = 1;
        while (used.contains("CustomEvent" + number)) number++;
        String name = "CustomEvent" + number;
        added(session, new ComponentView.Entry(ComponentView.CUSTOM, name), GraphEdits.addNode(root,
                GraphEdits.freshId(root, "Event.Custom"), "Event.Custom", Map.of("Name", name), 0.0f, bottom));
    }

    /** Новая запись сразу выбрана и готова к переименованию, как в Unreal. */
    private void added(EditorSession session, ComponentView.Entry entry, Edit edit) {
        if (!session.apply(edit)) {
            report(session);
            return;
        }
        collapsed.clear();
        renaming = entry;
        select(session, entry);
    }

    private void structural(EditorSession session, Edit edit) {
        if (!session.apply(edit)) report(session);
    }

    private void report(EditorSession session) {
        var found = session.diagnostics(EditorSession.Channel.EDIT);
        componentView.problem(found.isEmpty() ? "The edit was rejected" : found.get(0).message());
    }

    // ------------------------------------------------------------------ подписи

    /** Подпись типа без служебного префикса: структура называется своим именем. */
    private static String shortType(String type) {
        return type.startsWith("struct:") ? type.substring("struct:".length()) : type;
    }

    private static MutableColor pill(String type) {
        int value = EditorStyles.pinColor(type);
        return MutableColor.rgba255((value >> 16) & 255, (value >> 8) & 255, value & 255, (value >>> 24) & 255);
    }

    private static String arguments(String text) {
        if (text == null || text.isBlank()) return "";
        int count = text.split(",").length;
        return count == 1 ? "1 arg" : count + " args";
    }

    private static List<UiNode> contractNodes(UiNode root, String kind) {
        return ElementListEdit.items(root, "Component", "Contract").stream()
                .filter(UiNode.class::isInstance).map(UiNode.class::cast)
                .filter(node -> node.type().local().equals(kind)).toList();
    }
}
