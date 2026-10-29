package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.widgets.core.Orientation;


public enum DockSplitOrientation {
    HORIZONTAL,
    VERTICAL;

    public Orientation toWidgetOrientation() {
        return this == HORIZONTAL ? Orientation.HORIZONTAL : Orientation.VERTICAL;
    }
}
