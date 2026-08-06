package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class TableColumnMovedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<TableColumnMovedEvent> TYPE = EventType.create("table.column_moved");

    private final Widget target;
    private final int oldIndex;
    private final int newIndex;

    public TableColumnMovedEvent(Widget target, int oldIndex, int newIndex) {
        this.target = Objects.requireNonNull(target, "target");
        this.oldIndex = oldIndex;
        this.newIndex = newIndex;
    }

    @Override
    public EventType<TableColumnMovedEvent> type() {
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

    public int oldIndex() {
        return oldIndex;
    }

    public int newIndex() {
        return newIndex;
    }
}
