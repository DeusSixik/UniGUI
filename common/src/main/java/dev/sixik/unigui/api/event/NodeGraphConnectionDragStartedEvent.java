package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.NodeGraphPortRef;

import java.util.Objects;

public final class NodeGraphConnectionDragStartedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphConnectionDragStartedEvent> TYPE = EventType.create("node_graph.connection_drag_started");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final NodeGraphPortRef from;
    private final int pointerId;

    public NodeGraphConnectionDragStartedEvent(Widget target, NodeGraphPortRef from, int pointerId) {
        this(target, target, EventPhase.TARGET, from, pointerId);
    }

    public NodeGraphConnectionDragStartedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                               NodeGraphPortRef from, int pointerId) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.from = from == null ? new NodeGraphPortRef("", "") : from;
        this.pointerId = pointerId;
    }

    @Override
    public EventType<NodeGraphConnectionDragStartedEvent> type() {
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

    public NodeGraphPortRef from() {
        return from;
    }

    public int pointerId() {
        return pointerId;
    }

    @Override
    public NodeGraphConnectionDragStartedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphConnectionDragStartedEvent event = new NodeGraphConnectionDragStartedEvent(target, currentTarget, phase,
                from, pointerId);
        if (isCancelled()) event.cancel();
        return event;
    }
}

