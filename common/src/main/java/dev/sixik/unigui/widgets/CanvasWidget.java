package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.impl.widget.WidgetBase;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;

public final class CanvasWidget extends WidgetBase {
    private final List<CanvasDrawCallback> callbacks = new ObjectArrayList<>();
    private CanvasDrawCallback[] callbackSnapshot = new CanvasDrawCallback[0];
    private boolean callbackSnapshotDirty = true;

    public CanvasWidget onDraw(CanvasDrawCallback callback) {
        if (callback != null) {
            callbacks.add(callback);
            callbackSnapshotDirty = true;
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    public void clearDrawCallbacks() {
        callbacks.clear();
        callbackSnapshotDirty = true;
        invalidate(InvalidationFlags.VISUAL);
    }

    @Override
    public void render(RenderContext context) {
        pushOpacity(context);
        try {
            for (CanvasDrawCallback callback : callbackSnapshot()) {
                callback.draw(context);
            }
        } finally {
            popOpacity(context);
        }
    }

    private CanvasDrawCallback[] callbackSnapshot() {
        if (callbackSnapshotDirty) {
            callbackSnapshot = callbacks.toArray(new CanvasDrawCallback[callbacks.size()]);
            callbackSnapshotDirty = false;
        }
        return callbackSnapshot;
    }
}
