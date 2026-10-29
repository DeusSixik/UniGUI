package dev.sixik.unigui.api.event;

public interface EventEmitter {
    <T extends Event> EventSubscription on(EventType<T> type, EventListener<? super T> listener);

    /**
     * Подписывает слушателя на событие с возможностью получать его даже тогда, когда оно уже погашено
     * ({@link Event#isCancelled()}).
     *
     * @param type             тип события
     * @param receiveCancelled true, если слушатель должен вызываться даже для отменённых событий
     * @param listener         слушатель
     * @return дескриптор отписки
     */
    default <T extends Event> EventSubscription on(EventType<T> type, boolean receiveCancelled, EventListener<? super T> listener) {
        return on(type, listener);
    }

    void emit(Event event);
}
