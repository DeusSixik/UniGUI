package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class WindowActivatedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<WindowActivatedEvent> TYPE = EventType.create("window.activated");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final Widget previousWindow;

    public WindowActivatedEvent(Widget target, Widget previousWindow) {
        this(target, target, EventPhase.TARGET, previousWindow);
    }

    public WindowActivatedEvent(Widget target, Widget currentTarget, EventPhase phase, Widget previousWindow) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.previousWindow = previousWindow;
    }

    @Override
    public EventType<WindowActivatedEvent> type() {
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

    public Widget previousWindow() {
        return previousWindow;
    }

    @Override
    public WindowActivatedEvent routeTo(Widget currentTarget, EventPhase phase) {
        WindowActivatedEvent event = new WindowActivatedEvent(target, currentTarget, phase, previousWindow);
        if (isCancelled()) event.cancel();
        return event;
    }
}
