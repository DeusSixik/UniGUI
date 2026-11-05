package dev.sixik.unigui.demo.interactive.inventory;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.display.Label;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Builder for item-stack widgets and their separate in-slot/taken visuals.
 */
public final class InventoryItemFactory {
    private final InventoryStack stack;
    private InventoryItemVisual inSlotVisual;
    private InventoryItemVisual takenVisual;
    private Supplier<? extends Widget> sharedContentFactory;
    private Supplier<? extends Widget> inSlotContentFactory;
    private Supplier<? extends Widget> takenContentFactory;
    private boolean drawDefaultVisual = true;

    private InventoryItemFactory(InventoryStack stack) {
        this.stack = Objects.requireNonNull(stack, "stack");
    }

    /**
     * Starts creating a widget for the given immutable item stack.
     */
    public static InventoryItemFactory of(InventoryStack stack) {
        return new InventoryItemFactory(stack);
    }

    /**
     * Starts an item with stack identity/count while its visual is supplied as a child widget.
     */
    public static InventoryItemFactory of(String itemId, int count, int maxCount) {
        return of(new InventoryStack(itemId, count, maxCount));
    }

    /**
     * Sets the renderer used while this item is inside a slot.
     */
    public InventoryItemFactory inSlot(InventoryItemVisual visual) {
        inSlotVisual = visual;
        return this;
    }

    /**
     * Sets the renderer used while this item is carried by the pointer.
     */
    public InventoryItemFactory taken(InventoryItemVisual visual) {
        takenVisual = visual;
        return this;
    }

    /**
     * Uses a widget factory as the item's shared content, replacing the default color-and-label visual.
     */
    public InventoryItemFactory content(Supplier<? extends Widget> contentFactory) {
        sharedContentFactory = Objects.requireNonNull(contentFactory, "contentFactory");
        drawDefaultVisual = false;
        return this;
    }

    /**
     * Uses a shared widget content while retaining the default background visual.
     */
    public InventoryItemFactory overlay(Supplier<? extends Widget> contentFactory) {
        sharedContentFactory = Objects.requireNonNull(contentFactory, "contentFactory");
        return this;
    }

    /**
     * Provides separate child widgets for in-slot and cursor-taken presentations.
     */
    public InventoryItemFactory content(InventoryItemFactory.Presentation state,
                                        Supplier<? extends Widget> contentFactory) {
        Objects.requireNonNull(contentFactory, "contentFactory");
        if (state == InventoryItemFactory.Presentation.TAKEN) takenContentFactory = contentFactory;
        else inSlotContentFactory = contentFactory;
        return this;
    }

    /**
     * Uses a Label widget as the item's visual content.
     */
    public InventoryItemFactory text(String text) {
        return content(() -> new Label(text == null ? "" : text));
    }

    /**
     * Creates the item widget.
     */
    public InventoryItemWidget build() {
        return new InventoryItemWidget(stack, inSlotVisual, takenVisual,
                sharedContentFactory, inSlotContentFactory, takenContentFactory, drawDefaultVisual);
    }

    /**
     * Presentation modes supported by item widgets.
     */
    public enum Presentation {
        IN_SLOT,
        TAKEN
    }
}
