package dev.sixik.unigui.api.event;

public interface EventSubscription extends AutoCloseable {
    void unsubscribe();

    @Override
    default void close() {
        unsubscribe();
    }
}
