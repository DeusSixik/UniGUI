package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Фаза доставки routed-события в UI-дереве.
 *
 * <p>{@code CAPTURE} идёт от root к target, {@code TARGET} — на самом целевом виджете,
 * {@code BUBBLE} — обратно вверх к root.</p>
 */
@AiDoc(
        summary = "Phase of a routed widget event: CAPTURE (root→target), TARGET, BUBBLE (target→root).",
        purpose = "Used by WidgetEvent.phase() and RoutedEventDispatcher to distinguish the direction "
                + "of event propagation through the widget tree.",
        criticalRules = {
                "CAPTURE fires from root toward target; BUBBLE fires from target back to root.",
                "Most listeners subscribe at BUBBLE or TARGET phase.",
                "Cancelling an event during CAPTURE prevents it from reaching TARGET and BUBBLE."
        },
        related = {"WidgetEvent", "RoutableWidgetEvent", "RoutedEventDispatcher"})
public enum EventPhase {
    CAPTURE,
    TARGET,
    BUBBLE
}
