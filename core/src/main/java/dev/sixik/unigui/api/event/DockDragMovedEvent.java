package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.docking.DockDropIntent;

import java.util.Objects;

public final class DockDragMovedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<DockDragMovedEvent> TYPE = EventType.create("dock.drag_moved");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String paneId;
    private final float rootX;
    private final float rootY;
    private final DockDropIntent intent;

    public DockDragMovedEvent(Widget target, String paneId, float rootX, float rootY, DockDropIntent intent) {
        this(target, target, EventPhase.TARGET, paneId, rootX, rootY, intent);
    }

    public DockDragMovedEvent(Widget target, Widget currentTarget, EventPhase phase,
                              String paneId, float rootX, float rootY, DockDropIntent intent) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.paneId = paneId == null ? "" : paneId;
        this.rootX = rootX;
        this.rootY = rootY;
        this.intent = intent == null ? DockDropIntent.none() : intent;
    }

    @Override
    public EventType<DockDragMovedEvent> type() {
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

    @Override
    public DockDragMovedEvent routeTo(Widget currentTarget, EventPhase phase) {
        DockDragMovedEvent event = new DockDragMovedEvent(target, currentTarget, phase, paneId, rootX, rootY, intent);
        if (isCancelled()) event.cancel();
        return event;
    }
}
