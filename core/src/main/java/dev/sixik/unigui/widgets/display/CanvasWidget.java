package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(
        summary = "Procedural drawing surface widget invoking custom CanvasDrawCallback painting routines.",
        purpose = "Provides an immediate-mode 2D drawing canvas embedded directly in the widget tree.",
        useWhen = {
                "Custom rendering routines using DrawScope or graphics context.",
                "Custom animated dials, particle previews, or mini-games."
        },
        criticalRules = {
                "onDraw() callbacks are invoked sequentially on each render pass.",
                "Remember to invalidate VISUAL when canvas state changes."
        },
        related = {"CanvasDrawCallback", "DrawScope", "RenderContext"})
@WikiPage(
        title = "<key:canvas.widget.title>",
        slug = "canvas-widget",
        category = "<key:core.widgets.display.category>",
        summary = "<key:canvas.widget.summary>",
        order = 110)
@WikiSection(
        title = "<key:canvas.widget.section.overview.title>",
        markdown = "<key:canvas.widget.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:canvas.widget.example.basic.title>",
        code = "<key:canvas.widget.example.basic.code>",
        order = 10)
@XmlWidgetName("CanvasWidget")
public final class CanvasWidget extends WidgetBase {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.CANVAS_WIDGET;

    private final List<CanvasDrawCallback> callbacks = new ObjectArrayList<>();
    private CanvasDrawCallback[] callbackSnapshot = new CanvasDrawCallback[0];
    private boolean callbackSnapshotDirty = true;

    @WikiMethod(
            title = "<key:canvas.widget.method.on_draw.title>",
            summary = "<key:canvas.widget.method.on_draw.summary>")
    public CanvasWidget onDraw(CanvasDrawCallback callback) {
        if (callback != null) {
            callbacks.add(callback);
            callbackSnapshotDirty = true;
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    @WikiMethod(
            title = "<key:canvas.widget.method.clear_callbacks.title>",
            summary = "<key:canvas.widget.method.clear_callbacks.summary>")
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