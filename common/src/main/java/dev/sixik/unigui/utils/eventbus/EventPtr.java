package dev.sixik.unigui.utils.eventbus;

public class EventPtr<T> {

    protected final DODEventBus.EventType<T> eventType;

    public EventPtr(DODEventBus.EventType<T> eventType) {
        this.eventType = eventType;
    }

    public EventSubscription subscribe(DODEventBus.EventListener<T> listener) {
        return DODEventBus.DEFAULT_BUS.subscribe(eventType, listener);
    }

    public void unsubscribe(DODEventBus.EventListener<T> listener) {
        DODEventBus.DEFAULT_BUS.unsubscribe(eventType, listener);
    }
}
