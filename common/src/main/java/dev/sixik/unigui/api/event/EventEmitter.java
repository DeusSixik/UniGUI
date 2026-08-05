package dev.sixik.unigui.api.event;

public interface EventEmitter {
    <T extends Event> EventSubscription on(EventType<T> type, EventListener<? super T> listener);

    void emit(Event event);
}
