package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Отпускание payload над drop-зоной.
 *
 * <p>Событие доставляется напрямую зоне, которая последней приняла {@link DragOverEvent}.
 * К этому моменту указатель уже проверен в финальной точке отпускания.</p>
 */
public final class DragDropEvent extends DragRoutedEvent {
    public static final EventType<DragDropEvent> TYPE = EventType.create("dnd.drop");

    public DragDropEvent(Widget target, DragSession session) {
        this(target, target, EventPhase.TARGET, session);
    }

    public DragDropEvent(Widget target, Widget currentTarget, EventPhase phase, DragSession session) {
        super(TYPE, target, currentTarget, phase, session);
    }

    @Override
    public DragDropEvent routeTo(Widget currentTarget, EventPhase phase) {
        return inheritCancel(new DragDropEvent(target(), currentTarget, phase, session()));
    }
}
