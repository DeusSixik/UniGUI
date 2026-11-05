package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.sort.SortDirection;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class TableSortChangedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<TableSortChangedEvent> TYPE = EventType.create("table.sort_changed");

    private final Widget target;
    private final int oldColumnIndex;
    private final SortDirection oldDirection;
    private final int newColumnIndex;
    private final SortDirection newDirection;

    public TableSortChangedEvent(Widget target,
                                 int oldColumnIndex,
                                 SortDirection oldDirection,
                                 int newColumnIndex,
                                 SortDirection newDirection) {
        this.target = Objects.requireNonNull(target, "target");
        this.oldColumnIndex = oldColumnIndex;
        this.oldDirection = oldDirection == null ? SortDirection.NONE : oldDirection;
        this.newColumnIndex = newColumnIndex;
        this.newDirection = newDirection == null ? SortDirection.NONE : newDirection;
    }

    @Override
    public EventType<TableSortChangedEvent> type() {
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

    public int oldColumnIndex() {
        return oldColumnIndex;
    }

    public SortDirection oldDirection() {
        return oldDirection;
    }

    public int newColumnIndex() {
        return newColumnIndex;
    }

    public SortDirection newDirection() {
        return newDirection;
    }
}
