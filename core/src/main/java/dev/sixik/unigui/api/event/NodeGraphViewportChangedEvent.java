package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class NodeGraphViewportChangedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphViewportChangedEvent> TYPE = EventType.create("node_graph.viewport_changed");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final float oldX;
    private final float oldY;
    private final float oldZoom;
    private final float newX;
    private final float newY;
    private final float newZoom;

    public NodeGraphViewportChangedEvent(Widget target, float oldX, float oldY, float oldZoom,
                                         float newX, float newY, float newZoom) {
        this(target, target, EventPhase.TARGET, oldX, oldY, oldZoom, newX, newY, newZoom);
    }

    public NodeGraphViewportChangedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                         float oldX, float oldY, float oldZoom,
                                         float newX, float newY, float newZoom) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.oldX = oldX;
        this.oldY = oldY;
        this.oldZoom = oldZoom;
        this.newX = newX;
        this.newY = newY;
        this.newZoom = newZoom;
    }

    @Override
    public EventType<NodeGraphViewportChangedEvent> type() {
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

    public float oldX() {
        return oldX;
    }

    public float oldY() {
        return oldY;
    }

    public float oldZoom() {
        return oldZoom;
    }

    public float newX() {
        return newX;
    }

    public float newY() {
        return newY;
    }

    public float newZoom() {
        return newZoom;
    }

    @Override
    public NodeGraphViewportChangedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphViewportChangedEvent event = new NodeGraphViewportChangedEvent(target, currentTarget, phase,
                oldX, oldY, oldZoom, newX, newY, newZoom);
        if (isCancelled()) event.cancel();
        return event;
    }
}

