package dev.sixik.unigui.widgets.graph;




import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Visual placement edge for a node-graph port.
 *
 * <p>PortSide describes where the port is rendered and hit-tested on the node
 * bounds. It does not define input/output connection direction; use
 * {@link NodeGraphPortKind} for that.</p>
 */
@AiDoc(summary = "Visual placement edge for a node-graph port: LEFT, RIGHT, TOP, or BOTTOM.")
@WikiPage(
        title = "NodeGraphPortSide",
        slug = "node-graph-port-side",
        category = "<key:graph.category>",
        summary = "<key:node.graph.port.side.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.port.side.section.overview.body>",
        order = 10
)
public enum NodeGraphPortSide {
    /** Render the port on the left edge. */
    LEFT,

    /** Render the port on the right edge. */
    RIGHT,

    /** Render the port on the top edge. */
    TOP,

    /** Render the port on the bottom edge. */
    BOTTOM
}