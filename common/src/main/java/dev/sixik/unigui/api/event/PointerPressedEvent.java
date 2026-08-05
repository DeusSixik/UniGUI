package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.widget.Widget;

public final class PointerPressedEvent extends PointerEvent {
    public static final EventType<PointerPressedEvent> TYPE = EventType.create("pointer.pressed");

    public PointerPressedEvent(Widget target, float rootX, float rootY, float localX, float localY, int pointerId, PointerButton button) {
        this(target, target, EventPhase.TARGET, rootX, rootY, localX, localY, pointerId, button);
    }

    public PointerPressedEvent(Widget target, Widget currentTarget, EventPhase phase,
                               float rootX, float rootY, float localX, float localY, int pointerId, PointerButton button) {
        super(TYPE, target, currentTarget, phase, rootX, rootY, localX, localY, pointerId, button);
    }

    @Override
    public PointerPressedEvent routeTo(Widget currentTarget, EventPhase phase) {
        PointerPressedEvent event = new PointerPressedEvent(target(), currentTarget, phase,
                rootX(), rootY(), localX(), localY(), pointerId(), button());
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}
