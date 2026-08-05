package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

/**
 * Widget event that can create phase/current-target snapshots for routed dispatch.
 */
public interface RoutableWidgetEvent extends WidgetEvent {
    WidgetEvent routeTo(Widget currentTarget, EventPhase phase);
}
