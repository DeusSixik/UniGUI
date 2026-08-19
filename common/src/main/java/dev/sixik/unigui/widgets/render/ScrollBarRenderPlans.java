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

import java.util.ArrayList;
import java.util.List;

/** Declarative render-plan builder for ScrollBar visuals. */
public final class ScrollBarRenderPlans {
    private ScrollBarRenderPlans() {
    }

    public static RenderPlan defaultPlan(ScrollBarState state) {
        if (state == null) return RenderPlan.EMPTY;
        float width = state.width();
        float height = state.height();
        List<RenderPrimitive> primitives = new ArrayList<>(2);
        primitives.add(new RenderPrimitive.RoundedRect(state.x(), state.y(), width, height, Math.min(width, height) * 0.5f,
                Paint.fill(state.trackColor())));

        if (state.orientation() == Orientation.VERTICAL) {
            float thumbHeight = state.thumbLength(height);
            float thumbY = state.y() + (height - thumbHeight) * state.normalizedValue();
            primitives.add(new RenderPrimitive.RoundedRect(state.x(), thumbY, width, thumbHeight, width * 0.5f,
                    Paint.fill(state.thumbColor())));
        } else {
            float thumbWidth = state.thumbLength(width);
            float thumbX = state.x() + (width - thumbWidth) * state.normalizedValue();
            primitives.add(new RenderPrimitive.RoundedRect(thumbX, state.y(), thumbWidth, height, height * 0.5f,
                    Paint.fill(state.thumbColor())));
        }
        return RenderPlan.of(primitives);
    }

    public static RenderPlan styledPlan(ScrollBarState state, Style style, WidgetState widgetState) {
        if (state == null) return RenderPlan.EMPTY;
        ColorView trackColor = StyledRenderPlans.value(style, StyleKeys.TRACK_COLOR, widgetState, state.trackColor());
        ColorView thumbColor = StyledRenderPlans.value(style, StyleKeys.THUMB_COLOR, widgetState, state.thumbColor());
        return defaultPlan(new ScrollBarState(
                state.x(), state.y(), state.width(), state.height(), state.orientation(), state.min(), state.max(),
                state.value(), state.pageSize(), state.step(), state.normalizedValue(), state.dragging(), trackColor, thumbColor));
    }
}