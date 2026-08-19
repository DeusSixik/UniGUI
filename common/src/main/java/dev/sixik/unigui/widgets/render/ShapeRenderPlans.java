package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.plan.RenderPlan;
import dev.sixik.unigui.api.render.plan.RenderPrimitive;
import dev.sixik.unigui.api.render.plan.StyledRenderPlans;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.widgets.display.Shape;

import java.util.List;

/** Declarative render-plan builder for Shape visuals. */
public final class ShapeRenderPlans {
    private ShapeRenderPlans() {
    }

    public static RenderPlan defaultPlan(ShapeState state) {
        if (state == null) return RenderPlan.EMPTY;
        Paint paint = state.stroke()
                ? Paint.stroke(state.color(), state.strokeWidth())
                : Paint.fill(state.color());
        RenderPrimitive primitive = switch (state.type() == null ? Shape.Type.RECT : state.type()) {
            case RECT -> new RenderPrimitive.Rect(state.x(), state.y(), state.width(), state.height(), paint);
            case ROUNDED_RECT -> new RenderPrimitive.RoundedRect(state.x(), state.y(), state.width(), state.height(), state.radius(), paint);
            case CIRCLE -> new RenderPrimitive.Circle(state.x(), state.y(), state.width(), state.height(), paint);
            case LINE -> new RenderPrimitive.Line(state.x(), state.y(), state.x() + state.width(), state.y() + state.height(), paint);
        };
        return RenderPlan.of(List.of(primitive));
    }

    public static RenderPlan styledPlan(ShapeState state, Style style, WidgetState widgetState) {
        if (state == null) return RenderPlan.EMPTY;
        ColorView color = StyledRenderPlans.value(style,
                state.stroke() ? StyleKeys.BORDER_COLOR : StyleKeys.BACKGROUND_COLOR,
                widgetState,
                state.color());
        Float strokeWidth = StyledRenderPlans.value(style, StyleKeys.BORDER_WIDTH, widgetState, state.strokeWidth());
        Float radius = StyledRenderPlans.value(style, StyleKeys.RADIUS, widgetState, state.radius());
        return defaultPlan(new ShapeState(
                state.x(), state.y(), state.width(), state.height(), state.type(), color, state.stroke(),
                strokeWidth == null ? state.strokeWidth() : strokeWidth,
                radius == null ? state.radius() : radius));
    }
}