package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class WindowMoveStartedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<WindowMoveStartedEvent> TYPE = EventType.create("window.move_started");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final float x;
    private final float y;

    public WindowMoveStartedEvent(Widget target, float x, float y) {
        this(target, target, EventPhase.TARGET, x, y);
    }

    public WindowMoveStartedEvent(Widget target, Widget currentTarget, EventPhase phase, float x, float y) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.x = x;
        this.y = y;
    }

    @Override
    public EventType<WindowMoveStartedEvent> type() {
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

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    @Override
    public WindowMoveStartedEvent routeTo(Widget currentTarget, EventPhase phase) {
        WindowMoveStartedEvent event = new WindowMoveStartedEvent(target, currentTarget, phase, x, y);
        if (isCancelled()) event.cancel();
        return event;
    }
}
