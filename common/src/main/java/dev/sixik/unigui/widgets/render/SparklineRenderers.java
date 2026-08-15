package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.display.Sparkline;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Locale;

public final class SparklineRenderers {
    private static final float DEFAULT_POINT_RADIUS = 3.5f;

    public static final SparklineRenderer DEFAULT = (draw, state) -> {
        if (state.points().size() < 2) return;

        List<DrawPoint> line = new ObjectArrayList<>(state.points().size());
        for (Sparkline.SparkPoint point : state.points()) {
            line.add(new DrawPoint(point.x(), point.y()));
        }

        if (state.fillVisible()) {
            float baseline = state.y() + state.height();
            for (int i = 0; i < line.size() - 1; i++) {
                DrawPoint left = line.get(i);
                DrawPoint right = line.get(i + 1);
                draw.addQuadFilled(
                        left,
                        right,
                        new DrawPoint(right.x(), baseline),
                        new DrawPoint(left.x(), baseline),
                        state.fillColor());
            }
        }

        draw.addPolyline(line, state.lineColor(), false, 1.5f);
        renderInteractivePoints(draw, state);
    };

    private SparklineRenderers() {
    }

    private static void renderInteractivePoints(DrawScope draw, SparklineState state) {
        Sparkline.SparkPoint hovered = null;
        for (Sparkline.SparkPoint point : state.points()) {
            if (!point.renderable()) continue;
            if (point.hovered()) hovered = point;
            renderPoint(draw, state, point);
            if (state.pointLabelsVisible() && state.pointLabelPlacement() != Sparkline.PointLabelPlacement.NONE) {
                renderPointLabel(draw, state, point);
            }
        }
        if (hovered != null) {
            renderPointTooltip(draw, state, hovered);
        }
    }

    private static void renderPoint(DrawScope draw, SparklineState state, Sparkline.SparkPoint point) {
        if (state.pointRenderer() != null) {
            state.pointRenderer().render(draw, point);
            return;
        }
        float radius = point.hovered() ? DEFAULT_POINT_RADIUS + 1.25f : DEFAULT_POINT_RADIUS;
        draw.addCircleFilled(point.x(), point.y(), radius,
                point.hovered() ? state.hoveredPointColor() : state.pointColor(), 16);
        draw.addCircle(point.x(), point.y(), radius + 1.0f,
                MutableColor.rgba(0.0f, 0.0f, 0.0f, point.hovered() ? 0.65f : 0.35f), 16, 1.0f);
    }

    private static void renderPointLabel(DrawScope draw, SparklineState state, Sparkline.SparkPoint point) {
        if (state.pointLabelRenderer() != null) {
            state.pointLabelRenderer().render(draw, point);
            return;
        }
        String text = formatValue(point.value());
        float textWidth = Math.max(18.0f, text.length() * 6.0f);
        float textHeight = 10.0f;
        float tx = point.x() - textWidth * 0.5f;
        float ty = switch (state.pointLabelPlacement()) {
            case CENTER -> point.y() - textHeight * 0.5f;
            case BELOW -> point.y() + 5.0f;
            case ABOVE, NONE -> point.y() - textHeight - 5.0f;
        };
        TextEngine.draw(draw.context(), RichText.plain(text), tx, ty, textWidth, textHeight,
                Paint.fill(state.labelColor()), draw.transform(), Alignment.CENTER, Alignment.CENTER);
    }

    private static void renderPointTooltip(DrawScope draw, SparklineState state, Sparkline.SparkPoint point) {
        if (state.pointTooltipRenderer() != null) {
            state.pointTooltipRenderer().render(draw, point);
            return;
        }
        String text = "#" + point.index() + ": " + formatValue(point.value());
        float tooltipWidth = Math.max(44.0f, text.length() * 6.0f + 10.0f);
        float tooltipHeight = 16.0f;
        float tx = clamp(point.x() - tooltipWidth * 0.5f, state.x(), state.x() + state.width() - tooltipWidth);
        float ty = Math.max(state.y(), point.y() - tooltipHeight - 8.0f);
        draw.roundedRect(tx, ty, tooltipWidth, tooltipHeight, 3.0f, Paint.fill(state.tooltipBackground()));
        draw.roundedRect(tx, ty, tooltipWidth, tooltipHeight, 3.0f, Paint.stroke(state.tooltipBorder(), 1.0f));
        draw.addText(text, tx + 5.0f, ty + 3.0f, tooltipWidth - 10.0f, tooltipHeight - 4.0f, state.labelColor());
    }

    private static String formatValue(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001f) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static float clamp(float value, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }
}

