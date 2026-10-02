package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.containers.SurfaceWidget;

/** Стандартный полный визуал поверхности: план по snapshot виджета. */
public final class BoxRenderers {
    /** Дефолтный рендер поверхности Box: цвет, текстура или шейдер по active kind. */
    public static final WidgetRender DEFAULT = WidgetRender.of(SurfaceWidget.class,
            (draw, widget) -> SurfacePlans.defaultPlan(widget.surfaceSnapshot()).render(draw));

    private BoxRenderers() {
    }
}
