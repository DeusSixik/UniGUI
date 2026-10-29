package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;
import java.util.function.Predicate;
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
    private UiIcon icon = UiIcon.NONE;
    private Predicate<DockPane> closeGuard;
    private RichText measuredTitle;
    private float measuredTitleWidth;

    public DockPane(String title, Widget content) {
        this(null, title, content);
    }

    public DockPane(String id, String title, Widget content) {
        this.id = normalizeId(id);
        this.title = title == null ? "" : title;
        this.richTitle = RichText.resolve(this.title);
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
        this.richTitle = RichText.resolve(this.title);
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

    /**
     * Иконка вкладки.
     *
     * @return иконка или {@link UiIcon#NONE}
     */
    public UiIcon icon() {
        return icon;
    }

    /**
     * Задаёт иконку, которую рисуют вкладка в {@link DockingRoot} и заголовок окна, если панель вытянули
     * в плавающее окно.
     *
     * @param icon иконка; {@code null} убирает иконку
     * @return эта панель для fluent-настройки
     */
    public DockPane icon(UiIcon icon) {
        this.icon = icon == null ? UiIcon.NONE : icon;
        content.invalidate(InvalidationFlags.VISUAL | InvalidationFlags.LAYOUT);
        return this;
    }

    /**
     * Охранник закрытия: вызывается, когда пользователь закрывает вкладку крестиком, средней кнопкой или
     * {@code Ctrl+W}.
     *
     * @param guard предикат; {@code false} отменяет закрытие (например, чтобы спросить про сохранение);
     *              {@code null} снимает охранник
     * @return эта панель для fluent-настройки
     */
    public DockPane onCloseRequested(Predicate<DockPane> guard) {
        this.closeGuard = guard;
        return this;
    }

    /**
     * Можно ли сейчас закрыть панель действием пользователя.
     *
     * @return {@code true}, если панель закрываемая и охранник (если есть) не против
     */
    public boolean canClose() {
        return closable && (closeGuard == null || closeGuard.test(this));
    }

    /**
     * Ширина заголовка в пикселях; результат кэшируется до смены текста, потому что раскладка вкладок
     * вызывается на каждое движение указателя.
     *
     * @return ширина однострочного заголовка
     */
    float titleWidth() {
        if (measuredTitle != richTitle) {
            measuredTitle = richTitle;
            measuredTitleWidth = TextEngine.measureLineWidth(richTitle);
        }
        return measuredTitleWidth;
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
