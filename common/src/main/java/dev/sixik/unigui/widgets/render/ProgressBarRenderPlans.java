package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.plan.RenderPlan;
import dev.sixik.unigui.api.render.plan.RenderPrimitive;
import dev.sixik.unigui.api.render.plan.StyledRenderPlans;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;

import java.util.ArrayList;
import java.util.List;

/** Declarative render-plan builder for ProgressBar visuals. */
public final class ProgressBarRenderPlans {
    private ProgressBarRenderPlans() {
    }

    public static RenderPlan defaultPlan(ProgressBarState state) {
        if (state == null) return RenderPlan.EMPTY;
        float width = Math.max(0.0f, state.width());
        float fillWidth = Math.max(0.0f, Math.min(width, width * state.progress()));

        List<RenderPrimitive> primitives = new ArrayList<>(2);
        primitives.add(new RenderPrimitive.Rect(state.x(), state.y(), width, state.height(), Paint.fill(state.trackColor())));
        if (state.indeterminate()) {
            float segmentWidth = Math.max(8.0f, width * 0.32f);
            float travel = width + segmentWidth;
            float offset = state.indeterminateOffset() - (float) Math.floor(state.indeterminateOffset());
            float segmentX = state.x() + offset * travel - segmentWidth;
            primitives.add(new RenderPrimitive.Clip(state.x(), state.y(), width, state.height(), false,
                    List.of(new RenderPrimitive.Rect(segmentX, state.y(), segmentWidth, state.height(),
                            Paint.fill(state.fillColor())))));
            return RenderPlan.of(primitives);
        }
        if (fillWidth > 0.0f) {
            primitives.add(new RenderPrimitive.Rect(state.x(), state.y(), fillWidth, state.height(), Paint.fill(state.fillColor())));
        }
        return RenderPlan.of(primitives);
    }

    public static RenderPlan styledPlan(ProgressBarState state, Style style, WidgetState widgetState) {
        if (state == null) return RenderPlan.EMPTY;
        ColorView trackColor = StyledRenderPlans.value(style, StyleKeys.TRACK_COLOR, widgetState, state.trackColor());
        ColorView fillColor = StyledRenderPlans.value(style, StyleKeys.ACCENT_COLOR, widgetState, state.fillColor());
        return defaultPlan(new ProgressBarState(
                state.x(), state.y(), state.width(), state.height(), state.min(), state.max(), state.value(),
                state.progress(), state.indeterminate(), state.indeterminateOffset(), trackColor, fillColor));
    }
}