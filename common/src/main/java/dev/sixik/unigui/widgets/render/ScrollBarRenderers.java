package dev.sixik.unigui.widgets.render;

public final class ScrollBarRenderers {
    public static final ScrollBarRenderer DEFAULT = (draw, state) -> ScrollBarRenderPlans.defaultPlan(state).render(draw);

    private ScrollBarRenderers() {
    }
}