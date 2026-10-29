package dev.sixik.unigui.widgets.navigation;

import dev.sixik.unigui.api.editor.CommandManager;
import dev.sixik.unigui.api.editor.EditorCommand;

import java.util.Optional;

/** Lightweight menu item model used by {@link MenuBar}. */
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

    public static MenuItem action(String label, Runnable action) {
        return new MenuItem(Kind.ACTION).label(label).action(action);
    }

    public static MenuItem command(String commandId) {
        return new MenuItem(Kind.COMMAND).commandId(commandId);
    }

    public static MenuItem command(String commandId, String label) {
        return command(commandId).label(label);
    }

    public static MenuItem separator() {
        return new MenuItem(Kind.SEPARATOR).enabled(false);
    }

    public static MenuItem submenu(Menu submenu) {
        return new MenuItem(Kind.SUBMENU).menu(submenu);
    }

    public Kind kind() {
        return kind;
    }

    public String label() {
        return label;
    }

    public MenuItem label(String label) {
        this.label = normalizeText(label);
        return this;
    }

    public String commandId() {
        return commandId;
    }

    public MenuItem commandId(String commandId) {
        this.commandId = normalizeText(commandId);
        return this;
    }

    public Runnable action() {
        return action;
    }

    public MenuItem action(Runnable action) {
        this.action = action == null ? NO_ACTION : action;
        return this;
    }

    public Menu submenu() {
        return submenu;
    }

    public MenuItem menu(Menu submenu) {
        this.submenu = submenu;
        return this;
    }

    public boolean enabled() {
        return enabled;
    }

    public MenuItem enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public boolean checked() {
        return checked;
    }

    public MenuItem checked(boolean checked) {
        this.checked = checked;
        return this;
    }

    public String shortcutText() {
        return shortcutText;
    }

    public MenuItem shortcutText(String shortcutText) {
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
