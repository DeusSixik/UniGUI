package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.widgets.core.Orientation;

@AiDoc(summary = "Split layout orientation between adjacent dock nodes: HORIZONTAL (side-by-side) or VERTICAL (stacked top/bottom)."
)
@WikiPage(
        title = "DockSplitOrientation",
        slug = "dock-split-orientation",
        category = "<key:docking.category>",
        summary = "<key:dock.split.orientation.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:dock.split.orientation.section.overview.body>",
        order = 10
)
@WikiLink(
        label = "DockNode",
        target = "page:dock-node",
        description = "<key:dock.node.summary>"
)
@WikiLink(
        label = "DockingRoot",
        target = "page:docking-root",
        description = "<key:docking.root.summary>"
)
public enum DockSplitOrientation {
    HORIZONTAL,
    VERTICAL;

    @AiDoc(summary = "Converts the dock split orientation to the core widget Orientation enum.")
    @WikiMethod(summary = "<key:dock.split.orientation.method.toWidgetOrientation>")
    @WikiReturn("<key:dock.split.orientation.return.orientation>")
    public Orientation toWidgetOrientation() {
        return this == HORIZONTAL ? Orientation.HORIZONTAL : Orientation.VERTICAL;
    }
}
