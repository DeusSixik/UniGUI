package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.docking.DockPane;
import dev.sixik.unigui.widgets.interaction.ToolButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Compact editor pane header with title, dirty marker, pin, menu and close affordances. */
@XmlWidgetName("PaneHeader")
public class PaneHeader extends LinearBox {
    private static final float DEFAULT_HEIGHT = 22.0f;

    private final Label titleLabel = new Label("Pane");
    private final Label dirtyLabel = new Label("*");
    private final ToolButton pinButton = new ToolButton();
    private final ToolButton menuButton = new ToolButton();
    private final ToolButton closeButton = new ToolButton();
    private final List<Consumer<PaneHeaderAction>> listeners = new ArrayList<>();

    private DockPane pane;
    private String paneId = "";
    private String title = "Pane";
    private boolean dirty;
    private boolean pinned = true;
    private boolean closable = true;
    private boolean pinVisible = true;
    private boolean menuVisible = true;
    private boolean closeVisible = true;
    private float headerHeight = DEFAULT_HEIGHT;

    public PaneHeader() {
        super(Orientation.HORIZONTAL);
        spacing(4.0f);
        layout(style -> style.height(DEFAULT_HEIGHT).flexGrow(0.0f).flexShrink(0.0f));

        titleLabel.layout(style -> style.height(20.0f).flexGrow(1.0f).flexShrink(1.0f));
        dirtyLabel.layout(style -> style.size(10.0f, 20.0f).flexGrow(0.0f).flexShrink(0.0f));
        configureActionButton(pinButton, "P");
        configureActionButton(menuButton, "...");
        configureActionButton(closeButton, "x");

        pinButton.onClick(event -> togglePinned());
        menuButton.onClick(event -> requestMenu());
        closeButton.onClick(event -> requestClose());

        addChild(titleLabel);
        addChild(dirtyLabel);
        addChild(pinButton);
        addChild(menuButton);
        addChild(closeButton);
        applyQueuedMutations();
        refresh();
    }

    public DockPane pane() {
        return pane;
    }

    public PaneHeader pane(DockPane pane) {
        this.pane = pane;
        if (pane != null) {
            paneId(pane.id());
            title(pane.title());
            dirty(pane.dirty());
            pinned(pane.pinned());
            closable(pane.closable());
        }
        return this;
    }

    public String paneId() {
        return paneId;
    }

    @XmlAttribute(value = "paneId", category = "State", defaultValue = "", description = "Editor pane id represented by this header.")
    public PaneHeader paneId(String paneId) {
        this.paneId = paneId == null ? "" : paneId.trim();
        return this;
    }

    public String title() {
        return title;
    }

    @XmlAttribute(value = "title", category = "Content", defaultValue = "Pane", description = "Pane title shown in the header.")
    public PaneHeader title(String title) {
        String normalized = title == null || title.isBlank() ? "Pane" : title.trim();
        if (this.title.equals(normalized)) return this;
        this.title = normalized;
        if (pane != null && !pane.title().equals(normalized)) {
            pane.title(normalized);
        }
        refresh();
        return this;
    }

    public boolean dirty() {
        return dirty;
    }

    @XmlAttribute(value = "dirty", category = "State", defaultValue = "false", description = "Whether the pane has unsaved changes.")
    public PaneHeader dirty(boolean dirty) {
        if (this.dirty == dirty) return this;
        this.dirty = dirty;
        if (pane != null && pane.dirty() != dirty) {
            pane.dirty(dirty);
        }
        refresh();
        return this;
    }

    public boolean pinned() {
        return pinned;
    }

    @XmlAttribute(value = "pinned", category = "State", defaultValue = "true", description = "Whether the pane is pinned instead of auto-hidden.")
    public PaneHeader pinned(boolean pinned) {
        if (this.pinned == pinned) return this;
        this.pinned = pinned;
        if (pane != null && pane.pinned() != pinned) {
            pane.pinned(pinned);
        }
        refresh();
        return this;
    }

    public boolean closable() {
        return closable;
    }

    @XmlAttribute(value = "closable", category = "Behavior", defaultValue = "true", description = "Whether the close action is enabled.")
    public PaneHeader closable(boolean closable) {
        if (this.closable == closable) return this;
        this.closable = closable;
        if (pane != null && pane.closable() != closable) {
            pane.closable(closable);
        }
        refresh();
        return this;
    }

    public boolean pinVisible() {
        return pinVisible;
    }

    @XmlAttribute(value = "pinVisible", category = "Behavior", defaultValue = "true", description = "Whether the pin toggle is shown.")
    public PaneHeader pinVisible(boolean pinVisible) {
        if (this.pinVisible == pinVisible) return this;
        this.pinVisible = pinVisible;
        refresh();
        return this;
    }

    public boolean menuVisible() {
        return menuVisible;
    }

