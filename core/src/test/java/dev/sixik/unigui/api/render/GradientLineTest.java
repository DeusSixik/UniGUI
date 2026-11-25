package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Градиентная линия: одна команда {@link DrawCommandType#LINE} с конечным цветом в {@link Paint}, которую бэкенды
 * рисуют SDF-шейдером.
 */
class GradientLineTest {
    private static final float EPS = 1.0e-5f;
    private static final MutableColor RED = new MutableColor(1.0f, 0.0f, 0.0f, 1.0f);
    private static final MutableColor BLUE = new MutableColor(0.0f, 0.0f, 1.0f, 0.5f);

    @Test
    void addLine_withEndColor_emitsOneLineCarryingBothColors() {
        DrawList list = new DrawList();
        new DefaultRenderContext(list).addLine(0.0f, 0.0f, 10.0f, 5.0f, RED, BLUE, 3.0f);

        List<DrawCommand> lines = lines(list);
        assertEquals(1, lines.size());
        Paint paint = lines.get(0).paint();
        assertTrue(paint.hasGradient());
        assertTrue(paint.isStroke());
        assertEquals(3.0f, paint.strokeWidth(), EPS);
        assertColor(paint.color(), 1.0f, 0.0f, 0.0f, 1.0f);
        assertColor(paint.endColor(), 0.0f, 0.0f, 1.0f, 0.5f);
    }

    @Test
    void addLine_withNullEndColor_isPlainLine() {
        DrawList list = new DrawList();
        new DefaultRenderContext(list).addLine(0.0f, 0.0f, 10.0f, 0.0f, RED, null, 1.0f);

        assertFalse(lines(list).get(0).paint().hasGradient());
    }

    @Test
    void addLine_appliesOpacityToBothEnds() {
        DrawList list = new DrawList();
        DefaultRenderContext context = new DefaultRenderContext(list);
        context.pushOpacity(0.5f);
        context.addLine(0.0f, 0.0f, 10.0f, 0.0f, RED, BLUE, 1.0f);

        Paint paint = lines(list).get(0).paint();
        assertEquals(0.5f, paint.color().a(), EPS);
        assertEquals(0.25f, paint.endColor().a(), EPS);
    }

    @Test
    void line_withGradientPaintAndOpacity_scalesEndAlphaWithoutTouchingCallerPaint() {
        DrawList list = new DrawList();
        DefaultRenderContext context = new DefaultRenderContext(list);
        Paint paint = Paint.stroke(RED, 2.0f).gradientTo(BLUE);
        context.pushOpacity(0.5f);
        context.line(0.0f, 0.0f, 10.0f, 0.0f, paint);

        Paint drawn = lines(list).get(0).paint();
        assertTrue(drawn.hasGradient());
        assertEquals(0.25f, drawn.endColor().a(), EPS);
        assertEquals(0.5f, paint.endColor().a(), EPS);
    }

    @Test
    void dashedGradientLine_givesEachDashItsPartOfTheGradient() {
        DrawList list = new DrawList();
        Paint paint = Paint.stroke(new MutableColor(0.0f, 0.0f, 0.0f, 1.0f), 1.0f)
                .gradientTo(new MutableColor(1.0f, 1.0f, 1.0f, 1.0f))
                .dash(2.0f, 2.0f);
        new DefaultRenderContext(list).line(0.0f, 0.0f, 8.0f, 0.0f, paint);

        List<DrawCommand> dashes = lines(list);
        assertEquals(2, dashes.size());
        // Штрихи 0..2 и 4..6 на линии длиной 8.
        assertEquals(0.0f, dashes.get(0).paint().color().r(), EPS);
        assertEquals(0.25f, dashes.get(0).paint().endColor().r(), EPS);
        assertEquals(0.5f, dashes.get(1).paint().color().r(), EPS);
        assertEquals(0.75f, dashes.get(1).paint().endColor().r(), EPS);
    }

    @Test
    void paint_copyKeepsGradient_andClearGradientRemovesIt() {
        Paint paint = Paint.stroke(RED, 1.0f).gradientTo(BLUE);
        Paint copy = paint.copy();
        assertTrue(copy.hasGradient());
        assertColor(copy.endColor(), 0.0f, 0.0f, 1.0f, 0.5f);

        assertFalse(paint.gradientTo(null).hasGradient());
        assertTrue(copy.hasGradient());
        assertFalse(copy.clearGradient().hasGradient());
    }

    @Test
    void recycledCommand_doesNotKeepGradient() {
        DrawList list = new DrawList();
        DefaultRenderContext context = new DefaultRenderContext(list);
        context.addLine(0.0f, 0.0f, 10.0f, 0.0f, RED, BLUE, 1.0f);
        list.clear();
        context.addLine(0.0f, 0.0f, 10.0f, 0.0f, RED, 1.0f);

        assertFalse(lines(list).get(0).paint().hasGradient());
    }

    private static List<DrawCommand> lines(DrawList list) {
        List<DrawCommand> found = new ArrayList<>();
        for (DrawCommand command : list.commands()) {
            if (command.type() == DrawCommandType.LINE) found.add(command);
        }
        return found;
    }

    private static void assertColor(MutableColor color, float r, float g, float b, float a) {
        assertEquals(r, color.r(), EPS);
        assertEquals(g, color.g(), EPS);
        assertEquals(b, color.b(), EPS);
        assertEquals(a, color.a(), EPS);
    }
}
