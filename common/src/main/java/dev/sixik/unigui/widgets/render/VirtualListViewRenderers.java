package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;

public final class VirtualListViewRenderers {
    public static final VirtualListViewRenderer DEFAULT = (draw, state) -> {
        for (VirtualListViewRowState row : state.rows()) {
            if (state.phase() == VirtualListViewRenderPhase.BACKGROUND && row.selected()) {
                draw.rect(row.x(), row.y(), row.width(), row.height(), Paint.fill(state.selectedRowColor()));
            } else if (state.phase() == VirtualListViewRenderPhase.FOREGROUND && state.focused() && row.active()) {
                draw.rect(row.x(), row.y(), row.width(), row.height(), Paint.stroke(state.activeRowColor(), 1.0f));
            }
        }
    };

    private VirtualListViewRenderers() {
    }
}
