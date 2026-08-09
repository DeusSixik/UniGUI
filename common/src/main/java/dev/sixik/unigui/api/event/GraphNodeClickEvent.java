package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class GraphNodeClickEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<GraphNodeClickEvent> TYPE = EventType.create("graph.node_click");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final int index;
    private final String id;
    private final float normalizedX;
    private final float normalizedY;
    private final float x;
    private final float y;

    public GraphNodeClickEvent(Widget target, int index, String id,
                               float normalizedX, float normalizedY, float x, float y) {
        this(target, target, EventPhase.TARGET, index, id, normalizedX, normalizedY, x, y);
    }

    public GraphNodeClickEvent(Widget target, Widget currentTarget, EventPhase phase,
                               int index, String id, float normalizedX, float normalizedY, float x, float y) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.index = index;
        this.id = id == null ? "" : id;
        this.normalizedX = normalizedX;
        this.normalizedY = normalizedY;
        this.x = x;
        this.y = y;
    }

    @Override
    public EventType<GraphNodeClickEvent> type() {
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

    public int index() {
        return index;
    }

    public String id() {
        return id;
    }

    public float normalizedX() {
        return normalizedX;
    }

    public float normalizedY() {
        return normalizedY;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    @Override
    public GraphNodeClickEvent routeTo(Widget currentTarget, EventPhase phase) {
        GraphNodeClickEvent event = new GraphNodeClickEvent(target, currentTarget, phase,
                index, id, normalizedX, normalizedY, x, y);
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}

