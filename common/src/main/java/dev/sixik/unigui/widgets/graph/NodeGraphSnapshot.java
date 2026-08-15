package dev.sixik.unigui.widgets.graph;

import java.util.List;

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

