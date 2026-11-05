package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class NodeGraphItemResizedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphItemResizedEvent> TYPE = EventType.create("node_graph.item_resized");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String itemId;
    private final float oldWidth;
    private final float oldHeight;
    private final float newWidth;
    private final float newHeight;
    private final int pointerId;

    public NodeGraphItemResizedEvent(Widget target, String itemId,
                                     float oldWidth, float oldHeight, float newWidth, float newHeight, int pointerId) {
        this(target, target, EventPhase.TARGET, itemId, oldWidth, oldHeight, newWidth, newHeight, pointerId);
    }

    public NodeGraphItemResizedEvent(Widget target, Widget currentTarget, EventPhase phase, String itemId,
                                     float oldWidth, float oldHeight, float newWidth, float newHeight, int pointerId) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.itemId = itemId == null ? "" : itemId;
        this.oldWidth = oldWidth;
        this.oldHeight = oldHeight;
        this.newWidth = newWidth;
        this.newHeight = newHeight;
        this.pointerId = pointerId;
    }

    @Override public EventType<NodeGraphItemResizedEvent> type() { return TYPE; }
    @Override public Widget target() { return target; }
    @Override public Widget currentTarget() { return currentTarget; }
    @Override public EventPhase phase() { return phase; }
    public String itemId() { return itemId; }
    public float oldWidth() { return oldWidth; }
    public float oldHeight() { return oldHeight; }
    public float newWidth() { return newWidth; }
    public float newHeight() { return newHeight; }
    public int pointerId() { return pointerId; }

    @Override
    public NodeGraphItemResizedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphItemResizedEvent event = new NodeGraphItemResizedEvent(target, currentTarget, phase,
                itemId, oldWidth, oldHeight, newWidth, newHeight, pointerId);
        if (isCancelled()) event.cancel();
        return event;
    }
}

