package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.event.ChartBarClickEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.style.StyleKeyRegistry;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Шкала, раскладка слотов, наведение, события и стили Chart.
 *
 * <p>Точные размеры подписей зависят от шрифта бэкенда, которого в тесте нет, поэтому проверки опираются на
 * соотношения между полями раскладки, а не на абсолютные координаты.</p>
 */
class ChartTest {
    /** Допуск координат: float-вычисления с несколькими сложениями. */
    private static final float EPSILON = 1.0e-3f;
    private static final MutableColor RED = MutableColor.rgba(1.0f, 0.0f, 0.0f, 1.0f);
    private static final MutableColor GREEN = MutableColor.rgba(0.0f, 1.0f, 0.0f, 1.0f);

    private static DrawList render(Chart chart) {
        chart.measure(new LayoutContext(240.0f, 140.0f));
        chart.arrange(new MutableRect(0.0f, 0.0f, 240.0f, 140.0f));
        DrawList list = new DrawList();
        chart.render(new DefaultRenderContext(list));
        return list;
    }

    private static Chart chart(Chart.Type type, float... values) {
        Chart chart = new Chart().preferredSize(240.0f, 140.0f).values(values).type(type);
        render(chart);
        return chart;
    }

    private static float slotCenter(Chart chart, int index, int count) {
        return chart.plotX + chart.plotW * (index + 0.5f) / count;
    }

    private static boolean hasColor(DrawList list, MutableColor color) {
        for (DrawCommand command : list.commands()) {
            if (command.paint() == null) continue;
            var c = command.paint().color();
            if (Math.abs(c.r() - color.r()) < 1e-4f && Math.abs(c.g() - color.g()) < 1e-4f
                    && Math.abs(c.b() - color.b()) < 1e-4f && Math.abs(c.a() - color.a()) < 1e-4f) {
                return true;
            }
        }
        return false;
    }

    private static int count(DrawList list, DrawCommandType type) {
        int result = 0;
        for (DrawCommand command : list.commands()) {
            if (command.type() == type) result++;
        }
        return result;
    }

    @Test
    void scale_barChart_alwaysIncludesZeroAndCoversData() {
        Chart chart = chart(Chart.Type.BAR, 32.0f, 54.0f, 41.0f, 68.0f, 47.0f, 75.0f);

        assertEquals(0.0f, chart.scaleMin, EPSILON);
        assertTrue(chart.scaleMax >= 75.0f);
        float ticks = (chart.scaleMax - chart.scaleMin) / chart.tickStep;
        assertEquals(Math.round(ticks), ticks, 1.0e-3f, "range must be a whole number of steps");
    }

    @Test
    void scale_lineChart_doesNotForceZero() {
        Chart chart = chart(Chart.Type.LINE, 118.0f, 124.0f, 121.0f, 162.0f);

        // Ряд с большим смещением не прижимается к нулю: шкала начинается у ближайшего кратного шага ниже минимума.
        assertTrue(chart.scaleMin > 0.0f);
        assertTrue(chart.scaleMin <= 118.0f);
        assertTrue(chart.scaleMax >= 162.0f);
    }

    @Test
    void scale_negativeBars_keepZeroBaselineInsidePlot() {
        Chart chart = chart(Chart.Type.BAR, -5.0f, 10.0f);

        assertTrue(chart.scaleMin <= -5.0f);
        float baseline = chart.valueY(0.0f);
        assertTrue(baseline > chart.plotY && baseline < chart.plotY + chart.plotH);
    }

    @Test
    void pointX_lineChart_isCenterOfSlot() {
        Chart chart = chart(Chart.Type.LINE, 1.0f, 2.0f, 3.0f, 4.0f);

        for (int i = 0; i < 4; i++) {
            assertEquals(slotCenter(chart, i, 4), chart.pointX(i), EPSILON);
        }
    }

