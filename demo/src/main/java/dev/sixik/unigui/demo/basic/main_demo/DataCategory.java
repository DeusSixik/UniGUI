package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.api.selection.SelectionMode;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.data.VirtualTableView;
import dev.sixik.unigui.widgets.display.Chart;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.Sparkline;
import dev.sixik.unigui.widgets.graph.GraphView;

import java.util.List;

final class DataCategory extends DemoCategory {
    private static final String[] SERVICES = {"Gateway", "Accounts", "Search", "Media", "Billing", "Events"};
    private static final String[] STATES = {"Healthy", "Healthy", "Degraded", "Healthy", "Paused", "Healthy"};
    private static final List<Control> CONTROLS = List.of(
            new Control("virtual-table", "VirtualTableView", "Render large row sets efficiently with sortable and editable cells."),
            new Control("chart", "Chart", "Compare values with line and bar visualizations."),
            new Control("sparkline", "Sparkline", "Show compact trends beside related data."),
            new Control("graph-view", "GraphView", "Visualize relationships between nodes."));

    @Override
    String title() {
        return "Data";
    }

    @Override
    boolean expandedByDefault() {
        return false;
    }

    @Override
    List<Control> controls() {
        return CONTROLS;
    }

    @Override
    void addDemo(String controlId, VBox root) {
        switch (controlId) {
            case "virtual-table" -> addVirtualTables(root);
            case "chart" -> addCharts(root);
            case "sparkline" -> addSparklines(root);
            case "graph-view" -> addGraph(root);
            default -> root.addChild(samplePanel("Preview", "Interactive sample", new Label(controlId), ""));
        }
    }

    private void addVirtualTables(VBox root) {
        VirtualTableView virtualized = table(2500, new float[]{128.0f, 112.0f, 96.0f});
        virtualized.cellTextProvider((row, column) -> switch (column) {
            case 0 -> "Record " + (row + 1);
            case 1 -> STATES[row % STATES.length];
            default -> Integer.toString(35 + (row * 17) % 966);
        });
        virtualized.sortKeyProvider((row, column) -> column == 2
                ? Integer.valueOf(35 + (row * 17) % 966)
                : column == 0 ? "Record " + (row + 1) : STATES[row % STATES.length]);
        root.addChild(samplePanel("Large virtual dataset", "Only visible rows are materialized; click a column header to sort.", virtualized,
                "VirtualTableView table = new VirtualTableView().rowCount(2500);\ntable.addColumn(\"Service\", 128).addColumn(\"Status\", 112).addColumn(\"Requests\", 96);\ntable.cellTextProvider((row, column) -> valueAt(row, column));\ntable.sortKeyProvider((row, column) -> sortKeyAt(row, column));"));

        String[] services = SERVICES.clone();
        String[] states = STATES.clone();
        VirtualTableView editable = table(services.length, new float[]{128.0f, 112.0f});
        editable.editable(true);
        editable.cellTextProvider((row, column) -> column == 0 ? services[row] : states[row]);
        editable.onCellEditCommitted(event -> {
            int row = event.row();
            int column = event.column();
            if (column == 0 && row >= 0 && row < services.length) services[row] = event.newText();
            if (column == 1 && row >= 0 && row < states.length) states[row] = event.newText();
        });
        root.addChild(samplePanel("Inline editing", "Double-click a cell or press F2 to edit it; committed values update the backing arrays.", editable,
                "table.editable(true);\ntable.cellTextProvider((row, column) -> model[row][column]);\ntable.onCellEditCommitted(event ->\n        model[event.row()][event.column()] = event.newText());"));

        VirtualTableView generated = table(18, new float[]{92.0f, 92.0f, 92.0f});
        generated.selectionMode(SelectionMode.MULTIPLE);
        generated.cellTextProvider((row, column) -> "R" + (row + 1) + " / C" + (column + 1));
        root.addChild(samplePanel("Selection and generated cells", "A small generated grid with multi-row selection.", generated,
                "table.rowCount(18)\n        .selectionMode(SelectionMode.MULTIPLE)\n        .cellTextProvider((row, column) -> cellLabel(row, column));"));
    }

