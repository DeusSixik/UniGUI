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
import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.Edit;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.ElementListEdit;
import dev.sixik.unigui.xaml.edit.SessionChange;
import dev.sixik.unigui.xaml.edit.StyleEdits;

import java.util.List;

/**
 * Стили документа: ключ, тип-цель и сеттеры каждого стиля из ресурсов корня (для компонента — из ресурсов его тела).
 * Значение сеттера правится на месте, новый сеттер добавляется парой «свойство, значение» и кнопкой Add. Правки идут
 * через {@link StyleEdits}: неизвестное свойство и неподходящее значение отклоняются с причиной.
 *
 * <p>Идентификаторы: {@code style.add}, {@code style.key}, {@code style.target}, {@code style.<ключ>.add},
 * {@code style.<ключ>.property}, {@code style.<ключ>.value}, {@code style.<ключ>.setter.<свойство>},
 * {@code style.menu.<ключ>}.</p>
 */
final class StylePanel extends SessionPanel {
    /** Идентификатор панели. */
    static final String ID = "styles";

    private static final MutableColor PROBLEM = MutableColor.rgba255(235, 100, 100, 255);

    private final VBox content = new VBox();
    private final Label problem = new Label("");
    private final TextField newKey = new TextField();
    private final TextField newTarget = new TextField();
    private boolean syncing;
    private boolean applying;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String title() {
        return "Styles";
    }

    @Override
    public dev.sixik.unigui.api.render.UiIcon icon() {
        return dev.sixik.unigui.editor.style.EditorIcons.STYLE;
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
        newKey.id("style.key");
        newKey.placeholder("new style key");
        newTarget.id("style.target");
        newTarget.placeholder("target type (optional)");
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

    /** Узел, чьи ресурсы показывает панель: корень документа или, у компонента, корень тела. */
    private static UiNode host(EditorSession session) {
        UiNode root = session.document().root();
        if (root.type().local().equals("Struct")) return null;
        if (root.type().local().equals("Component")) return root.children().isEmpty() ? null : root.children().get(0);
        return root;
    }

    private void rebuild() {
        content.clearChildren();
        EditorSession session = session().orElse(null);
        UiNode host = session == null ? null : host(session);
        if (host == null) {
            content.addChild(EditorStyles.caption(session == null ? "No document" : "This document has no widget to hold styles"));
            content.applyQueuedMutations();
            EditorStyles.apply(content);
            return;
        }
        syncing = true;
        try {
            HBox add = new HBox();
            add.spacing(4.0f);
            newKey.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
            newTarget.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
            Button addStyle = new dev.sixik.unigui.editor.style.ToolButton(dev.sixik.unigui.editor.style.EditorIcons.PLUS, "Add style");
            addStyle.id("style.add");
            addStyle.onClick(click -> addStyle(session, host));
            add.addChild(newKey);
            add.addChild(newTarget);
            add.addChild(addStyle);
            add.applyQueuedMutations();
            content.addChild(add);
            for (UiContent item : ElementListEdit.items(host, host.type().local(), "Resources")) {
                if (item instanceof UiNode style && style.type().local().equals("Style")) {
                    content.addChild(styleCard(session, host, style));
                }
            }
            content.addChild(problem);
        } finally {
            syncing = false;
        }
        content.applyQueuedMutations();
        EditorStyles.apply(content);
    }

    private Widget styleCard(EditorSession session, UiNode host, UiNode style) {
        String key = keyOf(style);
        VBox card = new VBox();
        card.spacing(2.0f);
        card.layout(style_ -> style_.widthPercent(100.0f).flexShrink(0.0f));
        HBox head = new HBox();
        head.spacing(4.0f);
        String target = style.attributeText("TargetType", "");
        Label title = new Label(key + (target.isEmpty() ? "" : " : " + target));
        title.layout(style_ -> style_.flexGrow(1.0f).flexShrink(1.0f));
        Button menu = ContextMenus.button(context(), () -> List.of(
                ContextMenus.Entry.of("Remove style", () -> structural(session,
                        StyleEdits.removeStyle(session.document(), host, key, session.schema())))));
        menu.id("style.menu." + key);
        head.addChild(title);
        head.addChild(menu);
        head.applyQueuedMutations();
        card.addChild(head);

        for (UiNode setter : style.children()) {
            if (!setter.type().local().equals("Setter")) continue;
            String property = setter.attributeText("Property", "");
            HBox row = new HBox();
            row.spacing(4.0f);
            row.layout(style_ -> style_.widthPercent(100.0f).flexShrink(0.0f));
            Label name = new Label(property);
            name.layout(style_ -> style_.width(80.0f).flexShrink(0.0f));
            TextField value = new TextField(setter.attributeText("Value", ""));
            value.id("style." + key + ".setter." + property);
            value.layout(style_ -> style_.flexGrow(1.0f).flexShrink(1.0f));
            value.onTextChanged(event -> {
                if (!syncing) commit(session, StyleEdits.setSetter(session.document(), host, key, property, value.text(),
                        session.schema()));
            });
            Button remove = ContextMenus.button(context(), () -> List.of(
                    ContextMenus.Entry.of("Remove setter", () -> structural(session,
                            StyleEdits.removeSetter(session.document(), host, key, property, session.schema())))));
            row.addChild(name);
            row.addChild(value);
            row.addChild(remove);
            row.applyQueuedMutations();
            card.addChild(row);
        }

        HBox adder = new HBox();
        adder.spacing(4.0f);
        TextField property = new TextField();
        property.id("style." + key + ".property");
        property.placeholder("property");
        property.layout(style_ -> style_.flexGrow(1.0f).flexShrink(1.0f));
        TextField value = new TextField();
        value.id("style." + key + ".value");
        value.placeholder("value");
        value.layout(style_ -> style_.flexGrow(1.0f).flexShrink(1.0f));
        Button addSetter = new Button("Add");
        addSetter.id("style." + key + ".add");
        addSetter.onClick(click -> {
            if (property.text().isBlank()) {
                problem.text("Type the property name first.");
                return;
            }
            structural(session, StyleEdits.setSetter(session.document(), host, key, property.text().trim(),
                    value.text(), session.schema()));
        });
        adder.addChild(property);
        adder.addChild(value);
        adder.addChild(addSetter);
        adder.applyQueuedMutations();
        card.addChild(adder);
        card.applyQueuedMutations();
        return card;
    }

    private void addStyle(EditorSession session, UiNode host) {
        String key = newKey.text().trim();
        if (key.isEmpty()) {
            problem.text("Type a key for the new style.");
            return;
        }
        structural(session, StyleEdits.addStyle(session.document(), host, key, newTarget.text().trim(), null,
                session.schema()));
    }

    private static String keyOf(UiNode style) {
        return style.properties().stream().filter(property -> property.name().local().equals("Key"))
                .map(property -> property.value() instanceof dev.sixik.unigui.xaml.UiValue.Literal literal
                        ? literal.text().trim() : "").findFirst().orElse("");
    }

    /** Правка значения при вводе: панель не пересобирается. */
    private void commit(EditorSession session, Edit edit) {
        applying = true;
        boolean done;
        try {
            done = session.apply(edit);
        } finally {
            applying = false;
        }
        report(session, done);
    }

    /** Структурная правка меняет набор строк, поэтому панель пересобирается. */
    private void structural(EditorSession session, Edit edit) {
        boolean done = session.apply(edit);
        report(session, done);
        if (done) {
            newKey.text("");
            newTarget.text("");
            rebuild();
        }
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
