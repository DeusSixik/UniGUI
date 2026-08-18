package dev.sixik.unigui.widgets.navigation;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.editor.CommandManager;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collections;
import java.util.List;

/** Horizontal application menu bar backed by {@link CommandManager}. */
@XmlWidgetName("MenuBar")
public final class MenuBar extends LinearBox {
    private final List<Menu> menus = new ObjectArrayList<>();
    private final List<Menu> menusView = Collections.unmodifiableList(menus);
    private final List<Button> menuButtons = new ObjectArrayList<>();
    private final List<ContextMenu> menuPopups = new ObjectArrayList<>();
    private CommandManager commandManager = new CommandManager();
    private EventSubscription commandSubscription;
    private int openedMenuIndex = -1;

    public MenuBar() {
        super(Orientation.HORIZONTAL);
        focusable(true);
        spacing(2.0f);
        layout(style -> style.height(22.0f).flexShrink(0.0f).overflow(Overflow.VISIBLE));
        subscribeToCommandManager();
    }

    public CommandManager commandManager() {
        return commandManager;
    }

    public MenuBar commandManager(CommandManager commandManager) {
        CommandManager next = commandManager == null ? new CommandManager() : commandManager;
        if (this.commandManager == next) return this;
        if (commandSubscription != null) commandSubscription.unsubscribe();
        this.commandManager = next;
        subscribeToCommandManager();
        refreshOpenMenu();
        return this;
    }

    public List<Menu> menus() {
        return menusView;
    }

    public int menuCount() {
        return menus.size();
    }

    public Menu menu(String label) {
        Menu menu = new Menu(label);
        menu(menu);
        return menu;
    }

    public MenuBar menu(Menu menu) {
        if (menu == null) return this;
        menus.add(menu);
        rebuildHeaders();
        return this;
    }

    public MenuBar menus(List<Menu> menus) {
        this.menus.clear();
        if (menus != null) {
            for (Menu menu : menus) {
                if (menu != null) this.menus.add(menu);
            }
        }
        rebuildHeaders();
        return this;
    }

    public MenuBar clearMenus() {
        menus.clear();
        rebuildHeaders();
        return this;
    }

    public int openedMenuIndex() {
        syncOpenedMenuState();
        return openedMenuIndex;
    }

    public ContextMenu openedPopup() {
        syncOpenedMenuState();
        return openedPopup(true);
    }