    @Test
    void hitIndex_barChart_selectsSlotAtAnyHeightInsidePlot() {
        Chart chart = chart(Chart.Type.BAR, 10.0f, 20.0f, 30.0f);
        float topOfPlot = chart.plotY + 1.0f;

        assertEquals(0, chart.hitIndex(slotCenter(chart, 0, 3), topOfPlot));
        assertEquals(1, chart.hitIndex(slotCenter(chart, 1, 3), chart.plotY + chart.plotH - 1.0f));
        assertEquals(2, chart.hitIndex(slotCenter(chart, 2, 3), chart.plotY + chart.plotH * 0.5f));
    }

    @Test
    void hitIndex_barChart_outsidePlotSelectsNothing() {
        Chart chart = chart(Chart.Type.BAR, 10.0f, 20.0f, 30.0f);

        assertEquals(-1, chart.hitIndex(chart.plotX - 2.0f, chart.plotY + 2.0f));
        assertEquals(-1, chart.hitIndex(slotCenter(chart, 1, 3), chart.plotY - 2.0f));
        assertEquals(-1, chart.hitIndex(slotCenter(chart, 1, 3), chart.plotY + chart.plotH + 2.0f));
    }

    @Test
    void hover_barChart_followsPointer() {
        Chart chart = chart(Chart.Type.BAR, 10.0f, 20.0f, 30.0f);
        float x = slotCenter(chart, 2, 3);
        float y = chart.plotY + chart.plotH * 0.5f;

        chart.handle(new PointerMovedEvent(chart, x, y, x, y, 0));

        assertEquals(2, chart.hoveredIndex);
    }

    @Test
    void type_switch_resetsHoverAndRebuildsLayout() {
        Chart chart = chart(Chart.Type.BAR, 10.0f, 20.0f, 30.0f);
        float x = slotCenter(chart, 1, 3);
        float y = chart.plotY + chart.plotH * 0.5f;
        chart.handle(new PointerMovedEvent(chart, x, y, x, y, 0));
        assertEquals(1, chart.hoveredIndex);

        chart.type(Chart.Type.LINE);

        assertEquals(-1, chart.hoveredIndex);
        assertFalse(chart.layoutValid);
    }

    @Test
    void click_bar_firesEventWithGeometryAndBaseline() {
        Chart chart = chart(Chart.Type.BAR, 10.0f, 20.0f, 30.0f);
        AtomicReference<ChartBarClickEvent> received = new AtomicReference<>();
        chart.onBarClick(received::set);
        float x = slotCenter(chart, 1, 3);
        float y = chart.plotY + chart.plotH * 0.5f;
        PointerPressedEvent press = new PointerPressedEvent(chart, x, y, x, y, 0, PointerButton.PRIMARY);

        chart.handle(press);

        ChartBarClickEvent event = received.get();
        assertNotNull(event);
        assertEquals(1, event.index());
        assertEquals(20.0f, event.value(), EPSILON);
        assertEquals(chart.valueY(0.0f), event.baseline(), EPSILON);
        // Положительный столбец стоит на нулевой линии: низ столбца совпадает с baseline.
        assertEquals(event.baseline(), event.y() + event.height(), EPSILON);
        assertTrue(press.isCancelled());
    }

    @Test
    void click_lineChart_firesSparkPointClickInstead() {
        Chart chart = chart(Chart.Type.LINE, 10.0f, 20.0f, 30.0f);
        AtomicReference<Object> bar = new AtomicReference<>();
        AtomicReference<Object> point = new AtomicReference<>();
        chart.onBarClick(bar::set);
        chart.onPointClick(point::set);
        float x = slotCenter(chart, 1, 3);

        chart.handle(new PointerPressedEvent(chart, x, 60.0f, x, 60.0f, 0, PointerButton.PRIMARY));

        assertEquals(null, bar.get());
        assertNotNull(point.get());
    }

