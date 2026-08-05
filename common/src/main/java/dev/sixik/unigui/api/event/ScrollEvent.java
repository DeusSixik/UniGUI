package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class ScrollEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<ScrollEvent> TYPE = EventType.create("pointer.scroll");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final float rootX;
    private final float rootY;
    private final float localX;
    private final float localY;
    private final float deltaX;
    private final float deltaY;

    public ScrollEvent(Widget target, float rootX, float rootY, float localX, float localY, float deltaX, float deltaY) {
        this(target, target, EventPhase.TARGET, rootX, rootY, localX, localY, deltaX, deltaY);
    }

    public ScrollEvent(Widget target, Widget currentTarget, EventPhase phase,
                       float rootX, float rootY, float localX, float localY, float deltaX, float deltaY) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.rootX = rootX;
        this.rootY = rootY;
        this.localX = localX;
        this.localY = localY;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    @Override
    public EventType<ScrollEvent> type() {
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

    public float rootX() {
        return rootX;
    }

    public float rootY() {
        return rootY;
    }

    public float localX() {
        return localX;
    }

    public float localY() {
        return localY;
    }

    public float deltaX() {
        return deltaX;
    }

    public float deltaY() {
        return deltaY;
    }

    @Override
    public ScrollEvent routeTo(Widget currentTarget, EventPhase phase) {
        ScrollEvent event = new ScrollEvent(target, currentTarget, phase, rootX, rootY, localX, localY, deltaX, deltaY);
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}
