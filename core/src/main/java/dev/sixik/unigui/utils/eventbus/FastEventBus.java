package dev.sixik.unigui.utils.eventbus;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Сверхбыстрый Lock-Free EventBus.
 * Не использует рефлексию, не использует хэш-мапы, не создает мусор при диспетчеризации.
 */
@AiDoc(
        summary = "Ultra-fast, lock-free, zero-allocation event bus without reflection or hash-maps.",
        purpose = "Provides high-performance event dispatching for the entire UI runtime. Events are indexed "
                + "by sequential integer IDs and listeners are stored in contiguous arrays for lock-free traversal.",
        useWhen = {
                "Dispatch high-frequency events where reflection or Map lookup would cause GC pressure or latency.",
                "Serve as the backend for UI event emitters and global system buses."
        },
        criticalRules = {
                "Dispatches lock-free using volatile array reads: no synchronization or locking during fire().",
                "Subscribing and unsubscribing use Copy-On-Write and are synchronized.",
                "Listener exceptions are caught and logged so one failing listener does not break others.",
                "EventType IDs are assigned sequentially starting from 0; capacity expands automatically."
        },
        related = {"EventScope", "EventPtr", "EventPtrInvoker", "EventSubscription", "dev.sixik.unigui.api.event.EventEmitter"},
        examples = {
                @AiDoc.Example(
                        title = "Subscribe and fire event",
                        order = 10,
                        code = """
                                FastEventBus.EventType<String> MESSAGE_EVENT = new FastEventBus.EventType<>("message");
                                FastEventBus bus = FastEventBus.DEFAULT_BUS;

                                EventSubscription sub = bus.subscribe(MESSAGE_EVENT, msg -> {
                                    System.out.println("Received: " + msg);
                                });

                                bus.fire(MESSAGE_EVENT, "Hello World!");
                                sub.unsubscribe();
                                """)
        })
@WikiPage(
        title = "<key:fast.event.bus.title>",
        slug = "fast-event-bus",
        category = "<key:fast.event.bus.category>",
        summary = "<key:fast.event.bus.summary>",
        order = 20)
@WikiSection(title = "<key:fast.event.bus.section.overview.title>",
        markdown = "<key:fast.event.bus.section.overview.body>", order = 10)
@WikiSection(title = "<key:fast.event.bus.section.architecture.title>",
        markdown = "<key:fast.event.bus.section.architecture.body>", order = 20)
@WikiSection(title = "<key:fast.event.bus.section.concurrency.title>",
        markdown = "<key:fast.event.bus.section.concurrency.body>", order = 30)
@WikiExample(title = "<key:fast.event.bus.example.basic.title>",
        code = "<key:fast.event.bus.example.basic.code>", order = 10)
@WikiExample(title = "<key:fast.event.bus.example.typed.title>",
        code = "<key:fast.event.bus.example.typed.code>", order = 20)
@WikiLink(label = "EventEmitter", target = "page:event-emitter")
@WikiLink(label = "EventScope", target = "page:event-scope")
public final class FastEventBus {

    public static final FastEventBus DEFAULT_BUS = new FastEventBus();
    private static final Logger LOGGER = LoggerFactory.getLogger(FastEventBus.class);

    /**
     * Глобальный генератор ID для типов событий.
     * Каждому новому типу события выдается свой уникальный индекс (0, 1, 2...).
     */
    private static final AtomicInteger EVENT_TYPE_ID_GENERATOR = new AtomicInteger(0);

    /**
     * Класс-маркер для события. Заменяет {@code Class<?>} и служит ключом.
     *
     * @param <T> Тип данных события
     */
    @AiDoc(
            summary = "Unique key token identifying an event type with a sequential integer index.",
            purpose = "Replaces Class<?> keys to achieve direct array indexing without hash lookups.")
    public static class EventType<T> {
        public final int id = EVENT_TYPE_ID_GENERATOR.getAndIncrement();
        public final String name;

        public EventType(String name) {
            this.name = name;
        }
    }

    /**
     * Функциональный интерфейс для слушателей (заменяет @Subscribe)
     */
    @AiDoc(
            summary = "Functional listener callback for handling FastEventBus events.",
            purpose = "Passed to subscribe() without requiring method reflection or annotations.")
    @FunctionalInterface
    public interface EventListener<T> {
        void onEvent(T event);
    }

    private volatile AtomicReferenceArray<EventListener<?>[]> subscribers = new AtomicReferenceArray<>(128);

