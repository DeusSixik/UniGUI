package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.impl.text.TextEngine;

public final class TooltipRenderers {
    public static final TooltipRenderer DEFAULT = (draw, state) -> {
        renderChrome(draw, state);
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
    };

    private TooltipRenderers() {
    }

    private static void renderChrome(dev.sixik.unigui.api.render.DrawScope draw, TooltipState state) {
        if (state.backgroundVisible()) {
            draw.roundedRect(state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.fill(state.backgroundColor()));
        }
        if (state.borderVisible() && state.borderWidth() > 0.0f) {
            draw.roundedRect(state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.stroke(state.borderColor(), state.borderWidth()));
        }
    }
}
