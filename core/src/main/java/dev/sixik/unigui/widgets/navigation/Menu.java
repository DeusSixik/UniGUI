package dev.sixik.unigui.widgets.navigation;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collections;
import java.util.List;

/** Lightweight top-level or nested menu model used by {@link MenuBar}. */
@AiDoc(summary = "Hierarchical menu model containing actions, commands, submenus, and separators for MenuBar.")
@WikiPage(
        title = "Menu",
        slug = "menu",
        category = "<key:navigation.category>",
        summary = "<key:menu.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:menu.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:menu.example.basic.title>",
        code = "Menu fileMenu = new Menu(\"File\")\n    .action(\"New\", this::onNew)\n    .separator()\n    .action(\"Exit\", this::onExit);"
)
@WikiLink(
        label = "MenuBar",
        target = "page:menu-bar",
        description = "<key:menu.bar.summary>"
)
@WikiLink(
        label = "MenuItem",
        target = "page:menu-item",
        description = "<key:menu.item.summary>"
)
public final class Menu {
    private final List<MenuItem> items = new ObjectArrayList<>();
    private final List<MenuItem> itemsView = Collections.unmodifiableList(items);
    private String id;
    private String label;

        @AiDoc(summary = "Constructs a Menu with the given display label.")
    @WikiMethod(summary = "<key:menu.method.ctorLabel>")
    public Menu(
            @WikiParameter(description = "<key:menu.param.label>")
            String label
    ) {
        this(null, label);
    }

        @AiDoc(summary = "Constructs a Menu with an explicit identifier and display label.")
    @WikiMethod(summary = "<key:menu.method.ctorIdLabel>")
    public Menu(
            @WikiParameter(description = "<key:menu.param.id>")
            String id,
            @WikiParameter(description = "<key:menu.param.label>")
            String label
    ) {
        this.label = normalizeLabel(label, "Menu");
        this.id = normalizeId(id, this.label);
    }

        @AiDoc(summary = "Returns the menu identifier string.")
    @WikiMethod(summary = "<key:menu.method.idGet>")
    @WikiReturn("<key:menu.return.id>")
    public String id() {
        return id;
    }

        @AiDoc(summary = "Sets the menu identifier string.")
    @WikiMethod(summary = "<key:menu.method.idSet>")
    @WikiReturn("<key:menu.return.this>")
    public Menu id(
            @WikiParameter(description = "<key:menu.param.id>")
            String id
    ) {
        this.id = normalizeId(id, label);
        return this;
    }

        @AiDoc(summary = "Returns the menu display label.")
    @WikiMethod(summary = "<key:menu.method.labelGet>")
    @WikiReturn("<key:menu.return.label>")
    public String label() {
        return label;
    }

        @AiDoc(summary = "Sets the menu display label.")
    @WikiMethod(summary = "<key:menu.method.labelSet>")
    @WikiReturn("<key:menu.return.this>")
    public Menu label(
            @WikiParameter(description = "<key:menu.param.label>")
            String label
    ) {
        this.label = normalizeLabel(label, this.label);
        if (id == null || id.isEmpty()) {
            id = normalizeId(null, this.label);
        }
        return this;
    }

        @AiDoc(summary = "Returns an unmodifiable list of menu items.")
    @WikiMethod(summary = "<key:menu.method.items>")
    @WikiReturn("<key:menu.return.items>")
    public List<MenuItem> items() {
        return itemsView;
    }

        @AiDoc(summary = "Appends a MenuItem to this menu.")
    @WikiMethod(summary = "<key:menu.method.item>")
    @WikiReturn("<key:menu.return.this>")
    public Menu item(
            @WikiParameter(description = "<key:menu.param.item>")
            MenuItem item
    ) {
        if (item != null) items.add(item);
        return this;
    }

        @AiDoc(summary = "Appends an action item with label and callback.")
    @WikiMethod(summary = "<key:menu.method.action>")
    @WikiReturn("<key:menu.return.this>")
    public Menu action(
            @WikiParameter(description = "<key:menu.param.label>")
            String label,
            @WikiParameter(description = "<key:menu.param.action>")
            Runnable action
    ) {
        return item(MenuItem.action(label, action));
    }

        @AiDoc(summary = "Appends an editor command item by command identifier.")
    @WikiMethod(summary = "<key:menu.method.command>")
    @WikiReturn("<key:menu.return.this>")
    public Menu command(
            @WikiParameter(description = "<key:menu.param.commandId>")
            String commandId
    ) {
        return item(MenuItem.command(commandId));
    }

        @AiDoc(summary = "Appends an editor command item with custom display label.")
    @WikiMethod(summary = "<key:menu.method.commandLabel>")
    @WikiReturn("<key:menu.return.this>")
    public Menu command(
            @WikiParameter(description = "<key:menu.param.commandId>")
            String commandId,
            @WikiParameter(description = "<key:menu.param.label>")
            String label
    ) {
        return item(MenuItem.command(commandId, label));
    }

        @AiDoc(summary = "Appends a visual separator divider.")
    @WikiMethod(summary = "<key:menu.method.separator>")
    @WikiReturn("<key:menu.return.this>")
    public Menu separator() {
        if (!items.isEmpty() && items.get(items.size() - 1).kind() != MenuItem.Kind.SEPARATOR) {
            item(MenuItem.separator());
        }
        return this;
    }

        @AiDoc(summary = "Appends a cascading nested submenu.")
    @WikiMethod(summary = "<key:menu.method.submenu>")
    @WikiReturn("<key:menu.return.this>")
    public Menu submenu(
            @WikiParameter(description = "<key:menu.param.submenu>")
            Menu submenu
    ) {
        return item(MenuItem.submenu(submenu));
    }

        @AiDoc(summary = "Clears all items from this menu.")
    @WikiMethod(summary = "<key:menu.method.clear>")
    @WikiReturn("<key:menu.return.this>")
    public Menu clear() {
        items.clear();
        return this;
    }

    private static String normalizeLabel(String label, String fallback) {
        String normalized = label == null ? "" : label.trim();
        if (!normalized.isEmpty()) return normalized;
        return fallback == null || fallback.isBlank() ? "Menu" : fallback;
    }

    private static String normalizeId(String id, String label) {
        String normalized = id == null ? "" : id.trim();
        if (!normalized.isEmpty()) return normalized;
        String source = normalizeLabel(label, "menu").toLowerCase(java.util.Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                builder.append(ch);
            } else if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != '.') {
                builder.append('.');
            }
        }
        while (!builder.isEmpty() && builder.charAt(builder.length() - 1) == '.') {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.isEmpty() ? "menu" : builder.toString();
    }
}
