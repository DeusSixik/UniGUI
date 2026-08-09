package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;

import java.util.List;

public record NodeGraphState(
        float x,
        float y,
        float width,
        float height,
        float viewportX,
        float viewportY,
        float zoom,
        float gridSize,
        NodeGraphRenderPhase phase,
        List<NodeGraphItemState> items,
        List<NodeGraphPortState> ports,
        List<NodeGraphConnectionState> connections,
        NodeGraphConnectionPreviewState connectionPreview,
        NodeGraphSelectionBoxState selectionBox,
        ColorView backgroundColor,
        ColorView gridColor,
        ColorView majorGridColor,
        ColorView itemBorderColor,
        ColorView hoveredItemBorderColor,
        ColorView selectedItemBorderColor,
        ColorView portColor,
        ColorView hoveredPortColor,
        ColorView connectionColor,
        ColorView selectedConnectionColor,
        ColorView connectionPreviewColor,
        ColorView invalidConnectionPreviewColor,
        ColorView selectionBoxFillColor,
        ColorView selectionBoxBorderColor,
        ColorView resizeHandleColor
) {
    public NodeGraphState {
        zoom = Float.isFinite(zoom) && zoom > 0.0f ? zoom : 1.0f;
        gridSize = Float.isFinite(gridSize) && gridSize > 0.0f ? gridSize : 24.0f;
        phase = phase == null ? NodeGraphRenderPhase.BACKGROUND : phase;
        items = items == null ? List.of() : List.copyOf(items);
        ports = ports == null ? List.of() : List.copyOf(ports);
        connections = connections == null ? List.of() : List.copyOf(connections);
        connectionPreview = connectionPreview == null ? NodeGraphConnectionPreviewState.HIDDEN : connectionPreview;
        selectionBox = selectionBox == null ? NodeGraphSelectionBoxState.HIDDEN : selectionBox;
    }
}
