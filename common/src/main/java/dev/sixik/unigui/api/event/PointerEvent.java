package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public abstract class PointerEvent extends BaseEvent implements RoutableWidgetEvent {
    private final EventType<? extends Event> type;
    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final float rootX;
    private final float rootY;
    private final float localX;
    private final float localY;
    private final int pointerId;
    private final PointerButton button;

    protected PointerEvent(EventType<? extends Event> type, Widget target, Widget currentTarget, EventPhase phase,
                           float rootX, float rootY, float localX, float localY, int pointerId, PointerButton button) {
        this.type = Objects.requireNonNull(type, "type");
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.rootX = rootX;
        this.rootY = rootY;
        this.localX = localX;
        this.localY = localY;
        this.pointerId = pointerId;
        this.button = button == null ? PointerButton.UNKNOWN : button;
    }

    @Override
    public EventType<? extends Event> type() {
        return type;
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

    public float rootX() {
        return rootX;
    }

    public float rootY() {
        return rootY;
    }

    public float localX() {
        return localX;
    }

    public float localY() {
        return localY;
    }

    public int pointerId() {
        return pointerId;
    }

    public PointerButton button() {
        return button;
    }
}
