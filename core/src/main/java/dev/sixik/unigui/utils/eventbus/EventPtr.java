package dev.sixik.unigui.utils.eventbus;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Указатель на тип события в {@link FastEventBus#DEFAULT_BUS}.
 *
 * <p>Служит удобным фасадом для подписки и отписки от глобального дефолтного шины событий.</p>
 */
@AiDoc(
        summary = "Handle wrapping a FastEventBus.EventType for convenient subscription on DEFAULT_BUS.",
        purpose = "Provides a type-safe token that binds to FastEventBus.DEFAULT_BUS, simplifying listener "
                + "subscription without explicitly referencing the global bus instance.",
        useWhen = {
                "Expose an event pointer constant in a module API.",
                "Subscribe directly via eventPtr.subscribe(listener) or pass to EventScope.listen()."
        },
        related = {"EventPtrInvoker", "FastEventBus", "EventScope"})
public class EventPtr<T> {

    protected final FastEventBus.EventType<T> eventType;

    public EventPtr(FastEventBus.EventType<T> eventType) {
        this.eventType = eventType;
    }

    public EventSubscription subscribe(FastEventBus.EventListener<T> listener) {
        return FastEventBus.DEFAULT_BUS.subscribe(eventType, listener);
    }

    public void unsubscribe(FastEventBus.EventListener<T> listener) {
        FastEventBus.DEFAULT_BUS.unsubscribe(eventType, listener);
    }
}
