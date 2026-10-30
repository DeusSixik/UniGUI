package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.widget.Widget;

public final class PointerMovedEvent extends PointerEvent {
    public static final EventType<PointerMovedEvent> TYPE = EventType.create("pointer.moved");

    public PointerMovedEvent(Widget target, float rootX, float rootY, float localX, float localY, int pointerId) {
        this(target, target, EventPhase.TARGET, rootX, rootY, localX, localY, pointerId);
    }

    public PointerMovedEvent(Widget target, Widget currentTarget, EventPhase phase,
                             float rootX, float rootY, float localX, float localY, int pointerId) {
        super(TYPE, target, currentTarget, phase, rootX, rootY, localX, localY, pointerId, PointerButton.UNKNOWN);
    }

    @Override
    public PointerMovedEvent routeTo(Widget currentTarget, EventPhase phase) {
        PointerMovedEvent event = new PointerMovedEvent(target(), currentTarget, phase,
                rootX(), rootY(), localX(), localY(), pointerId());
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}
