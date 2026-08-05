package dev.sixik.unigui.impl.input;

import dev.sixik.unigui.api.event.FocusGainedEvent;
import dev.sixik.unigui.api.event.FocusLostEvent;
import dev.sixik.unigui.api.input.FocusManager;
import dev.sixik.unigui.api.widget.Widget;

public final class DefaultFocusManager implements FocusManager {
    private Widget focusedWidget;

    @Override
    public Widget focusedWidget() {
        return focusedWidget;
    }

    @Override
    public void requestFocus(Widget widget) {
        if (focusedWidget == widget) return;

        Widget previous = focusedWidget;
        focusedWidget = widget;

        if (previous != null) {
            previous.handle(new FocusLostEvent(previous, widget));
        }
        if (widget != null) {
            widget.handle(new FocusGainedEvent(widget, previous));
        }
    }
}
