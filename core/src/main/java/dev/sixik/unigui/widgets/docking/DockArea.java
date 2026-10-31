package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.widgets.containers.DockPanel;
import dev.sixik.unigui.widgets.containers.DockSide;

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
@AiDoc(
        description = "Target destination zone for docking operations: edge splits (LEFT, RIGHT, TOP, BOTTOM), tab groupings (CENTER, TAB), or detached floating windows (FLOAT)."
)
@WikiPage(
        title = "DockArea",
        slug = "dock-area",
        category = "<key:docking.category>",
        summary = "<key:dock.area.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:dock.area.section.overview.body>",
        order = 10
)
@WikiLink(
        label = "DockingRoot",
        target = "page:docking-root",
        description = "<key:docking.root.summary>"
)
@WikiLink(
        label = "DockingManager",
        target = "page:docking-manager",
        description = "<key:docking.manager.summary>"
)
public enum DockArea {
    @AiDoc(description = "Dock to the left side by creating a horizontal split with the target.")
    LEFT,
    @AiDoc(description = "Dock to the right side by creating a horizontal split with the target.")
    RIGHT,
    @AiDoc(description = "Dock above the target by creating a vertical split.")
    TOP,
    @AiDoc(description = "Dock below the target by creating a vertical split.")
    BOTTOM,
    @AiDoc(description = "Dock into the central area as an added tab.")
    CENTER,
    @AiDoc(description = "Dock explicitly into the tab strip of the target leaf.")
    TAB,
    @AiDoc(description = "Detach the pane into an independent floating window.")
    FLOAT;

    @AiDoc(description = "Returns the split orientation corresponding to edge areas, or null for non-split areas.")
    @WikiMethod(description = "<key:dock.area.method.splitOrientation>")
    @WikiReturn("<key:dock.area.return.splitOrientation>")
    public DockSplitOrientation splitOrientation() {
        return switch (this) {
            case LEFT, RIGHT -> DockSplitOrientation.HORIZONTAL;
            case TOP, BOTTOM -> DockSplitOrientation.VERTICAL;
            case CENTER, TAB, FLOAT -> null;
        };
    }

    @AiDoc(description = "Returns true if the area places the new pane before the existing target node.")
    @WikiMethod(description = "<key:dock.area.method.insertsBeforeTarget>")
    @WikiReturn("<key:dock.area.return.insertsBeforeTarget>")
    public boolean insertsBeforeTarget() {
        return this == LEFT || this == TOP;
    }
}
