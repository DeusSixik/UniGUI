package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class NodeGraphItemMoveStartedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphItemMoveStartedEvent> TYPE = EventType.create("node_graph.item_move_started");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String itemId;
    private final float x;
    private final float y;
    private final int pointerId;

    public NodeGraphItemMoveStartedEvent(Widget target, String itemId, float x, float y, int pointerId) {
        this(target, target, EventPhase.TARGET, itemId, x, y, pointerId);
    }

    public NodeGraphItemMoveStartedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                         String itemId, float x, float y, int pointerId) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.itemId = itemId == null ? "" : itemId;
        this.x = x;
        this.y = y;
        this.pointerId = pointerId;
    }

    @Override
    public EventType<NodeGraphItemMoveStartedEvent> type() {
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

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public int pointerId() {
        return pointerId;
    }

    @Override
    public NodeGraphItemMoveStartedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphItemMoveStartedEvent event = new NodeGraphItemMoveStartedEvent(target, currentTarget, phase,
                itemId, x, y, pointerId);
        if (isCancelled()) event.cancel();
        return event;
    }
}

