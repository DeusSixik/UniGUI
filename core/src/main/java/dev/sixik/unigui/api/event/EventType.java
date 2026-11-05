package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.utils.eventbus.FastEventBus;

import java.util.Objects;

/**
 * Строго типизированный маркер типа события.
 *
 * <p>Каждый конкретный класс события объявляет статическое поле {@code TYPE}, например:
 * {@code public static final EventType<ButtonClickEvent> TYPE = EventType.create("button.click");}.</p>
 */
@AiDoc(
        summary = "Strongly-typed event type marker used to register listeners and dispatch events.",
        purpose = "Each concrete event class declares a `public static final EventType<T> TYPE` field. "
                + "EventEmitter.on() uses it to match listeners to dispatched events with zero-map lookup.",
        useWhen = {
                "Declare a new event: `public static final EventType<MyEvent> TYPE = EventType.create(\"my.event\");`.",
                "Subscribe: `widget.on(MyEvent.TYPE, listener)`."
        },
        criticalRules = {
                "Create instances only via `EventType.create(id)` — the constructor is private.",
                "The `id` string must be non-null and is used for debugging and logging.",
                "Each EventType wraps a FastEventBus.EventType for zero-map dispatch."
        },
        related = {"Event", "EventEmitter", "FastEventBus"},
        examples = @AiDoc.Example(
                title = "Declare a custom event type",
                code = "public static final EventType<MyEvent> TYPE = EventType.create(\"my.event\");"))
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
