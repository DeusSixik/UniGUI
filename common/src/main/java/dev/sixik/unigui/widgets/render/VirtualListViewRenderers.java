package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.data.VirtualListView;

public final class VirtualListViewRenderers {
    public static final WidgetRender DEFAULT = WidgetRender.of(VirtualListView.class, (draw, view) -> {
        VirtualListViewState state = view.snapshot(view.renderPhase());
        for (VirtualListViewRowState row : state.rows()) {
            if (state.phase() == VirtualListViewRenderPhase.BACKGROUND && row.selected()) {
                draw.rect(row.x(), row.y(), row.width(), row.height(), Paint.fill(state.selectedRowColor()));
            } else if (state.phase() == VirtualListViewRenderPhase.FOREGROUND && state.focused() && row.active()) {
                draw.rect(row.x(), row.y(), row.width(), row.height(), Paint.stroke(state.activeRowColor(), 1.0f));
            }
        }
    });

    private VirtualListViewRenderers() {
    }
}
