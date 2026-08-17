package dev.sixik.unigui.widgets.feedback;

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
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Separator;
import dev.sixik.unigui.widgets.interaction.Button;

@XmlWidgetName("ContextMenu")
public final class ContextMenu extends Box implements OverlayHostAware {
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

    public ContextMenu() {
        backgroundVisible(true);
        borderVisible(true);
        radius(3.0f);
        background().set(0.025f, 0.030f, 0.040f, 0.98f);
        borderColor().set(0.25f, 0.78f, 1.0f, 0.85f);
        layout(style -> style.position(PositionType.ABSOLUTE).overflow(Overflow.VISIBLE));
        visible(false);
        focusable(true);
        itemsHost.spacing(1.0f);
        addChild(itemsHost);
        applyQueuedMutations();
    }

    public ContextMenu item(String text) {
        return item(RichText.plain(text), null);
    }

    public ContextMenu item(RichText text) {
        return item(text, null);
    }

    /** Prefer {@link #onItemSelected(EventListener)} for public UI actions. */
    public ContextMenu item(String text, Runnable action) {
        return item(RichText.plain(text), action);
    }

    /** Prefer {@link #onItemSelected(EventListener)} for public UI actions. */
    public ContextMenu item(RichText text, Runnable action) {
        RichText normalizedText = text == null ? RichText.plain("") : text;
        Button button = new Button(normalizedText);
        if (action != null) {
            button.onClick(event -> action.run());
        }
        return item(button);
    }

    public ContextMenu item(Button button) {
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

    public ContextMenu submenu(String text, ContextMenu submenu) {
        String label = text == null ? "" : text;
        return submenu(RichText.plain(label + " ▶"), submenu);
    }

    public ContextMenu submenu(RichText text, ContextMenu submenu) {
        if (submenu == null) {
            return item(text);
        }
        RichText normalizedText = text == null ? RichText.plain("▶") : text;
        int itemIndex = itemButtons.size();
        Button button = new Button(normalizedText);
        button.layout(style -> style.size(132.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
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

    public ContextMenu separator() {
        return separator(new Separator());
    }

    public ContextMenu separator(Separator separator) {
        Separator normalized = separator == null ? new Separator() : separator;
        normalized.layout(style -> style.size(132.0f, 1.0f).margin(2.0f, 3.0f).flexGrow(0).flexShrink(0.0f));
        itemsHost.addChild(normalized);
        itemsHost.applyQueuedMutations();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public int itemCount() {
        return itemButtons.size();
    }

    public Button itemButton(int index) {
        if (index < 0 || index >= itemButtons.size()) {
            throw new IndexOutOfBoundsException("Context menu item index out of range: " + index);
        }
        return itemButtons.get(index);
    }

    public int selectedItemIndex() {
        return selectedItemIndex;
    }

    @XmlAttribute(value = "selectedItem", category = "Behavior", defaultValue = "0", description = "Initially selected menu item index.")
    public ContextMenu selectItem(int index) {
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

    public boolean opened() {
        return open;
    }

    public ContextMenu openAt(float x, float y) {
        this.x = Float.isFinite(x) ? x : 0.0f;
        this.y = Float.isFinite(y) ? y : 0.0f;
        return open(true);
    }

    public ContextMenu open() {
        return open(true);
    }

    public ContextMenu close() {
        return open(false);
    }

    public ContextMenu toggle(float x, float y) {
        return open ? close() : openAt(x, y);
    }

    @XmlAttribute(value = "open", category = "Behavior", defaultValue = "false", description = "Whether the context menu is visible.")
    public ContextMenu open(boolean open) {
        if (this.open == open) return this;
        this.open = open;
        visible(open);
        if (!open) {
            closeSubmenu();
        }
        if (open && selectedItemIndex < 0 && !itemButtons.isEmpty()) {
            selectedItemIndex = 0;
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean closeOnOutsideClick() {
        return closeOnOutsideClick;
    }

    @XmlAttribute(value = "closeOnOutsideClick", category = "Behavior", defaultValue = "true", description = "Whether outside pointer presses close the menu.")
    public ContextMenu closeOnOutsideClick(boolean closeOnOutsideClick) {
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
    public ContextMenu padding(EdgeInsets padding) {
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
    public void arrangeInHost(RectView hostBounds) {
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
