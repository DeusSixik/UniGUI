package dev.sixik.unigui.widgets.graph;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.List;

@AiDoc(summary = "Serializable snapshot capturing node item position, dimensions, capabilities, and ports.")
@WikiPage(
        title = "NodeGraphItemSnapshot",
        slug = "node-graph-item-snapshot",
        category = "<key:graph.category>",
        summary = "<key:node.graph.item.snapshot.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.item.snapshot.section.overview.body>",
        order = 10
)
public record NodeGraphItemSnapshot(
        String id,
        String contentType,
        float x,
        float y,
        float width,
        float height,
        boolean selectable,
        boolean movable,
        boolean resizable,
        boolean visible,
        List<NodeGraphPortSnapshot> ports
) {
    public NodeGraphItemSnapshot {
        id = id == null ? "" : id;
        contentType = contentType == null ? "" : contentType;
        ports = ports == null ? List.of() : List.copyOf(ports);
    }
}

