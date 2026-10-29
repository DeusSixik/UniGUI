package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.widget.Widget;

public final class PointerReleasedEvent extends PointerEvent {
    public static final EventType<PointerReleasedEvent> TYPE = EventType.create("pointer.released");

    public PointerReleasedEvent(Widget target, float rootX, float rootY, float localX, float localY, int pointerId, PointerButton button) {
        this(target, target, EventPhase.TARGET, rootX, rootY, localX, localY, pointerId, button);
    }

    public PointerReleasedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                float rootX, float rootY, float localX, float localY, int pointerId, PointerButton button) {
        super(TYPE, target, currentTarget, phase, rootX, rootY, localX, localY, pointerId, button);
    }

    @Override
    public PointerReleasedEvent routeTo(Widget currentTarget, EventPhase phase) {
        PointerReleasedEvent event = new PointerReleasedEvent(target(), currentTarget, phase,
                rootX(), rootY(), localX(), localY(), pointerId(), button());
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}
