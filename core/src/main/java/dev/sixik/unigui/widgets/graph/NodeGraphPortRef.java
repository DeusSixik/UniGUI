package dev.sixik.unigui.widgets.graph;




import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(summary = "Identifies a specific port belonging to an item in a node graph.")
@WikiPage(
        title = "NodeGraphPortRef",
        slug = "node-graph-port-ref",
        category = "<key:graph.category>",
        summary = "<key:node.graph.port.ref.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.port.ref.section.overview.body>",
        order = 10
)
public record NodeGraphPortRef(
        @WikiParameter(description = "<key:node.graph.port.ref.param.itemId>")
        String itemId,
        @WikiParameter(description = "<key:node.graph.port.ref.param.portId>")
        String portId
) {
    public NodeGraphPortRef {
        itemId = normalize(itemId);
        portId = normalize(portId);
    }

    @AiDoc(summary = "Returns true if either item ID or port ID is empty.")
    @WikiMethod(summary = "<key:node.graph.port.ref.method.empty>")
    @WikiReturn("<key:node.graph.port.ref.return.empty>")
    public boolean empty() {
        return itemId.isEmpty() || portId.isEmpty();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

