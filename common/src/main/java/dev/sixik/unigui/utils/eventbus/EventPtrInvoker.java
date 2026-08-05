package dev.sixik.unigui.utils.eventbus;

public class EventPtrInvoker<T> extends EventPtr<T> {

    public EventPtrInvoker(FastEventBus.EventType<T> eventType) {
        super(eventType);
    }

    public void invoke(T event) {
        FastEventBus.DEFAULT_BUS.fire(eventType, event);
    }
}
