package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class SparkPointClickEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<SparkPointClickEvent> TYPE = EventType.create("sparkline.point_click");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final int index;
    private final float value;
    private final float x;
    private final float y;

    public SparkPointClickEvent(Widget target, int index, float value, float x, float y) {
        this(target, target, EventPhase.TARGET, index, value, x, y);
    }

    public SparkPointClickEvent(Widget target, Widget currentTarget, EventPhase phase,
                                int index, float value, float x, float y) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.index = index;
        this.value = value;
        this.x = x;
        this.y = y;
    }

    @Override
    public EventType<SparkPointClickEvent> type() {
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

    public int index() {
        return index;
    }

    public float value() {
        return value;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    @Override
    public SparkPointClickEvent routeTo(Widget currentTarget, EventPhase phase) {
        SparkPointClickEvent event = new SparkPointClickEvent(target, currentTarget, phase, index, value, x, y);
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}

