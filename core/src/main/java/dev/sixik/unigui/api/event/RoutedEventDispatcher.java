package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Диспетчер маршрутизируемых событий в иерархии виджетов.
 *
 * <p>Реализует обход дерева виджетов по фазам (CAPTURE, TARGET, BUBBLE) либо прямую доставку
 * целевому виджету ({@link #DIRECT}).</p>
 */
@AiDoc(
        summary = "Dispatches routed widget events across the UI tree or directly to targets.",
        purpose = "Coordinates tree traversal during routed event dispatch (capture down to target, "
                + "and bubble back up to root) or delivers directly via the DIRECT constant.",
        useWhen = {
                "Dispatch input/pointer/focus events across the hierarchy using routed phases.",
                "Use RoutedEventDispatcher.DIRECT for simple point-to-point dispatch without bubbling."
        },
        criticalRules = {
                "Returns true if the event was cancelled during dispatch, false otherwise.",
                "Null events or events with null targets are ignored safely."
        },
        related = {"WidgetEvent", "RoutableWidgetEvent", "EventPhase", "Widget"})
public interface RoutedEventDispatcher {
    RoutedEventDispatcher DIRECT = event -> {
        if (event == null || event.target() == null) {
            return false;
        }

        event.target().handle(event);
        return event.isCancelled();
    };

    boolean dispatch(WidgetEvent event);
}