    @XmlAttribute(value = "menuVisible", category = "Behavior", defaultValue = "true", description = "Whether the pane menu button is shown.")
    public PaneHeader menuVisible(boolean menuVisible) {
        if (this.menuVisible == menuVisible) return this;
        this.menuVisible = menuVisible;
        refresh();
        return this;
    }

    public boolean closeVisible() {
        return closeVisible;
    }

    @XmlAttribute(value = "closeVisible", category = "Behavior", defaultValue = "true", description = "Whether the close button is shown when closable.")
    public PaneHeader closeVisible(boolean closeVisible) {
        if (this.closeVisible == closeVisible) return this;
        this.closeVisible = closeVisible;
        refresh();
        return this;
    }

    public float headerHeight() {
        return headerHeight;
    }

    @XmlAttribute(value = "headerHeight", category = "Layout", defaultValue = "22", description = "Pane header height in UI pixels.")
    public PaneHeader headerHeight(float headerHeight) {
        float normalized = Float.isFinite(headerHeight) ? Math.max(16.0f, headerHeight) : DEFAULT_HEIGHT;
        if (this.headerHeight == normalized) return this;
        this.headerHeight = normalized;
        layout(style -> style.height(normalized));
        titleLabel.layout(style -> style.height(Math.max(0.0f, normalized - 2.0f)));
        dirtyLabel.layout(style -> style.size(10.0f, Math.max(0.0f, normalized - 2.0f)));
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Label titleLabel() {
        return titleLabel;
    }

    public Label dirtyLabel() {
        return dirtyLabel;
    }

    public ToolButton pinButton() {
        return pinButton;
    }

    public ToolButton menuButton() {
        return menuButton;
    }

    public ToolButton closeButton() {
        return closeButton;
    }

    public boolean togglePinned() {
        pinned(!pinned);
        emit(Action.PIN_CHANGED);
        return true;
    }

    public boolean requestMenu() {
        if (!menuVisible) return false;
        emit(Action.MENU_REQUESTED);
        return true;
    }

    public boolean requestClose() {
        if (!closable) return false;
        emit(Action.CLOSE_REQUESTED);
        return true;
    }

    public EventSubscription onAction(Consumer<PaneHeaderAction> listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public EventSubscription onCloseRequested(Consumer<PaneHeaderAction> listener) {
        return onFiltered(Action.CLOSE_REQUESTED, listener);
    }

    public EventSubscription onPinChanged(Consumer<PaneHeaderAction> listener) {
        return onFiltered(Action.PIN_CHANGED, listener);
    }

    public EventSubscription onMenuRequested(Consumer<PaneHeaderAction> listener) {
        return onFiltered(Action.MENU_REQUESTED, listener);
    }

    protected void refresh() {
        titleLabel.text(title);
        dirtyLabel.visibility(dirty ? Visibility.VISIBLE : Visibility.COLLAPSED);
        pinButton.visible(pinVisible);
        pinButton.checked(pinned);
        pinButton.icon(pinned ? "P" : "p");
        pinButton.tooltip(pinned ? "Pinned" : "Unpinned");
        menuButton.visible(menuVisible);
        closeButton.visible(closeVisible && closable);
        closeButton.enabled(closable);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    protected void configureActionButton(ToolButton button, String icon) {
        button.icon(icon)
                .displayMode(ToolButton.DisplayMode.ICON_ONLY)
                .layout(style -> style.size(22.0f, 20.0f).flexGrow(0.0f).flexShrink(0.0f));
        button.textPadding(0.0f, 2.0f);
    }

    private EventSubscription onFiltered(Action action, Consumer<PaneHeaderAction> listener) {
        Objects.requireNonNull(listener, "listener");
        Consumer<PaneHeaderAction> wrapper = event -> {
            if (event.action() == action) listener.accept(event);
        };
        listeners.add(wrapper);
        return () -> listeners.remove(wrapper);
    }

    private void emit(Action action) {
        PaneHeaderAction event = new PaneHeaderAction(this, paneId, title, dirty, pinned, action);
        List<Consumer<PaneHeaderAction>> snapshot = List.copyOf(listeners);
        for (Consumer<PaneHeaderAction> listener : snapshot) {
            listener.accept(event);
        }
    }

    public enum Action {
        CLOSE_REQUESTED,
        PIN_CHANGED,
        MENU_REQUESTED
    }

    public record PaneHeaderAction(PaneHeader source,
                                   String paneId,
                                   String title,
                                   boolean dirty,
                                   boolean pinned,
                                   Action action) {
        public PaneHeaderAction {
            Objects.requireNonNull(source, "source");
            paneId = paneId == null ? "" : paneId.trim();
            title = title == null ? "" : title.trim();
            Objects.requireNonNull(action, "action");
        }
    }
}
