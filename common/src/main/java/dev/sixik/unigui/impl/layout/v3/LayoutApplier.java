package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.v3.LayoutNodeId;
import dev.sixik.unigui.api.layout.v3.LayoutOutput;
import dev.sixik.unigui.api.layout.v3.LayoutResult;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Map;

/** Applies backend-neutral Layout V3 results back to live widgets. */
public final class LayoutApplier {
    private LayoutApplier() {
    }

    public static void apply(LayoutOutput output, Map<LayoutNodeId, ? extends Widget> widgets, RectView offsetBounds) {
        if (offsetBounds == null) {
            apply(output, widgets, 0.0f, 0.0f);
            return;
        }
        apply(output, widgets, offsetBounds.x(), offsetBounds.y());
    }

    public static void apply(LayoutOutput output, Map<LayoutNodeId, ? extends Widget> widgets, float offsetX, float offsetY) {
        if (output == null || widgets == null || widgets.isEmpty()) {
            return;
        }
        for (Map.Entry<LayoutNodeId, ? extends Widget> entry : widgets.entrySet()) {
            Widget widget = entry.getValue();
            LayoutResult result = output.result(entry.getKey());
            if (widget != null && result != null) {
                widget.arrange(new MutableRect(
                        offsetX + result.x(),
                        offsetY + result.y(),
                        result.width(),
                        result.height()));
            }
        }
    }
}
