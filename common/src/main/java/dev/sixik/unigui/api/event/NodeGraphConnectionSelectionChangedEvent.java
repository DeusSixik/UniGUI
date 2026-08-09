package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.List;
import java.util.Objects;

public final class NodeGraphConnectionSelectionChangedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<NodeGraphConnectionSelectionChangedEvent> TYPE = EventType.create("node_graph.connection_selection_changed");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final List<String> oldSelection;
    private final List<String> newSelection;

    public NodeGraphConnectionSelectionChangedEvent(Widget target, List<String> oldSelection, List<String> newSelection) {
        this(target, target, EventPhase.TARGET, oldSelection, newSelection);
    }

    public NodeGraphConnectionSelectionChangedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                                    List<String> oldSelection, List<String> newSelection) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.oldSelection = oldSelection == null ? List.of() : List.copyOf(oldSelection);
        this.newSelection = newSelection == null ? List.of() : List.copyOf(newSelection);
    }

    @Override
    public EventType<NodeGraphConnectionSelectionChangedEvent> type() {
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
    public NodeGraphConnectionSelectionChangedEvent routeTo(Widget currentTarget, EventPhase phase) {
        NodeGraphConnectionSelectionChangedEvent event = new NodeGraphConnectionSelectionChangedEvent(target, currentTarget, phase,
                oldSelection, newSelection);
        if (isCancelled()) event.cancel();
        return event;
    }
}

