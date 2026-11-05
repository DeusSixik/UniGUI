package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class NodeGraphItemMoveEndedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphItemMoveEndedEvent> TYPE = EventType.create("node_graph.item_move_ended");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String itemId;
    private final float startX;
    private final float startY;
    private final float endX;
    private final float endY;
    private final int pointerId;

    public NodeGraphItemMoveEndedEvent(Widget target, String itemId,
                                       float startX, float startY, float endX, float endY, int pointerId) {
        this(target, target, EventPhase.TARGET, itemId, startX, startY, endX, endY, pointerId);
    }

    public NodeGraphItemMoveEndedEvent(Widget target, Widget currentTarget, EventPhase phase, String itemId,
                                       float startX, float startY, float endX, float endY, int pointerId) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.itemId = itemId == null ? "" : itemId;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.pointerId = pointerId;
    }

    @Override
    public EventType<NodeGraphItemMoveEndedEvent> type() {
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

    public float startX() {
        return startX;
    }

    public float startY() {
        return startY;
    }

    public float endX() {
        return endX;
    }

    public float endY() {
        return endY;
    }

    public int pointerId() {
        return pointerId;
    }

    @Override
    public NodeGraphItemMoveEndedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphItemMoveEndedEvent event = new NodeGraphItemMoveEndedEvent(target, currentTarget, phase,
                itemId, startX, startY, endX, endY, pointerId);
        if (isCancelled()) event.cancel();
        return event;
    }
}

