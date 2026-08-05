package dev.sixik.unigui.api.widget;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventEmitter;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.RenderContext;

import java.util.List;

public interface Widget extends EventEmitter {
    UIContext uiContext();

    Widget parent();

    List<Widget> children();

    RectView layoutBounds();

    Transform transform();

    int invalidationFlags();

    default int subtreeInvalidationFlags() {
        int flags = invalidationFlags();
        for (Widget child : children()) {
            flags |= child.subtreeInvalidationFlags();
        }
        return flags;
    }

    void invalidate(int flags);

    void clearInvalidation(int flags);

    void measure(LayoutContext context);

    void arrange(RectView bounds);

    void render(RenderContext context);

    void handle(Event event);

    default void tick(FrameContext frame) {
    }

    default void dispose() {
    }
}
