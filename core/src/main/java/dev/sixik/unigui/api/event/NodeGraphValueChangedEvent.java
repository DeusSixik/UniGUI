package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

/** Значение редактора входа ноды графа изменилось. */
public final class NodeGraphValueChangedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphValueChangedEvent> TYPE = EventType.create("node_graph.value_changed");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String itemId;
    private final String portId;
    private final Object value;
    private final String serialized;

    public NodeGraphValueChangedEvent(Widget target, String itemId, String portId, Object value, String serialized) {
        this(target, target, EventPhase.TARGET, itemId, portId, value, serialized);
    }

    public NodeGraphValueChangedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                      String itemId, String portId, Object value, String serialized) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.itemId = itemId == null ? "" : itemId;
        this.portId = portId == null ? "" : portId;
        this.value = value;
        this.serialized = serialized == null ? "" : serialized;
    }

    @Override
    public EventType<NodeGraphValueChangedEvent> type() {
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

    public String portId() {
        return portId;
    }

    /** @return новое типизированное значение: String, Double, Integer, Boolean, double[] и т. п. */
    public Object value() {
        return value;
    }

    /** @return текстовая форма нового значения, та же, что сохраняется в snapshot */
    public String serialized() {
        return serialized;
    }

    @Override
    public NodeGraphValueChangedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphValueChangedEvent event = new NodeGraphValueChangedEvent(target, currentTarget, phase,
                itemId, portId, value, serialized);
        if (isCancelled()) event.cancel();
        return event;
    }
}
