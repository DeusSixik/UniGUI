package dev.sixik.unigui.widgets.render;

public final class TextureWidgetRenderers {
    public static final TextureWidgetRenderer DEFAULT = (draw, state) -> TextureWidgetRenderPlans.defaultPlan(state).render(draw);

    private TextureWidgetRenderers() {
    }
}