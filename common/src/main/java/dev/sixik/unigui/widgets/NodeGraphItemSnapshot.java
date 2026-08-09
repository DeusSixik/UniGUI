package dev.sixik.unigui.widgets;

import java.util.List;

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

