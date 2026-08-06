package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class TableCellEditCommittedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<TableCellEditCommittedEvent> TYPE = EventType.create("table.cell_edit_committed");

    private final Widget target;
    private final int row;
    private final int column;
    private final String oldText;
    private final String newText;

    public TableCellEditCommittedEvent(Widget target, int row, int column, String oldText, String newText) {
        this.target = Objects.requireNonNull(target, "target");
        this.row = row;
        this.column = column;
        this.oldText = oldText == null ? "" : oldText;
        this.newText = newText == null ? "" : newText;
    }

    @Override
    public EventType<TableCellEditCommittedEvent> type() {
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

    public int row() {
        return row;
    }

    public int column() {
        return column;
    }

    public String oldText() {
        return oldText;
    }

    public String newText() {
        return newText;
    }
}
