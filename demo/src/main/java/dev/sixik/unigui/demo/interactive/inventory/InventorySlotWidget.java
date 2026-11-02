package dev.sixik.unigui.demo.interactive.inventory;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.PanelWidget;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * A named inventory slot that may contain an item widget and arbitrary child widgets.
 */
public final class InventorySlotWidget extends PanelWidget {
    private static final MutableColor SLOT_FILL = MutableColor.rgba(0.12f, 0.12f, 0.12f, 0.96f);
    private static final MutableColor SLOT_BORDER = MutableColor.rgba(0.48f, 0.46f, 0.41f, 1.0f);
    private static final MutableColor SLOT_HOVER = MutableColor.rgba(0.96f, 0.84f, 0.53f, 1.0f);
    private static final MutableColor TEXTURE_TINT = MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f);

    private final String id;
    private final float slotSize;
    private final Predicate<InventoryItemWidget> accepts;
    private InventoryWidget inventory;
    private InventoryItemWidget item;
    private InventoryWorkspace workspace;
    private TextureHandle texture;
    private boolean hovered;

    InventorySlotWidget(String id, float slotSize, Predicate<InventoryItemWidget> accepts) {
        this.id = Objects.requireNonNull(id, "id");
        this.slotSize = Math.max(8.0f, slotSize);
        this.accepts = accepts == null ? item -> true : accepts;
        focusable(true);
    }

    /**
     * Returns this slot's stable inventory-local identifier.
     */
    public String id() {
        return id;
    }

    /**
     * Returns the inventory that owns this slot.
     */
    public InventoryWidget inventory() {
        return inventory;
    }

    /**
     * Returns the item widget currently occupying this slot.
     */
    public InventoryItemWidget itemWidget() {
        return item;
    }

    /**
     * Sets the background texture used by this slot.
     */
    public InventorySlotWidget texture(TextureHandle texture) {
        this.texture = texture;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    float slotSize() {
        return slotSize;
    }

    /**
     * Returns whether the candidate item is allowed in this slot.
     */
    public boolean accepts(InventoryItemWidget candidate) {
        return candidate != null && accepts.test(candidate);
    }

    /**
     * Adds a decorative or interactive child widget to this slot.
     */
    public InventorySlotWidget content(Widget child) {
        addChild(child);
        return this;
    }

    /**
     * Assigns an item widget and updates its in-slot presentation.
     */
    public InventorySlotWidget item(InventoryItemWidget item) {
        if (item != null && !accepts(item)) throw new IllegalArgumentException("Item is not accepted by slot " + id);
        InventoryStack oldStack = this.item == null ? null : this.item.stack();
        setItemWidget(item);
        InventoryStack newStack = item == null ? null : item.stack();
        if (inventory != null && !Objects.equals(oldStack, newStack)) inventory.slotChanged(this, oldStack, newStack);
        return this;
    }

    void owner(InventoryWidget inventory, InventoryWorkspace workspace) {
        this.inventory = inventory;
        this.workspace = workspace;
    }

    void hovered(boolean hovered) {
        if (this.hovered == hovered) return;
        this.hovered = hovered;
        invalidate(InvalidationFlags.VISUAL);
    }

    void setItemWidget(InventoryItemWidget next) {
        if (item == next) return;
        if (item != null) {
            removeChild(item);
            applyQueuedMutations();
        }
        item = next;
        if (next != null) {
            next.presentation(InventoryItemFactory.Presentation.IN_SLOT);
            next.layout(style -> style.position(dev.sixik.unigui.api.layout.PositionType.STATIC)
                    .left(0.0f).top(0.0f).sizePercent(100.0f, 100.0f).align(
                            dev.sixik.unigui.api.layout.Alignment.STRETCH,
                            dev.sixik.unigui.api.layout.Alignment.STRETCH));
            addChild(next);
            applyQueuedMutations();
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    InventoryWorkspace workspace() {
        return workspace;
    }

    @Override
    public void measure(LayoutContext context) {
        applyQueuedMutations();
        for (Widget child : children()) child.measure(new LayoutContext(slotSize, slotSize));
        setDesiredSize(resolveDesiredSize(context, slotSize, slotSize));
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled() || !(event instanceof PointerPressedEvent pointer)
                || pointer.phase() == EventPhase.CAPTURE) return;
        if (workspace != null) {
            workspace.press(this, pointer);
            event.cancel();
        }
    }

    @Override
    public void render(RenderContext context) {
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        if (texture != null) {
            draw.texture(texture, x, y, width, height, Paint.fill(TEXTURE_TINT));
            if (hovered) draw.rect(x, y, width, height, Paint.stroke(SLOT_HOVER, 2.0f));
        } else {
            draw.addRectFilled(x, y, width, height, SLOT_FILL);
            draw.rect(x, y, width, height, Paint.stroke(hovered ? SLOT_HOVER : SLOT_BORDER, hovered ? 2.0f : 1.0f));
        }
        renderChildren(context);
    }
}
