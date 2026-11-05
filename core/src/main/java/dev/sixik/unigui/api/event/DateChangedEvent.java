package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.time.LocalDate;
import java.util.Objects;

public final class DateChangedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<DateChangedEvent> TYPE = EventType.create("date_picker.date_changed");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final LocalDate oldValue;
    private final LocalDate newValue;

    public DateChangedEvent(Widget target, LocalDate oldValue, LocalDate newValue) {
        this(target, target, EventPhase.TARGET, oldValue, newValue);
    }

    public DateChangedEvent(Widget target, Widget currentTarget, EventPhase phase,
                            LocalDate oldValue, LocalDate newValue) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public EventType<DateChangedEvent> type() {
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

    public LocalDate oldValue() {
        return oldValue;
    }

    public LocalDate newValue() {
        return newValue;
    }

    @Override
    public DateChangedEvent routeTo(Widget currentTarget, EventPhase phase) {
        DateChangedEvent event = new DateChangedEvent(target, currentTarget, phase, oldValue, newValue);
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}

