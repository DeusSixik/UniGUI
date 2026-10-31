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

@AiDoc(summary = "Comprehensive serializable snapshot representing the entire node graph state, viewport, items, and connections.")
@WikiPage(
        title = "NodeGraphSnapshot",
        slug = "node-graph-snapshot",
        category = "<key:graph.category>",
        summary = "<key:node.graph.snapshot.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.snapshot.section.overview.body>",
        order = 10
)
public record NodeGraphSnapshot(
        float viewportX,
        float viewportY,
        float zoom,
        List<NodeGraphItemSnapshot> items,
        List<NodeGraphConnectionSnapshot> connections,
        List<String> selectedItemIds,
        List<String> selectedConnectionIds
) {
    public NodeGraphSnapshot {
        items = items == null ? List.of() : List.copyOf(items);
        connections = connections == null ? List.of() : List.copyOf(connections);
        selectedItemIds = selectedItemIds == null ? List.of() : List.copyOf(selectedItemIds);
        selectedConnectionIds = selectedConnectionIds == null ? List.of() : List.copyOf(selectedConnectionIds);
    }
}

