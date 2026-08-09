package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.Chart;

import java.util.Locale;

public final class ChartRenderers {
    public static final ChartRenderer DEFAULT = (draw, state) -> {
        if (state.width() <= 0.0f || state.height() <= 0.0f) return;

        draw.rect(state.x(), state.y(), state.width(), state.height(), Paint.stroke(state.axisColor(), 1.0f));
        if (state.chartType() == Chart.Type.BAR) {
            renderBarChart(draw, state);
        } else {
            draw.addLine(state.x() + 4.0f, state.y() + state.height() - 10.0f,
                    state.x() + state.width() - 4.0f, state.y() + state.height() - 10.0f,
                    state.axisColor(), 1.0f);
            draw.addLine(state.x() + 10.0f, state.y() + 4.0f,
                    state.x() + 10.0f, state.y() + state.height() - 4.0f,
                    state.axisColor(), 1.0f);
            SparklineRenderers.DEFAULT.render(draw, state);
        }
    };

    private ChartRenderers() {
    }

    private static void renderBarChart(dev.sixik.unigui.api.render.DrawScope draw, SparklineState state) {
        PlotArea plot = plotArea(state);
        draw.addLine(plot.x(), plot.baseline(), plot.x() + plot.width(), plot.baseline(), state.axisColor(), 1.0f);
        draw.addLine(plot.x() - 4.0f, plot.y(), plot.x() - 4.0f, plot.y() + plot.height(), state.axisColor(), 1.0f);
        if (state.bars().isEmpty()) return;

        Chart.Bar hovered = null;
        for (Chart.Bar bar : state.bars()) {
            if (bar.hovered()) hovered = bar;
            renderBar(draw, state, bar);
            if (state.barValuesVisible() && state.barValuePlacement() != Chart.BarValuePlacement.NONE) {
                renderBarValue(draw, state, bar);
            }
        }
        if (hovered != null) {
            renderBarTooltip(draw, state, hovered);
        }
    }

    private static void renderBar(dev.sixik.unigui.api.render.DrawScope draw, SparklineState state, Chart.Bar bar) {
        if (state.barRenderer() != null) {
            state.barRenderer().render(draw, bar);
            return;
        }
        draw.addRectFilled(bar.x(), bar.y(), bar.width(), bar.height(), bar.hovered() ? state.hoveredBarColor() : state.barColor());
        if (bar.value() == 0.0f) {
            draw.addLine(bar.x(), bar.baseline(), bar.x() + bar.width(), bar.baseline(), state.hoveredBarColor(), 1.25f);
        }
    }

    private static void renderBarValue(dev.sixik.unigui.api.render.DrawScope draw, SparklineState state, Chart.Bar bar) {
        if (state.barValueRenderer() != null) {
            state.barValueRenderer().render(draw, bar);
            return;
        }
        String text = formatValue(bar.value());
        float textWidth = Math.max(bar.width() + 8.0f, text.length() * 6.0f);
        float textHeight = 10.0f;
        float tx = bar.centerX() - textWidth * 0.5f;
        float ty = switch (state.barValuePlacement()) {
            case CENTER -> bar.y() + Math.max(0.0f, bar.height() - textHeight) * 0.5f;
            case BASE -> bar.value() >= 0.0f ? bar.baseline() - textHeight - 2.0f : bar.baseline() + 2.0f;
            case BELOW -> bar.baseline() + 2.0f;
            case HEAD, NONE -> bar.value() >= 0.0f ? bar.y() - textHeight - 2.0f : bar.y() + bar.height() + 2.0f;
        };
        ty = clamp(ty, state.y() + 1.0f, state.y() + state.height() - textHeight - 1.0f);
        TextEngine.draw(draw.context(), RichText.plain(text), tx, ty, textWidth, textHeight,
                Paint.fill(state.valueColor()), draw.transform(), Alignment.CENTER, Alignment.CENTER);
    }

    private static void renderBarTooltip(dev.sixik.unigui.api.render.DrawScope draw, SparklineState state, Chart.Bar bar) {
        if (state.barTooltipRenderer() != null) {
            state.barTooltipRenderer().render(draw, bar);
            return;
        }
        String text = "#" + bar.index() + ": " + formatValue(bar.value());
        float tooltipWidth = Math.max(48.0f, text.length() * 6.0f + 10.0f);
        float tooltipHeight = 16.0f;
        float tx = clamp(bar.centerX() - tooltipWidth * 0.5f,
                state.x(), state.x() + state.width() - tooltipWidth);
        float ty = Math.max(state.y(), bar.y() - tooltipHeight - 8.0f);
        draw.roundedRect(tx, ty, tooltipWidth, tooltipHeight, 3.0f, Paint.fill(state.tooltipBackground()));
        draw.roundedRect(tx, ty, tooltipWidth, tooltipHeight, 3.0f, Paint.stroke(state.tooltipBorder(), 1.0f));
        draw.addText(text, tx + 5.0f, ty + 3.0f, tooltipWidth - 10.0f, tooltipHeight - 4.0f, state.valueColor());
    }

    private static PlotArea plotArea(SparklineState state) {
        float plotX = state.x() + 14.0f;
        float plotY = state.y() + 8.0f;
        float plotWidth = Math.max(0.0f, state.width() - 22.0f);
        float plotHeight = Math.max(0.0f, state.height() - 22.0f);
        float min = 0.0f;
        float max = 0.0f;
        for (Chart.Bar bar : state.bars()) {
            min = Math.min(min, bar.value());
            max = Math.max(max, bar.value());
        }
        if (Math.abs(max - min) < 0.0001f) {
            max = min + 1.0f;
        }
        return new PlotArea(plotX, plotY, plotWidth, plotHeight, min, max);
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

    private record PlotArea(float x, float y, float width, float height, float min, float max) {
        private float baseline() {
            return valueY(0.0f);
        }

        private float valueY(float value) {
            float range = Math.max(0.0001f, max - min);
            return y + height - ((value - min) / range) * height;
        }
    }
}

