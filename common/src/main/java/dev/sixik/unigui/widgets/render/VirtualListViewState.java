package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;

import java.util.List;

public record VirtualListViewState(
        float x,
        float y,
        float width,
        float height,
        float viewportWidth,
        float viewportHeight,
        boolean focused,
        VirtualListViewRenderPhase phase,
        ColorView selectedRowColor,
        ColorView activeRowColor,
        List<VirtualListViewRowState> rows
) {
    public VirtualListViewState {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
