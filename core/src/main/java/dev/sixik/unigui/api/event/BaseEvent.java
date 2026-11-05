package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Стандартная реализация {@link Event} с поддержкой отмены.
 *
 * <p>Большинство конкретных событий наследуют этот класс вместо прямой реализации {@link Event}.</p>
 */
@AiDoc(
        summary = "Default abstract Event implementation with a boolean cancelled flag.",
        purpose = "Provides the standard `cancel()` / `isCancelled()` implementation so concrete event "
                + "classes only need to add their payload fields and `type()`.",
        useWhen = {
                "Extend to create a concrete event: override `type()` and add payload fields.",
                "Use instead of implementing Event directly to get cancellation for free."
        },
        criticalRules = {
                "Subclasses must override `type()` to return their static EventType constant.",
                "Cancellation is one-way and cannot be reversed."
        },
        related = {"Event", "EventType", "WidgetEvent", "PointerEvent"})
public abstract class BaseEvent implements Event {
    private boolean cancelled;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }
}
