package dev.sixik.unigui.widgets.navigation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collections;
import java.util.List;

/** Lightweight top-level or nested menu model used by {@link MenuBar}. */
public final class Menu {
    private final List<MenuItem> items = new ObjectArrayList<>();
    private final List<MenuItem> itemsView = Collections.unmodifiableList(items);
    private String id;
    private String label;

    public Menu(String label) {
        this(null, label);
    }

    public Menu(String id, String label) {
        this.label = normalizeLabel(label, "Menu");
        this.id = normalizeId(id, this.label);
    }

    public String id() {
        return id;
    }

    public Menu id(String id) {
        this.id = normalizeId(id, label);
        return this;
    }

    public String label() {
        return label;
    }

    public Menu label(String label) {
        this.label = normalizeLabel(label, this.label);
        if (id == null || id.isEmpty()) {
            id = normalizeId(null, this.label);
        }
        return this;
    }

    public List<MenuItem> items() {
        return itemsView;
    }

    public Menu item(MenuItem item) {
        if (item != null) items.add(item);
        return this;
    }

    public Menu action(String label, Runnable action) {
        return item(MenuItem.action(label, action));
    }

    public Menu command(String commandId) {
        return item(MenuItem.command(commandId));
    }

    public Menu command(String commandId, String label) {
        return item(MenuItem.command(commandId, label));
    }

    public Menu separator() {
        if (!items.isEmpty() && items.get(items.size() - 1).kind() != MenuItem.Kind.SEPARATOR) {
            item(MenuItem.separator());
        }
        return this;
    }

    public Menu submenu(Menu submenu) {
        return item(MenuItem.submenu(submenu));
    }

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