    /**
     * Подписка на событие (Copy-On-Write)
     * Использует synchronized, так как подписки происходят редко (обычно при загрузке мира/мода).
     */
    @WikiMethod(
            title = "<key:fast.event.bus.method.subscribe.title>",
            summary = "<key:fast.event.bus.method.subscribe.summary>",
            order = 10)
    @WikiReturn("<key:fast.event.bus.method.subscribe.return>")
    @AiDoc(
            section = "Subscription",
            sectionOrder = 10,
            order = 10,
            summary = "Registers a listener for an event type using copy-on-write semantics.")
    @SuppressWarnings("unchecked")
    public synchronized <T> EventSubscription subscribe(
            @WikiParameter(description = "<key:fast.event.bus.method.subscribe.param.type>") EventType<T> type,
            @WikiParameter(description = "<key:fast.event.bus.method.subscribe.param.listener>") EventListener<T> listener) {
        ensureCapacity(type.id);

        final EventListener<T>[] currentListeners = (EventListener<T>[]) subscribers.get(type.id);

        if (currentListeners == null) {
            final EventListener<T>[] newListeners = new EventListener[]{listener};
            subscribers.set(type.id, newListeners);
        } else {
            final EventListener<T>[] newListeners = Arrays.copyOf(currentListeners, currentListeners.length + 1);
            newListeners[newListeners.length - 1] = listener;

            subscribers.set(type.id, newListeners);
        }

        return new EventSubscription() {
            private boolean closed;

            @Override
            public synchronized void unsubscribe() {
                if (closed) {
                    return;
                }

                closed = true;
                FastEventBus.this.unsubscribe(type, listener);
            }
        };
    }

    /**
     * Отписка от события (Copy-On-Write)
     */
    @WikiMethod(
            title = "<key:fast.event.bus.method.unsubscribe.title>",
            summary = "<key:fast.event.bus.method.unsubscribe.summary>",
            order = 20)
    @AiDoc(
            section = "Subscription",
            sectionOrder = 10,
            order = 20,
            summary = "Removes a listener using copy-on-write array shrinking.")
    @SuppressWarnings("unchecked")
    public synchronized <T> void unsubscribe(
            @WikiParameter(description = "<key:fast.event.bus.method.unsubscribe.param.type>") EventType<T> type,
            @WikiParameter(description = "<key:fast.event.bus.method.unsubscribe.param.listener>") EventListener<T> listener) {
        if (type.id >= subscribers.length()) return;

        final EventListener<T>[] currentListeners = (EventListener<T>[]) subscribers.get(type.id);
        if (currentListeners == null) return;

        int index = -1;
        for (int i = 0; i < currentListeners.length; i++) {
            if (currentListeners[i] == listener) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            if (currentListeners.length == 1) {
                subscribers.set(type.id, null);
            } else {
                EventListener<T>[] newListeners = new EventListener[currentListeners.length - 1];
                System.arraycopy(currentListeners, 0, newListeners, 0, index);
                System.arraycopy(currentListeners, index + 1, newListeners, index, currentListeners.length - index - 1);

                subscribers.set(type.id, newListeners);
            }
        }
    }

    /**
     * Вызов события (LockFree)
     */
    @WikiMethod(
            title = "<key:fast.event.bus.method.fire.title>",
            summary = "<key:fast.event.bus.method.fire.summary>",
            order = 30)
    @AiDoc(
            section = "Dispatch",
            sectionOrder = 20,
            order = 10,
            summary = "Dispatches an event lock-free with zero garbage allocation.")
    @SuppressWarnings("unchecked")
    public <T> void fire(
            @WikiParameter(description = "<key:fast.event.bus.method.fire.param.type>") EventType<T> type,
            @WikiParameter(description = "<key:fast.event.bus.method.fire.param.event>") T event) {
        final AtomicReferenceArray<EventListener<?>[]> subscribers = this.subscribers;
        if (type.id >= subscribers.length()) return;
        final EventListener<T>[] listeners = (EventListener<T>[]) subscribers.get(type.id);

        if (listeners != null) {
            for (int i = 0; i < listeners.length; i++) {
                try {
                    listeners[i].onEvent(event);
                } catch (RuntimeException exception) {
                    LOGGER.error("Failed to handle event {}", type.name, exception);
                }
            }
        }
    }

    /**
     * Безопасное увеличение размера массива типов событий,
     * если кто-то зарегистрирует больше 128 типов.
     */
    private void ensureCapacity(int neededId) {
        if (neededId >= subscribers.length()) {
            int newSize = Math.max(subscribers.length() * 2, neededId + 1);
            AtomicReferenceArray<EventListener<?>[]> newSubscribers = new AtomicReferenceArray<>(newSize);

            for (int i = 0; i < subscribers.length(); i++) {
                newSubscribers.set(i, subscribers.get(i));
            }

            this.subscribers = newSubscribers;
        }
    }
}
