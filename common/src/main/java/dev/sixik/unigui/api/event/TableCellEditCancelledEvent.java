package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class TableCellEditCancelledEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<TableCellEditCancelledEvent> TYPE = EventType.create("table.cell_edit_cancelled");

    private final Widget target;
    private final int row;
    private final int column;
    private final String text;

    public TableCellEditCancelledEvent(Widget target, int row, int column, String text) {
        this.target = Objects.requireNonNull(target, "target");
        this.row = row;
        this.column = column;
        this.text = text == null ? "" : text;
    }

    @Override
    public EventType<TableCellEditCancelledEvent> type() {
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

    public String text() {
        return text;
    }
}
