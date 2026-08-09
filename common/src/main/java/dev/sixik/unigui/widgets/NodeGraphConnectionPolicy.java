package dev.sixik.unigui.widgets;

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

    NodeGraphConnectionValidation validate(NodeGraph graph, NodeGraphPortRef from, NodeGraphPortRef to);

    private static boolean canStart(NodeGraphPortKind kind) {
        return kind == NodeGraphPortKind.OUTPUT || kind == NodeGraphPortKind.BIDIRECTIONAL;
    }

    private static boolean canEnd(NodeGraphPortKind kind) {
        return kind == NodeGraphPortKind.INPUT || kind == NodeGraphPortKind.BIDIRECTIONAL;
    }
}
