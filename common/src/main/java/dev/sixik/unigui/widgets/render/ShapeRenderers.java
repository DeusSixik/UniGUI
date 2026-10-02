package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.display.Shape;

public final class ShapeRenderers {
    public static final WidgetRender DEFAULT = WidgetRender.of(Shape.class, (draw, w) -> ShapeRenderPlans.defaultPlan(w.snapshot()).render(draw));

    private ShapeRenderers() {
    }
}
