package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.feedback.ProgressBar;

public final class ProgressBarRenderers {
    public static final WidgetRender DEFAULT = WidgetRender.of(ProgressBar.class,
            (draw, bar) -> ProgressBarRenderPlans.defaultPlan(bar.snapshot()).render(draw));

    private ProgressBarRenderers() {
    }
}
