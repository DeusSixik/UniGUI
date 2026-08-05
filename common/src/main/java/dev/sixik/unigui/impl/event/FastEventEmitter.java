package dev.sixik.unigui.impl.event;

import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventEmitter;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.utils.eventbus.FastEventBus;

public class FastEventEmitter implements EventEmitter {
    private final FastEventBus bus;

    public FastEventEmitter() {
        this(new FastEventBus());
    }

    public FastEventEmitter(FastEventBus bus) {
        this.bus = bus == null ? new FastEventBus() : bus;
    }

    public FastEventBus bus() {
        return bus;
    }

    @Override
    public <T extends Event> EventSubscription on(EventType<T> type, EventListener<? super T> listener) {
        FastEventBus.EventListener<T> fastListener = event -> {
            if (!event.isCancelled()) {
                listener.handle(event);
            }
        };
        dev.sixik.unigui.utils.eventbus.EventSubscription subscription = bus.subscribe(type.fastType(), fastListener);
        return subscription::unsubscribe;
    }

    @Override
    public void emit(Event event) {
        emitTyped(event);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends Event> void emitTyped(Event event) {
        EventType type = event.type();
        bus.fire(type.fastType(), event);
    }
}
