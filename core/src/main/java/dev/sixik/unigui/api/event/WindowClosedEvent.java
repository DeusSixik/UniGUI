package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class WindowClosedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<WindowClosedEvent> TYPE = EventType.create("window.closed");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;

    public WindowClosedEvent(Widget target) {
        this(target, target, EventPhase.TARGET);
    }

    public WindowClosedEvent(Widget target, Widget currentTarget, EventPhase phase) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
    }

    @Override
    public EventType<WindowClosedEvent> type() {
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
    public WindowClosedEvent routeTo(Widget currentTarget, EventPhase phase) {
        WindowClosedEvent event = new WindowClosedEvent(target, currentTarget, phase);
        if (isCancelled()) event.cancel();
        return event;
    }
}
