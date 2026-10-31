package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Итог drag-жеста.
 */
@AiDoc(summary = "Represents the terminal outcome of a drag-and-drop gesture in UniGUI: successfully dropped, dropped without target, or cancelled."
)
@WikiPage(
        title = "DragOutcome",
        slug = "drag-outcome",
        category = "<key:dnd.category>",
        summary = "<key:drag.outcome.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:drag.outcome.section.overview.body>",
        order = 10
)
@WikiLink(
        label = "DragController",
        target = "page:drag-controller",
        description = "<key:drag.controller.summary>"
)
@WikiLink(
        label = "DragSession",
        target = "page:drag-session",
        description = "<key:drag.session.summary>"
)
public enum DragOutcome {
    /**
     * Отпускание над drop-зоной, которая приняла payload.
     */
    DROPPED,
    /**
     * Отпускание там, где нет подходящей drop-зоны.
     */
    NO_TARGET,
    /**
     * Жест отменён программно или источник пропал.
     */
    CANCELLED
}
