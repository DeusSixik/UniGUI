package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

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
