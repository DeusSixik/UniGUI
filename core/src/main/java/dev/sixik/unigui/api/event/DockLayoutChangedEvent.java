package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class DockLayoutChangedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<DockLayoutChangedEvent> TYPE = EventType.create("dock.layout.changed");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String operation;
    private final String paneId;
    private final String targetPaneId;

    public DockLayoutChangedEvent(Widget target, String operation, String paneId, String targetPaneId) {
        this(target, target, EventPhase.TARGET, operation, paneId, targetPaneId);
    }

    public DockLayoutChangedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                  String operation, String paneId, String targetPaneId) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.operation = operation == null ? "" : operation;
        this.paneId = paneId == null ? "" : paneId;
        this.targetPaneId = targetPaneId == null ? "" : targetPaneId;
    }

    @Override
    public EventType<DockLayoutChangedEvent> type() {
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

    public String operation() {
        return operation;
    }

    public String paneId() {
        return paneId;
    }

    public String targetPaneId() {
        return targetPaneId;
    }

    @Override
    public DockLayoutChangedEvent routeTo(Widget currentTarget, EventPhase phase) {
        DockLayoutChangedEvent event = new DockLayoutChangedEvent(
                target, currentTarget, phase, operation, paneId, targetPaneId);
        if (isCancelled()) event.cancel();
        return event;
    }
}
