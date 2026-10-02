package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.interaction.ToggleButton;

/** Стандартные полные визуалы toggle button: поверхность плюс центрированный label. */
public final class ToggleButtonRenderers {
    /** Полный визуал toggle button: поверхность плюс центрированный label. */
    public static final WidgetRender DEFAULT = WidgetRender.of(ToggleButton.class, (draw, toggle) -> {
        SurfacePlans.renderWidgetSurface(draw, toggle);
        renderDefault(draw, toggle.toggleButtonSnapshot(draw.context()));
    });

    private ToggleButtonRenderers() {
    }

    private static void renderDefault(DrawScope draw, ToggleButtonRenderState state) {
        if (state == null) return;
        if (!state.hasText()) return;

        float contentWidth = Math.max(0.0f, state.width() - state.textPaddingX() * 2.0f);
        float drawWidth = Math.min(contentWidth, Math.max(0.0f, state.textWidth()));
        float drawHeight = Math.min(Math.max(0.0f, state.height()),
                Math.max(0.0f, state.textHeight()));
        if (contentWidth <= 0.0f || drawHeight <= 0.0f) return;
        float contentX = state.x() + state.textPaddingX();
        float drawX = contentX + Math.max(0.0f, contentWidth - drawWidth) * 0.5f;
        float drawY = state.y() + Math.max(0.0f, state.height() - drawHeight) * 0.5f;
        LabelPart.render(draw, state.richText(), contentX, state.y(), contentWidth, state.height(),
                drawX, drawY, drawWidth, drawHeight, state.textColor());
    }
}
