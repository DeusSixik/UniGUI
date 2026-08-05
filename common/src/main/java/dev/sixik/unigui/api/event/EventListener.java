package dev.sixik.unigui.api.event;

@FunctionalInterface
public interface EventListener<T extends Event> {
    void handle(T event);
}
