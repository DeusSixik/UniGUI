package dev.sixik.unigui.widgets.feedback;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.ContextMenuItemSelectedEvent;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Separator;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.WidgetStyles;

@XmlWidgetName("ContextMenu")
@AiDoc(summary = "Hierarchical popup context menu supporting actions, submenus, separators, and keyboard navigation.")
@WikiPage(
        title = "ContextMenu",
        slug = "context-menu",
        category = "<key:feedback.category>",
        summary = "<key:context.menu.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:context.menu.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:context.menu.example.basic.title>",
        code = "ContextMenu menu = new ContextMenu();\nmenu.item(\"Copy\", () -> copy()).separator().item(\"Paste\", () -> paste());\nmenu.openAt(mouseX, mouseY);"
)
@WikiLink(
        label = "Popup",
        target = "page:popup",
        description = "<key:popup.summary>"
)
@WikiLink(
        label = "OverlayLayer",
        target = "page:overlay-layer",
        description = "<key:overlay.layer.summary>"
)
public final class ContextMenu extends Box implements OverlayHostAware {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.CONTEXT_MENU;

    private final VBox itemsHost = new VBox();
    private final List<Button> itemButtons = new ObjectArrayList<>();
    private final List<ContextMenu> itemSubmenus = new ObjectArrayList<>();
    private ContextMenu parentMenu;
    private ContextMenu openSubmenu;
    private boolean open;
    private boolean closeOnOutsideClick = true;
    private float x;
    private float y;
    private EdgeInsets padding = EdgeInsets.all(3.0f);
    private int selectedItemIndex = -1;
    private int pendingSelectedItemIndex = -1;

        @AiDoc(summary = "Constructs an empty ContextMenu.")
    @WikiMethod(summary = "<key:context.menu.method.ctorDefault>")
    public ContextMenu() {
        applyComponentStyleDefaults(WidgetStyles.ContextMenu.DEFAULT);
        layout(style -> style.position(PositionType.ABSOLUTE).overflow(Overflow.VISIBLE));
        visible(false);
        focusable(true);
        itemsHost.spacing(1.0f);
        addChild(itemsHost);
        applyQueuedMutations();
    }

    /**
     * Возвращает стандартный стиль поверхности контекстного меню, уступающий стилям темы и экземпляра.
     *
     * @return стандартные значения оформления контекстного меню
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.ContextMenu.DEFAULT;
    }

        @AiDoc(summary = "Adds a menu item with plain text.")
    @WikiMethod(summary = "<key:context.menu.method.itemText>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu item(
            @WikiParameter(description = "<key:context.menu.param.text>")
            String text
    ) {
        return item(RichText.resolve(text), null);
    }

        @AiDoc(summary = "Adds a menu item with RichText.")
    @WikiMethod(summary = "<key:context.menu.method.itemRich>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu item(
            @WikiParameter(description = "<key:context.menu.param.richText>")
            RichText text
    ) {
        return item(text, null);
    }

    /** Prefer {@link #onItemSelected(EventListener)} for public UI actions. */
        @AiDoc(summary = "Adds a menu item with plain text and click callback.")
    @WikiMethod(summary = "<key:context.menu.method.itemTextAction>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu item(
            @WikiParameter(description = "<key:context.menu.param.text>")
            String text,
            @WikiParameter(description = "<key:context.menu.param.action>")
            Runnable action
    ) {
        return item(RichText.resolve(text), action);
    }

    /** Prefer {@link #onItemSelected(EventListener)} for public UI actions. */
        @AiDoc(summary = "Adds a menu item with RichText and click callback.")
    @WikiMethod(summary = "<key:context.menu.method.itemRichAction>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu item(
            @WikiParameter(description = "<key:context.menu.param.richText>")
            RichText text,
            @WikiParameter(description = "<key:context.menu.param.action>")
            Runnable action
    ) {
        RichText normalizedText = text == null ? RichText.plain("") : text;
        Button button = new Button(normalizedText);
        if (action != null) {
            button.onClick(event -> action.run());
        }
        return item(button);
    }