    private VirtualTableView table(int rows, float[] widths) {
        VirtualTableView table = new VirtualTableView().rowCount(rows).rowHeight(30.0f);
        String[] headers = {"Service", "Status", "Requests"};
        for (int i = 0; i < widths.length; i++) table.addColumn(headers[i], widths[i]);
        table.style(style -> style
                .put(StyleKeys.TEXT_COLOR, UniGuiDemo.GalleryTemplate.TEXT_DARK)
                .put(StyleKeys.TABLE_HEADER_COLOR, UniGuiDemo.GalleryTemplate.NAV_GROUP)
                .put(StyleKeys.TABLE_HEADER_HOVER_COLOR, UniGuiDemo.GalleryTemplate.NAV_SELECTED)
                .put(StyleKeys.TABLE_HEADER_TEXT_COLOR, UniGuiDemo.GalleryTemplate.color(255, 255, 255))
                .put(StyleKeys.TABLE_ROW_COLOR, UniGuiDemo.GalleryTemplate.color(255, 255, 255))
                .put(StyleKeys.TABLE_ALTERNATE_ROW_COLOR, UniGuiDemo.GalleryTemplate.color(239, 244, 248))
                .put(StyleKeys.TABLE_HOVER_COLOR, UniGuiDemo.GalleryTemplate.color(225, 234, 242))
                .put(StyleKeys.TABLE_SELECTED_COLOR, UniGuiDemo.GalleryTemplate.color(206, 226, 243))
                .put(StyleKeys.TABLE_GRID_COLOR, UniGuiDemo.GalleryTemplate.color(198, 210, 220))
                .put(StyleKeys.TABLE_ACTIVE_CELL_COLOR, UniGuiDemo.GalleryTemplate.color(182, 216, 242))
                .put(StyleKeys.TABLE_SORT_INDICATOR_COLOR, UniGuiDemo.GalleryTemplate.color(255, 255, 255)));
        table.layout(style -> style.widthPercent(100.0f).height(210.0f));
        return table;
    }

