package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.feedback.Tooltip;

public final class TooltipRenderers {
    public static final WidgetRender DEFAULT = WidgetRender.of(Tooltip.class, (draw, tooltip) -> {
        SurfacePlans.renderWidgetSurface(draw, tooltip);
        TooltipState state = tooltip.snapshot(draw.context());
        draw.pushTextClip(state.textX(), state.textY(), state.textWidth(), state.textHeight());
        try {
            float lineY = state.textY();
            float limitY = state.textY() + state.textHeight();
            for (int i = 0; i < state.lines().size(); i++) {
                float lineHeight = state.lineHeight(i);
                if (lineY >= limitY) break;
                TextEngine.drawInline(draw,
                        state.lines().get(i),
                        state.textX(),
                        lineY,
                        state.textWidth(),
                        lineHeight,
                        Paint.fill(state.textColor()));
                lineY += lineHeight;
            }
        } finally {
            draw.popClip();
        }
    });

    private TooltipRenderers() {
    }
}
