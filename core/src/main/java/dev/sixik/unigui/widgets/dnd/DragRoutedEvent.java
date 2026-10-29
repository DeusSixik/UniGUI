package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.event.BaseEvent;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.event.RoutableWidgetEvent;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

/**
 * Базовый routed-event drag-and-drop протокола.
 *
 * <p>Все события несут {@link DragSession}: payload, позицию указателя и источник читаются из неё,
 * поэтому копии события при маршрутизации (capture/target/bubble) остаются лёгкими.</p>
 */
public abstract class DragRoutedEvent extends BaseEvent implements RoutableWidgetEvent {
    private final EventType<? extends Event> type;
    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final DragSession session;

    protected DragRoutedEvent(EventType<? extends Event> type, Widget target, Widget currentTarget,
                              EventPhase phase, DragSession session) {
        this.type = Objects.requireNonNull(type, "type");
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.session = Objects.requireNonNull(session, "session");
    }

    @Override
    public EventType<? extends Event> type() {
        return type;
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

    /**
     * @return активная drag-сессия
     */
    public DragSession session() {
        return session;
    }

    /**
     * @return payload перетаскиваемого объекта
     */
    public DragPayload payload() {
        return session.payload();
    }

    /**
     * @return X указателя в координатах корня
     */
    public float rootX() {
        return session.pointerX();
    }

    /**
     * @return Y указателя в координатах корня
     */
    public float rootY() {
        return session.pointerY();
    }

    /**
     * Копирует состояние отмены в маршрутизированный экземпляр.
     */
    protected <T extends DragRoutedEvent> T inheritCancel(T routed) {
        if (isCancelled()) routed.cancel();
        return routed;
    }
}
