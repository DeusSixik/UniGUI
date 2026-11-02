package dev.sixik.unigui.api.utils.reactive;

/**
 * Handle for a signal listener registration. Closing it prevents delivery of later changes.
 */
@FunctionalInterface
public interface SignalSubscription extends AutoCloseable {
    /**
     * Removes the listener. Repeated calls have no effect.
     */
    void unsubscribe();

    @Override
    default void close() {
        unsubscribe();
    }
}
