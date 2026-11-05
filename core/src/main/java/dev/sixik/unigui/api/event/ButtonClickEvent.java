package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class ButtonClickEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<ButtonClickEvent> TYPE = EventType.create("button.click");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;

    public ButtonClickEvent(Widget target) {
        this(target, target, EventPhase.TARGET);
    }

    public ButtonClickEvent(Widget target, Widget currentTarget, EventPhase phase) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
    }

    @Override
    public EventType<ButtonClickEvent> type() {
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

    @Override
    public ButtonClickEvent routeTo(Widget currentTarget, EventPhase phase) {
        ButtonClickEvent event = new ButtonClickEvent(target, currentTarget, phase);
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}
