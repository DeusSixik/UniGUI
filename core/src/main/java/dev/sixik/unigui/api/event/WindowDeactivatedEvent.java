package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class WindowDeactivatedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<WindowDeactivatedEvent> TYPE = EventType.create("window.deactivated");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final Widget nextWindow;

    public WindowDeactivatedEvent(Widget target, Widget nextWindow) {
        this(target, target, EventPhase.TARGET, nextWindow);
    }

    public WindowDeactivatedEvent(Widget target, Widget currentTarget, EventPhase phase, Widget nextWindow) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.nextWindow = nextWindow;
    }

    @Override
    public EventType<WindowDeactivatedEvent> type() {
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

    public Widget nextWindow() {
        return nextWindow;
    }

    @Override
    public WindowDeactivatedEvent routeTo(Widget currentTarget, EventPhase phase) {
        WindowDeactivatedEvent event = new WindowDeactivatedEvent(target, currentTarget, phase, nextWindow);
        if (isCancelled()) event.cancel();
        return event;
    }
}
