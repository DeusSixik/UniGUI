package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class TextChangedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<TextChangedEvent> TYPE = EventType.create("text.changed");

    private final Widget target;
    private final String oldText;
    private final String newText;

    public TextChangedEvent(Widget target, String oldText, String newText) {
        this.target = Objects.requireNonNull(target, "target");
        this.oldText = oldText == null ? "" : oldText;
        this.newText = newText == null ? "" : newText;
    }

    @Override
    public EventType<TextChangedEvent> type() {
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

    public String oldText() {
        return oldText;
    }

    public String newText() {
        return newText;
    }
}
