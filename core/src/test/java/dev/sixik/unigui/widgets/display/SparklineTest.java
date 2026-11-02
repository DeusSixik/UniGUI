package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.SparkPointClickEvent;
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
 * Раскладка, наведение, события и стили Sparkline.
 *
 * <p>Геометрия в тестах считается вручную для виджета 100x40 без стиля темы: отступ
 * {@code pad = max(pointRadius + 2, lineWidth) = max(3 + 2, 1.5) = 5}, поэтому область построения —
 * {@code x in [5, 95]}, {@code y in [5, 35]}.</p>
 */
class SparklineTest {
    /** Допуск координат: вычисления идут в float с несколькими сложениями, ошибка порядка 1e-5. */
    private static final float EPSILON = 1.0e-3f;
    private static final MutableColor RED = MutableColor.rgba(1.0f, 0.0f, 0.0f, 1.0f);
    private static final MutableColor GREEN = MutableColor.rgba(0.0f, 1.0f, 0.0f, 1.0f);

    private static DrawList render(Sparkline sparkline) {
        sparkline.measure(new LayoutContext(100.0f, 40.0f));
        sparkline.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 40.0f));
        DrawList list = new DrawList();
        sparkline.render(new DefaultRenderContext(list));
        return list;
    }

    private static Sparkline sparkline(float... values) {
        Sparkline sparkline = new Sparkline().preferredSize(100.0f, 40.0f).values(values);
        render(sparkline);
        return sparkline;
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

    private static void move(Sparkline sparkline, float x, float y) {
        sparkline.handle(new PointerMovedEvent(sparkline, x, y, x, y, 0));
    }

    @Test
    void computePoints_threeValues_mapOntoPlotArea() {
        Sparkline sparkline = sparkline(0.0f, 10.0f, 5.0f);

        List<Sparkline.SparkPoint> points = sparkline.computePoints();

        assertEquals(3, points.size());
        // x = 5 + 90 * i / 2; y = 35 - value / 10 * 30
        assertEquals(5.0f, points.get(0).x(), EPSILON);
        assertEquals(50.0f, points.get(1).x(), EPSILON);
        assertEquals(95.0f, points.get(2).x(), EPSILON);
        assertEquals(35.0f, points.get(0).y(), EPSILON);
        assertEquals(5.0f, points.get(1).y(), EPSILON);
        assertEquals(20.0f, points.get(2).y(), EPSILON);
    }

    @Test
    void computePoints_flatSeries_isCenteredVertically() {
        Sparkline sparkline = sparkline(7.0f, 7.0f, 7.0f);

        for (Sparkline.SparkPoint point : sparkline.computePoints()) {
            assertEquals(20.0f, point.y(), EPSILON);
        }
    }

    @Test
    void computePoints_singleValue_isCenteredHorizontally() {
        Sparkline sparkline = sparkline(3.0f);

        List<Sparkline.SparkPoint> points = sparkline.computePoints();

        assertEquals(1, points.size());
        assertEquals(50.0f, points.get(0).x(), EPSILON);
        assertEquals(20.0f, points.get(0).y(), EPSILON);
    }

    @Test
    void computePoints_nonFiniteValue_isSkippedAndLeavesGap() {
        Sparkline sparkline = sparkline(1.0f, Float.NaN, 3.0f);

        List<Sparkline.SparkPoint> points = sparkline.computePoints();

        assertEquals(2, points.size());
        assertEquals(0, points.get(0).index());
        assertEquals(2, points.get(1).index());
    }

    @Test
    void computePoints_emptySeries_isEmptyAndRenderDoesNotFail() {
        Sparkline sparkline = sparkline();

        assertTrue(sparkline.computePoints().isEmpty());
    }

    @Test
    void hover_pointerBetweenPoints_selectsNearestByX() {
        Sparkline sparkline = sparkline(0.0f, 10.0f, 5.0f);

        move(sparkline, 60.0f, 38.0f);

        // x = 60 ближе к точке 1 (x = 50), чем к точке 2 (x = 95): высота указателя не важна.
        assertEquals(1, sparkline.hoveredIndex);
    }

    @Test
    void hover_pointerNearGap_skipsNonFinitePoint() {
        Sparkline sparkline = sparkline(1.0f, Float.NaN, 3.0f);

        move(sparkline, 60.0f, 20.0f);

        assertEquals(2, sparkline.hoveredIndex);
    }

    @Test
    void hover_pointerLeaves_clearsHover() {
        Sparkline sparkline = sparkline(0.0f, 10.0f, 5.0f);
        move(sparkline, 50.0f, 20.0f);
        assertEquals(1, sparkline.hoveredIndex);

        sparkline.handle(new PointerExitedEvent(sparkline, 200.0f, 200.0f, 200.0f, 200.0f, 0));

        assertEquals(-1, sparkline.hoveredIndex);
    }

    @Test
    void hover_pointerOutsideBounds_selectsNothing() {
        Sparkline sparkline = sparkline(0.0f, 10.0f, 5.0f);

        move(sparkline, 150.0f, 20.0f);

        assertEquals(-1, sparkline.hoveredIndex);
    }

    @Test
    void hover_pointModeNone_isNotInteractive() {
        Sparkline sparkline = new Sparkline().preferredSize(100.0f, 40.0f)
                .values(0.0f, 10.0f, 5.0f).pointMode(Sparkline.PointMode.NONE);
        render(sparkline);

        move(sparkline, 50.0f, 20.0f);

        assertEquals(-1, sparkline.hoveredIndex);
    }

    @Test
    void click_primaryButton_firesEventForNearestPointAndCancels() {
        Sparkline sparkline = sparkline(0.0f, 10.0f, 5.0f);
        AtomicReference<SparkPointClickEvent> received = new AtomicReference<>();
        sparkline.onPointClick(received::set);
        PointerPressedEvent press = new PointerPressedEvent(sparkline, 94.0f, 30.0f, 94.0f, 30.0f, 0,
                PointerButton.PRIMARY);

        sparkline.handle(press);

        SparkPointClickEvent event = received.get();
        assertNotNull(event);
        assertEquals(2, event.index());
        assertEquals(5.0f, event.value(), EPSILON);
        assertEquals(95.0f, event.x(), EPSILON);
        assertEquals(20.0f, event.y(), EPSILON);
        assertTrue(press.isCancelled(), "a handled click must not reach ancestors");
    }

    @Test
    void click_secondaryButton_isIgnored() {
        Sparkline sparkline = sparkline(0.0f, 10.0f, 5.0f);
        AtomicReference<SparkPointClickEvent> received = new AtomicReference<>();
        sparkline.onPointClick(received::set);

        sparkline.handle(new PointerPressedEvent(sparkline, 50.0f, 20.0f, 50.0f, 20.0f, 0,
                PointerButton.SECONDARY));

        assertEquals(null, received.get());
    }

    @Test
    void markerVisible_extremaMode_marksLocalExtremaAndGlobalBounds() {
        Sparkline sparkline = sparkline(1.0f, 3.0f, 2.0f, 4.0f, 3.0f);

        // Индексы 1, 2, 3 — локальные экстремумы; 0 — глобальный минимум; 4 — не экстремум и не глобальный.
        assertTrue(sparkline.markerVisible(0));
        assertTrue(sparkline.markerVisible(1));
        assertTrue(sparkline.markerVisible(2));
        assertTrue(sparkline.markerVisible(3));
        assertFalse(sparkline.markerVisible(4));
    }

    @Test
    void markerVisible_monotonicSeries_stillMarksMinimumAndMaximum() {
        Sparkline sparkline = sparkline(1.0f, 2.0f, 3.0f, 4.0f);

        assertTrue(sparkline.markerVisible(0));
        assertFalse(sparkline.markerVisible(1));
        assertFalse(sparkline.markerVisible(2));
        assertTrue(sparkline.markerVisible(3));
    }

    @Test
    void markerVisible_allMode_marksEveryFinitePoint() {
        Sparkline sparkline = new Sparkline().preferredSize(100.0f, 40.0f)
                .values(1.0f, Float.NaN, 3.0f).pointMode(Sparkline.PointMode.ALL);
        render(sparkline);

        assertTrue(sparkline.markerVisible(0));
        assertFalse(sparkline.markerVisible(1));
        assertTrue(sparkline.markerVisible(2));
    }

    @Test
    void markerVisible_hoveredPoint_isAlwaysVisibleInExtremaMode() {
        Sparkline sparkline = sparkline(1.0f, 2.0f, 3.0f, 4.0f);
        assertFalse(sparkline.markerVisible(1));

        move(sparkline, 35.0f, 20.0f);

        assertEquals(1, sparkline.hoveredIndex);
        assertTrue(sparkline.markerVisible(1));
    }

    @Test
    void style_plotLineColorKey_isUsedWhenNothingOverridesIt() {
        Sparkline sparkline = new Sparkline().preferredSize(100.0f, 40.0f).values(0.0f, 10.0f, 5.0f);
        sparkline.style(style -> style.put(StyleKeys.PLOT_LINE_COLOR, RED));

        DrawList list = render(sparkline);

        assertTrue(hasColor(list, RED));
    }

    @Test
    void style_instanceColor_winsOverStyle() {
        Sparkline sparkline = new Sparkline().preferredSize(100.0f, 40.0f).values(0.0f, 10.0f, 5.0f);
        sparkline.style(style -> style.put(StyleKeys.PLOT_LINE_COLOR, RED));
        sparkline.lineColor().set(GREEN);

        DrawList list = render(sparkline);

        assertTrue(hasColor(list, GREEN));
        assertFalse(hasColor(list, RED));
    }

    @Test
    void style_fillVisibleKey_hidesFillWhenFalse() {
        Sparkline sparkline = new Sparkline().preferredSize(100.0f, 40.0f).values(0.0f, 10.0f, 5.0f);
        // Заливка — это MESH-команды четырёхугольников (по одной на сегмент), у них нет paint().
        int filled = count(render(sparkline), DrawCommandType.MESH);
        assertTrue(filled > 0, "default style draws the area fill");

        sparkline.style(style -> style.put(StyleKeys.PLOT_FILL_VISIBLE, false));

        assertEquals(0, count(render(sparkline), DrawCommandType.MESH));
    }

    @Test
    void style_fillVisibleSetter_winsOverStyleKey() {
        Sparkline sparkline = new Sparkline().preferredSize(100.0f, 40.0f).values(0.0f, 10.0f, 5.0f);
        sparkline.style(style -> style.put(StyleKeys.PLOT_FILL_VISIBLE, true));

        sparkline.fillVisible(false);

        assertEquals(0, count(render(sparkline), DrawCommandType.MESH));
    }

    @Test
    void style_backgroundVisible_drawsSurface() {
        Sparkline sparkline = new Sparkline().preferredSize(100.0f, 40.0f).values(0.0f, 10.0f, 5.0f);
        int plain = render(sparkline).commands().size();

        sparkline.style(style -> style.put(StyleKeys.BACKGROUND_VISIBLE, true).put(StyleKeys.BORDER_VISIBLE, true));

        assertTrue(render(sparkline).commands().size() > plain, "surface adds background and border commands");
    }

    @Test
    void style_plotLineWidthKey_changesPaddingOfPlotArea() {
        Sparkline sparkline = new Sparkline().preferredSize(100.0f, 40.0f).values(0.0f, 10.0f);
        sparkline.style(style -> style.put(StyleKeys.PLOT_LINE_WIDTH, 12.0f));

        render(sparkline);

        // pad = max(pointRadius + 2, lineWidth) = max(5, 12) = 12: линия толщиной 12 не должна выходить за край.
        assertEquals(12.0f, sparkline.plotX, EPSILON);
        assertEquals(76.0f, sparkline.plotW, EPSILON);
    }

    @Test
    void render_withCustomRenderers_receivesPointsOnlyForVisibleMarkers() {
        Sparkline sparkline = new Sparkline().preferredSize(100.0f, 40.0f).values(1.0f, 2.0f, 3.0f, 4.0f);
        AtomicReference<Integer> calls = new AtomicReference<>(0);
        sparkline.pointRenderer((draw, point) -> calls.set(calls.get() + 1));

        render(sparkline);

        // Монотонный ряд в режиме EXTREMA: маркеры только у минимума и максимума.
        assertEquals(2, calls.get());
    }

    @Test
    void values_replaced_resetsHoverAndRecomputesGeometry() {
        Sparkline sparkline = sparkline(0.0f, 10.0f, 5.0f);
        move(sparkline, 50.0f, 20.0f);
        assertEquals(1, sparkline.hoveredIndex);

        sparkline.values(1.0f, 2.0f);
        render(sparkline);

        assertEquals(-1, sparkline.hoveredIndex);
        assertEquals(2, sparkline.computePoints().size());
    }

    @Test
    void styleKeys_areRegisteredForEditors() {
        StyleKeyRegistry registry = StyleKeyRegistry.builtIns();

        assertTrue(registry.descriptor(StyleKeys.PLOT_LINE_COLOR).isPresent());
        assertTrue(registry.descriptor(StyleKeys.PLOT_FILL_VISIBLE).isPresent());
        assertTrue(registry.descriptor(StyleKeys.PLOT_TOOLTIP_BACKGROUND_COLOR).isPresent());
    }
}
