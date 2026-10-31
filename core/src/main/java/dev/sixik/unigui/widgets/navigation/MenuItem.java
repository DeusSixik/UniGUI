package dev.sixik.unigui.widgets.navigation;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.editor.CommandManager;
import dev.sixik.unigui.api.editor.EditorCommand;

import java.util.Optional;

/** Lightweight menu item model used by {@link MenuBar}. */
@AiDoc(summary = "Menu item model representing an action, command, visual separator, or cascading submenu.")
@WikiPage(
        title = "MenuItem",
        slug = "menu-item",
        category = "<key:navigation.category>",
        summary = "<key:menu.item.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:menu.item.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:menu.item.example.basic.title>",
        code = "MenuItem item = MenuItem.action(\"Save\", this::save)\n    .shortcutText(\"Ctrl+S\");"
)
@WikiLink(
        label = "Menu",
        target = "page:menu",
        description = "<key:menu.summary>"
)
@WikiLink(
        label = "MenuBar",
        target = "page:menu-bar",
        description = "<key:menu.bar.summary>"
)
public final class MenuItem {
    private static final Runnable NO_ACTION = () -> {
    };

    private final Kind kind;
    private String label = "";
    private String commandId = "";
    private Runnable action = NO_ACTION;
    private Menu submenu;
    private boolean enabled = true;
    private boolean checked;
    private String shortcutText = "";

    private MenuItem(Kind kind) {
        this.kind = kind == null ? Kind.ACTION : kind;
    }

        @AiDoc(summary = "Creates an action menu item with display label and click callback.")
    @WikiMethod(summary = "<key:menu.item.method.actionCreate>")
    @WikiReturn("<key:menu.item.return.menuItem>")
    public static MenuItem action(
            @WikiParameter(description = "<key:menu.item.param.label>")
            String label,
            @WikiParameter(description = "<key:menu.item.param.action>")
            Runnable action
    ) {
        return new MenuItem(Kind.ACTION).label(label).action(action);
    }

        @AiDoc(summary = "Creates a command menu item linked to an editor command identifier.")
    @WikiMethod(summary = "<key:menu.item.method.commandCreate>")
    @WikiReturn("<key:menu.item.return.menuItem>")
    public static MenuItem command(
            @WikiParameter(description = "<key:menu.item.param.commandId>")
            String commandId
    ) {
        return new MenuItem(Kind.COMMAND).commandId(commandId);
    }

        @AiDoc(summary = "Creates a command menu item with an explicit custom label.")
    @WikiMethod(summary = "<key:menu.item.method.commandLabelCreate>")
    @WikiReturn("<key:menu.item.return.menuItem>")
    public static MenuItem command(
            @WikiParameter(description = "<key:menu.item.param.commandId>")
            String commandId,
            @WikiParameter(description = "<key:menu.item.param.label>")
            String label
    ) {
        return command(commandId).label(label);
    }

        @AiDoc(summary = "Creates a visual divider separator item.")
    @WikiMethod(summary = "<key:menu.item.method.separatorCreate>")
    @WikiReturn("<key:menu.item.return.menuItem>")
    public static MenuItem separator() {
        return new MenuItem(Kind.SEPARATOR).enabled(false);
    }

        @AiDoc(summary = "Creates a cascading submenu item.")
    @WikiMethod(summary = "<key:menu.item.method.submenuCreate>")
    @WikiReturn("<key:menu.item.return.menuItem>")
    public static MenuItem submenu(
            @WikiParameter(description = "<key:menu.item.param.submenu>")
            Menu submenu
    ) {
        return new MenuItem(Kind.SUBMENU).menu(submenu);
    }

        @AiDoc(summary = "Returns the kind of this menu item (ACTION, COMMAND, SEPARATOR, SUBMENU).")
    @WikiMethod(summary = "<key:menu.item.method.kind>")
    @WikiReturn("<key:menu.item.return.kind>")
    public Kind kind() {
        return kind;
    }

        @AiDoc(summary = "Returns the item display label text.")
    @WikiMethod(summary = "<key:menu.item.method.labelGet>")
    @WikiReturn("<key:menu.item.return.label>")
    public String label() {
        return label;
    }

        @AiDoc(summary = "Sets the item display label text.")
    @WikiMethod(summary = "<key:menu.item.method.labelSet>")
    @WikiReturn("<key:menu.item.return.this>")
    public MenuItem label(
            @WikiParameter(description = "<key:menu.item.param.label>")
            String label
    ) {
        this.label = normalizeText(label);
        return this;
    }

        @AiDoc(summary = "Returns the linked editor command identifier.")
    @WikiMethod(summary = "<key:menu.item.method.commandIdGet>")
    @WikiReturn("<key:menu.item.return.commandId>")
    public String commandId() {
        return commandId;
    }

        @AiDoc(summary = "Sets the linked editor command identifier.")
    @WikiMethod(summary = "<key:menu.item.method.commandIdSet>")
    @WikiReturn("<key:menu.item.return.this>")
    public MenuItem commandId(
            @WikiParameter(description = "<key:menu.item.param.commandId>")
            String commandId
    ) {
        this.commandId = normalizeText(commandId);
        return this;
    }

