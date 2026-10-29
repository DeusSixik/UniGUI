package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.widget.Widget;

public final class PointerExitedEvent extends PointerEvent {
    public static final EventType<PointerExitedEvent> TYPE = EventType.create("pointer.exited");

    public PointerExitedEvent(Widget target, float rootX, float rootY, float localX, float localY, int pointerId) {
        this(target, target, EventPhase.TARGET, rootX, rootY, localX, localY, pointerId);
    }

    public PointerExitedEvent(Widget target, Widget currentTarget, EventPhase phase,
                              float rootX, float rootY, float localX, float localY, int pointerId) {
        super(TYPE, target, currentTarget, phase, rootX, rootY, localX, localY, pointerId, PointerButton.UNKNOWN);
    }

    @Override
    public PointerExitedEvent routeTo(Widget currentTarget, EventPhase phase) {
        PointerExitedEvent event = new PointerExitedEvent(target(), currentTarget, phase,
                rootX(), rootY(), localX(), localY(), pointerId());
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}
