package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.docking.DockingRoot;

public final class DockDropPreviewRenderers {
    private static final MutableColor FILL = new MutableColor(0.18f, 0.56f, 1.0f, 0.22f);
    private static final MutableColor STROKE = new MutableColor(0.28f, 0.78f, 1.0f, 0.92f);

    public static final WidgetRender DEFAULT = WidgetRender.of(DockingRoot.class, (draw, root) ->
            renderDefault(draw, root.dropPreviewSnapshot()));

    private static void renderDefault(DrawScope draw, DockDropPreviewState state) {
        if (!state.visible() || state.width() <= 0.0f || state.height() <= 0.0f) return;
        draw.roundedRect(state.x(), state.y(), state.width(), state.height(), 4.0f, Paint.fill(FILL));
        draw.roundedRect(state.x(), state.y(), state.width(), state.height(), 4.0f, Paint.stroke(STROKE, 1.5f));
    }

    private DockDropPreviewRenderers() {
    }
}
