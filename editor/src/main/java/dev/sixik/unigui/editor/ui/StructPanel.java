package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.ContextMenus;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.SessionChange;
import dev.sixik.unigui.xaml.struct.StructEdits;

import java.util.List;

/**
 * Поля структуры данных ({@code <Struct>}): имя и тип каждого поля, добавление, удаление, перестановка. Все правки идут
 * через {@link StructEdits}: неверное имя, повтор или неизвестный тип отклоняются с причиной под таблицей, а документ
 * остаётся прежним.
 *
 * <p>Идентификаторы {@code struct.add}, {@code struct.field.<имя>.name}, {@code struct.field.<имя>.type},
 * {@code struct.menu.<имя>} стабильны для тестов.</p>
 */
final class StructPanel extends SessionPanel {
    /** Идентификатор панели. */
    static final String ID = "struct";

    private static final MutableColor PROBLEM = MutableColor.rgba255(235, 100, 100, 255);

    private final VBox content = new VBox();
    private final Label problem = new Label("");
    private boolean syncing;
    private boolean applying;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String title() {
        return "Struct";
    }

    @Override
    public dev.sixik.unigui.api.render.UiIcon icon() {
        return dev.sixik.unigui.editor.style.EditorIcons.STRUCT;
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
        content.spacing(3.0f);
        content.layout(style -> style.widthPercent(100.0f).padding(6.0f, 6.0f, 8.0f, 10.0f));
        problem.color(PROBLEM);
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
            case DOCUMENT_CHANGED -> {
                if (!applying) rebuild();
            }
            case REVERTED -> rebuild();
            default -> { }
        }
    }

    private void rebuild() {
        content.clearChildren();
        EditorSession session = session().orElse(null);
        UiNode root = session == null ? null : session.document().root();
        if (root == null || !root.type().local().equals("Struct")) {
            content.addChild(EditorStyles.caption(session == null ? "No document" : "Not a struct document"));
            content.applyQueuedMutations();
            EditorStyles.apply(content);
            return;
        }
        syncing = true;
        try {
            HBox header = new HBox();
            header.spacing(4.0f);
            Label title = new Label("Fields");
            title.color(EditorStyles.TEXT_BOLD);
            title.layout(style -> style.flexGrow(1.0f).height(EditorStyles.ROW_HEIGHT + 2.0f));
            Button add = new dev.sixik.unigui.editor.style.ToolButton(dev.sixik.unigui.editor.style.EditorIcons.PLUS, "Add");
            add.id("struct.add");
            add.onClick(click -> addField(session));
            header.addChild(title);
            header.addChild(add);
            header.applyQueuedMutations();
            content.addChild(header);
            for (UiNode field : root.children()) content.addChild(fieldRow(session, field));
            content.addChild(problem);
        } finally {
            syncing = false;
        }
        content.applyQueuedMutations();
        EditorStyles.apply(content);
    }

    private Widget fieldRow(EditorSession session, UiNode field) {
        String[] current = {field.attributeText("Name", "")};
        VBox card = new VBox();
        card.spacing(2.0f);
        card.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));

        HBox head = new HBox();
        head.spacing(4.0f);
        head.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));
        TextField name = new TextField(current[0]);
        name.id("struct.field." + current[0] + ".name");
        name.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        name.onTextChanged(event -> {
            if (syncing || name.text().equals(current[0])) return;
            if (commit(session, () -> StructEdits.renameField(session, current[0], name.text()))) current[0] = name.text();
        });
        Button menu = ContextMenus.button(context(), () -> List.of(
                ContextMenus.Entry.of("Move up", () -> structural(session, () -> StructEdits.move(session, current[0], -1))),
                ContextMenus.Entry.of("Move down", () -> structural(session, () -> StructEdits.move(session, current[0], 1))),
                ContextMenus.Entry.separator(),
                ContextMenus.Entry.of("Remove", () -> structural(session, () -> StructEdits.removeField(session, current[0])))));
        menu.id("struct.menu." + current[0]);
        head.addChild(name);
        head.addChild(menu);
        head.applyQueuedMutations();

        TextField type = new TextField(field.attributeText("Type", ""));
        type.id("struct.field." + current[0] + ".type");
        type.placeholder("string, int, struct:Name…");
        type.layout(style -> style.widthPercent(100.0f).flexShrink(1.0f));
        type.onTextChanged(event -> {
            if (!syncing) commit(session, () -> StructEdits.setType(session, current[0], type.text()));
        });
        card.addChild(head);
        card.addChild(type);
        card.applyQueuedMutations();
        return card;
    }

    private void addField(EditorSession session) {
        UiNode root = session.document().root();
        int index = root.children().size() + 1;
        String name = "field" + index;
        while (exists(root, name)) name = "field" + (++index);
        String chosen = name;
        structural(session, () -> StructEdits.addField(session, chosen, "string"));
    }

    private static boolean exists(UiNode root, String name) {
        return root.children().stream().anyMatch(field -> name.equals(field.attributeText("Name", "")));
    }

    /** Правка поля при вводе: панель не пересобирается, чтобы ввод не терял фокус. */
    private boolean commit(EditorSession session, java.util.function.BooleanSupplier edit) {
        applying = true;
        boolean done;
        try {
            done = edit.getAsBoolean();
        } finally {
            applying = false;
        }
        report(session, done);
        return done;
    }

    /** Структурная правка меняет набор строк, поэтому панель пересобирается. */
    private void structural(EditorSession session, java.util.function.BooleanSupplier edit) {
        boolean done = edit.getAsBoolean();
        report(session, done);
        if (done) rebuild();
    }

    private void report(EditorSession session, boolean done) {
        if (done) {
            problem.text("");
            return;
        }
        var found = session.diagnostics(EditorSession.Channel.EDIT);
        problem.text(found.isEmpty() ? "The edit was rejected" : found.get(0).message());
    }
}
