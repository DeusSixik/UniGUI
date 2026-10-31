package dev.sixik.unigui.widgets.navigation;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

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
@AiDoc(summary = "Desktop-style top menu bar displaying menus, keyboard shortcuts, command execution, and popup dropdowns.")
@WikiPage(
        title = "MenuBar",
        slug = "menu-bar",
        category = "<key:navigation.category>",
        summary = "<key:menu.bar.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:menu.bar.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:menu.bar.example.basic.title>",
        code = "MenuBar menuBar = new MenuBar();\nmenuBar.menu(fileMenu).menu(editMenu);"
)
@WikiLink(
        label = "Menu",
        target = "page:menu",
        description = "<key:menu.summary>"
)
@WikiLink(
        label = "MenuItem",
        target = "page:menu-item",
        description = "<key:menu.item.summary>"
)
@XmlWidgetName("MenuBar")
public final class MenuBar extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.MENU_BAR;

    private final List<Menu> menus = new ObjectArrayList<>();
    private final List<Menu> menusView = Collections.unmodifiableList(menus);
    private final List<Button> menuButtons = new ObjectArrayList<>();
    private final List<ContextMenu> menuPopups = new ObjectArrayList<>();
    private CommandManager commandManager = new CommandManager();
    private EventSubscription commandSubscription;
    private int openedMenuIndex = -1;

        @AiDoc(summary = "Constructs an empty top-level MenuBar.")
    @WikiMethod(summary = "<key:menu.bar.method.ctorDefault>")
    public MenuBar() {
        super(Orientation.HORIZONTAL);
        focusable(true);
        spacing(2.0f);
        layout(style -> style.height(22.0f).flexShrink(0.0f).overflow(Overflow.VISIBLE));
        subscribeToCommandManager();
    }

        @AiDoc(summary = "Returns the CommandManager used to resolve commands and shortcut bindings.")
    @WikiMethod(summary = "<key:menu.bar.method.commandManagerGet>")
    @WikiReturn("<key:menu.bar.return.commandManager>")
    public CommandManager commandManager() {
        return commandManager;
    }

        @AiDoc(summary = "Sets the CommandManager used to execute menu commands.")
    @WikiMethod(summary = "<key:menu.bar.method.commandManagerSet>")
    @WikiReturn("<key:menu.bar.return.this>")
    public MenuBar commandManager(
            @WikiParameter(description = "<key:menu.bar.param.commandManager>")
            CommandManager commandManager
    ) {
        CommandManager next = commandManager == null ? new CommandManager() : commandManager;
        if (this.commandManager == next) return this;
        if (commandSubscription != null) commandSubscription.unsubscribe();
        this.commandManager = next;
        subscribeToCommandManager();
        refreshOpenMenu();
        return this;
    }

        @AiDoc(summary = "Returns an unmodifiable list of top-level menus.")
    @WikiMethod(summary = "<key:menu.bar.method.menusGet>")
    @WikiReturn("<key:menu.bar.return.menus>")
    public List<Menu> menus() {
        return menusView;
    }

        @AiDoc(summary = "Returns the number of top-level menus in the menu bar.")
    @WikiMethod(summary = "<key:menu.bar.method.menuCount>")
    @WikiReturn("<key:menu.bar.return.count>")
    public int menuCount() {
        return menus.size();
    }

        @AiDoc(summary = "Finds a top-level menu by its label, or returns null if not found.")
    @WikiMethod(summary = "<key:menu.bar.method.menuByLabel>")
    @WikiReturn("<key:menu.bar.return.menu>")
    public Menu menu(
            @WikiParameter(description = "<key:menu.bar.param.label>")
            String label
    ) {
        Menu menu = new Menu(label);
        menu(menu);
        return menu;
    }

        @AiDoc(summary = "Appends a top-level Menu to the menu bar.")
    @WikiMethod(summary = "<key:menu.bar.method.addMenu>")
    @WikiReturn("<key:menu.bar.return.this>")
    public MenuBar menu(
            @WikiParameter(description = "<key:menu.bar.param.menu>")
            Menu menu
    ) {
        if (menu == null) return this;
        menus.add(menu);
        rebuildHeaders();
        return this;
    }

        @AiDoc(summary = "Replaces the entire list of top-level menus.")
    @WikiMethod(summary = "<key:menu.bar.method.menusSet>")
    @WikiReturn("<key:menu.bar.return.this>")
    public MenuBar menus(
            @WikiParameter(description = "<key:menu.bar.param.menus>")
            List<Menu> menus
    ) {
        this.menus.clear();
        if (menus != null) {
            for (Menu menu : menus) {
                if (menu != null) this.menus.add(menu);
            }
        }
        rebuildHeaders();
        return this;
    }

        @AiDoc(summary = "Removes all menus from the menu bar.")
    @WikiMethod(summary = "<key:menu.bar.method.clearMenus>")
    @WikiReturn("<key:menu.bar.return.this>")
    public MenuBar clearMenus() {
        menus.clear();
        rebuildHeaders();
        return this;
    }

        @AiDoc(summary = "Returns the index of the currently opened dropdown menu, or -1 if closed.")
    @WikiMethod(summary = "<key:menu.bar.method.openedMenuIndex>")
    @WikiReturn("<key:menu.bar.return.index>")
    public int openedMenuIndex() {
        syncOpenedMenuState();
        return openedMenuIndex;
    }

        @AiDoc(summary = "Returns the active dropdown ContextMenu popup widget, or null if closed.")
    @WikiMethod(summary = "<key:menu.bar.method.openedPopup>")
    @WikiReturn("<key:menu.bar.return.popup>")
    public ContextMenu openedPopup() {
        syncOpenedMenuState();
        return openedPopup(true);
    }

        @AiDoc(summary = "Opens the dropdown menu at the specified index.")
    @WikiMethod(summary = "<key:menu.bar.method.openMenu>")
    @WikiReturn("<key:menu.bar.return.this>")
    public MenuBar openMenu(
            @WikiParameter(description = "<key:menu.bar.param.index>")
            int index
    ) {
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

        @AiDoc(summary = "Closes the active dropdown menu.")
    @WikiMethod(summary = "<key:menu.bar.method.closeOpenMenu>")
    @WikiReturn("<key:menu.bar.return.this>")
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
