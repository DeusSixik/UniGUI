package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Зондирование drop-зоны: отправляется виджету под указателем на каждое движение и каждый тик
 * drag-сессии и всплывает вверх по дереву.
 *
 * <p>Drop-зона, готовая принять payload, вызывает {@link #accept(Widget)}. Это отмечает её
 * активной целью сессии и останавливает всплытие, поэтому побеждает ближайшая к указателю зона.
 * Зона, которая payload не принимает, ничего не делает, и событие уходит к родителям.</p>
 */
public final class DragOverEvent extends DragRoutedEvent {
    public static final EventType<DragOverEvent> TYPE = EventType.create("dnd.over");

    public DragOverEvent(Widget target, DragSession session) {
        this(target, target, EventPhase.TARGET, session);
    }

    public DragOverEvent(Widget target, Widget currentTarget, EventPhase phase, DragSession session) {
        super(TYPE, target, currentTarget, phase, session);
    }

    /**
     * Объявляет зону активной целью drop и останавливает всплытие.
     *
     * @param zone виджет-drop-зона, который получит {@link DragDropEvent} при отпускании
     */
    public void accept(Widget zone) {
        session().acceptDropTarget(zone);
        cancel();
    }

    @Override
    public DragOverEvent routeTo(Widget currentTarget, EventPhase phase) {
        return inheritCancel(new DragOverEvent(target(), currentTarget, phase, session()));
    }
}
