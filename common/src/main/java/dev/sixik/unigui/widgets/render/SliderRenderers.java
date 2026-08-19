package dev.sixik.unigui.widgets.render;

public final class SliderRenderers {
    public static final SliderRenderer DEFAULT = (draw, state) -> SliderRenderPlans.defaultPlan(state).render(draw);

    private SliderRenderers() {
    }
}