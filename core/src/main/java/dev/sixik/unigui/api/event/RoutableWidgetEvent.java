package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Widget event that can create phase/current-target snapshots for routed dispatch.
 */
@AiDoc(
        summary = "WidgetEvent that can create phase/current-target snapshots for routed dispatch.",
        purpose = "RoutedEventDispatcher calls `routeTo()` to produce a shallow copy of the event "
                + "with updated `currentTarget` and `phase` as it walks the widget tree.",
        criticalRules = {
                "`routeTo()` must preserve cancellation state from the original event.",
                "The returned event shares the same `target()` but has a new `currentTarget` and `phase`."
        },
        related = {"WidgetEvent", "RoutedEventDispatcher", "PointerEvent", "EventPhase"})
public interface RoutableWidgetEvent extends WidgetEvent {
    WidgetEvent routeTo(Widget currentTarget, EventPhase phase);
}
