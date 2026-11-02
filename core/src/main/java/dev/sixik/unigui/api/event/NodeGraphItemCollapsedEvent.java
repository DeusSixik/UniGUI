package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

/**
 * Нода графа была свёрнута или развёрнута.
 */
public final class NodeGraphItemCollapsedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphItemCollapsedEvent> TYPE = EventType.create("node_graph.item_collapsed");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String itemId;
    private final boolean collapsed;

    public NodeGraphItemCollapsedEvent(Widget target, String itemId, boolean collapsed) {
        this(target, target, EventPhase.TARGET, itemId, collapsed);
    }

    public NodeGraphItemCollapsedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                       String itemId, boolean collapsed) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.itemId = itemId == null ? "" : itemId;
        this.collapsed = collapsed;
    }

    @Override
    public EventType<NodeGraphItemCollapsedEvent> type() {
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

    /**
     * @return новое состояние: {@code true} — нода свёрнута
     */
    public boolean collapsed() {
        return collapsed;
    }

    @Override
    public NodeGraphItemCollapsedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphItemCollapsedEvent event = new NodeGraphItemCollapsedEvent(target, currentTarget, phase, itemId, collapsed);
        if (isCancelled()) event.cancel();
        return event;
    }
}
