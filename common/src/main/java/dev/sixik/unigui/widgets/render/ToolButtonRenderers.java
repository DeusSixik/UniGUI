package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.interaction.ToolButton;

/** Стандартные полные визуалы toolbar-кнопок: поверхность плюс текст. */
public final class ToolButtonRenderers {
    /** Полный визуал toolbar-кнопки: поверхность плюс текстовый контент. */
    public static final WidgetRender DEFAULT = WidgetRender.of(ToolButton.class, (draw, button) -> {
        SurfacePlans.renderWidgetSurface(draw, button);
        ButtonRenderPlans.textPlan(button.snapshot(draw.context())).render(draw);
    });

    private ToolButtonRenderers() {
    }
}
