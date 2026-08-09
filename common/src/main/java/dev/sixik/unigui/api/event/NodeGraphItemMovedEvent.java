package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class NodeGraphItemMovedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphItemMovedEvent> TYPE = EventType.create("node_graph.item_moved");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String itemId;
    private final float oldX;
    private final float oldY;
    private final float newX;
    private final float newY;
    private final int pointerId;

    public NodeGraphItemMovedEvent(Widget target, String itemId,
                                   float oldX, float oldY, float newX, float newY, int pointerId) {
        this(target, target, EventPhase.TARGET, itemId, oldX, oldY, newX, newY, pointerId);
    }

    public NodeGraphItemMovedEvent(Widget target, Widget currentTarget, EventPhase phase, String itemId,
                                   float oldX, float oldY, float newX, float newY, int pointerId) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.itemId = itemId == null ? "" : itemId;
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
        this.pointerId = pointerId;
    }

    @Override
    public EventType<NodeGraphItemMovedEvent> type() {
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

    public float oldX() {
        return oldX;
    }

    public float oldY() {
        return oldY;
    }

    public float newX() {
        return newX;
    }

    public float newY() {
        return newY;
    }

    public int pointerId() {
        return pointerId;
    }

    @Override
    public NodeGraphItemMovedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphItemMovedEvent event = new NodeGraphItemMovedEvent(target, currentTarget, phase,
                itemId, oldX, oldY, newX, newY, pointerId);
        if (isCancelled()) event.cancel();
        return event;
    }
}

