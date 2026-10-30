package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class TableColumnResizedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<TableColumnResizedEvent> TYPE = EventType.create("table.column_resized");

    private final Widget target;
    private final int column;
    private final float oldWidth;
    private final float newWidth;

    public TableColumnResizedEvent(Widget target, int column, float oldWidth, float newWidth) {
        this.target = Objects.requireNonNull(target, "target");
        this.column = column;
        this.oldWidth = oldWidth;
        this.newWidth = newWidth;
    }

    @Override
    public EventType<TableColumnResizedEvent> type() {
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

    public int column() {
        return column;
    }

    public float oldWidth() {
        return oldWidth;
    }

    public float newWidth() {
        return newWidth;
    }
}
