package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class ModalClosedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<ModalClosedEvent> TYPE = EventType.create("modal.closed");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final int stackDepth;

    public ModalClosedEvent(Widget target, int stackDepth) {
        this(target, target, EventPhase.TARGET, stackDepth);
    }

    public ModalClosedEvent(Widget target, Widget currentTarget, EventPhase phase, int stackDepth) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.stackDepth = stackDepth;
    }

    @Override
    public EventType<ModalClosedEvent> type() {
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

    public int stackDepth() {
        return stackDepth;
    }

    @Override
    public ModalClosedEvent routeTo(Widget currentTarget, EventPhase phase) {
        ModalClosedEvent event = new ModalClosedEvent(target, currentTarget, phase, stackDepth);
        if (isCancelled()) event.cancel();
        return event;
    }
}
