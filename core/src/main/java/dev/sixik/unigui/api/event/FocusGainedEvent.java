package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class FocusGainedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<FocusGainedEvent> TYPE = EventType.create("focus.gained");

    private final Widget target;
    private final Widget previousFocus;

    public FocusGainedEvent(Widget target, Widget previousFocus) {
        this.target = Objects.requireNonNull(target, "target");
        this.previousFocus = previousFocus;
    }

    @Override
    public EventType<FocusGainedEvent> type() {
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

    public Widget previousFocus() {
        return previousFocus;
    }
}
