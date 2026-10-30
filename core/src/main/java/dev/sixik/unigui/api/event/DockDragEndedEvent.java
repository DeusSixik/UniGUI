package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.docking.DockDropIntent;

import java.util.Objects;

public final class DockDragEndedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<DockDragEndedEvent> TYPE = EventType.create("dock.drag_ended");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String paneId;
    private final float rootX;
    private final float rootY;
    private final DockDropIntent intent;
    private final boolean dropped;

    public DockDragEndedEvent(Widget target, String paneId, float rootX, float rootY,
                              DockDropIntent intent, boolean dropped) {
        this(target, target, EventPhase.TARGET, paneId, rootX, rootY, intent, dropped);
    }

    public DockDragEndedEvent(Widget target, Widget currentTarget, EventPhase phase,
                              String paneId, float rootX, float rootY,
                              DockDropIntent intent, boolean dropped) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.paneId = paneId == null ? "" : paneId;
        this.rootX = rootX;
        this.rootY = rootY;
        this.intent = intent == null ? DockDropIntent.none() : intent;
        this.dropped = dropped;
    }

    @Override
    public EventType<DockDragEndedEvent> type() {
        return TYPE;
    }

    @Override
    public Widget target() {
        return target;
    }

    @Override
    public Widget currentTarget() {
        return currentTarget;
    }

    @Override
    public EventPhase phase() {
        return phase;
    }

    public String paneId() {
        return paneId;
    }

    public float rootX() {
        return rootX;
    }

    public float rootY() {
        return rootY;
    }

    public DockDropIntent intent() {
        return intent;
    }

    public boolean dropped() {
        return dropped;
    }

    @Override
    public DockDragEndedEvent routeTo(Widget currentTarget, EventPhase phase) {
        DockDragEndedEvent event = new DockDragEndedEvent(
                target, currentTarget, phase, paneId, rootX, rootY, intent, dropped);
        if (isCancelled()) event.cancel();
        return event;
    }
}
