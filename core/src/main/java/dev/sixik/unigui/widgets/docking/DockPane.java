package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.containers.PanelWidget;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

@AiDoc(
        description = "Descriptor representing an individual dockable pane hosting arbitrary widget content, tab title, icon, dirty state, and close guards."
)
@WikiPage(
        title = "DockPane",
        slug = "dock-pane",
        category = "<key:docking.category>",
        summary = "<key:dock.pane.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:dock.pane.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:dock.pane.example.document.title>",
        code = "DockPane doc = DockPane.document(\"main-editor\", \"Main.java\", editorWidget)\n"
                + "        .dirty(true)\n"
                + "        .closable(true);"
)
@WikiLink(
        label = "DockingRoot",
        target = "page:docking-root",
        description = "<key:docking.root.summary>"
)
@WikiLink(
        label = "DockPaneKind",
        target = "page:dock-pane-kind",
        description = "<key:dock.pane.kind.summary>"
)
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

    @AiDoc(description = "Creates a dock pane with generated unique identifier.")
    @WikiMethod(description = "<key:dock.pane.method.ctorTitleContent>")
    public DockPane(
            @WikiParameter(description = "<key:dock.pane.param.title>")
            String title,
            @WikiParameter(description = "<key:dock.pane.param.content>")
            Widget content
    ) {
        this(null, title, content);
    }

    @AiDoc(description = "Creates a dock pane with explicit identifier, string title, and widget content.")
    @WikiMethod(description = "<key:dock.pane.method.ctorFull>")
    public DockPane(
            @WikiParameter(description = "<key:dock.pane.param.id>")
            String id,
            @WikiParameter(description = "<key:dock.pane.param.title>")
            String title,
            @WikiParameter(description = "<key:dock.pane.param.content>")
            Widget content
    ) {
        this.id = normalizeId(id);
        this.title = title == null ? "" : title;
        this.richTitle = RichText.resolve(this.title);
        this.content = content == null ? new PanelWidget() : content;
    }

    @AiDoc(description = "Creates a document-style dock pane with string title.")
    @WikiMethod(description = "<key:dock.pane.method.docString>")
    @WikiReturn("<key:dock.pane.return.pane>")
    public static DockPane document(
            @WikiParameter(description = "<key:dock.pane.param.id>")
            String id,
            @WikiParameter(description = "<key:dock.pane.param.title>")
            String title,
            @WikiParameter(description = "<key:dock.pane.param.content>")
            Widget content
    ) {
        return new DockPane(id, title, content).kind(DockPaneKind.DOCUMENT);
    }

    @AiDoc(description = "Creates a document-style dock pane with RichText title.")
    @WikiMethod(description = "<key:dock.pane.method.docRich>")
    @WikiReturn("<key:dock.pane.return.pane>")
    public static DockPane document(
            @WikiParameter(description = "<key:dock.pane.param.id>")
            String id,
            @WikiParameter(description = "<key:dock.pane.param.richTitle>")
            RichText title,
            @WikiParameter(description = "<key:dock.pane.param.content>")
            Widget content
    ) {
        return new DockPane(id, title, content).kind(DockPaneKind.DOCUMENT);
    }

    @AiDoc(description = "Creates a tool-style dock pane with string title.")
    @WikiMethod(description = "<key:dock.pane.method.toolString>")
    @WikiReturn("<key:dock.pane.return.pane>")
    public static DockPane tool(
            @WikiParameter(description = "<key:dock.pane.param.id>")
            String id,
            @WikiParameter(description = "<key:dock.pane.param.title>")
            String title,
            @WikiParameter(description = "<key:dock.pane.param.content>")
            Widget content
    ) {
        return new DockPane(id, title, content).kind(DockPaneKind.TOOL);
    }

    @AiDoc(description = "Creates a tool-style dock pane with RichText title.")
    @WikiMethod(description = "<key:dock.pane.method.toolRich>")
    @WikiReturn("<key:dock.pane.return.pane>")
    public static DockPane tool(
            @WikiParameter(description = "<key:dock.pane.param.id>")
            String id,
            @WikiParameter(description = "<key:dock.pane.param.richTitle>")
            RichText title,
            @WikiParameter(description = "<key:dock.pane.param.content>")
            Widget content
    ) {
        return new DockPane(id, title, content).kind(DockPaneKind.TOOL);
    }

    @AiDoc(description = "Creates a dock pane with explicit identifier, RichText title, and content.")
    @WikiMethod(description = "<key:dock.pane.method.ctorRich>")
    public DockPane(
            @WikiParameter(description = "<key:dock.pane.param.id>")
            String id,
            @WikiParameter(description = "<key:dock.pane.param.richTitle>")
            RichText title,
            @WikiParameter(description = "<key:dock.pane.param.content>")
            Widget content
    ) {
        this.id = normalizeId(id);
        this.richTitle = title == null ? RichText.plain("") : title;
        this.title = this.richTitle.plainText();
        this.content = content == null ? new PanelWidget() : content;
    }

    @AiDoc(description = "Returns the unique pane identifier.")
    @WikiMethod(description = "<key:dock.pane.method.id>")
    @WikiReturn("<key:dock.pane.return.id>")
    public String id() {
        return id;
    }

    @AiDoc(description = "Returns the plain-text title string.")
    @WikiMethod(description = "<key:dock.pane.method.title>")
    @WikiReturn("<key:dock.pane.return.title>")
    public String title() {
        return title;
    }

    @AiDoc(description = "Returns the RichText formatted title.")
    @WikiMethod(description = "<key:dock.pane.method.richTitle>")
    @WikiReturn("<key:dock.pane.return.richTitle>")
    public RichText richTitle() {
        return richTitle;
    }

    @AiDoc(description = "Returns the hosted widget content.")
    @WikiMethod(description = "<key:dock.pane.method.content>")
    @WikiReturn("<key:dock.pane.return.content>")
    public Widget content() {
        return content;
    }

    @AiDoc(description = "Returns the pane kind (DOCUMENT or TOOL).")
    @WikiMethod(description = "<key:dock.pane.method.kind>")
    @WikiReturn("<key:dock.pane.return.kind>")
    public DockPaneKind kind() {
        return kind;
    }

    @AiDoc(description = "Returns true if this pane is classified as a document.")
    @WikiMethod(description = "<key:dock.pane.method.isDocument>")
    @WikiReturn("<key:dock.pane.return.isDocument>")
    public boolean document() {
        return kind == DockPaneKind.DOCUMENT;
    }

    @AiDoc(description = "Returns true if this pane is classified as a tool window.")
    @WikiMethod(description = "<key:dock.pane.method.isTool>")
    @WikiReturn("<key:dock.pane.return.isTool>")
    public boolean tool() {
        return kind == DockPaneKind.TOOL;
    }

    @AiDoc(description = "Returns true if this pane can be closed by the user.")
    @WikiMethod(description = "<key:dock.pane.method.closable>")
    @WikiReturn("<key:dock.pane.return.closable>")
    public boolean closable() {
        return closable;
    }

    @AiDoc(description = "Returns true if this pane has unsaved changes (shows dot indicator).")
    @WikiMethod(description = "<key:dock.pane.method.dirty>")
    @WikiReturn("<key:dock.pane.return.dirty>")
    public boolean dirty() {
        return dirty;
    }

    @AiDoc(description = "Returns true if this pane is pinned in the dock hierarchy.")
    @WikiMethod(description = "<key:dock.pane.method.pinned>")
    @WikiReturn("<key:dock.pane.return.pinned>")
    public boolean pinned() {
        return pinned;
    }

    @AiDoc(description = "Returns true if this pane automatically hides when focus is lost.")
    @WikiMethod(description = "<key:dock.pane.method.autoHide>")
    @WikiReturn("<key:dock.pane.return.autoHide>")
    public boolean autoHide() {
        return autoHide;
    }

    @AiDoc(description = "Sets the plain-text title of the pane.")
    @WikiMethod(description = "<key:dock.pane.method.setTitleString>")
    @WikiReturn("<key:dock.pane.return.this>")
    public DockPane title(
            @WikiParameter(description = "<key:dock.pane.param.title>")
            String title
    ) {
        this.title = title == null ? "" : title;
        this.richTitle = RichText.resolve(this.title);
        content.invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(description = "Sets the RichText title of the pane.")
    @WikiMethod(description = "<key:dock.pane.method.setTitleRich>")
    @WikiReturn("<key:dock.pane.return.this>")
    public DockPane richTitle(
            @WikiParameter(description = "<key:dock.pane.param.richTitle>")
            RichText title
    ) {
        this.richTitle = title == null ? RichText.plain("") : title;
        this.title = this.richTitle.plainText();
        content.invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(description = "Sets whether the pane can be closed.")
    @WikiMethod(description = "<key:dock.pane.method.setClosable>")
    @WikiReturn("<key:dock.pane.return.this>")
    public DockPane closable(
            @WikiParameter(description = "<key:dock.pane.param.closable>")
            boolean closable
    ) {
        this.closable = closable;
        return this;
    }

    @AiDoc(description = "Sets the pane classification (DOCUMENT or TOOL).")
    @WikiMethod(description = "<key:dock.pane.method.setKind>")
    @WikiReturn("<key:dock.pane.return.this>")
    public DockPane kind(
            @WikiParameter(description = "<key:dock.pane.param.kind>")
            DockPaneKind kind
    ) {
        this.kind = kind == null ? DockPaneKind.TOOL : kind;
        content.invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(description = "Marks the pane as having unsaved modifications.")
    @WikiMethod(description = "<key:dock.pane.method.setDirty>")
    @WikiReturn("<key:dock.pane.return.this>")
    public DockPane dirty(
            @WikiParameter(description = "<key:dock.pane.param.dirty>")
            boolean dirty
    ) {
        this.dirty = dirty;
        content.invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(description = "Sets whether the pane is pinned.")
    @WikiMethod(description = "<key:dock.pane.method.setPinned>")
    @WikiReturn("<key:dock.pane.return.this>")
    public DockPane pinned(
            @WikiParameter(description = "<key:dock.pane.param.pinned>")
            boolean pinned
    ) {
        this.pinned = pinned;
        if (!pinned) {
            autoHide = false;
        }
        content.invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(description = "Enables or disables auto-hide for unpinned panes.")
    @WikiMethod(description = "<key:dock.pane.method.setAutoHide>")
    @WikiReturn("<key:dock.pane.return.this>")
    public DockPane autoHide(
            @WikiParameter(description = "<key:dock.pane.param.autoHide>")
            boolean autoHide
    ) {
        this.autoHide = autoHide && !pinned;
        content.invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Иконка вкладки.
     *
     * @return иконка или {@link UiIcon#NONE}
     */
    @AiDoc(description = "Returns the icon associated with this tab.")
    @WikiMethod(description = "<key:dock.pane.method.icon>")
    @WikiReturn("<key:dock.pane.return.icon>")
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
    @AiDoc(description = "Sets the icon displayed in tab strips and floating window headers.")
    @WikiMethod(description = "<key:dock.pane.method.setIcon>")
    @WikiReturn("<key:dock.pane.return.this>")
    public DockPane icon(
            @WikiParameter(description = "<key:dock.pane.param.icon>")
            UiIcon icon
    ) {
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
    @AiDoc(description = "Sets a close request guard predicate that can prompt for saving or prevent closing.")
    @WikiMethod(description = "<key:dock.pane.method.onCloseRequested>")
    @WikiReturn("<key:dock.pane.return.this>")
    public DockPane onCloseRequested(
            @WikiParameter(description = "<key:dock.pane.param.guard>")
            Predicate<DockPane> guard
    ) {
        this.closeGuard = guard;
        return this;
    }

    /**
     * Можно ли сейчас закрыть панель действием пользователя.
     *
     * @return {@code true}, если панель закрываемая и охранник (если есть) не против
     */
    @AiDoc(description = "Returns true if closable and closeGuard permits closing.")
    @WikiMethod(description = "<key:dock.pane.method.canClose>")
    @WikiReturn("<key:dock.pane.return.canClose>")
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
