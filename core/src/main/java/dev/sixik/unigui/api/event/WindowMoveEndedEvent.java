package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class WindowMoveEndedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<WindowMoveEndedEvent> TYPE = EventType.create("window.move_ended");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final float x;
    private final float y;

    public WindowMoveEndedEvent(Widget target, float x, float y) {
        this(target, target, EventPhase.TARGET, x, y);
    }

    public WindowMoveEndedEvent(Widget target, Widget currentTarget, EventPhase phase, float x, float y) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.x = x;
        this.y = y;
    }

    @Override
    public EventType<WindowMoveEndedEvent> type() {
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
    public WindowMoveEndedEvent routeTo(Widget currentTarget, EventPhase phase) {
        WindowMoveEndedEvent event = new WindowMoveEndedEvent(target, currentTarget, phase, x, y);
        if (isCancelled()) event.cancel();
        return event;
    }
}
