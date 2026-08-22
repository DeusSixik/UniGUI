package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class AdminConsoleCloseRequestedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<AdminConsoleCloseRequestedEvent> TYPE = EventType.create("adminConsole.closeRequested");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;

    public AdminConsoleCloseRequestedEvent(Widget target) {
        this(target, target, EventPhase.TARGET);
    }

    public AdminConsoleCloseRequestedEvent(Widget target, Widget currentTarget, EventPhase phase) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
    }

    @Override
    public EventType<AdminConsoleCloseRequestedEvent> type() {
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

    @Override
    public AdminConsoleCloseRequestedEvent routeTo(Widget currentTarget, EventPhase phase) {
        AdminConsoleCloseRequestedEvent event = new AdminConsoleCloseRequestedEvent(target, currentTarget, phase);
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}