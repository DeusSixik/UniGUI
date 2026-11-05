package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.List;
import java.util.Objects;

public final class SelectionChangedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<SelectionChangedEvent> TYPE = EventType.create("selection.changed");

    private final Widget target;
    private final List<Integer> oldSelection;
    private final List<Integer> newSelection;

    public SelectionChangedEvent(Widget target, List<Integer> oldSelection, List<Integer> newSelection) {
        this.target = Objects.requireNonNull(target, "target");
        this.oldSelection = List.copyOf(oldSelection == null ? List.of() : oldSelection);
        this.newSelection = List.copyOf(newSelection == null ? List.of() : newSelection);
    }

    @Override
    public EventType<SelectionChangedEvent> type() {
        return TYPE;
    }

    @Override
    public Widget target() {
        return target;
    }

    @Override
    public Widget currentTarget() {
        return target;
    }

    @Override
    public EventPhase phase() {
        return EventPhase.TARGET;
    }

    public List<Integer> oldSelection() {
        return oldSelection;
    }

    public List<Integer> newSelection() {
        return newSelection;
    }
}
