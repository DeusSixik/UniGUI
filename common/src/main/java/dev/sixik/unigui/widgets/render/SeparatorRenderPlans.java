package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.plan.RenderPlan;
import dev.sixik.unigui.api.render.plan.RenderPrimitive;
import dev.sixik.unigui.api.render.plan.StyledRenderPlans;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.widgets.core.Orientation;

import java.util.List;

/** Declarative render-plan builder for Separator visuals. */
public final class SeparatorRenderPlans {
    private SeparatorRenderPlans() {
    }

    public static RenderPlan defaultPlan(SeparatorState state) {
        if (state == null) return RenderPlan.EMPTY;
        boolean horizontal = state.orientation() != Orientation.VERTICAL;
        float width = horizontal ? state.width() : state.thickness();
        float height = horizontal ? state.thickness() : state.height();
        return RenderPlan.of(List.of(new RenderPrimitive.Rect(state.x(), state.y(), width, height, Paint.fill(state.color()))));
    }

    public static RenderPlan styledPlan(SeparatorState state, Style style, WidgetState widgetState) {
        if (state == null) return RenderPlan.EMPTY;
        ColorView color = StyledRenderPlans.value(style, StyleKeys.BORDER_COLOR, widgetState, state.color());
        return defaultPlan(new SeparatorState(
                state.x(), state.y(), state.width(), state.height(), state.orientation(), state.thickness(), color));
    }
}