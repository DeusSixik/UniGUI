package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.widget.Widget;

public final class PointerEnteredEvent extends PointerEvent {
    public static final EventType<PointerEnteredEvent> TYPE = EventType.create("pointer.entered");

    public PointerEnteredEvent(Widget target, float rootX, float rootY, float localX, float localY, int pointerId) {
        this(target, target, EventPhase.TARGET, rootX, rootY, localX, localY, pointerId);
    }

    public PointerEnteredEvent(Widget target, Widget currentTarget, EventPhase phase,
                               float rootX, float rootY, float localX, float localY, int pointerId) {
        super(TYPE, target, currentTarget, phase, rootX, rootY, localX, localY, pointerId, PointerButton.UNKNOWN);
    }

    @Override
    public PointerEnteredEvent routeTo(Widget currentTarget, EventPhase phase) {
        PointerEnteredEvent event = new PointerEnteredEvent(target(), currentTarget, phase,
                rootX(), rootY(), localX(), localY(), pointerId());
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}
