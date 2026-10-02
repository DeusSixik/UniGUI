package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.interaction.Button;

/**
 * Стандартные полные визуалы кнопки: поверхность плюс текст.
 */
public final class ButtonRenderers {
    /** Полный визуал обычной кнопки: поверхность (цвет/текстура/шейдер) плюс текст. */
    public static final WidgetRender DEFAULT = WidgetRender.of(Button.class, (draw, button) -> {
        SurfacePlans.renderWidgetSurface(draw, button);
        ButtonRenderPlans.textPlan(button.snapshot(draw.context())).render(draw);
    });

    private ButtonRenderers() {
    }
}
