package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class NodeGraphItemResizeStartedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphItemResizeStartedEvent> TYPE = EventType.create("node_graph.item_resize_started");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String itemId;
    private final float width;
    private final float height;
    private final int pointerId;

    public NodeGraphItemResizeStartedEvent(Widget target, String itemId, float width, float height, int pointerId) {
        this(target, target, EventPhase.TARGET, itemId, width, height, pointerId);
    }

    public NodeGraphItemResizeStartedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                           String itemId, float width, float height, int pointerId) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.itemId = itemId == null ? "" : itemId;
        this.width = width;
        this.height = height;
        this.pointerId = pointerId;
    }

    @Override public EventType<NodeGraphItemResizeStartedEvent> type() { return TYPE; }
    @Override public Widget target() { return target; }
    @Override public Widget currentTarget() { return currentTarget; }
    @Override public EventPhase phase() { return phase; }
    public String itemId() { return itemId; }
    public float width() { return width; }
    public float height() { return height; }
    public int pointerId() { return pointerId; }

    @Override
    public NodeGraphItemResizeStartedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphItemResizeStartedEvent event = new NodeGraphItemResizeStartedEvent(target, currentTarget, phase,
                itemId, width, height, pointerId);
        if (isCancelled()) event.cancel();
        return event;
    }
}

