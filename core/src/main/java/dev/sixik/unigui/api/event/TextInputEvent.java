package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class TextInputEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<TextInputEvent> TYPE = EventType.create("text.input");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final int codePoint;
    private final int modifiers;

    public TextInputEvent(Widget target, int codePoint, int modifiers) {
        this(target, target, EventPhase.TARGET, codePoint, modifiers);
    }

    public TextInputEvent(Widget target, Widget currentTarget, EventPhase phase, int codePoint, int modifiers) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.codePoint = codePoint;
        this.modifiers = modifiers;
    }

    @Override
    public EventType<TextInputEvent> type() {
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

    public int codePoint() {
        return codePoint;
    }

    public int modifiers() {
        return modifiers;
    }

    @Override
    public TextInputEvent routeTo(Widget currentTarget, EventPhase phase) {
        TextInputEvent event = new TextInputEvent(target, currentTarget, phase, codePoint, modifiers);
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}
