package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class WindowMovedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<WindowMovedEvent> TYPE = EventType.create("window.moved");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final float oldX;
    private final float oldY;
    private final float newX;
    private final float newY;

    public WindowMovedEvent(Widget target, float oldX, float oldY, float newX, float newY) {
        this(target, target, EventPhase.TARGET, oldX, oldY, newX, newY);
    }

    public WindowMovedEvent(Widget target, Widget currentTarget, EventPhase phase,
                            float oldX, float oldY, float newX, float newY) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
    }

    @Override
    public EventType<WindowMovedEvent> type() {
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

    public float oldX() {
        return oldX;
    }

    public float oldY() {
        return oldY;
    }

    public float newX() {
        return newX;
    }

    public float newY() {
        return newY;
    }

    @Override
    public WindowMovedEvent routeTo(Widget currentTarget, EventPhase phase) {
        WindowMovedEvent event = new WindowMovedEvent(target, currentTarget, phase, oldX, oldY, newX, newY);
        if (isCancelled()) event.cancel();
        return event;
    }
}
