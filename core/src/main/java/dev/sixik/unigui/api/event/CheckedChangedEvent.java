package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class CheckedChangedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<CheckedChangedEvent> TYPE = EventType.create("checked.changed");

    private final Widget target;
    private final boolean oldValue;
    private final boolean newValue;

    public CheckedChangedEvent(Widget target, boolean oldValue, boolean newValue) {
        this.target = Objects.requireNonNull(target, "target");
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public EventType<CheckedChangedEvent> type() {
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

    public boolean oldValue() {
        return oldValue;
    }

    public boolean newValue() {
        return newValue;
    }
}
