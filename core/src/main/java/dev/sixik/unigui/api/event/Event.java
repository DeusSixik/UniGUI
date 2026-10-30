package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Базовый контракт любого события в UI-системе.
 *
 * <p>Каждое событие несёт свой {@link EventType} и поддерживает отмену: вызов {@link #cancel()}
 * позволяет прервать дальнейшую обработку.</p>
 */
@AiDoc(
        summary = "Base contract for all UI events: carries an EventType and supports cancellation.",
        purpose = "Implemented by every event in the system. Listeners check `isCancelled()` to short-circuit "
                + "processing; calling `cancel()` signals upstream handlers to stop dispatch.",
        useWhen = {
                "Define a new event type by implementing Event (or extending BaseEvent).",
                "Check or cancel an event inside a listener."
        },
        criticalRules = {
                "Every Event must return a non-null EventType from type().",
                "Once cancelled, isCancelled() returns true for all subsequent listeners unless they opt in via receiveCancelled.",
                "Cancellation is one-way: there is no uncancel()."
        },
        related = {"BaseEvent", "EventType", "EventEmitter", "EventListener"})
public interface Event {
    EventType<? extends Event> type();

    boolean isCancelled();

    void cancel();
}
