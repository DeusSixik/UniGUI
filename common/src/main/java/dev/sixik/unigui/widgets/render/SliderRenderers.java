package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.interaction.Slider;

public final class SliderRenderers {
    public static final WidgetRender DEFAULT = WidgetRender.of(Slider.class, (draw, slider) ->
            SliderRenderPlans.defaultPlan(slider.snapshot()).render(draw));

    private SliderRenderers() {
    }
}