    public MenuBar openMenu(int index) {
        if (index < 0 || index >= menus.size()) {
            closeOpenMenu();
            return this;
        }
        closeOpenMenu();
        if (index < menuPopups.size()) detachPopup(menuPopups.get(index));
        ContextMenu popup = buildPopup(menus.get(index));
        while (menuPopups.size() <= index) menuPopups.add(null);
        menuPopups.set(index, popup);
        ensurePopupHost(popup);
        if (popup.parent() == this) applyQueuedMutations();

        Button button = menuButtons.get(index);
        popup.openAt(button.layoutBounds().x(), button.layoutBounds().y() + button.layoutBounds().height() + 2.0f);
        openedMenuIndex = index;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MenuBar closeOpenMenu() {
        ContextMenu popup = openedPopup();
        if (popup != null) popup.close();
        openedMenuIndex = -1;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (!(event instanceof KeyPressedEvent key) || key.phase() != EventPhase.TARGET) return;
        boolean focusedHere = uiContext() != null && uiContext().focusManager().isFocused(this);
        if (!focusedHere) return;
        if (key.keyCode() == KeyCodes.RIGHT) {
            openMenu(nextMenuIndex(1));
            event.cancel();
        } else if (key.keyCode() == KeyCodes.LEFT) {
            openMenu(nextMenuIndex(-1));
            event.cancel();
        } else if (key.keyCode() == KeyCodes.DOWN
                || key.keyCode() == KeyCodes.ENTER
                || key.keyCode() == KeyCodes.KEYPAD_ENTER
                || key.keyCode() == KeyCodes.SPACE) {
            openMenu(openedMenuIndex >= 0 ? openedMenuIndex : 0);
            event.cancel();
        } else if (key.keyCode() == KeyCodes.ESCAPE) {
            closeOpenMenu();
            event.cancel();
        }
    }

    @Override
    public void measure(LayoutContext context) {
        super.measure(context);
        for (ContextMenu popup : menuPopups) {
            if (popup != null && popup.parent() == this) popup.measure(context);
        }
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        RectView hostBounds = parent() == null ? bounds : parent().layoutBounds();
        for (ContextMenu popup : menuPopups) {
            if (popup != null && popup.parent() == this) popup.arrangeInHost(hostBounds);
        }
    }

    private void rebuildHeaders() {
        detachPopups();
        menuButtons.clear();
        menuPopups.clear();
        openedMenuIndex = -1;
        clearChildren();
        for (int index = 0; index < menus.size(); index++) {
            Menu menu = menus.get(index);
            Button button = new Button(menu.label());
            button.textPadding(8.0f, 2.0f);
            button.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
            final int menuIndex = index;
            button.onClick(event -> openMenu(menuIndex));
            button.on(PointerEnteredEvent.TYPE, event -> {
                if (event.phase() == EventPhase.TARGET && menuTraversalActive() && openedMenuIndex != menuIndex) {
                    openMenu(menuIndex);
                }
            });
            menuButtons.add(button);
            menuPopups.add(null);
            addChild(button);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private ContextMenu buildPopup(Menu menu) {
        ContextMenu popup = new ContextMenu();
        popup.closeOnOutsideClick(true);
        for (MenuItem item : menu.items()) {
            addPopupItem(popup, item);
        }
        return popup;
    }

    private void addPopupItem(ContextMenu popup, MenuItem item) {
        if (item == null) return;
        if (item.kind() == MenuItem.Kind.SEPARATOR) {
            popup.separator();
            return;
        }
        if (item.kind() == MenuItem.Kind.SUBMENU) {
            ContextMenu submenu = item.submenu() == null ? new ContextMenu() : buildPopup(item.submenu());
            popup.submenu(RichText.plain(item.resolvedLabel(commandManager) + " >"), submenu);
            popup.itemButton(popup.itemCount() - 1).enabled(item.resolvedEnabled(commandManager));
            return;
        }
        int itemIndex = popup.itemCount();
        popup.item(renderItemLabel(item), () -> item.activate(commandManager));
        popup.itemButton(itemIndex).enabled(item.resolvedEnabled(commandManager));
    }

    private String renderItemLabel(MenuItem item) {
        String label = item.resolvedLabel(commandManager);
        if (item.resolvedChecked(commandManager)) label = "[x] " + label;
        String shortcut = item.resolvedShortcutText(commandManager);
        return shortcut.isEmpty() ? label : label + "    " + shortcut;
    }

    private void refreshOpenMenu() {
        int index = openedMenuIndex;
        if (index >= 0 && openedPopup(true) != null) {
            openMenu(index);
        } else {
            openedMenuIndex = -1;
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    private boolean menuTraversalActive() {
        return openedPopup(true) != null;
    }

    private void syncOpenedMenuState() {
        if (openedMenuIndex >= 0 && openedPopup(true) == null) {
            openedMenuIndex = -1;
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    private ContextMenu openedPopup(boolean requireOpen) {
        ContextMenu popup = openedMenuIndex >= 0 && openedMenuIndex < menuPopups.size()
                ? menuPopups.get(openedMenuIndex)
                : null;
        if (requireOpen && (popup == null || !popup.opened())) return null;
        return popup;
    }

    private int nextMenuIndex(int delta) {
        if (menus.isEmpty()) return -1;
        int base = openedMenuIndex >= 0 ? openedMenuIndex : 0;
        return (base + delta + menus.size()) % menus.size();
    }

    private void ensurePopupHost(ContextMenu popup) {
        OverlayLayer layer = overlayLayer();
        if (layer != null) {
            if (popup.parent() != layer) layer.addOverlay(popup);
        } else if (popup.parent() != this) {
            addChild(popup);
        }
    }

    private void detachPopups() {
        for (ContextMenu popup : menuPopups) {
            detachPopup(popup);
        }
    }

    private void detachPopup(ContextMenu popup) {
        if (popup == null) return;
        popup.close();
        Widget parent = popup.parent();
        if (parent instanceof OverlayLayer layer) {
            layer.removeOverlay(popup);
        } else if (parent instanceof PanelWidget panel) {
            panel.removeChild(popup);
        }
    }

    private OverlayLayer overlayLayer() {
        Widget current = this;
        while (current != null) {
            if (current instanceof OverlayLayer layer) return layer;
            current = current.parent();
        }
        return null;
    }

    private void subscribeToCommandManager() {
        commandSubscription = commandManager.onChanged(event -> refreshOpenMenu());
    }
}
