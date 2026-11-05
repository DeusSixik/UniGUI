package dev.sixik.unigui.utils.eventbus;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Расширение {@link EventPtr}, позволяющее как подписываться, так и вызывать (диспатчить) событие.
 */
@AiDoc(
        summary = "EventPtr extension that enables firing/invoking the event on DEFAULT_BUS.",
        purpose = "Used by event publishers to hold both subscription and invocation capabilities in a single handle.",
        useWhen = {
                "Define an event endpoint where the owning component needs to trigger the event via invoke()."
        },
        related = {"EventPtr", "FastEventBus", "EventScope"})
public class EventPtrInvoker<T> extends EventPtr<T> {

    public EventPtrInvoker(FastEventBus.EventType<T> eventType) {
        super(eventType);
    }

    public void invoke(T event) {
        FastEventBus.DEFAULT_BUS.fire(eventType, event);
    }
}
