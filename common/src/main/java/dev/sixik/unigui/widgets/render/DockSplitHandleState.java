package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.widgets.docking.DockSplitOrientation;

public record DockSplitHandleState(
        float x,
        float y,
        float width,
        float height,
        DockSplitOrientation orientation,
        boolean hovered,
        boolean pressed
) {
    public DockSplitHandleState {
        orientation = orientation == null ? DockSplitOrientation.HORIZONTAL : orientation;
    }
}
