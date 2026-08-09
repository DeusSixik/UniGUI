package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class NodeGraphItemResizeEndedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphItemResizeEndedEvent> TYPE = EventType.create("node_graph.item_resize_ended");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String itemId;
    private final float startWidth;
    private final float startHeight;
    private final float endWidth;
    private final float endHeight;
    private final int pointerId;

    public NodeGraphItemResizeEndedEvent(Widget target, String itemId,
                                         float startWidth, float startHeight, float endWidth, float endHeight, int pointerId) {
        this(target, target, EventPhase.TARGET, itemId, startWidth, startHeight, endWidth, endHeight, pointerId);
    }

    public NodeGraphItemResizeEndedEvent(Widget target, Widget currentTarget, EventPhase phase, String itemId,
                                         float startWidth, float startHeight, float endWidth, float endHeight, int pointerId) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.itemId = itemId == null ? "" : itemId;
        this.startWidth = startWidth;
        this.startHeight = startHeight;
        this.endWidth = endWidth;
        this.endHeight = endHeight;
        this.pointerId = pointerId;
    }

    @Override public EventType<NodeGraphItemResizeEndedEvent> type() { return TYPE; }
    @Override public Widget target() { return target; }
    @Override public Widget currentTarget() { return currentTarget; }
    @Override public EventPhase phase() { return phase; }
    public String itemId() { return itemId; }
    public float startWidth() { return startWidth; }
    public float startHeight() { return startHeight; }
    public float endWidth() { return endWidth; }
    public float endHeight() { return endHeight; }
    public int pointerId() { return pointerId; }

    @Override
    public NodeGraphItemResizeEndedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphItemResizeEndedEvent event = new NodeGraphItemResizeEndedEvent(target, currentTarget, phase,
                itemId, startWidth, startHeight, endWidth, endHeight, pointerId);
        if (isCancelled()) event.cancel();
        return event;
    }
}

