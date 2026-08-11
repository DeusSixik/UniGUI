package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class ContextMenuItemSelectedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<ContextMenuItemSelectedEvent> TYPE = EventType.create("context_menu.item_selected");

    private final Widget target;
    private final int index;
    private final String text;

    public ContextMenuItemSelectedEvent(Widget target, int index, String text) {
        this.target = Objects.requireNonNull(target, "target");
        this.index = index;
        this.text = text == null ? "" : text;
    }

    @Override
    public EventType<ContextMenuItemSelectedEvent> type() {
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

    public int index() {
        return index;
    }

    public String text() {
        return text;
    }
}
