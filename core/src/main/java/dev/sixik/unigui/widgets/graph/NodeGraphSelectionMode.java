package dev.sixik.unigui.widgets.graph;




import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(summary = "Selection interaction modes for items and connections in a node graph: NONE, SINGLE, or MULTIPLE.")
@WikiPage(
        title = "NodeGraphSelectionMode",
        slug = "node-graph-selection-mode",
        category = "<key:graph.category>",
        summary = "<key:node.graph.selection.mode.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.selection.mode.section.overview.body>",
        order = 10
)
public enum NodeGraphSelectionMode {
    NONE,
    SINGLE,
    MULTIPLE
}

