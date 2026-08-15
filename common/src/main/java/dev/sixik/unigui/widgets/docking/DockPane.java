package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;
import java.util.UUID;
import dev.sixik.unigui.widgets.containers.PanelWidget;

public final class DockPane {
    private final String id;
    private String title;
    private RichText richTitle;
    private final Widget content;
    private DockPaneKind kind = DockPaneKind.TOOL;
    private boolean closable = true;
    private boolean dirty;
    private boolean pinned = true;
    private boolean autoHide;

    public DockPane(String title, Widget content) {
        this(null, title, content);
    }

    public DockPane(String id, String title, Widget content) {
        this.id = normalizeId(id);
        this.title = title == null ? "" : title;
        this.richTitle = RichText.plain(this.title);
        this.content = content == null ? new PanelWidget() : content;
    }

    public static DockPane document(String id, String title, Widget content) {
        return new DockPane(id, title, content).kind(DockPaneKind.DOCUMENT);
    }

    public static DockPane document(String id, RichText title, Widget content) {
        return new DockPane(id, title, content).kind(DockPaneKind.DOCUMENT);
    }

    public static DockPane tool(String id, String title, Widget content) {
        return new DockPane(id, title, content).kind(DockPaneKind.TOOL);
    }

    public static DockPane tool(String id, RichText title, Widget content) {
        return new DockPane(id, title, content).kind(DockPaneKind.TOOL);
    }

    public DockPane(String id, RichText title, Widget content) {
        this.id = normalizeId(id);
        this.richTitle = title == null ? RichText.plain("") : title;
        this.title = this.richTitle.plainText();
        this.content = content == null ? new PanelWidget() : content;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public RichText richTitle() {
        return richTitle;
    }

    public Widget content() {
        return content;
    }

    public DockPaneKind kind() {
        return kind;
    }

    public boolean document() {
        return kind == DockPaneKind.DOCUMENT;
    }

    public boolean tool() {
        return kind == DockPaneKind.TOOL;
    }

    public boolean closable() {
        return closable;
    }

    public boolean dirty() {
        return dirty;
    }

    public boolean pinned() {
        return pinned;
    }

    public boolean autoHide() {
        return autoHide;
    }

    public DockPane title(String title) {
        this.title = title == null ? "" : title;
        this.richTitle = RichText.plain(this.title);
        content.invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public DockPane richTitle(RichText title) {
        this.richTitle = title == null ? RichText.plain("") : title;
        this.title = this.richTitle.plainText();
        content.invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public DockPane closable(boolean closable) {
        this.closable = closable;
        return this;
    }

    public DockPane kind(DockPaneKind kind) {
        this.kind = kind == null ? DockPaneKind.TOOL : kind;
        content.invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public DockPane dirty(boolean dirty) {
        this.dirty = dirty;
        content.invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public DockPane pinned(boolean pinned) {
        this.pinned = pinned;
        if (!pinned) {
            autoHide = false;
        }
        content.invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public DockPane autoHide(boolean autoHide) {
        this.autoHide = autoHide && !pinned;
        content.invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    private static String normalizeId(String id) {
        String normalized = id == null ? "" : id.trim();
        return normalized.isEmpty() ? UUID.randomUUID().toString() : normalized;
    }

    @Override
    public String toString() {
        return "DockPane{" + id + ":" + title + "}";
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof DockPane pane && id.equals(pane.id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
