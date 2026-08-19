package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.plan.RenderPlan;
import dev.sixik.unigui.api.render.plan.RenderPrimitive;
import dev.sixik.unigui.api.render.plan.StyledRenderPlans;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;

import java.util.List;

/** Declarative render-plan builder for Border visuals. */
public final class BorderRenderPlans {
    private BorderRenderPlans() {
    }

    public static RenderPlan defaultPlan(BorderState state) {
        if (state == null) return RenderPlan.EMPTY;
        return RenderPlan.of(List.of(new RenderPrimitive.RoundedRect(
                state.x(), state.y(), state.width(), state.height(), state.radius(),
                Paint.stroke(state.color(), state.thickness()))));
    }

    public static RenderPlan styledPlan(BorderState state, Style style, WidgetState widgetState) {
        if (state == null) return RenderPlan.EMPTY;
        ColorView color = StyledRenderPlans.value(style, StyleKeys.BORDER_COLOR, widgetState, state.color());
        Float thickness = StyledRenderPlans.value(style, StyleKeys.BORDER_WIDTH, widgetState, state.thickness());
        Float radius = StyledRenderPlans.value(style, StyleKeys.RADIUS, widgetState, state.radius());
        return defaultPlan(new BorderState(
                state.x(), state.y(), state.width(), state.height(), color,
                thickness == null ? state.thickness() : thickness,
                radius == null ? state.radius() : radius));
    }
}