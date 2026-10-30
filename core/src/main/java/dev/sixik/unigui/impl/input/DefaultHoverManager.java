package dev.sixik.unigui.impl.input;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.WidgetEvent;
import dev.sixik.unigui.api.input.HoverManager;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;

public final class DefaultHoverManager implements HoverManager {
    private Widget hoveredWidget;
    private float lastRootX;
    private float lastRootY;
    private float lastLocalX;
    private float lastLocalY;
    private int lastPointerId;

    @Override
    public Widget hoveredWidget() {
        return hoveredWidget;
    }

    @Override
    public void updateHover(Widget widget, float rootX, float rootY, float localX, float localY, int pointerId) {
        Widget next = hoverable(widget) ? widget : null;
        float previousLocalX = lastLocalX;
        float previousLocalY = lastLocalY;
        lastRootX = rootX;
        lastRootY = rootY;
        lastLocalX = localX;
        lastLocalY = localY;
        lastPointerId = pointerId;
        if (hoveredWidget == next) return;

        Widget previous = hoveredWidget;
        hoveredWidget = next;

        if (previous != null) {
            dispatch(previous, new PointerExitedEvent(previous, rootX, rootY, previousLocalX, previousLocalY, pointerId));
        }
        if (next != null) {
            dispatch(next, new PointerEnteredEvent(next, rootX, rootY, localX, localY, pointerId));
        }
    }

    @Override
    public void clearHover() {
        Widget previous = hoveredWidget;
        if (previous == null) return;
        hoveredWidget = null;
        dispatch(previous, new PointerExitedEvent(previous, lastRootX, lastRootY, lastLocalX, lastLocalY, lastPointerId));
    }

    private static boolean hoverable(Widget widget) {
        return widget != null && widget.visibility() == Visibility.VISIBLE && widget.enabled();
    }

    private static void dispatch(Widget target, WidgetEvent event) {
        UIContext context = target.uiContext();
        if (context == null) {
            target.handle(event);
        } else {
            context.routedEvents().dispatch(event);
        }
    }
}
