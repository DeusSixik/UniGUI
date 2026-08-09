package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class DockDragStartedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<DockDragStartedEvent> TYPE = EventType.create("dock.drag_started");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String paneId;
    private final float rootX;
    private final float rootY;

    public DockDragStartedEvent(Widget target, String paneId, float rootX, float rootY) {
        this(target, target, EventPhase.TARGET, paneId, rootX, rootY);
    }

    public DockDragStartedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                String paneId, float rootX, float rootY) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.paneId = paneId == null ? "" : paneId;
        this.rootX = rootX;
        this.rootY = rootY;
    }

    @Override
    public EventType<DockDragStartedEvent> type() {
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

    @Override
    public DockDragStartedEvent routeTo(Widget currentTarget, EventPhase phase) {
        DockDragStartedEvent event = new DockDragStartedEvent(target, currentTarget, phase, paneId, rootX, rootY);
        if (isCancelled()) event.cancel();
        return event;
    }
}
