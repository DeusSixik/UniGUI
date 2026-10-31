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
 * Connection direction for a node-graph port.
 *
 * <p>PortKind controls connection validation: {@link #OUTPUT} and
 * {@link #BIDIRECTIONAL} can start connection drags, while {@link #INPUT} and
 * {@link #BIDIRECTIONAL} can receive them. It is not a Blueprint-style
 * EXEC/DATA category; use {@link NodeGraphPort#type()} today, or introduce a
 * separate data/flow enum if the node model needs that later.</p>
 */
@AiDoc(summary = "Connection direction for a node-graph port: INPUT, OUTPUT, or BIDIRECTIONAL.")
@WikiPage(
        title = "NodeGraphPortKind",
        slug = "node-graph-port-kind",
        category = "<key:graph.category>",
        summary = "<key:node.graph.port.kind.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.port.kind.section.overview.body>",
        order = 10
)
public enum NodeGraphPortKind {
    /** Receives connections. */
    INPUT,

    /** Starts outgoing connections. */
    OUTPUT,

    /** Can both start and receive connections. */
    BIDIRECTIONAL
}