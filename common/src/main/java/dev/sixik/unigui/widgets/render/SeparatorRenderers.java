package dev.sixik.unigui.widgets.render;

public final class SeparatorRenderers {
    public static final SeparatorRenderer DEFAULT = (draw, state) -> SeparatorRenderPlans.defaultPlan(state).render(draw);

    private SeparatorRenderers() {
    }
}