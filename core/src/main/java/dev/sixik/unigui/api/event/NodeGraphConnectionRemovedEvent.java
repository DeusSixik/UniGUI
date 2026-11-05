package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.graph.NodeGraphPortRef;

import java.util.Objects;

public final class NodeGraphConnectionRemovedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphConnectionRemovedEvent> TYPE = EventType.create("node_graph.connection_removed");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String connectionId;
    private final NodeGraphPortRef from;
    private final NodeGraphPortRef to;

    public NodeGraphConnectionRemovedEvent(Widget target, String connectionId, NodeGraphPortRef from, NodeGraphPortRef to) {
        this(target, target, EventPhase.TARGET, connectionId, from, to);
    }

    public NodeGraphConnectionRemovedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                           String connectionId, NodeGraphPortRef from, NodeGraphPortRef to) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.connectionId = connectionId == null ? "" : connectionId;
        this.from = from == null ? new NodeGraphPortRef("", "") : from;
        this.to = to == null ? new NodeGraphPortRef("", "") : to;
    }

    @Override
    public EventType<NodeGraphConnectionRemovedEvent> type() {
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

    public String connectionId() {
        return connectionId;
    }

    public NodeGraphPortRef from() {
        return from;
    }

    public NodeGraphPortRef to() {
        return to;
    }

    @Override
    public NodeGraphConnectionRemovedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphConnectionRemovedEvent event = new NodeGraphConnectionRemovedEvent(target, currentTarget, phase,
                connectionId, from, to);
        if (isCancelled()) event.cancel();
        return event;
    }
}

