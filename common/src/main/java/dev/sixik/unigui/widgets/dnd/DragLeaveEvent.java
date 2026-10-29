package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Указатель покинул drop-зону или сессия отменена, пока зона была активной целью.
 */
public final class DragLeaveEvent extends DragRoutedEvent {
    public static final EventType<DragLeaveEvent> TYPE = EventType.create("dnd.leave");

    public DragLeaveEvent(Widget target, DragSession session) {
        this(target, target, EventPhase.TARGET, session);
    }

    public DragLeaveEvent(Widget target, Widget currentTarget, EventPhase phase, DragSession session) {
        super(TYPE, target, currentTarget, phase, session);
    }

    @Override
    public DragLeaveEvent routeTo(Widget currentTarget, EventPhase phase) {
        return inheritCancel(new DragLeaveEvent(target(), currentTarget, phase, session()));
    }
}
