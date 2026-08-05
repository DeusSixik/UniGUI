package dev.sixik.unigui.api.event;

public interface Event {
    EventType<? extends Event> type();

    boolean isCancelled();

    void cancel();
}
