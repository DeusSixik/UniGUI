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
@AiDoc(summary = "Target destination zone for docking operations: edge splits (LEFT, RIGHT, TOP, BOTTOM), tab groupings (CENTER, TAB), or detached floating windows (FLOAT)."
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
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    CENTER,
    TAB,
    FLOAT;

    @AiDoc(summary = "Returns the split orientation corresponding to edge areas, or null for non-split areas.")
    @WikiMethod(summary = "<key:dock.area.method.splitOrientation>")
    @WikiReturn("<key:dock.area.return.splitOrientation>")
    public DockSplitOrientation splitOrientation() {
        return switch (this) {
            case LEFT, RIGHT -> DockSplitOrientation.HORIZONTAL;
            case TOP, BOTTOM -> DockSplitOrientation.VERTICAL;
            case CENTER, TAB, FLOAT -> null;
        };
    }

    @AiDoc(summary = "Returns true if the area places the new pane before the existing target node.")
    @WikiMethod(summary = "<key:dock.area.method.insertsBeforeTarget>")
    @WikiReturn("<key:dock.area.return.insertsBeforeTarget>")
    public boolean insertsBeforeTarget() {
        return this == LEFT || this == TOP;
    }
}
