package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Функциональный интерфейс слушателя событий.
 *
 * @param <T> конкретный тип события
 */
@AiDoc(
        summary = "Functional listener interface for handling a single typed Event.",
        purpose = "Passed to `EventEmitter.on()` to receive events. Being a @FunctionalInterface, "
                + "it can be expressed as a lambda or method reference.",
        criticalRules = {
                "Listeners run synchronously on the thread that calls emit().",
                "Throwing from handle() does not prevent other listeners from running — FastEventBus catches and logs exceptions."
        },
        related = {"EventEmitter", "Event", "EventSubscription"})
@FunctionalInterface
public interface EventListener<T extends Event> {
    void handle(T event);
}
