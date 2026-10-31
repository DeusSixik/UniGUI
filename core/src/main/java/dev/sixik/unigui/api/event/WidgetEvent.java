package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Событие, привязанное к виджету в UI-дереве.
 *
 * <p>Предоставляет {@link #target()} (исходный виджет), {@link #currentTarget()} (виджет, обрабатывающий
 * событие на текущей фазе) и {@link #phase()} для routed dispatch.</p>
 */
@AiDoc(
        summary = "Event bound to a Widget: carries target, currentTarget, and the dispatch phase.",
        purpose = "Extended by all events that travel through the widget tree (pointer, focus, keyboard). "
                + "The three accessors let listeners distinguish the originator from the current handler.",
        criticalRules = {
                "`target()` is the widget where the event originated; `currentTarget()` changes during routing.",
                "`phase()` indicates CAPTURE, TARGET, or BUBBLE stage of dispatch."
        },
        related = {"Event", "Widget", "EventPhase", "RoutableWidgetEvent", "PointerEvent"})
public interface WidgetEvent extends Event {
    Widget target();

    Widget currentTarget();

    EventPhase phase();
}
