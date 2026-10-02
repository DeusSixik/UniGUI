package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.plan.RenderPlan;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.WidgetState;

/**
 * Declarative render-plan builder for the default Box visuals.
 *
 * <p>Тонкий фасад над {@link SurfacePlans}: хром рисуется единым surface-планом,
 * поддерживающим цвет, текстуру и шейдер. Kind переключается данными, а не
 * сменой renderer-класса.</p>
 */
public final class BoxRenderPlans {
    private BoxRenderPlans() {
    }

    public static RenderPlan defaultPlan(BoxState state) {
        if (state == null) return RenderPlan.EMPTY;
        return SurfacePlans.defaultPlan(state.toSurface());
    }

    public static RenderPlan styledPlan(BoxState state, Style style, WidgetState widgetState) {
        if (state == null) return RenderPlan.EMPTY;
        return SurfacePlans.styledPlan(state.toSurface(), style, widgetState);
    }
}
