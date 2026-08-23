package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

/** Routed event отпускания клавиши. */
public final class KeyReleasedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<KeyReleasedEvent> TYPE = EventType.create("key.released");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final int keyCode;
    private final int scanCode;
    private final int modifiers;

    public KeyReleasedEvent(Widget target, int keyCode, int scanCode, int modifiers) {
        this(target, target, EventPhase.TARGET, keyCode, scanCode, modifiers);
    }

    public KeyReleasedEvent(Widget target, Widget currentTarget, EventPhase phase, int keyCode, int scanCode, int modifiers) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.keyCode = keyCode;
        this.scanCode = scanCode;
        this.modifiers = modifiers;
    }

    @Override
    public EventType<KeyReleasedEvent> type() {
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

    public int keyCode() {
        return keyCode;
    }

    public int scanCode() {
        return scanCode;
    }

    public int modifiers() {
        return modifiers;
    }

    @Override
    public KeyReleasedEvent routeTo(Widget currentTarget, EventPhase phase) {
        KeyReleasedEvent event = new KeyReleasedEvent(target, currentTarget, phase, keyCode, scanCode, modifiers);
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}