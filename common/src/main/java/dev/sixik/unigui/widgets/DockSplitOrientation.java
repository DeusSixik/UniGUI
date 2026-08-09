package dev.sixik.unigui.widgets;

public enum DockSplitOrientation {
    HORIZONTAL,
    VERTICAL;

    public Orientation toWidgetOrientation() {
        return this == HORIZONTAL ? Orientation.HORIZONTAL : Orientation.VERTICAL;
    }
}
