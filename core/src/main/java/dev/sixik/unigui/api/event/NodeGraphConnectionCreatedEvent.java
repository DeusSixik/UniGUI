package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.graph.NodeGraphPortRef;

import java.util.Objects;

public final class NodeGraphConnectionCreatedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphConnectionCreatedEvent> TYPE = EventType.create("node_graph.connection_created");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String connectionId;
    private final NodeGraphPortRef from;
    private final NodeGraphPortRef to;
    private final String type;

    public NodeGraphConnectionCreatedEvent(Widget target, String connectionId, NodeGraphPortRef from, NodeGraphPortRef to, String type) {
        this(target, target, EventPhase.TARGET, connectionId, from, to, type);
    }

    public NodeGraphConnectionCreatedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                           String connectionId, NodeGraphPortRef from, NodeGraphPortRef to, String type) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.connectionId = connectionId == null ? "" : connectionId;
        this.from = from == null ? new NodeGraphPortRef("", "") : from;
        this.to = to == null ? new NodeGraphPortRef("", "") : to;
        this.type = type == null ? "" : type;
    }

    @Override
    public EventType<NodeGraphConnectionCreatedEvent> type() {
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

    public String connectionType() {
        return type;
    }

    @Override
    public NodeGraphConnectionCreatedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphConnectionCreatedEvent event = new NodeGraphConnectionCreatedEvent(target, currentTarget, phase,
                connectionId, from, to, type);
        if (isCancelled()) event.cancel();
        return event;
    }
}

