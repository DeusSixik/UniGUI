package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;

public final class TooltipRenderers {
    public static final TooltipRenderer DEFAULT = (draw, state) -> {
        draw.pushClip(state.textX(), state.textY(), state.textWidth(), state.textHeight());
        try {
            float lineY = state.textY();
            float limitY = state.textY() + state.textHeight();
            for (int i = 0; i < state.lines().size(); i++) {
                float lineHeight = state.lineHeight(i);
                if (lineY >= limitY) break;
                draw.text(state.lines().get(i),
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
}
