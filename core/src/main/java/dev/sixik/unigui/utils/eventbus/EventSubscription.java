package dev.sixik.unigui.utils.eventbus;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Дескриптор подписки в {@link FastEventBus}.
 */
@AiDoc(
        summary = "Subscription handle returned by FastEventBus and EventScope for cancellation.",
        purpose = "Allows listeners to unregister cleanly from FastEventBus. Implements AutoCloseable.",
        related = {"FastEventBus", "EventScope"})
@FunctionalInterface
public interface EventSubscription extends AutoCloseable {

    EventSubscription NOOP = () -> {
    };

    void unsubscribe();

    @Override
    default void close() {
        unsubscribe();
    }
}