        @AiDoc(summary = "Returns the callback runnable executed when this item is clicked.")
    @WikiMethod(summary = "<key:menu.item.method.actionGet>")
    @WikiReturn("<key:menu.item.return.action>")
    public Runnable action() {
        return action;
    }

        @AiDoc(summary = "Sets the callback runnable executed when this item is clicked.")
    @WikiMethod(summary = "<key:menu.item.method.actionSet>")
    @WikiReturn("<key:menu.item.return.this>")
    public MenuItem action(
            @WikiParameter(description = "<key:menu.item.param.action>")
            Runnable action
    ) {
        this.action = action == null ? NO_ACTION : action;
        return this;
    }

        @AiDoc(summary = "Returns the nested submenu, or null if not a submenu item.")
    @WikiMethod(summary = "<key:menu.item.method.submenuGet>")
    @WikiReturn("<key:menu.item.return.submenu>")
    public Menu submenu() {
        return submenu;
    }

        @AiDoc(summary = "Sets the nested submenu container.")
    @WikiMethod(summary = "<key:menu.item.method.submenuSet>")
    @WikiReturn("<key:menu.item.return.this>")
    public MenuItem menu(
            @WikiParameter(description = "<key:menu.item.param.submenu>")
            Menu submenu
    ) {
        this.submenu = submenu;
        return this;
    }

        @AiDoc(summary = "Returns true if this item is interactive and clickable.")
    @WikiMethod(summary = "<key:menu.item.method.enabledGet>")
    @WikiReturn("<key:menu.item.return.enabled>")
    public boolean enabled() {
        return enabled;
    }

        @AiDoc(summary = "Sets whether this item is interactive and clickable.")
    @WikiMethod(summary = "<key:menu.item.method.enabledSet>")
    @WikiReturn("<key:menu.item.return.this>")
    public MenuItem enabled(
            @WikiParameter(description = "<key:menu.item.param.enabled>")
            boolean enabled
    ) {
        this.enabled = enabled;
        return this;
    }

        @AiDoc(summary = "Returns true if this item displays a checkmark indicator.")
    @WikiMethod(summary = "<key:menu.item.method.checkedGet>")
    @WikiReturn("<key:menu.item.return.checked>")
    public boolean checked() {
        return checked;
    }

        @AiDoc(summary = "Sets whether this item displays a checkmark indicator.")
    @WikiMethod(summary = "<key:menu.item.method.checkedSet>")
    @WikiReturn("<key:menu.item.return.this>")
    public MenuItem checked(
            @WikiParameter(description = "<key:menu.item.param.checked>")
            boolean checked
    ) {
        this.checked = checked;
        return this;
    }

        @AiDoc(summary = "Returns the shortcut key combination display text.")
    @WikiMethod(summary = "<key:menu.item.method.shortcutTextGet>")
    @WikiReturn("<key:menu.item.return.shortcutText>")
    public String shortcutText() {
        return shortcutText;
    }

        @AiDoc(summary = "Sets the shortcut key combination display text.")
    @WikiMethod(summary = "<key:menu.item.method.shortcutTextSet>")
    @WikiReturn("<key:menu.item.return.this>")
    public MenuItem shortcutText(
            @WikiParameter(description = "<key:menu.item.param.shortcutText>")
            String shortcutText
    ) {
        this.shortcutText = normalizeText(shortcutText);
        return this;
    }

    String resolvedLabel(CommandManager manager) {
        if (!label.isEmpty()) return label;
        if (kind == Kind.SUBMENU && submenu != null) return submenu.label();
        Optional<EditorCommand> command = command(manager);
        return command.map(EditorCommand::label).orElse(commandId.isEmpty() ? "Item" : commandId);
    }

    boolean resolvedEnabled(CommandManager manager) {
        if (!enabled || kind == Kind.SEPARATOR) return false;
        if (kind == Kind.COMMAND) return command(manager).map(EditorCommand::enabled).orElse(false);
        if (kind == Kind.SUBMENU) return submenu != null && !submenu.items().isEmpty();
        return true;
    }

    boolean resolvedChecked(CommandManager manager) {
        if (checked) return true;
        return kind == Kind.COMMAND && command(manager).map(EditorCommand::checked).orElse(false);
    }

    String resolvedShortcutText(CommandManager manager) {
        if (!shortcutText.isEmpty()) return shortcutText;
        return command(manager).map(EditorCommand::shortcutText).orElse("");
    }

    void activate(CommandManager manager) {
        if (!resolvedEnabled(manager)) return;
        if (kind == Kind.COMMAND) {
            if (manager != null && !commandId.isEmpty()) manager.execute(commandId);
        } else if (kind == Kind.ACTION) {
            action.run();
        }
    }

    private Optional<EditorCommand> command(CommandManager manager) {
        if (manager == null || commandId.isEmpty()) return Optional.empty();
        return manager.command(commandId);
    }

    private static String normalizeText(String text) {
        return text == null ? "" : text.trim();
    }

    public enum Kind {
        ACTION,
        COMMAND,
        SEPARATOR,
        SUBMENU
    }
}
