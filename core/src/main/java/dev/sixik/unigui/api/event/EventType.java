package dev.sixik.unigui.api.event;

import dev.sixik.unigui.utils.eventbus.FastEventBus;

import java.util.Objects;

public final class EventType<T extends Event> {
    private final String id;
    private final FastEventBus.EventType<T> fastType;

    private EventType(String id) {
        this.id = Objects.requireNonNull(id, "id");
        this.fastType = new FastEventBus.EventType<>(id);
    }

    public static <T extends Event> EventType<T> create(String id) {
        return new EventType<>(id);
    }

    public String id() {
        return id;
    }

    /**
     * Internal bridge to the FastUtils event infrastructure.
     * Public event contracts should keep using {@link EventType}; emitters may use this for zero-map dispatch.
     */
    public FastEventBus.EventType<T> fastType() {
        return fastType;
    }

    @Override
    public String toString() {
        return id;
    }
}
