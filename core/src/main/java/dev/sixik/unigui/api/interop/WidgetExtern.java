package dev.sixik.unigui.api.interop;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.event.Event;

public interface WidgetExtern {
    default void onAttach(WidgetExternHost host) {
    }

    default void onDetach() {
    }

    default void measure(ExternMeasureContext context) {
    }

    default void render(ExternRenderContext context) {
    }

    default void handle(Event event) {
    }

    default void tick(FrameContext frame) {
    }

    default void dispose() {
    }
}
