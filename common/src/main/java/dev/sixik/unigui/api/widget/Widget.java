package dev.sixik.unigui.api.widget;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventEmitter;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.Style;

import java.util.List;

public interface Widget extends EventEmitter {
    UIContext uiContext();

    Widget parent();

    List<Widget> children();

    RectView layoutBounds();

    default LayoutSize desiredSize() {
        return LayoutSize.ZERO;
    }

    Transform transform();

    int invalidationFlags();

    default LayoutConstraints layoutConstraints() {
        return LayoutConstraints.DEFAULT;
    }

    default Visibility visibility() {
        return Visibility.VISIBLE;
    }

    default boolean visible() {
        return visibility() == Visibility.VISIBLE;
    }

    default boolean enabled() {
        return true;
    }

    default boolean hovered() {
        return false;
    }

    /** Returns the cursor to display at the supplied widget-local coordinates. */
    default MouseCursor mouseCursorAt(float localX, float localY) {
        return MouseCursor.DEFAULT;
    }

    default boolean focusable() {
        return false;
    }

    default boolean focusScope() {
        return false;
    }

    default int focusOrder() {
        return 0;
    }

    default boolean styleScope() {
        return false;
    }

    default Style localStyle(String widgetType) {
        return Style.EMPTY;
    }

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
