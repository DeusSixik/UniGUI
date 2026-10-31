package dev.sixik.unigui.widgets.graph;




import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(summary = "Strategy interface defining rules and compatibility validation for forming connections between ports.")
@WikiPage(
        title = "NodeGraphConnectionPolicy",
        slug = "node-graph-connection-policy",
        category = "<key:graph.category>",
        summary = "<key:node.graph.connection.policy.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.connection.policy.section.overview.body>",
        order = 10
)
@FunctionalInterface
public interface NodeGraphConnectionPolicy {
    NodeGraphConnectionPolicy DEFAULT = (graph, from, to) -> {
        if (graph == null || from == null || to == null || from.empty() || to.empty()) {
            return NodeGraphConnectionValidation.invalid("Missing port");
        }
        if (from.equals(to)) {
            return NodeGraphConnectionValidation.invalid("Cannot connect a port to itself");
        }
        NodeGraphPort fromPort = graph.port(from);
        NodeGraphPort toPort = graph.port(to);
        if (fromPort == null || toPort == null) {
            return NodeGraphConnectionValidation.invalid("Port not found");
        }
        if (!fromPort.enabled() || !fromPort.visible() || !toPort.enabled() || !toPort.visible()) {
            return NodeGraphConnectionValidation.invalid("Port disabled");
        }
        if (!canStart(fromPort.kind())) {
            return NodeGraphConnectionValidation.invalid("Source port cannot start a connection");
        }
        if (!canEnd(toPort.kind())) {
            return NodeGraphConnectionValidation.invalid("Target port cannot accept a connection");
        }
        if (!fromPort.type().isEmpty() && !toPort.type().isEmpty() && !fromPort.type().equals(toPort.type())) {
            return NodeGraphConnectionValidation.invalid("Port type mismatch");
        }
        if (graph.hasConnection(from, to)) {
            return NodeGraphConnectionValidation.invalid("Connection already exists");
        }
        return NodeGraphConnectionValidation.accepted();
    };

    @AiDoc(summary = "Validates whether a connection can be established between source and target ports.")
    @WikiMethod(summary = "<key:node.graph.connection.policy.method.validate>")
    @WikiReturn("<key:node.graph.connection.policy.return.validation>")
    NodeGraphConnectionValidation validate(@WikiParameter(name = "graph", description = "<key:node.graph.connection.policy.param.graph>") NodeGraph graph, @WikiParameter(name = "from", description = "<key:node.graph.connection.policy.param.from>") NodeGraphPortRef from, @WikiParameter(name = "to", description = "<key:node.graph.connection.policy.param.to>") NodeGraphPortRef to);

    private static boolean canStart(NodeGraphPortKind kind) {
        return kind == NodeGraphPortKind.OUTPUT || kind == NodeGraphPortKind.BIDIRECTIONAL;
    }

    private static boolean canEnd(NodeGraphPortKind kind) {
        return kind == NodeGraphPortKind.INPUT || kind == NodeGraphPortKind.BIDIRECTIONAL;
    }
}
