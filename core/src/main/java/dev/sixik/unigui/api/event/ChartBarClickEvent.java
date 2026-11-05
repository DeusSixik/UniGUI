package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class ChartBarClickEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<ChartBarClickEvent> TYPE = EventType.create("chart.bar_click");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final int index;
    private final float value;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final float baseline;

    public ChartBarClickEvent(Widget target, int index, float value,
                              float x, float y, float width, float height, float baseline) {
        this(target, target, EventPhase.TARGET, index, value, x, y, width, height, baseline);
    }

    public ChartBarClickEvent(Widget target, Widget currentTarget, EventPhase phase,
                              int index, float value,
                              float x, float y, float width, float height, float baseline) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.index = index;
        this.value = value;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.baseline = baseline;
    }

    @Override
    public EventType<ChartBarClickEvent> type() {
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

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public float baseline() {
        return baseline;
    }

    @Override
    public ChartBarClickEvent routeTo(Widget currentTarget, EventPhase phase) {
        ChartBarClickEvent event = new ChartBarClickEvent(target, currentTarget, phase,
                index, value, x, y, width, height, baseline);
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}

