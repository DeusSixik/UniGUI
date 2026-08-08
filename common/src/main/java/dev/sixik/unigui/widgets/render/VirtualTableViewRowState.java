package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;

import java.util.List;

public record VirtualTableViewRowState(
        int visualRow,
        int sourceRow,
        float x,
        float y,
        float width,
        float height,
        boolean selected,
        boolean alternate,
        ColorView background,
        List<VirtualTableViewCellState> cells
) {
    public VirtualTableViewRowState {
        cells = cells == null ? List.of() : List.copyOf(cells);
    }
}
