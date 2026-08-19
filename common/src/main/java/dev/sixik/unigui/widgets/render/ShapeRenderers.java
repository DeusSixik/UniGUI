package dev.sixik.unigui.widgets.render;

public final class ShapeRenderers {
    public static final ShapeRenderer DEFAULT = (draw, state) -> ShapeRenderPlans.defaultPlan(state).render(draw);

    private ShapeRenderers() {
    }
}