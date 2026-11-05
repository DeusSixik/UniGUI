package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class NodeGraphItemAddedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphItemAddedEvent> TYPE = EventType.create("node_graph.item_added");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String itemId;

    public NodeGraphItemAddedEvent(Widget target, String itemId) {
        this(target, target, EventPhase.TARGET, itemId);
    }

    public NodeGraphItemAddedEvent(Widget target, Widget currentTarget, EventPhase phase, String itemId) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.itemId = itemId == null ? "" : itemId;
    }

    @Override
    public EventType<NodeGraphItemAddedEvent> type() {
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

    public String itemId() {
        return itemId;
    }

    @Override
    public NodeGraphItemAddedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphItemAddedEvent event = new NodeGraphItemAddedEvent(target, currentTarget, phase, itemId);
        if (isCancelled()) event.cancel();
        return event;
    }
}

