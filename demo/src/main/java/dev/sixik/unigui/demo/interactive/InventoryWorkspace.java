package dev.sixik.unigui.demo.interactive;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.SliceBorders;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.PanelWidget;

import java.util.ArrayList;
import java.util.List;

/** Full-screen composition root that coordinates pointer and stack operations across inventories. */
public final class InventoryWorkspace extends PanelWidget {
    private static final MutableColor BACKGROUND = MutableColor.rgba(0.035f, 0.045f, 0.060f, 0.56f);
    private static final MutableColor TEXTURE_TINT = MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f);
    private final List<InventoryWidget> inventories = new ArrayList<>();
    private final List<MutableRect> inventoryPositions = new ArrayList<>();
    private InventoryItemWidget carried;
    private InventorySlotWidget hoveredSlot;
    private InventorySlotWidget pressedSlot;
    private int activePointerId = -1;
    private boolean pressStartedCarrying;
    private float cursorX;
    private float cursorY;
    private boolean centerInventories;
    private TextureHandle backgroundTexture;

    /** Adds and positions an inventory panel; all panels in this workspace share one cursor and controller. */
    public InventoryWorkspace inventory(InventoryWidget inventory, float x, float y) {
        if (inventory == null) throw new NullPointerException("inventory");
        if (inventories.contains(inventory)) throw new IllegalArgumentException("Inventory already attached: " + inventory.id());
        inventories.add(inventory);
        inventoryPositions.add(new MutableRect(x, y, inventory.inventoryWidth(), inventory.inventoryHeight()));
        inventory.workspace(this);
        inventory.layout(style -> style.size(inventory.inventoryWidth(), inventory.inventoryHeight())
                .position(PositionType.ABSOLUTE).left(x).top(y));
        addChild(inventory);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /** Centers the complete inventory group in the workspace while preserving its relative layout. */
    public InventoryWorkspace centerInventories(boolean center) {
        centerInventories = center;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /** Sets the shared background texture drawn behind all attached inventories. */
    public InventoryWorkspace backgroundTexture(TextureHandle texture) {
        backgroundTexture = texture;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /** Returns the widget currently carried by the pointer, if any. */
    public InventoryItemWidget carriedItem() { return carried; }

    /** Applies a click operation directly, useful for game integration and tests. */
    public void clickSlot(InventorySlotWidget slot, PointerButton button, boolean shift) {
        if (slot == null || slot.inventory() == null || !inventories.contains(slot.inventory())) return;
        if (shift) quickMove(slot);
        else if (button == PointerButton.PRIMARY) leftClick(slot);
        else if (button == PointerButton.SECONDARY) rightClick(slot);
        invalidate(InvalidationFlags.VISUAL | InvalidationFlags.LAYOUT);
    }

    void press(InventorySlotWidget slot, PointerPressedEvent pointer) {
        if (slot == null || !inventories.contains(slot.inventory())) return;
        UIContext context = uiContext();
        if (context != null) {
            context.focusManager().requestFocus(this);
            context.capturePointer(pointer.pointerId(), this);
        }
        activePointerId = pointer.pointerId();
        pressedSlot = slot;
        pressStartedCarrying = carried != null;
        cursorX = pointer.rootX() - layoutBounds().x();
        cursorY = pointer.rootY() - layoutBounds().y();
        boolean shift = context != null && (context.keyboard().isDown(KeyCodes.LEFT_SHIFT)
                || context.keyboard().isDown(KeyCodes.RIGHT_SHIFT));
        clickSlot(slot, pointer.button(), shift);
    }

    @Override
    public void measure(LayoutContext context) {
        applyQueuedMutations();
        for (Widget child : children()) child.measure(context);
        float width = Float.isFinite(context.availableWidth()) ? context.availableWidth() : contentWidth();
        float height = Float.isFinite(context.availableHeight()) ? context.availableHeight() : contentHeight();
        setDesiredSize(resolveDesiredSize(context, width, height));
    }

    @Override
    public void arrange(dev.sixik.unigui.api.math.RectView bounds) {
        if (centerInventories && !inventories.isEmpty()) {
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            for (MutableRect position : inventoryPositions) {
                minX = Math.min(minX, position.x());
                minY = Math.min(minY, position.y());
                maxX = Math.max(maxX, position.x() + position.width());
                maxY = Math.max(maxY, position.y() + position.height());
            }
            float offsetX = (bounds.width() - (maxX - minX)) * 0.5f - minX;
            float offsetY = (bounds.height() - (maxY - minY)) * 0.5f - minY;
            for (int i = 0; i < inventories.size(); i++) {
                MutableRect position = inventoryPositions.get(i);
                InventoryWidget inventory = inventories.get(i);
                float x = position.x() + offsetX;
                float y = position.y() + offsetY;
                inventory.layout(style -> style.left(x).top(y));
            }
        }
        super.arrange(bounds);
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.CAPTURE) {
            updatePointer(pointer.rootX(), pointer.rootY());
            return;
        }
        if (event instanceof PointerMovedEvent pointer) {
            updatePointer(pointer.rootX(), pointer.rootY());
        } else if (event instanceof PointerReleasedEvent pointer && pointer.pointerId() == activePointerId) {
            updatePointer(pointer.rootX(), pointer.rootY());
            InventorySlotWidget destination = findSlot(pointer.rootX(), pointer.rootY());
            if (!pressStartedCarrying && pointer.button() == PointerButton.PRIMARY && carried != null
                    && destination != null && destination != pressedSlot) {
                clickSlot(destination, PointerButton.PRIMARY, false);
            }
            UIContext context = uiContext();
            if (context != null) context.releasePointer(pointer.pointerId(), this);
            activePointerId = -1;
            pressedSlot = null;
            pressStartedCarrying = false;
            event.cancel();
        } else if (event instanceof KeyPressedEvent key && key.phase() != EventPhase.CAPTURE) {
            if (key.keyCode() == KeyCodes.Q && carried != null) {
                int count = carried.stack().count();
                if ((key.modifiers() & 2) != 0 || count == 1) clearCarried();
                else carried.stack(carried.stack().withCount(count - 1));
                event.cancel();
                invalidate(InvalidationFlags.VISUAL);
            }
        }
    }

    @Override
    public void render(RenderContext context) {
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        draw.addRectFilled(layoutBounds().x(), layoutBounds().y(),
                layoutBounds().width(), layoutBounds().height(), BACKGROUND);
        if (backgroundTexture != null && !inventories.isEmpty()) {
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            for (InventoryWidget inventory : inventories) {
                minX = Math.min(minX, inventory.layoutBounds().x());
                minY = Math.min(minY, inventory.layoutBounds().y());
                maxX = Math.max(maxX, inventory.layoutBounds().x() + inventory.inventoryWidth());
                maxY = Math.max(maxY, inventory.layoutBounds().y() + inventory.inventoryHeight());
            }
            float padding = 8.0f;
            draw.nineSlice(SliceBorders.symmetric(4.0f, 4.0f).slice(backgroundTexture),
                    minX - padding, minY - padding, maxX - minX + padding * 2.0f,
                    maxY - minY + padding * 2.0f, Paint.fill(TEXTURE_TINT));
        }
        renderChildren(context);
    }

    private void updatePointer(float rootX, float rootY) {
        float nextX = rootX - layoutBounds().x();
        float nextY = rootY - layoutBounds().y();
        boolean moved = cursorX != nextX || cursorY != nextY;
        cursorX = nextX;
        cursorY = nextY;
        InventorySlotWidget nextHovered = findSlot(rootX, rootY);
        if (hoveredSlot != nextHovered) {
            if (hoveredSlot != null) hoveredSlot.hovered(false);
            hoveredSlot = nextHovered;
            if (hoveredSlot != null) hoveredSlot.hovered(true);
        }
        if (carried != null && moved) {
            placeCarriedAtCursor();
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }
    }

    private InventorySlotWidget findSlot(float rootX, float rootY) {
        for (int inventoryIndex = inventories.size() - 1; inventoryIndex >= 0; inventoryIndex--) {
            InventoryWidget inventory = inventories.get(inventoryIndex);
            List<InventorySlotWidget> slots = inventory.slots();
            for (int slotIndex = slots.size() - 1; slotIndex >= 0; slotIndex--) {
                InventorySlotWidget slot = slots.get(slotIndex);
                dev.sixik.unigui.api.math.RectView bounds = slot.layoutBounds();
                if (rootX >= bounds.x() && rootX < bounds.x() + bounds.width()
                        && rootY >= bounds.y() && rootY < bounds.y() + bounds.height()) return slot;
            }
        }
        return null;
    }

    private void leftClick(InventorySlotWidget slot) {
        if (carried != null && !slot.accepts(carried)) return;
        InventoryItemWidget target = slot.itemWidget();
        if (carried == null) {
            if (target != null) takeWhole(slot, target);
        } else if (target == null) {
            placeWhole(slot, carried);
        } else if (target.stack().sameItem(carried.stack())
                && target.stack().count() < target.stack().maxCount()) {
            merge(slot, target, carried, carried.stack().count());
        } else {
            InventoryItemWidget incoming = carried;
            InventoryStack oldTarget = target.stack();
            slot.setItemWidget(null);
            slot.inventory().slotChanged(slot, oldTarget, null);
            detachCarried();
            slot.setItemWidget(incoming);
            slot.inventory().slotChanged(slot, null, incoming.stack());
            carried = target;
            carried.presentation(InventoryItemFactory.Presentation.TAKEN);
            addCarriedToWorkspace();
        }
    }

    private void rightClick(InventorySlotWidget slot) {
        InventoryItemWidget target = slot.itemWidget();
        if (carried == null) {
            if (target == null) return;
            int takeCount = (target.stack().count() + 1) / 2;
            if (takeCount == target.stack().count()) {
                takeWhole(slot, target);
            } else {
                InventoryStack oldStack = target.stack();
                target.stack(oldStack.withCount(oldStack.count() - takeCount));
                slot.inventory().slotChanged(slot, oldStack, target.stack());
                carried = target.copyWithCount(takeCount);
                carried.presentation(InventoryItemFactory.Presentation.TAKEN);
                addCarriedToWorkspace();
            }
        } else if (target == null) {
            if (!slot.accepts(carried)) return;
            InventoryItemWidget placed = carried.copyWithCount(1);
            slot.setItemWidget(placed);
            slot.inventory().slotChanged(slot, null, placed.stack());
            decrementCarried(1);
        } else if (target.stack().sameItem(carried.stack())
                && target.stack().count() < target.stack().maxCount()) {
            InventoryStack oldTarget = target.stack();
            target.stack(oldTarget.withCount(oldTarget.count() + 1));
            slot.inventory().slotChanged(slot, oldTarget, target.stack());
            decrementCarried(1);
        }
    }

    private void takeWhole(InventorySlotWidget slot, InventoryItemWidget item) {
        InventoryStack oldStack = item.stack();
        slot.setItemWidget(null);
        slot.inventory().slotChanged(slot, oldStack, null);
        carried = item;
        carried.presentation(InventoryItemFactory.Presentation.TAKEN);
        addCarriedToWorkspace();
    }

    private void placeWhole(InventorySlotWidget slot, InventoryItemWidget item) {
        if (!slot.accepts(item)) return;
        detachCarried();
        slot.setItemWidget(item);
        slot.inventory().slotChanged(slot, null, item.stack());
    }

    private void merge(InventorySlotWidget slot, InventoryItemWidget target,
                       InventoryItemWidget source, int requested) {
        if (!slot.accepts(source)) return;
        InventoryStack oldTarget = target.stack();
        InventoryStack oldSource = source.stack();
        int moved = Math.min(requested, oldTarget.maxCount() - oldTarget.count());
        if (moved <= 0) return;
        target.stack(oldTarget.withCount(oldTarget.count() + moved));
        slot.inventory().slotChanged(slot, oldTarget, target.stack());
        if (moved == oldSource.count()) clearCarried();
        else source.stack(oldSource.withCount(oldSource.count() - moved));
    }

    private void decrementCarried(int amount) {
        int remaining = carried.stack().count() - amount;
        if (remaining <= 0) clearCarried();
        else carried.stack(carried.stack().withCount(remaining));
    }

    private void clearCarried() {
        detachCarried();
    }

    private void detachCarried() {
        if (carried != null) {
            removeChild(carried);
            applyQueuedMutations();
            carried = null;
        }
    }

    private void addCarriedToWorkspace() {
        if (carried == null) return;
        carried.layout(style -> style.size(32.0f, 32.0f).position(PositionType.ABSOLUTE)
                .left(cursorX + 8.0f).top(cursorY + 8.0f));
        addChild(carried);
        applyQueuedMutations();
    }

    private void placeCarriedAtCursor() {
        if (carried == null) return;
        carried.layout(style -> style.size(32.0f, 32.0f).position(PositionType.ABSOLUTE)
                .left(cursorX + 8.0f).top(cursorY + 8.0f));
    }

    private void quickMove(InventorySlotWidget sourceSlot) {
        InventoryItemWidget sourceItem = sourceSlot.itemWidget();
        if (sourceItem == null) return;
        List<InventorySlotWidget> destinations = new ArrayList<>();
        for (InventoryWidget inventory : inventories) {
            if (inventory != sourceSlot.inventory()) destinations.addAll(inventory.quickMoveSlots());
        }
        for (InventorySlotWidget slot : sourceSlot.inventory().quickMoveSlots()) {
            if (slot != sourceSlot) destinations.add(slot);
        }

        for (InventorySlotWidget target : destinations) {
            InventoryItemWidget targetItem = target.itemWidget();
            if (targetItem == null || !targetItem.stack().sameItem(sourceItem.stack())
                    || !target.accepts(sourceItem)) continue;
            int capacity = targetItem.stack().maxCount() - targetItem.stack().count();
            transfer(sourceSlot, target, Math.min(capacity, sourceItem.stack().count()));
            if (sourceSlot.itemWidget() == null) return;
            sourceItem = sourceSlot.itemWidget();
        }
        for (InventorySlotWidget target : destinations) {
            if (target.itemWidget() != null || !target.accepts(sourceItem)) continue;
            int moved = Math.min(sourceItem.stack().count(), sourceItem.stack().maxCount());
            transfer(sourceSlot, target, moved);
            if (sourceSlot.itemWidget() == null) return;
            sourceItem = sourceSlot.itemWidget();
        }
    }

    private void transfer(InventorySlotWidget sourceSlot, InventorySlotWidget targetSlot, int moved) {
        if (moved <= 0) return;
        InventoryItemWidget source = sourceSlot.itemWidget();
        InventoryItemWidget target = targetSlot.itemWidget();
        InventoryStack oldSource = source.stack();
        InventoryStack oldTarget = target == null ? null : target.stack();
        if (target == null && moved == oldSource.count()) {
            sourceSlot.setItemWidget(null);
            sourceSlot.inventory().slotChanged(sourceSlot, oldSource, null);
            targetSlot.setItemWidget(source);
            targetSlot.inventory().slotChanged(targetSlot, null, oldSource);
        } else if (target == null) {
            InventoryItemWidget split = source.copyWithCount(moved);
            targetSlot.setItemWidget(split);
            targetSlot.inventory().slotChanged(targetSlot, null, split.stack());
            source.stack(oldSource.withCount(oldSource.count() - moved));
            sourceSlot.inventory().slotChanged(sourceSlot, oldSource, source.stack());
        } else {
            target.stack(oldTarget.withCount(oldTarget.count() + moved));
            targetSlot.inventory().slotChanged(targetSlot, oldTarget, target.stack());
            if (moved == oldSource.count()) {
                sourceSlot.setItemWidget(null);
                sourceSlot.inventory().slotChanged(sourceSlot, oldSource, null);
            } else {
                source.stack(oldSource.withCount(oldSource.count() - moved));
                sourceSlot.inventory().slotChanged(sourceSlot, oldSource, source.stack());
            }
        }
    }

    private float contentWidth() {
        float extent = 0.0f;
        for (InventoryWidget inventory : inventories) extent = Math.max(extent,
                inventory.layoutBounds().x() + inventory.inventoryWidth());
        return extent;
    }

    private float contentHeight() {
        float extent = 0.0f;
        for (InventoryWidget inventory : inventories) extent = Math.max(extent,
                inventory.layoutBounds().y() + inventory.inventoryHeight());
        return extent;
    }
}
