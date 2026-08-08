package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;

import java.util.List;

public record VirtualTableViewState(
        float x,
        float y,
        float width,
        float height,
        float viewportWidth,
        float headerHeight,
        float rowViewportY,
        float rowViewportHeight,
        boolean focused,
        boolean editing,
        VirtualTableViewRenderPhase phase,
        ColorView headerBackground,
        ColorView activeCellColor,
        ColorView gridColor,
        ColorView textColor,
        List<VirtualTableViewColumnState> columns,
        List<VirtualTableViewRowState> rows
) {
    public VirtualTableViewState {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
