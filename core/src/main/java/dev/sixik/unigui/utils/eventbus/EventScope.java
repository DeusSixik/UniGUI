package dev.sixik.unigui.utils.eventbus;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Контейнер для группового управления жизненным циклом подписок на события.
 *
 * <p>Реализует {@link AutoCloseable}, позволяя автоматически отписывать все зарегистрированные слушатели
 * при закрытии экрана, уничтожении виджета или выходе из контекста.</p>
 */
@AiDoc(
        summary = "Scoped collector for EventSubscriptions that unregisters all listeners when closed or cleared.",
        purpose = "Prevents listener leaks in screens, controllers, and short-lived UI components. "
                + "Listeners registered via listen() are tracked and unsubscribed in reverse order when close() or clear() is called.",
        useWhen = {
                "Manage lifecycle of subscriptions in a screen, dialog, or controller.",
                "Unsubscribe multiple event listeners in bulk during widget teardown.",
                "Use EventScope.noop() to pass a disabled scope when subscriptions are optional."
        },
        criticalRules = {
                "Calling close() marks the scope closed and unsubscribes all registered listeners.",
                "Registering listeners on a closed or noop scope returns EventSubscription.NOOP.",
                "All mutations (listen, clear, close) are synchronized and thread-safe."
        },
        related = {"FastEventBus", "EventPtr", "EventSubscription", "dev.sixik.unigui.api.event.EventEmitter"},
        examples = {
                @AiDoc.Example(
                        title = "Scoped subscriptions in a screen",
                        order = 10,
                        code = """
                                try (EventScope scope = new EventScope()) {
                                    scope.listen(USER_LOGGED_IN, user -> updateAvatar(user));
                                    scope.listen(SETTINGS_CHANGED, s -> applySettings(s));
                                    // when scope closes (e.g. on screen exit), all subscriptions are automatically removed
                                }
                                """)
        })
@WikiPage(
        title = "<key:event.scope.title>",
        slug = "event-scope",
        category = "<key:event.scope.category>",
        summary = "<key:event.scope.summary>",
        order = 30)
@WikiSection(title = "<key:event.scope.section.overview.title>",
        markdown = "<key:event.scope.section.overview.body>", order = 10)
@WikiSection(title = "<key:event.scope.section.lifecycle.title>",
        markdown = "<key:event.scope.section.lifecycle.body>", order = 20)
@WikiExample(title = "<key:event.scope.example.screen.title>",
        code = "<key:event.scope.example.screen.code>", order = 10)
@WikiLink(label = "FastEventBus", target = "page:fast-event-bus")
@WikiLink(label = "EventEmitter", target = "page:event-emitter")
public final class EventScope implements AutoCloseable {

    private final List<EventSubscription> subscriptions = new ObjectArrayList<>();
    private final boolean enabled;
    private boolean closed;

    public EventScope() {
        this(true);
    }

    private EventScope(boolean enabled) {
        this.enabled = enabled;
    }

    @WikiMethod(
            title = "<key:event.scope.method.noop.title>",
            summary = "<key:event.scope.method.noop.summary>",
            order = 10)
    @WikiReturn("<key:event.scope.method.noop.return>")
    @AiDoc(
            section = "Factory",
            sectionOrder = 10,
            order = 10,
            summary = "Returns a no-op scope that ignores all registrations.")
    public static EventScope noop() {
        return new EventScope(false);
    }

    @WikiMethod(
            title = "<key:event.scope.method.listen.title>",
            summary = "<key:event.scope.method.listen.summary>",
            order = 20)
    @WikiReturn("<key:event.scope.method.listen.return>")
    @AiDoc(
            section = "Subscription",
            sectionOrder = 20,
            order = 10,
            summary = "Subscribes a listener to the event and tracks the subscription for cleanup.")
    public synchronized <T> EventSubscription listen(
            @WikiParameter(description = "<key:event.scope.method.listen.param.event>") EventPtr<T> event,
            @WikiParameter(description = "<key:event.scope.method.listen.param.listener>") FastEventBus.EventListener<T> listener) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(listener, "listener");

        if (!enabled || closed) {
            return EventSubscription.NOOP;
        }

        EventSubscription subscription = event.subscribe(listener);
        subscriptions.add(subscription);
        return subscription;
    }

    @WikiMethod(
            title = "<key:event.scope.method.clear.title>",
            summary = "<key:event.scope.method.clear.summary>",
            order = 30)
    @AiDoc(
            section = "Lifecycle",
            sectionOrder = 30,
            order = 10,
            summary = "Unsubscribes all currently tracked subscriptions without permanently closing the scope.")
    public synchronized void clear() {
        for (int i = subscriptions.size() - 1; i >= 0; i--) {
            subscriptions.get(i).close();
        }
        subscriptions.clear();
    }

    @WikiMethod(
            title = "<key:event.scope.method.close.title>",
            summary = "<key:event.scope.method.close.summary>",
            order = 40)
    @AiDoc(
            section = "Lifecycle",
            sectionOrder = 30,
            order = 20,
            summary = "Closes the scope, unsubscribes all listeners, and rejects future registrations.")
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }

        closed = true;
        clear();
    }
}