    @Test
    void categories_providedLabels_reserveSpaceBelowPlotAndNameTheTooltip() {
        Chart plain = chart(Chart.Type.BAR, 10.0f, 54.0f, 30.0f);
        Chart labelled = chart(Chart.Type.BAR, 10.0f, 54.0f, 30.0f);

        labelled.categories(List.of("Mon", "Tue", "Wed"));
        render(labelled);

        assertTrue(labelled.plotH < plain.plotH, "category labels take room from the plot");
        assertEquals("Tue: 54", labelled.tooltipLabel(1));
        assertEquals("#1: 54", plain.tooltipLabel(1));
    }

    @Test
    void style_gridHidden_removesValueAxisMargin() {
        Chart withGrid = chart(Chart.Type.LINE, 1.0f, 2.0f, 3.0f);
        Chart withoutGrid = new Chart().preferredSize(240.0f, 140.0f).values(1.0f, 2.0f, 3.0f);
        withoutGrid.style(style -> style.put(StyleKeys.PLOT_GRID_VISIBLE, false));
        render(withoutGrid);

        assertTrue(withoutGrid.plotX < withGrid.plotX, "no value labels means no left margin for them");
        assertTrue(withoutGrid.plotW > withGrid.plotW);
    }

    @Test
    void style_gridHidden_dropsTickLabels() {
        Chart withGrid = new Chart().preferredSize(240.0f, 140.0f).values(1.0f, 2.0f, 3.0f);
        Chart withoutGrid = new Chart().preferredSize(240.0f, 140.0f).values(1.0f, 2.0f, 3.0f);
        withoutGrid.style(style -> style.put(StyleKeys.PLOT_GRID_VISIBLE, false));

        int labelled = count(render(withGrid), DrawCommandType.TEXT);
        int bare = count(render(withoutGrid), DrawCommandType.TEXT);

        assertTrue(labelled > bare, "tick labels are text commands");
    }

    @Test
    void style_barColorKey_isUsedAndInstanceColorWins() {
        Chart chart = new Chart().preferredSize(240.0f, 140.0f).values(10.0f, 20.0f).type(Chart.Type.BAR);
        chart.style(style -> style.put(StyleKeys.PLOT_BAR_COLOR, RED));
        assertTrue(hasColor(render(chart), RED));

        chart.barColor().set(GREEN);

        DrawList list = render(chart);
        assertTrue(hasColor(list, GREEN));
        assertFalse(hasColor(list, RED));
    }

    @Test
    void style_barRadiusZero_drawsPlainRectanglesWithoutClip() {
        Chart rounded = new Chart().preferredSize(240.0f, 140.0f).values(10.0f, 20.0f).type(Chart.Type.BAR);
        Chart square = new Chart().preferredSize(240.0f, 140.0f).values(10.0f, 20.0f).type(Chart.Type.BAR);
        square.style(style -> style.put(StyleKeys.PLOT_BAR_RADIUS, 0.0f));

        int roundedClips = count(render(rounded), DrawCommandType.PUSH_CLIP);
        int squareClips = count(render(square), DrawCommandType.PUSH_CLIP);

        assertTrue(roundedClips > squareClips, "rounded bars are clipped to keep the base edge square");
        assertEquals(0, squareClips);
    }

    @Test
    void values_nonFinite_barIsSkippedWithoutFailure() {
        Chart chart = chart(Chart.Type.BAR, 10.0f, Float.NaN, 30.0f);

        assertEquals(-1, chart.hitIndex(slotCenter(chart, 1, 3), chart.plotY + 2.0f));
        assertEquals(0, chart.hitIndex(slotCenter(chart, 0, 3), chart.plotY + 2.0f));
    }

    @Test
    void styleKeys_chartKeysAreRegisteredForEditors() {
        StyleKeyRegistry registry = StyleKeyRegistry.builtIns();

        assertTrue(registry.descriptor(StyleKeys.PLOT_BAR_COLOR).isPresent());
        assertTrue(registry.descriptor(StyleKeys.PLOT_GRID_VISIBLE).isPresent());
        assertTrue(registry.descriptor(StyleKeys.PLOT_BAR_RADIUS).isPresent());
    }
}
