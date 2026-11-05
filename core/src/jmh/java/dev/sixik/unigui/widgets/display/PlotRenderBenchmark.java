package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.graph.GraphView;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Стоимость кадра виджетов данных в установившемся режиме: данные, размер и стиль не меняются, раскладка лежит
 * в кэше. Запускать с профайлером {@code gc}: {@code alloc.rate.norm} показывает байты на кадр.
 *
 * <p>Остаточные аллокации приходят из API рисования ({@code Paint} и вершины на каждый {@code add*}); сами
 * виджеты в кадре без изменений новых массивов точек, списков и сетки заливки не создают. Замеры до и после
 * кэширования сетки заливки (раньше — по одному {@code addQuadFilled} на сегмент): {@code sparklineFrame}
 * 39744 -> 1984 B/op и 8.0 -> 3.2 мкс, {@code lineChartFrame} 41016 -> 3256 B/op и 9.0 -> 4.0 мкс.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class PlotRenderBenchmark {
    /** Число точек линии: типичный график телеметрии за минуту с шагом в секунду. */
    private static final int LINE_POINTS = 60;
    /** Число столбцов гистограммы. */
    private static final int BARS = 24;
    /** Число узлов диаграммы связей. */
    private static final int NODES = 30;

    private Sparkline sparkline;
    private Chart lineChart;
    private Chart barChart;
    private GraphView graph;
    private DrawList list;
    private DefaultRenderContext context;

    /** Создаёт виджеты, раскладывает их и рисует один кадр, чтобы кэши раскладки были прогреты. */
    @Setup
    public void setup() {
        float[] series = new float[LINE_POINTS];
        for (int i = 0; i < series.length; i++) series[i] = (float) (50.0 + 30.0 * Math.sin(i * 0.4));
        float[] bars = new float[BARS];
        for (int i = 0; i < bars.length; i++) bars[i] = 10.0f + (i * 37) % 90;

        sparkline = new Sparkline().preferredSize(240.0f, 48.0f).values(series);
        lineChart = new Chart().preferredSize(320.0f, 160.0f).values(series).type(Chart.Type.LINE);
        barChart = new Chart().preferredSize(320.0f, 160.0f).values(bars).type(Chart.Type.BAR);
        graph = new GraphView().preferredSize(320.0f, 160.0f);
        for (int i = 0; i < NODES; i++) {
            graph.addNode("n" + i, (i % 6) / 5.0f, (i / 6) / 4.0f);
        }
        for (int i = 0; i < NODES - 1; i++) {
            graph.addEdge("n" + i, "n" + (i + 1));
            graph.addEdge("n" + i, "n" + ((i * 7 + 3) % NODES));
        }
        list = new DrawList();
        context = new DefaultRenderContext(list);
        layout(sparkline, 240.0f, 48.0f);
        layout(lineChart, 320.0f, 160.0f);
        layout(barChart, 320.0f, 160.0f);
        layout(graph, 320.0f, 160.0f);
        renderAll();
    }

    private static void layout(dev.sixik.unigui.api.widget.Widget widget, float width, float height) {
        widget.measure(new LayoutContext(width, height));
        widget.arrange(new MutableRect(0.0f, 0.0f, width, height));
    }

    private void renderAll() {
        list.clear();
        sparkline.render(context);
        lineChart.render(context);
        barChart.render(context);
        graph.render(context);
    }

    /** Кадр Sparkline из 60 точек: заливка, линия, маркеры экстремумов. */
    @Benchmark
    public int sparklineFrame() {
        list.clear();
        sparkline.render(context);
        return list.commands().size();
    }

    /** Кадр линейного Chart: сетка, подписи осей и ряд. */
    @Benchmark
    public int lineChartFrame() {
        list.clear();
        lineChart.render(context);
        return list.commands().size();
    }

    /** Кадр столбчатого Chart из 24 столбцов с подписями значений. */
    @Benchmark
    public int barChartFrame() {
        list.clear();
        barChart.render(context);
        return list.commands().size();
    }

    /** Кадр GraphView из 30 узлов и 58 рёбер со стрелками и подписями. */
    @Benchmark
    public int graphFrame() {
        list.clear();
        graph.render(context);
        return list.commands().size();
    }
}