    private void addCharts(VBox root) {
        Chart line = new Chart().values(List.of(18, 24, 21, 36, 31, 48, 43, 62)).type(Chart.Type.LINE);
        lightPlot(line);
        line.layout(style -> style.widthPercent(100.0f).height(210.0f));
        root.addChild(samplePanel("Traffic trend", "A line chart emphasizes changes across an ordered series.", line,
                "Chart chart = new Chart()\n        .values(List.of(18, 24, 21, 36, 31, 48, 43, 62))\n        .type(Chart.Type.LINE);"));

        Chart bars = new Chart().values(List.of(32, 54, 41, 68, 47, 75)).type(Chart.Type.BAR)
                .categories(List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat"));
        lightPlot(bars);
        bars.layout(style -> style.widthPercent(100.0f).height(210.0f));
        root.addChild(samplePanel("Weekly totals", "Bar charts make discrete value comparisons easier to scan.", bars,
                "Chart chart = new Chart()\n        .values(List.of(32, 54, 41, 68, 47, 75))\n        .type(Chart.Type.BAR)\n        .categories(List.of(\"Mon\", \"Tue\", \"Wed\", \"Thu\", \"Fri\", \"Sat\"));"));
    }

    private void addSparklines(VBox root) {
        VBox samples = new VBox();
        samples.spacing(10.0f);
        samples.layout(style -> style.widthPercent(100.0f));
        samples.addChild(sparklineRow("CPU usage", List.of(22, 28, 25, 42, 36, 55, 48, 61), Sparkline.PointMode.EXTREMA));
        samples.addChild(sparklineRow("Memory", List.of(36, 37, 36, 38, 39, 39, 40, 42), Sparkline.PointMode.NONE));
        samples.addChild(sparklineRow("Response time", List.of(65, 51, 57, 33, 42, 28, 31, 19), Sparkline.PointMode.ALL));
        root.addChild(samplePanel("Compact trends", "Sparklines fit small metrics rows; point modes control marker density.", samples,
                "Sparkline sparkline = new Sparkline()\n        .values(List.of(22, 28, 25, 42, 36, 55, 48, 61))\n        .pointMode(Sparkline.PointMode.EXTREMA)\n        .pointLabelsVisible(true);"));
    }

    private VBox sparklineRow(String title, List<Integer> values, Sparkline.PointMode pointMode) {
        VBox row = new VBox();
        row.spacing(5.0f);
        row.layout(style -> style.widthPercent(100.0f));
        row.addChild(label(title, UniGuiDemo.GalleryTemplate.TEXT_DARK));
        Sparkline sparkline = new Sparkline().values(values).pointMode(pointMode).pointLabelsVisible(pointMode != Sparkline.PointMode.NONE);
        lightPlot(sparkline);
        sparkline.layout(style -> style.widthPercent(100.0f).height(48.0f));
        row.addChild(sparkline);
        return row;
    }

    private void addGraph(VBox root) {
        GraphView graph = new GraphView()
                .addNode("API", 0.18f, 0.5f)
                .addNode("Auth", 0.48f, 0.24f)
                .addNode("Data", 0.48f, 0.76f)
                .addNode("Web", 0.82f, 0.5f)
                .addEdge("API", "Auth")
                .addEdge("API", "Data")
                .addEdge("Auth", "Web")
                .addEdge("Data", "Web");
        lightPlot(graph);
        graph.layout(style -> style.widthPercent(100.0f).height(240.0f));
        root.addChild(samplePanel("Service dependencies", "Nodes use normalized coordinates; edges communicate dependencies.", graph,
                "GraphView graph = new GraphView()\n        .addNode(\"API\", 0.18f, 0.5f)\n        .addNode(\"Auth\", 0.48f, 0.24f)\n        .addNode(\"Web\", 0.82f, 0.5f)\n        .addEdge(\"API\", \"Auth\")\n        .addEdge(\"Auth\", \"Web\");"));
    }

    /**
     * Перекрашивает график, спарклайн или диаграмму связей под светлую карточку галереи.
     *
     * <p>Значения по умолчанию рассчитаны на тёмную тему: у {@code Sparkline} нет фона, поэтому белые подписи на
     * светлой карточке были бы невидимы. Локальный стиль действует всегда, как и у таблицы выше.</p>
     */
    private static void lightPlot(dev.sixik.unigui.api.widget.Widget plot) {
        var accent = UniGuiDemo.GalleryTemplate.color(30, 166, 222);
        var accentFill = dev.sixik.unigui.api.math.MutableColor.rgba(0.118f, 0.651f, 0.871f, 0.34f);
        var hover = UniGuiDemo.GalleryTemplate.color(232, 142, 40);
        var text = UniGuiDemo.GalleryTemplate.color(56, 72, 88);
        ((dev.sixik.unigui.impl.widget.WidgetBase) plot).style(style -> style
                .put(StyleKeys.BACKGROUND_COLOR, UniGuiDemo.GalleryTemplate.color(255, 255, 255))
                .put(StyleKeys.BORDER_COLOR, UniGuiDemo.GalleryTemplate.CARD_BORDER)
                .put(StyleKeys.TEXT_COLOR, text)
                .put(StyleKeys.PLOT_LINE_COLOR, accent)
                .put(StyleKeys.PLOT_FILL_COLOR, accentFill)
                .put(StyleKeys.PLOT_POINT_COLOR, text)
                .put(StyleKeys.PLOT_POINT_HOVER_COLOR, hover)
                .put(StyleKeys.PLOT_AXIS_COLOR, UniGuiDemo.GalleryTemplate.color(150, 164, 176))
                .put(StyleKeys.PLOT_GRID_COLOR, UniGuiDemo.GalleryTemplate.color(228, 234, 240))
                .put(StyleKeys.PLOT_BAR_COLOR, accent)
                .put(StyleKeys.PLOT_BAR_HOVER_COLOR, hover)
                .put(StyleKeys.GRAPH_NODE_COLOR, accent)
                .put(StyleKeys.GRAPH_NODE_HOVER_COLOR, hover)
                .put(StyleKeys.GRAPH_NODE_BORDER_COLOR, UniGuiDemo.GalleryTemplate.color(20, 110, 150))
                .put(StyleKeys.GRAPH_EDGE_COLOR, UniGuiDemo.GalleryTemplate.color(140, 156, 170))
                .put(StyleKeys.GRAPH_EDGE_HOVER_COLOR, hover));
    }
}
