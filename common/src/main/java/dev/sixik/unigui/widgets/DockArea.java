package dev.sixik.unigui.widgets;

public enum DockArea {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    CENTER,
    TAB,
    FLOAT;

    public DockSplitOrientation splitOrientation() {
        return switch (this) {
            case LEFT, RIGHT -> DockSplitOrientation.HORIZONTAL;
            case TOP, BOTTOM -> DockSplitOrientation.VERTICAL;
            case CENTER, TAB, FLOAT -> null;
        };
    }

    public boolean insertsBeforeTarget() {
        return this == LEFT || this == TOP;
    }
}
