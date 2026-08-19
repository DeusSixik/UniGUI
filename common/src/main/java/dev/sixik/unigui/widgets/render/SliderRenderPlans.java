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

/** Declarative render-plan builder for Slider visuals. */
public final class SliderRenderPlans {
    private SliderRenderPlans() {
    }

    public static RenderPlan defaultPlan(SliderState state) {
        if (state == null) return RenderPlan.EMPTY;
        float height = Math.max(1.0f, state.height());
        float trackHeight = Math.max(2.0f, Math.min(4.0f, height * 0.25f));
        float trackY = state.y() + (height - trackHeight) * 0.5f;
        float fillWidth = state.width() * state.normalizedValue();
        float knobX = state.x() + fillWidth - state.knobWidth() * 0.5f;

        return RenderPlan.of(List.of(
                new RenderPrimitive.RoundedRect(state.x(), trackY, state.width(), trackHeight, trackHeight * 0.5f,
                        Paint.fill(state.trackColor())),
                new RenderPrimitive.RoundedRect(state.x(), trackY, fillWidth, trackHeight, trackHeight * 0.5f,
                        Paint.fill(state.fillColor())),
                new RenderPrimitive.RoundedRect(knobX, state.y() + 2.0f, state.knobWidth(), Math.max(1.0f, height - 4.0f), 2.0f,
                        Paint.fill(state.knobColor()))));
    }

    public static RenderPlan styledPlan(SliderState state, Style style, WidgetState widgetState) {
        if (state == null) return RenderPlan.EMPTY;
        ColorView trackColor = StyledRenderPlans.value(style, StyleKeys.TRACK_COLOR, widgetState, state.trackColor());
        ColorView fillColor = StyledRenderPlans.value(style, StyleKeys.ACCENT_COLOR, widgetState, state.fillColor());
        ColorView knobColor = StyledRenderPlans.value(style, StyleKeys.THUMB_COLOR, widgetState, state.knobColor());
        return defaultPlan(new SliderState(
                state.x(), state.y(), state.width(), state.height(), state.min(), state.max(), state.value(),
                state.step(), state.normalizedValue(), state.knobWidth(), state.dragging(), trackColor, fillColor, knobColor));
    }
}