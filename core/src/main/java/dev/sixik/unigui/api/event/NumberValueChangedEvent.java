package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class NumberValueChangedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<NumberValueChangedEvent> TYPE = EventType.create("number.value_changed");

    private final Widget target;
    private final double oldValue;
    private final double newValue;

    public NumberValueChangedEvent(Widget target, double oldValue, double newValue) {
        this.target = Objects.requireNonNull(target, "target");
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public EventType<NumberValueChangedEvent> type() {
        return TYPE;
    }

    @Override
    public Widget target() {
        return target;
    }

    @Override
    public Widget currentTarget() {
        return target;
    }

    @Override
    public EventPhase phase() {
        return EventPhase.TARGET;
    }

    public double oldValue() {
        return oldValue;
    }

    public double newValue() {
        return newValue;
    }
}
