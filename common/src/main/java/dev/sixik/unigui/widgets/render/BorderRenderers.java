package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.containers.Border;

public final class BorderRenderers {
    public static final WidgetRender DEFAULT = WidgetRender.of(Border.class, (draw, w) -> BorderRenderPlans.defaultPlan(w.snapshot()).render(draw));

    private BorderRenderers() {
    }
}
