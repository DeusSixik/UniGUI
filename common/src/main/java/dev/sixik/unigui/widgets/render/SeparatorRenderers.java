package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.display.Separator;

public final class SeparatorRenderers {
    public static final WidgetRender DEFAULT = WidgetRender.of(Separator.class, (draw, w) -> SeparatorRenderPlans.defaultPlan(w.snapshot()).render(draw));

    private SeparatorRenderers() {
    }
}
