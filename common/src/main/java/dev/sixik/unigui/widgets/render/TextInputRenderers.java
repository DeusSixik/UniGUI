package dev.sixik.unigui.widgets.render;

public final class TextInputRenderers {
    public static final TextInputRenderer DEFAULT = (draw, state) -> TextInputRenderPlans.defaultPlan(state).render(draw);
    public static final TextInputRenderer SEARCH_FIELD = (draw, state) -> TextInputRenderPlans.searchFieldPlan(state).render(draw);

    private TextInputRenderers() {
    }
}
