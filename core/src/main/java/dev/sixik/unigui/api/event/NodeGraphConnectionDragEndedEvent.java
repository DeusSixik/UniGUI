package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.graph.NodeGraphPortRef;

import java.util.Objects;

public final class NodeGraphConnectionDragEndedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphConnectionDragEndedEvent> TYPE = EventType.create("node_graph.connection_drag_ended");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final NodeGraphPortRef from;
    private final NodeGraphPortRef to;
    private final String connectionId;
    private final boolean valid;
    private final String reason;
    private final int pointerId;

    public NodeGraphConnectionDragEndedEvent(Widget target, NodeGraphPortRef from, NodeGraphPortRef to,
                                             String connectionId, boolean valid, String reason, int pointerId) {
        this(target, target, EventPhase.TARGET, from, to, connectionId, valid, reason, pointerId);
    }

    public NodeGraphConnectionDragEndedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                             NodeGraphPortRef from, NodeGraphPortRef to, String connectionId,
                                             boolean valid, String reason, int pointerId) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.from = from == null ? new NodeGraphPortRef("", "") : from;
        this.to = to == null ? new NodeGraphPortRef("", "") : to;
        this.connectionId = connectionId == null ? "" : connectionId;
        this.valid = valid;
        this.reason = reason == null ? "" : reason;
        this.pointerId = pointerId;
    }

    @Override
    public EventType<NodeGraphConnectionDragEndedEvent> type() {
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

    public NodeGraphPortRef to() {
        return to;
    }

    public String connectionId() {
        return connectionId;
    }

    public boolean valid() {
        return valid;
    }

    public String reason() {
        return reason;
    }

    public int pointerId() {
        return pointerId;
    }

    @Override
    public NodeGraphConnectionDragEndedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphConnectionDragEndedEvent event = new NodeGraphConnectionDragEndedEvent(target, currentTarget, phase,
                from, to, connectionId, valid, reason, pointerId);
        if (isCancelled()) event.cancel();
        return event;
    }
}

