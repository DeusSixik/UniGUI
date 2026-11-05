package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

/**
 * Fired when a {@code HoldButton} reaches its required hold duration.
 *
 * <p>The event is routed like {@link ButtonClickEvent}. Cancelling this event
 * prevents the follow-up button click from being emitted.</p>
 */
public final class HoldCompletedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<HoldCompletedEvent> TYPE = EventType.create("hold.completed");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final float holdDurationSeconds;

    public HoldCompletedEvent(Widget target, float holdDurationSeconds) {
        this(target, target, EventPhase.TARGET, holdDurationSeconds);
    }

    public HoldCompletedEvent(Widget target, Widget currentTarget, EventPhase phase, float holdDurationSeconds) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.holdDurationSeconds = Float.isFinite(holdDurationSeconds) ? Math.max(0.0f, holdDurationSeconds) : 0.0f;
    }

    @Override
    public EventType<HoldCompletedEvent> type() {
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

    public float holdDurationSeconds() {
        return holdDurationSeconds;
    }

    @Override
    public HoldCompletedEvent routeTo(Widget currentTarget, EventPhase phase) {
        HoldCompletedEvent event = new HoldCompletedEvent(target, currentTarget, phase, holdDurationSeconds);
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}
