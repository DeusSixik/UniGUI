package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class WindowOpenedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<WindowOpenedEvent> TYPE = EventType.create("window.opened");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;

    public WindowOpenedEvent(Widget target) {
        this(target, target, EventPhase.TARGET);
    }

    public WindowOpenedEvent(Widget target, Widget currentTarget, EventPhase phase) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
    }

    @Override
    public EventType<WindowOpenedEvent> type() {
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
    public WindowOpenedEvent routeTo(Widget currentTarget, EventPhase phase) {
        WindowOpenedEvent event = new WindowOpenedEvent(target, currentTarget, phase);
        if (isCancelled()) event.cancel();
        return event;
    }
}
