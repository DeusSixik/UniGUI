package dev.sixik.unigui.api.interop;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.core.UiDispatcher;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.widget.Widget;

public interface WidgetExternHost {
    Widget widget();

    UIContext ui();

    UiDispatcher dispatcher();

    void invalidateLayout();

    void invalidateVisual();

    void invalidateTexture();

    void emit(Event event);
}
