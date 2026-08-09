package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.List;
import java.util.Objects;

public final class NodeGraphItemSelectionChangedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphItemSelectionChangedEvent> TYPE = EventType.create("node_graph.selection_changed");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final List<String> oldSelection;
    private final List<String> newSelection;

    public NodeGraphItemSelectionChangedEvent(Widget target, List<String> oldSelection, List<String> newSelection) {
        this(target, target, EventPhase.TARGET, oldSelection, newSelection);
    }

    public NodeGraphItemSelectionChangedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                              List<String> oldSelection, List<String> newSelection) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.oldSelection = oldSelection == null ? List.of() : List.copyOf(oldSelection);
        this.newSelection = newSelection == null ? List.of() : List.copyOf(newSelection);
    }

    @Override
    public EventType<NodeGraphItemSelectionChangedEvent> type() {
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

    public List<String> oldSelection() {
        return oldSelection;
    }

    public List<String> newSelection() {
        return newSelection;
    }

    @Override
    public NodeGraphItemSelectionChangedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphItemSelectionChangedEvent event = new NodeGraphItemSelectionChangedEvent(target, currentTarget, phase,
                oldSelection, newSelection);
        if (isCancelled()) event.cancel();
        return event;
    }
}

