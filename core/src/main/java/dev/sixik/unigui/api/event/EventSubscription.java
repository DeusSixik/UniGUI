package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Дескриптор подписки на событие. Реализует {@link AutoCloseable} для use в try-with-resources.
 *
 * <p>Вызов {@link #unsubscribe()} или {@link #close()} снимает подписку.</p>
 */
@AiDoc(
        summary = "Handle returned by EventEmitter.on() to unsubscribe a listener.",
        purpose = "Allows the caller to remove a specific listener registration. Implements AutoCloseable "
                + "so subscriptions can be managed with try-with-resources or EventScope.",
        useWhen = {
                "Store the returned subscription and call `unsubscribe()` when the listener is no longer needed.",
                "Use in try-with-resources: `try (var sub = emitter.on(...)) { ... }`."
        },
        criticalRules = {
                "Calling `unsubscribe()` or `close()` more than once is safe and idempotent.",
                "After unsubscribe, the listener will not receive any further events."
        },
        related = {"EventEmitter", "EventListener", "EventScope"})
public interface EventSubscription extends AutoCloseable {
    void unsubscribe();

    @Override
    default void close() {
        unsubscribe();
    }
}
