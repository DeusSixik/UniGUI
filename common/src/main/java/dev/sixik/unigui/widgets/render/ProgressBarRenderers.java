package dev.sixik.unigui.widgets.render;

public final class ProgressBarRenderers {
    public static final ProgressBarRenderer DEFAULT = (draw, state) -> ProgressBarRenderPlans.defaultPlan(state).render(draw);

    private ProgressBarRenderers() {
    }
}