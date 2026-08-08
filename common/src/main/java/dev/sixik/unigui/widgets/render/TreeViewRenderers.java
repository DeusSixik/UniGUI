package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;

public final class TreeViewRenderers {
    public static final TreeViewRenderer DEFAULT = (draw, state) -> {
        if (state.text() == null || state.text().isEmpty()) return;
        draw.text(state.text(), state.textX(), state.textY(), state.textWidth(), state.textHeight(),
                Paint.fill(state.textColor()));
    };

    private TreeViewRenderers() {
    }
}
