package dev.sixik.unigui.widgets;

/**
 * DockPanel child layout side.
 *
 * <p>DockSide is limited to the four edge slots supported by DockPanel's
 * retained layout algorithm. It is not a docking-system drop zone enum; use
 * {@link DockArea} for DockingRoot/DockingManager drag-drop semantics.</p>
 */
public enum DockSide {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM
}
