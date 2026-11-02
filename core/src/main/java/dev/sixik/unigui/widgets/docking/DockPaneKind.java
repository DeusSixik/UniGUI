package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(summary = "Classification of a docking pane: DOCUMENT (primary central workspace content) or TOOL (dockable side utility tool window)."
)
@WikiPage(
        title = "DockPaneKind",
        slug = "dock-pane-kind",
        category = "<key:docking.category>",
        summary = "<key:dock.pane.kind.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:dock.pane.kind.section.overview.body>",
        order = 10
)
@WikiLink(
        label = "DockPane",
        target = "page:dock-pane",
        description = "<key:dock.pane.summary>"
)
@WikiLink(
        label = "DockingRoot",
        target = "page:docking-root",
        description = "<key:docking.root.summary>"
)
public enum DockPaneKind {
    DOCUMENT,
    TOOL
}
