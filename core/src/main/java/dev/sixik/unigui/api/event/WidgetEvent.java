package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

public interface WidgetEvent extends Event {
    Widget target();

    Widget currentTarget();

    EventPhase phase();
}
