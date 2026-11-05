package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class ScrollBarValueChangedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<ScrollBarValueChangedEvent> TYPE = EventType.create("scrollbar.value_changed");

    private final Widget target;
    private final float oldValue;
    private final float newValue;

    public ScrollBarValueChangedEvent(Widget target, float oldValue, float newValue) {
        this.target = Objects.requireNonNull(target, "target");
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public EventType<ScrollBarValueChangedEvent> type() {
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

    public float oldValue() {
        return oldValue;
    }

    public float newValue() {
        return newValue;
    }
}