        @AiDoc(summary = "Adds a preconfigured Button widget as a menu item.")
    @WikiMethod(summary = "<key:context.menu.method.itemButton>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu item(
            @WikiParameter(description = "<key:context.menu.param.button>")
            Button button
    ) {
        if (button == null) return this;
        int itemIndex = itemButtons.size();
        button.layout(style -> style.size(132.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        button.onClick(event -> {
            selectItem(itemIndex);
            emitItemSelected(itemIndex);
            closeMenuChain();
        });
        button.on(PointerEnteredEvent.TYPE, event -> {
            if (event.phase() == EventPhase.TARGET) {
                selectItem(itemIndex);
            }
        });
        itemButtons.add(button);
        itemSubmenus.add(null);
        itemsHost.addChild(button);
        itemsHost.applyQueuedMutations();
        syncSelectionAfterItemAdded();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Adds a cascading submenu item with plain text.")
    @WikiMethod(summary = "<key:context.menu.method.submenuText>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu submenu(
            @WikiParameter(description = "<key:context.menu.param.text>")
            String text,
            @WikiParameter(description = "<key:context.menu.param.submenu>")
            ContextMenu submenu
    ) {
        String label = text == null ? "" : text;
        return submenu(RichText.plain(label + " ▶"), submenu);
    }

        @AiDoc(summary = "Adds a cascading submenu item with RichText.")
    @WikiMethod(summary = "<key:context.menu.method.submenuRich>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu submenu(
            @WikiParameter(description = "<key:context.menu.param.richText>")
            RichText text,
            @WikiParameter(description = "<key:context.menu.param.submenu>")
            ContextMenu submenu
    ) {
        if (submenu == null) {
            return item(text);
        }
        RichText normalizedText = text == null ? RichText.plain("▶") : text;
        Button button = new Button(normalizedText);
        button.layout(style -> style.size(132.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        return submenu(button, submenu);
    }

        @AiDoc(summary = "Adds a cascading submenu item with a custom button.")
    @WikiMethod(summary = "<key:context.menu.method.submenuButton>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu submenu(
            @WikiParameter(description = "<key:context.menu.param.button>")
            Button button,
            @WikiParameter(description = "<key:context.menu.param.submenu>")
            ContextMenu submenu
    ) {
        if (button == null) return this;
        if (submenu == null) return item(button);
        int itemIndex = itemButtons.size();
        button.onClick(event -> {
            selectItem(itemIndex);
            openSubmenu(itemIndex);
        });
        button.on(PointerEnteredEvent.TYPE, event -> {
            if (event.phase() == EventPhase.TARGET) {
                selectItem(itemIndex);
                openSubmenu(itemIndex);
            }
        });
        itemButtons.add(button);
        itemSubmenus.add(submenu);
        submenu.parentMenu = this;
        submenu.closeOnOutsideClick(closeOnOutsideClick);
        submenu.close();
        itemsHost.addChild(button);
        itemsHost.applyQueuedMutations();
        syncSelectionAfterItemAdded();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Adds a visual divider separator line.")
    @WikiMethod(summary = "<key:context.menu.method.separatorDefault>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu separator() {
        return separator(new Separator());
    }

        @AiDoc(summary = "Adds a custom Separator widget.")
    @WikiMethod(summary = "<key:context.menu.method.separatorCustom>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu separator(
            @WikiParameter(description = "<key:context.menu.param.separator>")
            Separator separator
    ) {
        Separator normalized = separator == null ? new Separator() : separator;
        normalized.layout(style -> style.size(132.0f, 1.0f).margin(2.0f, 3.0f).flexGrow(0).flexShrink(0.0f));
        itemsHost.addChild(normalized);
        itemsHost.applyQueuedMutations();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the total number of menu items.")
    @WikiMethod(summary = "<key:context.menu.method.itemCount>")
    @WikiReturn("<key:context.menu.return.itemCount>")
    public int itemCount() {
        return itemButtons.size();
    }

        @AiDoc(summary = "Returns the button widget for the item at the specified index.")
    @WikiMethod(summary = "<key:context.menu.method.itemButtonGet>")
    @WikiReturn("<key:context.menu.return.button>")
    public Button itemButton(
            @WikiParameter(description = "<key:context.menu.param.index>")
            int index
    ) {
        if (index < 0 || index >= itemButtons.size()) {
            throw new IndexOutOfBoundsException("Context menu item index out of range: " + index);
        }
        return itemButtons.get(index);
    }

        @AiDoc(summary = "Returns the index of the currently hovered or keyboard-selected item.")
    @WikiMethod(summary = "<key:context.menu.method.selectedItemIndex>")
    @WikiReturn("<key:context.menu.return.index>")
    public int selectedItemIndex() {
        return selectedItemIndex;
    }

    @XmlAttribute(value = "selectedItem", category = "Behavior", defaultValue = "0", description = "Initially selected menu item index.")
        @AiDoc(summary = "Selects the item at the specified index.")
    @WikiMethod(summary = "<key:context.menu.method.selectItem>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu selectItem(
            @WikiParameter(description = "<key:context.menu.param.index>")
            int index
    ) {
        if (itemButtons.isEmpty()) {
            selectedItemIndex = -1;
            pendingSelectedItemIndex = Math.max(0, index);
            return this;
        }
        pendingSelectedItemIndex = -1;
        int normalized = Math.max(0, Math.min(index, itemButtons.size() - 1));
        selectedItemIndex = normalized;
        if (submenuAt(normalized) == null) {
            closeSubmenu();
        }
        if (uiContext() != null) {
            uiContext().focusManager().requestFocus(itemButtons.get(normalized));
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    private void syncSelectionAfterItemAdded() {
        if (pendingSelectedItemIndex >= 0 && itemButtons.size() > pendingSelectedItemIndex) {
            int target = pendingSelectedItemIndex;
            pendingSelectedItemIndex = -1;
            selectItem(target);
        } else if (selectedItemIndex < 0) {
            selectedItemIndex = 0;
        }
    }

    public EventSubscription onItemSelected(EventListener<? super ContextMenuItemSelectedEvent> listener) {
        return on(ContextMenuItemSelectedEvent.TYPE, listener);
    }

        @AiDoc(summary = "Returns true if this menu is open.")
    @WikiMethod(summary = "<key:context.menu.method.opened>")
    @WikiReturn("<key:context.menu.return.opened>")
    public boolean opened() {
        return open;
    }

        @AiDoc(summary = "Opens the menu positioned at the specified root coordinates.")
    @WikiMethod(summary = "<key:context.menu.method.openAt>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu openAt(
            @WikiParameter(description = "<key:context.menu.param.x>")
            float x,
            @WikiParameter(description = "<key:context.menu.param.y>")
            float y
    ) {
        this.x = Float.isFinite(x) ? x : 0.0f;
        this.y = Float.isFinite(y) ? y : 0.0f;
        return open(true);
    }

        @AiDoc(summary = "Opens the menu at its last configured position.")
    @WikiMethod(summary = "<key:context.menu.method.open>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu open() {
        return open(true);
    }

        @AiDoc(summary = "Closes the menu and any active submenus.")
    @WikiMethod(summary = "<key:context.menu.method.close>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu close() {
        return open(false);
    }

        @AiDoc(summary = "Toggles open/closed state at the specified coordinates.")
    @WikiMethod(summary = "<key:context.menu.method.toggleAt>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu toggle(
            @WikiParameter(description = "<key:context.menu.param.x>")
            float x,
            @WikiParameter(description = "<key:context.menu.param.y>")
            float y
    ) {
        return open ? close() : openAt(x, y);
    }

    @XmlAttribute(value = "open", category = "Behavior", defaultValue = "false", description = "Whether the context menu is visible.")
        @AiDoc(summary = "Sets the open state.")
    @WikiMethod(summary = "<key:context.menu.method.openState>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu open(
            @WikiParameter(description = "<key:context.menu.param.open>")
            boolean open
    ) {
        if (this.open == open) return this;
        this.open = open;
        visible(open);
        if (!open) {
            closeSubmenu();
            if (parent() instanceof OverlayLayer layer) {
                layer.removeOverlay(this);
            }
        }
        if (open && selectedItemIndex < 0 && !itemButtons.isEmpty()) {
            selectedItemIndex = 0;
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns true if clicking outside automatically dismisses the menu.")
    @WikiMethod(summary = "<key:context.menu.method.closeOnOutsideClickGet>")
    @WikiReturn("<key:context.menu.return.closeOnOutsideClick>")
    public boolean closeOnOutsideClick() {
        return closeOnOutsideClick;
    }

    @XmlAttribute(value = "closeOnOutsideClick", category = "Behavior", defaultValue = "true", description = "Whether outside pointer presses close the menu.")
        @AiDoc(summary = "Sets whether clicking outside closes the menu.")
    @WikiMethod(summary = "<key:context.menu.method.closeOnOutsideClickSet>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu closeOnOutsideClick(
            @WikiParameter(description = "<key:context.menu.param.closeOnOutsideClick>")
            boolean closeOnOutsideClick
    ) {
        this.closeOnOutsideClick = closeOnOutsideClick;
        for (ContextMenu submenu : itemSubmenus) {
            if (submenu != null) {
                submenu.closeOnOutsideClick(closeOnOutsideClick);
            }
        }
        return this;
    }

    boolean isMenuInteractionTarget(Widget target) {
        if (containsMenuTreeTarget(target)) return true;
        ContextMenu parent = parentMenu;
        while (parent != null) {
            if (isDescendantOrSelf(target, parent)) return true;
            parent = parent.parentMenu;
        }
        return false;
    }

    private boolean containsMenuTreeTarget(Widget target) {
        if (isDescendantOrSelf(target, this)) return true;
        for (ContextMenu submenu : itemSubmenus) {
            if (submenu != null && submenu.containsMenuTreeTarget(target)) {
                return true;
            }
        }
        return false;
    }

    @XmlAttribute(value = "padding", category = "Layout", defaultValue = "3", description = "Inner padding around menu items.")
        @AiDoc(summary = "Sets interior padding around the menu items.")
    @WikiMethod(summary = "<key:context.menu.method.padding>")
    @WikiReturn("<key:context.menu.return.this>")
    public ContextMenu padding(
            @WikiParameter(description = "<key:context.menu.param.padding>")
            EdgeInsets padding
    ) {
        this.padding = padding == null ? EdgeInsets.all(0.0f) : padding;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (!open || !(event instanceof KeyPressedEvent key) || key.phase() != EventPhase.TARGET) return;
        boolean focusedHere = uiContext() != null
                && (uiContext().focusManager().isFocused(this)
                || (selectedItemIndex >= 0
                && selectedItemIndex < itemButtons.size()
                && uiContext().focusManager().isFocused(itemButtons.get(selectedItemIndex))));
        if (!focusedHere) return;

        if (key.keyCode() == KeyCodes.ESCAPE) {
            close();
            event.cancel();
        } else if (key.keyCode() == KeyCodes.DOWN) {
            moveSelection(1);
            event.cancel();
        } else if (key.keyCode() == KeyCodes.UP) {
            moveSelection(-1);
            event.cancel();
        } else if (key.keyCode() == KeyCodes.RIGHT) {
            if (openSelectedSubmenu()) {
                event.cancel();
            }
        } else if (key.keyCode() == KeyCodes.LEFT && parentMenu != null) {
            close();
            parentMenu.openSubmenu = null;
            parentMenu.focusSelectedItem();
            event.cancel();
        } else if (key.keyCode() == KeyCodes.ENTER
                || key.keyCode() == KeyCodes.KEYPAD_ENTER
                || key.keyCode() == KeyCodes.SPACE) {
            activateSelectedItem();
            event.cancel();
        }
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED || !open) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        itemsHost.measure(context);
        setDesiredSize(resolveDesiredSize(context,
                itemsHost.desiredSize().width() + padding.horizontal(),
                itemsHost.desiredSize().height() + padding.vertical()));
    }

    @Override
    public void arrange(RectView bounds) {
        arrangeInHost(bounds);
    }

    @Override
        @AiDoc(summary = "Arranges the menu within host boundaries, flipping placement if screen edges are exceeded.")
    @WikiMethod(summary = "<key:context.menu.method.arrangeInHost>")
    public void arrangeInHost(
            @WikiParameter(description = "<key:context.menu.param.hostBounds>")
            RectView hostBounds
    ) {
        if (!open || visibility() == Visibility.COLLAPSED) {
            mutableLayoutBounds().set(hostBounds.x(), hostBounds.y(), 0.0f, 0.0f);
            return;
        }
        float width = desiredSize().width();
        float height = desiredSize().height();
        float placedX = Math.max(hostBounds.x(), Math.min(x, hostBounds.x() + Math.max(0.0f, hostBounds.width() - width)));
        float placedY = Math.max(hostBounds.y(), Math.min(y, hostBounds.y() + Math.max(0.0f, hostBounds.height() - height)));
        mutableLayoutBounds().set(placedX, placedY, width, height);
        itemsHost.arrange(new MutableRect(
                placedX + padding.left(),
                placedY + padding.top(),
                Math.max(0.0f, width - padding.horizontal()),
                Math.max(0.0f, height - padding.vertical())));
    }

    private void moveSelection(int delta) {
        if (itemButtons.isEmpty()) return;
        int base = selectedItemIndex < 0 ? 0 : selectedItemIndex;
        selectItem((base + delta + itemButtons.size()) % itemButtons.size());
    }

    private void activateSelectedItem() {
        if (selectedItemIndex < 0 || selectedItemIndex >= itemButtons.size()) return;
        if (openSelectedSubmenu()) return;
        Button button = itemButtons.get(selectedItemIndex);
        if (button.enabled()) button.click();
    }

    private boolean openSelectedSubmenu() {
        return openSubmenu(selectedItemIndex);
    }

    private boolean openSubmenu(int index) {
        ContextMenu submenu = submenuAt(index);
        if (submenu == null || index < 0 || index >= itemButtons.size()) return false;
        if (openSubmenu != null && openSubmenu != submenu) {
            openSubmenu.close();
        }
        openSubmenu = submenu;
        submenu.parentMenu = this;
        submenu.closeOnOutsideClick(closeOnOutsideClick);
        ensureSubmenuOverlay(submenu);
        Button anchor = itemButtons.get(index);
        submenu.openAt(anchor.layoutBounds().x() + anchor.layoutBounds().width() + 4.0f,
                anchor.layoutBounds().y());
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return true;
    }

    private void closeSubmenu() {
        if (openSubmenu == null) return;
        ContextMenu submenu = openSubmenu;
        openSubmenu = null;
        submenu.close();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private ContextMenu submenuAt(int index) {
        return index >= 0 && index < itemSubmenus.size() ? itemSubmenus.get(index) : null;
    }

    private void closeMenuChain() {
        ContextMenu root = this;
        while (root.parentMenu != null) {
            root = root.parentMenu;
        }
        root.close();
    }

    private void focusSelectedItem() {
        if (uiContext() != null && selectedItemIndex >= 0 && selectedItemIndex < itemButtons.size()) {
            uiContext().focusManager().requestFocus(itemButtons.get(selectedItemIndex));
        }
    }

    private void ensureSubmenuOverlay(ContextMenu submenu) {
        OverlayLayer layer = overlayLayer();
        if (layer != null && submenu.parent() != layer) {
            layer.addOverlay(submenu);
        } else if (layer == null && submenu.parent() != this) {
            addChild(submenu);
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

    private static boolean isDescendantOrSelf(Widget widget, Widget ancestor) {
        Widget current = widget;
        while (current != null) {
            if (current == ancestor) return true;
            current = current.parent();
        }
        return false;
    }

    private void emitItemSelected(int index) {
        if (index < 0 || index >= itemButtons.size()) return;
        emit(new ContextMenuItemSelectedEvent(this, index, itemButtons.get(index).text()));
    }
}
