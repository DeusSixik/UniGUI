package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class FocusLostEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<FocusLostEvent> TYPE = EventType.create("focus.lost");

    private final Widget target;
    private final Widget nextFocus;

    public FocusLostEvent(Widget target, Widget nextFocus) {
        this.target = Objects.requireNonNull(target, "target");
        this.nextFocus = nextFocus;
    }

    @Override
    public EventType<FocusLostEvent> type() {
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

    public Widget nextFocus() {
        return nextFocus;
    }
}
