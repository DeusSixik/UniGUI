package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.ArrayList;
import java.util.List;

public final class CanvasWidget extends WidgetBase {
    private final List<CanvasDrawCallback> callbacks = new ArrayList<>();

    public CanvasWidget onDraw(CanvasDrawCallback callback) {
        if (callback != null) {
            callbacks.add(callback);
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    public void clearDrawCallbacks() {
        callbacks.clear();
        invalidate(InvalidationFlags.VISUAL);
    }

    @Override
    public void render(RenderContext context) {
        for (CanvasDrawCallback callback : List.copyOf(callbacks)) {
            callback.draw(context);
        }
    }
}
