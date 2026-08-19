package dev.sixik.unigui.api.render.plan;

import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.WidgetState;

/** Builds an inspectable render plan from a widget render-state snapshot and resolved style data. */
@FunctionalInterface
public interface StyledRenderPlanBuilder<S> {
    RenderPlan build(S state, Style style, WidgetState widgetState);
}