package dev.sixik.unigui.widgets.graph;




import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(summary = "Serializable snapshot describing a connection between two node ports.")
@WikiPage(
        title = "NodeGraphConnectionSnapshot",
        slug = "node-graph-connection-snapshot",
        category = "<key:graph.category>",
        summary = "<key:node.graph.connection.snapshot.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.connection.snapshot.section.overview.body>",
        order = 10
)
public record NodeGraphConnectionSnapshot(
        String id,
        NodeGraphPortRef from,
        NodeGraphPortRef to,
        boolean enabled,
        String type
) {
    public NodeGraphConnectionSnapshot {
        id = id == null ? "" : id;
        from = from == null ? new NodeGraphPortRef("", "") : from;
        to = to == null ? new NodeGraphPortRef("", "") : to;
        type = type == null ? "" : type;
    }
}

