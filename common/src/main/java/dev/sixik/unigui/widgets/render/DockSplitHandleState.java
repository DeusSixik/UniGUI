package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.widgets.DockSplitOrientation;

public record DockSplitHandleState(
        float x,
        float y,
        float width,
        float height,
        DockSplitOrientation orientation
) {
    public DockSplitHandleState {
        orientation = orientation == null ? DockSplitOrientation.HORIZONTAL : orientation;
    }
}
