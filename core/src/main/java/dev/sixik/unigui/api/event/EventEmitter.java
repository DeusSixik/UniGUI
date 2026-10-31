package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Mixin-интерфейс для объектов, способных принимать подписки на типизированные события и рассылать их.
 *
 * <p>Каждый {@link dev.sixik.unigui.api.widget.Widget Widget} наследует этот контракт, что позволяет
 * подписываться на события ввода, фокуса и жизненного цикла единообразно.</p>
 */
@AiDoc(
        summary = "Mixin interface for any object that can subscribe listeners to typed events and dispatch them.",
        purpose = "Provides the subscribe/emit contract used by Widget and other observable components. "
                + "Listeners receive strongly-typed events and can be unsubscribed via the returned EventSubscription.",
        useWhen = {
                "Subscribe to widget events: `widget.on(ButtonClickEvent.TYPE, e -> ...)`.",
                "Dispatch a custom event to registered listeners with `emit(event)`.",
                "Receive cancelled events by passing `receiveCancelled = true` in the three-arg overload."
        },
        criticalRules = {
                "`on()` returns an EventSubscription; call `unsubscribe()` or `close()` to stop receiving events.",
                "`emit()` delivers the event to all matching listeners synchronously on the calling thread.",
                "The two-arg `on(type, listener)` skips cancelled events by default; use the three-arg overload to receive them.",
                "Implementations must be safe against listener removal during dispatch."
        },
        related = {"Event", "EventType", "EventListener", "EventSubscription", "Widget"},
        examples = {
                @AiDoc.Example(
                        title = "Subscribe to a button click",
                        order = 10,
                        code = """
                                widget.on(ButtonClickEvent.TYPE, e -> {
                                    System.out.println("Clicked!");
                                });
                                """),
                @AiDoc.Example(
                        title = "Emit a custom event",
                        order = 20,
                        code = "emitter.emit(new MyCustomEvent(target));")
        })
@WikiPage(
        title = "<key:event.emitter.title>",
        slug = "event-emitter",
        category = "<key:event.emitter.category>",
        summary = "<key:event.emitter.summary>",
        order = 10)
@WikiSection(title = "<key:event.emitter.section.overview.title>",
        markdown = "<key:event.emitter.section.overview.body>", order = 10)
@WikiSection(title = "<key:event.emitter.section.subscription.title>",
        markdown = "<key:event.emitter.section.subscription.body>", order = 20)
@WikiSection(title = "<key:event.emitter.section.cancelled.title>",
        markdown = "<key:event.emitter.section.cancelled.body>", order = 30)
@WikiExample(title = "<key:event.emitter.example.subscribe.title>",
        code = "<key:event.emitter.example.subscribe.code>", order = 10)
@WikiExample(title = "<key:event.emitter.example.emit.title>",
        code = "<key:event.emitter.example.emit.code>", order = 20)
@WikiLink(label = "Widget", target = "page:widget")
@WikiLink(label = "FastEventBus", target = "page:fast-event-bus")
public interface EventEmitter {

    @WikiMethod(
            title = "<key:event.emitter.method.on.title>",
            summary = "<key:event.emitter.method.on.summary>",
            order = 10)
    @WikiReturn("<key:event.emitter.method.on.return>")
    @AiDoc(
            section = "Subscription",
            sectionOrder = 10,
            order = 10,
            summary = "Subscribes a listener to the specified event type.")
    <T extends Event> EventSubscription on(
            @WikiParameter(description = "<key:event.emitter.method.on.param.type>") EventType<T> type,
            @WikiParameter(description = "<key:event.emitter.method.on.param.listener>") EventListener<? super T> listener);

    /**
     * Подписывает слушателя на событие с возможностью получать его даже тогда, когда оно уже погашено
     * ({@link Event#isCancelled()}).
     *
     * @param type             тип события
     * @param receiveCancelled true, если слушатель должен вызываться даже для отменённых событий
     * @param listener         слушатель
     * @return дескриптор отписки
     */
    @WikiMethod(
            title = "<key:event.emitter.method.on_cancelled.title>",
            summary = "<key:event.emitter.method.on_cancelled.summary>",
            order = 20)
    @WikiReturn("<key:event.emitter.method.on.return>")
    @AiDoc(
            section = "Subscription",
            sectionOrder = 10,
            order = 20,
            summary = "Subscribes a listener with an option to receive cancelled events.")
    default <T extends Event> EventSubscription on(
            @WikiParameter(description = "<key:event.emitter.method.on.param.type>") EventType<T> type,
            @WikiParameter(description = "<key:event.emitter.method.on.param.receive_cancelled>") boolean receiveCancelled,
            @WikiParameter(description = "<key:event.emitter.method.on.param.listener>") EventListener<? super T> listener) {
        return on(type, listener);
    }

    @WikiMethod(
            title = "<key:event.emitter.method.emit.title>",
            summary = "<key:event.emitter.method.emit.summary>",
            order = 30)
    @AiDoc(
            section = "Dispatch",
            sectionOrder = 20,
            order = 10,
            summary = "Emits an event to all registered listeners.")
    void emit(@WikiParameter(description = "<key:event.emitter.method.emit.param.event>") Event event);
}
