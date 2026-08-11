package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.CheckboxState;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class CheckboxStateChangedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<CheckboxStateChangedEvent> TYPE = EventType.create("checkbox.state_changed");

    private final Widget target;
    private final CheckboxState oldState;
    private final CheckboxState newState;

    public CheckboxStateChangedEvent(Widget target, CheckboxState oldState, CheckboxState newState) {
        this.target = Objects.requireNonNull(target, "target");
        this.oldState = Objects.requireNonNull(oldState, "oldState");
        this.newState = Objects.requireNonNull(newState, "newState");
    }

    @Override
    public EventType<CheckboxStateChangedEvent> type() {
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

    public CheckboxState oldState() {
        return oldState;
    }

    public CheckboxState newState() {
        return newState;
    }
}
