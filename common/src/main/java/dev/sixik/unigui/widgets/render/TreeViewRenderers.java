package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.impl.text.TextEngine;

public final class TreeViewRenderers {
    public static final TreeViewRenderer DEFAULT = (draw, state) -> {
        if (state.text() == null || state.text().isEmpty()) return;
        float clipX = state.x() + state.textPaddingX() + state.depth() * state.indentWidth();
        float clipWidth = Math.max(0.0f, state.width() - state.textPaddingX() * 2.0f - state.depth() * state.indentWidth());
        draw.pushTextClip(clipX, state.y(), clipWidth, state.height());
        try {
            TextEngine.drawInline(draw, state.text(), state.textX(), state.textY(), Math.max(0.0f, state.textWidth()), state.textHeight(),
                    Paint.fill(state.textColor()));
        } finally {
            draw.popClip();
        }
    };

    private TreeViewRenderers() {
    }
}
