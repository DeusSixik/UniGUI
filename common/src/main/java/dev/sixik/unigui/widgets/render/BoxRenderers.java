package dev.sixik.unigui.widgets.render;

public final class BoxRenderers {
    public static final BoxRenderer DEFAULT = (draw, state) -> BoxRenderPlans.defaultPlan(state).render(draw);

    private BoxRenderers() {
    }
}