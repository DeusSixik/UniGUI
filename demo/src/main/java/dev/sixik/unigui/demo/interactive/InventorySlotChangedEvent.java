package dev.sixik.unigui.demo.interactive;

import dev.sixik.unigui.api.event.BaseEvent;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.event.RoutableWidgetEvent;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

/**
 * Emitted by an inventory whenever one of its slot stacks changes.
 */
public final class InventorySlotChangedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<InventorySlotChangedEvent> TYPE = EventType.create("inventory.slot.changed");

    private final InventoryWidget inventory;
    private final InventorySlotWidget slot;
    private final InventoryStack oldStack;
    private final InventoryStack newStack;
    private final Widget currentTarget;
    private final EventPhase phase;

    public InventorySlotChangedEvent(InventoryWidget inventory, InventorySlotWidget slot,
                                     InventoryStack oldStack, InventoryStack newStack) {
        this(inventory, slot, oldStack, newStack, inventory, EventPhase.TARGET);
    }

    private InventorySlotChangedEvent(InventoryWidget inventory, InventorySlotWidget slot,
                                      InventoryStack oldStack, InventoryStack newStack,
                                      Widget currentTarget, EventPhase phase) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.slot = Objects.requireNonNull(slot, "slot");
        this.oldStack = oldStack;
        this.newStack = newStack;
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
    }

    @Override
    public EventType<InventorySlotChangedEvent> type() {
        return TYPE;
    }

    @Override
    public Widget target() {
        return inventory;
    }

    @Override
    public Widget currentTarget() {
        return currentTarget;
    }

    @Override
    public EventPhase phase() {
        return phase;
    }

    public InventoryWidget inventory() {
        return inventory;
    }

    public InventorySlotWidget slot() {
        return slot;
    }

    public String slotId() {
        return slot.id();
    }

    public InventoryStack oldStack() {
        return oldStack;
    }

    public InventoryStack newStack() {
        return newStack;
    }

    @Override
    public InventorySlotChangedEvent routeTo(Widget currentTarget, EventPhase phase) {
        InventorySlotChangedEvent event = new InventorySlotChangedEvent(
                inventory, slot, oldStack, newStack, currentTarget, phase);
        if (isCancelled()) event.cancel();
        return event;
    }
}
