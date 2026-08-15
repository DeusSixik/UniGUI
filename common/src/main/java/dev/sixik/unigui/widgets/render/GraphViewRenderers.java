package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.graph.GraphView;

public final class GraphViewRenderers {
    public static final GraphViewRenderer DEFAULT = (draw, state) -> {
        if (state.width() <= 0.0f || state.height() <= 0.0f) return;

        draw.rect(state.x(), state.y(), state.width(), state.height(), Paint.stroke(state.edgeColor(), 1.0f));
        for (GraphView.Edge edge : state.edges()) {
            GraphView.NodePoint from = node(state, edge.from());
            GraphView.NodePoint to = node(state, edge.to());
            if (from != null && to != null) {
                draw.addLine(from.x(), from.y(), to.x(), to.y(), state.edgeColor(), 1.0f);
            }
        }

        GraphView.NodePoint hovered = null;
        for (GraphView.NodePoint point : state.nodes()) {
            if (point.hovered()) hovered = point;
            renderNode(draw, state, point);
            if (state.nodeLabelsVisible() && state.nodeLabelPlacement() != GraphView.NodeLabelPlacement.NONE) {
                renderNodeLabel(draw, state, point);
            }
        }
        if (hovered != null) {
            renderNodeTooltip(draw, state, hovered);
        }
    };

    private GraphViewRenderers() {
    }

    private static GraphView.NodePoint node(GraphViewState state, String id) {
        for (GraphView.NodePoint node : state.nodes()) {
            if (node.id().equals(id)) return node;
        }
        return null;
    }

    private static void renderNode(DrawScope draw, GraphViewState state, GraphView.NodePoint node) {
        if (state.nodeRenderer() != null) {
            state.nodeRenderer().render(draw, node);
            return;
        }
        float radius = node.hovered() ? state.nodeRadius() + 1.5f : state.nodeRadius();
        draw.addCircleFilled(node.x(), node.y(), radius, node.hovered() ? state.hoveredNodeColor() : state.nodeColor(), 16);
        draw.addCircle(node.x(), node.y(), radius + 1.0f,
                MutableColor.rgba(0.0f, 0.0f, 0.0f, node.hovered() ? 0.70f : 0.35f), 16, 1.0f);
    }

    private static void renderNodeLabel(DrawScope draw, GraphViewState state, GraphView.NodePoint node) {
        if (state.nodeLabelRenderer() != null) {
            state.nodeLabelRenderer().render(draw, node);
            return;
        }
        String text = state.nodeLabelProvider().apply(node);
        if (text == null || text.isEmpty()) return;
        float textWidth = Math.max(14.0f, text.length() * 6.0f);
        float textHeight = 10.0f;
        float tx = switch (state.nodeLabelPlacement()) {
            case CENTER -> node.x() - textWidth * 0.5f;
            case ABOVE, BELOW -> node.x() - textWidth * 0.5f;
            case RIGHT, NONE -> node.x() + state.nodeRadius() + 3.0f;
        };
        float ty = switch (state.nodeLabelPlacement()) {
            case CENTER -> node.y() - textHeight * 0.5f;
            case ABOVE -> node.y() - state.nodeRadius() - textHeight - 3.0f;
            case BELOW -> node.y() + state.nodeRadius() + 3.0f;
            case RIGHT, NONE -> node.y() - textHeight * 0.5f;
        };
        TextEngine.draw(draw.context(), RichText.plain(text), tx, ty, textWidth, textHeight,
                Paint.fill(state.labelColor()), draw.transform(), Alignment.CENTER, Alignment.CENTER);
    }

    private static void renderNodeTooltip(DrawScope draw, GraphViewState state, GraphView.NodePoint node) {
        if (state.nodeTooltipRenderer() != null) {
            state.nodeTooltipRenderer().render(draw, node);
            return;
        }
        String text = state.nodeTooltipProvider().apply(node);
        if (text == null || text.isEmpty()) return;
        float tooltipWidth = Math.max(44.0f, text.length() * 6.0f + 10.0f);
        float tooltipHeight = 16.0f;
        float tx = clamp(node.x() - tooltipWidth * 0.5f,
                state.x(), state.x() + state.width() - tooltipWidth);
        float ty = Math.max(state.y(), node.y() - state.nodeRadius() - tooltipHeight - 8.0f);
        draw.roundedRect(tx, ty, tooltipWidth, tooltipHeight, 3.0f, Paint.fill(state.tooltipBackground()));
        draw.roundedRect(tx, ty, tooltipWidth, tooltipHeight, 3.0f, Paint.stroke(state.tooltipBorder(), 1.0f));
        draw.addText(text, tx + 5.0f, ty + 3.0f, tooltipWidth - 10.0f, tooltipHeight - 4.0f, state.labelColor());
    }

    private static float clamp(float value, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }
}

