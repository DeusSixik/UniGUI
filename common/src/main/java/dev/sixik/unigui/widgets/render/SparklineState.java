package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.widgets.Chart;
import dev.sixik.unigui.widgets.Sparkline;

import java.util.List;

public record SparklineState(
        float x,
        float y,
        float width,
        float height,
        List<Sparkline.SparkPoint> points,
        boolean fillVisible,
        boolean pointLabelsVisible,
        Sparkline.PointLabelPlacement pointLabelPlacement,
        ColorView lineColor,
        ColorView fillColor,
        ColorView pointColor,
        ColorView hoveredPointColor,
        ColorView labelColor,
        ColorView tooltipBackground,
        ColorView tooltipBorder,
        Sparkline.SparkPointRenderer pointRenderer,
        Sparkline.SparkPointLabelRenderer pointLabelRenderer,
        Sparkline.SparkPointTooltipRenderer pointTooltipRenderer,
        Chart.Type chartType,
        List<Chart.Bar> bars,
        boolean barValuesVisible,
        Chart.BarValuePlacement barValuePlacement,
        ColorView axisColor,
        ColorView barColor,
        ColorView hoveredBarColor,
        ColorView valueColor,
        Chart.BarRenderer barRenderer,
        Chart.BarValueRenderer barValueRenderer,
        Chart.BarTooltipRenderer barTooltipRenderer
) {
    public SparklineState(
            float x,
            float y,
            float width,
            float height,
            List<Sparkline.SparkPoint> points,
            boolean fillVisible,
            boolean pointLabelsVisible,
            Sparkline.PointLabelPlacement pointLabelPlacement,
            ColorView lineColor,
            ColorView fillColor,
            ColorView pointColor,
            ColorView hoveredPointColor,
            ColorView labelColor,
            ColorView tooltipBackground,
            ColorView tooltipBorder,
            Sparkline.SparkPointRenderer pointRenderer,
            Sparkline.SparkPointLabelRenderer pointLabelRenderer,
            Sparkline.SparkPointTooltipRenderer pointTooltipRenderer) {
        this(x, y, width, height, points, fillVisible, pointLabelsVisible, pointLabelPlacement,
                lineColor, fillColor, pointColor, hoveredPointColor, labelColor,
                tooltipBackground, tooltipBorder, pointRenderer, pointLabelRenderer, pointTooltipRenderer,
                null, List.of(), false, Chart.BarValuePlacement.HEAD, null, null, null, null,
                null, null, null);
    }

    public SparklineState {
        points = points == null ? List.of() : List.copyOf(points);
        pointLabelPlacement = pointLabelPlacement == null ? Sparkline.PointLabelPlacement.ABOVE : pointLabelPlacement;
        chartType = chartType == null ? Chart.Type.LINE : chartType;
        bars = bars == null ? List.of() : List.copyOf(bars);
        barValuePlacement = barValuePlacement == null ? Chart.BarValuePlacement.HEAD : barValuePlacement;
    }
}
