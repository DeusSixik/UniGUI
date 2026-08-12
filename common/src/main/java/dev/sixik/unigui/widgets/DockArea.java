package dev.sixik.unigui.widgets;

/**
 * Docking drop target area used by DockingRoot/DockingManager.
 *
 * <p>DockArea describes what should happen when a pane is dropped onto a
 * docking target: split to an edge, tab into the target, center-tab, or float.
 * It is intentionally broader than {@link DockSide}; do not use DockArea for
 * plain DockPanel child layout.</p>
 *
 * <p>{@link #CENTER} and {@link #TAB} both mean tab insertion today. CENTER is
 * the geometric central drop zone, while TAB is the explicit tab-strip semantic.</p>
 */
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
