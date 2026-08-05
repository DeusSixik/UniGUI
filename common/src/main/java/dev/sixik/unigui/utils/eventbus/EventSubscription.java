package dev.sixik.unigui.utils.eventbus;

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
