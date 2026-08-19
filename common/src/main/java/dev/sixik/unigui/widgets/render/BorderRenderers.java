package dev.sixik.unigui.widgets.render;

public final class BorderRenderers {
    public static final BorderRenderer DEFAULT = (draw, state) -> BorderRenderPlans.defaultPlan(state).render(draw);

    private BorderRenderers() {
    }
}