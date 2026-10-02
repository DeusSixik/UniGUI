package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.interaction.ScrollBar;

public final class ScrollBarRenderers {
    public static final WidgetRender DEFAULT = WidgetRender.of(ScrollBar.class, (draw, scrollBar) ->
            ScrollBarRenderPlans.defaultPlan(scrollBar.snapshot()).render(draw));

    private ScrollBarRenderers() {
    }
}
