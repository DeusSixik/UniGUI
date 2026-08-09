package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;

public final class NodeGraphRenderers {
    public static final NodeGraphRenderer DEFAULT = (draw, state) -> {
        if (state.width() <= 0.0f || state.height() <= 0.0f) return;
        if (state.phase() == NodeGraphRenderPhase.BACKGROUND) {
            draw.rect(state.x(), state.y(), state.width(), state.height(), Paint.fill(state.backgroundColor()));
            drawGrid(draw, state);
            for (NodeGraphConnectionState connection : state.connections()) {
                if (!connection.enabled()) continue;
                ColorView color = connection.selected() || connection.hovered()
                        ? state.selectedConnectionColor()
                        : state.connectionColor();
                float thickness = connection.selected() || connection.hovered() ? 2.0f : 1.0f;
                draw.addLine(connection.startX(), connection.startY(), connection.endX(), connection.endY(), color, thickness);
            }
            return;
        }

        for (NodeGraphItemState item : state.items()) {
            ColorView color = item.selected()
                    ? state.selectedItemBorderColor()
                    : item.hovered() || item.dragging()
                    ? state.hoveredItemBorderColor()
                    : state.itemBorderColor();
            float thickness = item.selected() || item.dragging() ? 2.0f : 1.0f;
            draw.roundedRect(item.x(), item.y(), item.width(), item.height(), 4.0f, Paint.stroke(color, thickness));
            if (item.selected() && item.resizable()) {
                float handle = 8.0f;
                draw.rect(item.x() + item.width() - handle, item.y() + item.height() - handle,
                        handle, handle, Paint.fill(state.resizeHandleColor()));
            }
        }
        NodeGraphConnectionPreviewState preview = state.connectionPreview();
        if (preview.visible()) {
            draw.addLine(preview.startX(), preview.startY(), preview.endX(), preview.endY(),
                    preview.valid() ? state.connectionPreviewColor() : state.invalidConnectionPreviewColor(),
                    preview.valid() ? 2.0f : 1.5f);
        }
        for (NodeGraphPortState port : state.ports()) {
            ColorView color = port.hovered() ? state.hoveredPortColor() : state.portColor();
            draw.addCircleFilled(port.x(), port.y(), port.radius(), color, 12);
            draw.addCircle(port.x(), port.y(), port.radius() + 1.0f,
                    port.connectable() ? state.connectionPreviewColor() : state.itemBorderColor(),
                    12,
                    port.hovered() ? 1.5f : 1.0f);
        }
        NodeGraphSelectionBoxState selectionBox = state.selectionBox();
        if (selectionBox.visible()) {
            draw.rect(selectionBox.x(), selectionBox.y(), selectionBox.width(), selectionBox.height(),
                    Paint.fill(state.selectionBoxFillColor()));
            draw.rect(selectionBox.x(), selectionBox.y(), selectionBox.width(), selectionBox.height(),
                    Paint.stroke(state.selectionBoxBorderColor(), 1.0f));
        }
    };

    private NodeGraphRenderers() {
    }

    public static void drawGrid(DrawScope draw, NodeGraphState state) {
        float step = Math.max(2.0f, state.gridSize() * state.zoom());
        float majorStep = step * 4.0f;
        float startX = state.x() + positiveModulo(state.viewportX(), step);
        float startY = state.y() + positiveModulo(state.viewportY(), step);

        for (float x = startX; x <= state.x() + state.width(); x += step) {
            draw.line(x, state.y(), x, state.y() + state.height(), Paint.stroke(state.gridColor(), 1.0f));
        }
        for (float y = startY; y <= state.y() + state.height(); y += step) {
            draw.line(state.x(), y, state.x() + state.width(), y, Paint.stroke(state.gridColor(), 1.0f));
        }

        float majorStartX = state.x() + positiveModulo(state.viewportX(), majorStep);
        float majorStartY = state.y() + positiveModulo(state.viewportY(), majorStep);
        for (float x = majorStartX; x <= state.x() + state.width(); x += majorStep) {
            draw.line(x, state.y(), x, state.y() + state.height(), Paint.stroke(state.majorGridColor(), 1.0f));
        }
        for (float y = majorStartY; y <= state.y() + state.height(); y += majorStep) {
            draw.line(state.x(), y, state.x() + state.width(), y, Paint.stroke(state.majorGridColor(), 1.0f));
        }
    }

    private static float positiveModulo(float value, float modulo) {
        if (modulo <= 0.0f) return 0.0f;
        float result = value % modulo;
        return result < 0.0f ? result + modulo : result;
    }
}
