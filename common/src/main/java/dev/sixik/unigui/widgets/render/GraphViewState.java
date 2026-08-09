package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.widgets.GraphView;

import java.util.List;
import java.util.function.Function;

public record GraphViewState(
        float x,
        float y,
        float width,
        float height,
        List<GraphView.NodePoint> nodes,
        List<GraphView.Edge> edges,
        boolean nodeLabelsVisible,
        GraphView.NodeLabelPlacement nodeLabelPlacement,
        float nodeRadius,
        ColorView nodeColor,
        ColorView hoveredNodeColor,
        ColorView edgeColor,
        ColorView labelColor,
        ColorView tooltipBackground,
        ColorView tooltipBorder,
        GraphView.GraphNodeRenderer nodeRenderer,
        GraphView.GraphNodeLabelRenderer nodeLabelRenderer,
        GraphView.GraphNodeTooltipRenderer nodeTooltipRenderer,
        Function<GraphView.NodePoint, String> nodeLabelProvider,
        Function<GraphView.NodePoint, String> nodeTooltipProvider
) {
    public GraphViewState {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
        nodeLabelPlacement = nodeLabelPlacement == null ? GraphView.NodeLabelPlacement.RIGHT : nodeLabelPlacement;
        nodeLabelProvider = nodeLabelProvider == null ? GraphView.NodePoint::id : nodeLabelProvider;
        nodeTooltipProvider = nodeTooltipProvider == null ? GraphView.NodePoint::id : nodeTooltipProvider;
    }
}

