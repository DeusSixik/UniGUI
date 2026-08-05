package dev.sixik.unigui.impl.event;

import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.RoutableWidgetEvent;
import dev.sixik.unigui.api.event.RoutedEventDispatcher;
import dev.sixik.unigui.api.event.WidgetEvent;
import dev.sixik.unigui.api.widget.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DefaultRoutedEventDispatcher implements RoutedEventDispatcher {
    public static final DefaultRoutedEventDispatcher INSTANCE = new DefaultRoutedEventDispatcher();

    private DefaultRoutedEventDispatcher() {
    }

    @Override
    public boolean dispatch(WidgetEvent event) {
        if (event == null || event.target() == null) {
            return false;
        }

        if (!(event instanceof RoutableWidgetEvent routable)) {
            event.target().handle(event);
            return event.isCancelled();
        }

        List<Widget> route = routeToRoot(event.target());
        if (route.isEmpty()) {
            return false;
        }

        dispatchCapture(routable, event, route);
        if (!event.isCancelled()) {
            dispatchTarget(routable, event);
        }
        if (!event.isCancelled()) {
            dispatchBubble(routable, event, route);
        }

        return event.isCancelled();
    }

    private static void dispatchCapture(RoutableWidgetEvent source, WidgetEvent rootEvent, List<Widget> route) {
        for (int index = 0; index < route.size() - 1; index++) {
            dispatchTo(source, rootEvent, route.get(index), EventPhase.CAPTURE);
            if (rootEvent.isCancelled()) {
                return;
            }
        }
    }

    private static void dispatchTarget(RoutableWidgetEvent source, WidgetEvent rootEvent) {
        dispatchTo(source, rootEvent, rootEvent.target(), EventPhase.TARGET);
    }

    private static void dispatchBubble(RoutableWidgetEvent source, WidgetEvent rootEvent, List<Widget> route) {
        for (int index = route.size() - 2; index >= 0; index--) {
            dispatchTo(source, rootEvent, route.get(index), EventPhase.BUBBLE);
            if (rootEvent.isCancelled()) {
                return;
            }
        }
    }

    private static void dispatchTo(RoutableWidgetEvent source, WidgetEvent rootEvent, Widget currentTarget, EventPhase phase) {
        if (rootEvent.isCancelled()) {
            return;
        }

        WidgetEvent routed = source.routeTo(currentTarget, phase);
        if (rootEvent.isCancelled()) {
            routed.cancel();
        }

        currentTarget.handle(routed);
        if (routed.isCancelled()) {
            rootEvent.cancel();
        }
    }

    private static List<Widget> routeToRoot(Widget target) {
        List<Widget> route = new ArrayList<>();
        Widget current = target;
        while (current != null) {
            route.add(current);
            current = current.parent();
        }
        Collections.reverse(route);
        return route;
    }
}
