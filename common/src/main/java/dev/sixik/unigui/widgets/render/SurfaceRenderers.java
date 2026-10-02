package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.containers.SurfaceWidget;

/**
 * Стандартные рендеры поверхности.
 */
public final class SurfaceRenderers {
    /** Дефолтный рендер поверхности: план по snapshot виджета. */
    public static final WidgetRender DEFAULT = WidgetRender.of(SurfaceWidget.class,
            (draw, widget) -> SurfacePlans.defaultPlan(widget.surfaceSnapshot()).render(draw));

    private SurfaceRenderers() {
    }
}
