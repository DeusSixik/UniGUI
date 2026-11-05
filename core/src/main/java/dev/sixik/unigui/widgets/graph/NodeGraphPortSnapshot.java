package dev.sixik.unigui.widgets.graph;




import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(summary = "Serializable snapshot representing the state, geometry offset, and configuration of a node port.")
@WikiPage(
        title = "NodeGraphPortSnapshot",
        slug = "node-graph-port-snapshot",
        category = "<key:graph.category>",
        summary = "<key:node.graph.port.snapshot.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.port.snapshot.section.overview.body>",
        order = 10
)
public record NodeGraphPortSnapshot(
        String id,
        NodeGraphPortKind kind,
        NodeGraphPortSide side,
        float offset,
        String type,
        boolean enabled,
        boolean visible
) {
    public NodeGraphPortSnapshot {
        id = id == null ? "" : id;
        kind = kind == null ? NodeGraphPortKind.BIDIRECTIONAL : kind;
        side = side == null ? NodeGraphPortSide.RIGHT : side;
        type = type == null ? "" : type;
    }
}

