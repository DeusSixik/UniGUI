package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.render.PathAntiAliasing;
import dev.sixik.unigui.impl.render.SimpleDrawBatcher;

/**
 * Проверяет геометрическое сглаживание путей (ImGui-style) и разделение batch'ей PATH/SDF.
 */
public final class PathAntiAliasingSelfTest {
    private static final float EPS = 1.0e-4f;

    private PathAntiAliasingSelfTest() {
    }

    public static void main(String[] args) {
        fillSquareHasHalfPixelFringe();
        fillIsIndependentOfWinding();
        fillWithoutFringeIsPlainFan();
        tinyFillKeepsInnerPolygonValidAndScalesCoverage();
        thickLineHasCoreAndTwoFringes();
        thinLineKeepsEnergy();
        closedStrokeUsesMiterJoins();
        sharpAngleRespectsMiterLimit();
        degenerateInputIsIgnored();
        pathCommandsDoNotShareBatchWithSdfPrimitives();
        System.out.println("PathAntiAliasingSelfTest passed");
    }

    private static void fillSquareHasHalfPixelFringe() {
        Capture capture = new Capture();
        int vertices = new PathAntiAliasing().fillConvex(square(0, 0, 10), 4, 1.0f, capture);

        expect(vertices == 30 && capture.size == 30, "square: 2 fan triangles + 4 fringe quads = 30 vertices");
        expect(capture.allCoverageInRange(), "coverage must stay inside [0, 1]");
        expectNear(-0.5f, capture.minX(0.0f), "outer fringe starts half a pixel outside");
        expectNear(10.5f, capture.maxX(0.0f), "outer fringe ends half a pixel outside");
        expectNear(0.5f, capture.minX(1.0f), "solid core starts half a pixel inside");
        expectNear(9.5f, capture.maxX(1.0f), "solid core ends half a pixel inside");
    }

    private static void fillIsIndependentOfWinding() {
        float[] clockwise = square(0, 0, 10);
        float[] counter = {clockwise[0], clockwise[1], clockwise[6], clockwise[7],
                clockwise[4], clockwise[5], clockwise[2], clockwise[3]};
        Capture a = new Capture();
        Capture b = new Capture();
        new PathAntiAliasing().fillConvex(clockwise, 4, 1.0f, a);
        new PathAntiAliasing().fillConvex(counter, 4, 1.0f, b);

        expectNear(a.minX(0.0f), b.minX(0.0f), "winding must not change outer fringe (min)");
        expectNear(a.maxY(0.0f), b.maxY(0.0f), "winding must not change outer fringe (max)");
        expectNear(a.minY(1.0f), b.minY(1.0f), "winding must not change inner core");
    }

    private static void fillWithoutFringeIsPlainFan() {
        Capture capture = new Capture();
        int vertices = new PathAntiAliasing().fillConvex(square(0, 0, 10), 4, 0.0f, capture);

        expect(vertices == 6, "no fringe: plain fan of two triangles");
        expectNear(0.0f, capture.minX(1.0f), "no fringe: geometry is not inset");
        expectNear(10.0f, capture.maxX(1.0f), "no fringe: geometry is not outset");
    }

    private static void tinyFillKeepsInnerPolygonValidAndScalesCoverage() {
        Capture capture = new Capture();
        new PathAntiAliasing().fillConvex(square(0, 0, 0.25f), 4, 1.0f, capture);

        float coverage = capture.maxCoverage();
        expectNear(0.0625f, coverage, "tiny shape: coverage scales with area / fringe^2");
        expect(capture.maxX(coverage) - capture.minX(coverage) > 0.0f,
                "tiny shape: inner polygon must not invert");
    }

    private static void thickLineHasCoreAndTwoFringes() {
        Capture capture = new Capture();
        int vertices = new PathAntiAliasing().stroke(new float[]{0, 0, 10, 0}, 2, false, 3.0f, 1.0f, capture);

        expect(vertices == 18, "thick line: three quads");
        expectNear(2.0f, capture.maxY(0.0f), "thick line: outer fringe at half thickness + 0.5");
        expectNear(-2.0f, capture.minY(0.0f), "thick line: outer fringe on both sides");
        expectNear(1.0f, capture.maxY(1.0f), "thick line: core half-width is (t - fringe) / 2");
        expectNear(0.0f, capture.minX(1.0f), "open line: butt cap at the first point");
        expectNear(10.0f, capture.maxX(1.0f), "open line: butt cap at the last point");
    }

    private static void thinLineKeepsEnergy() {
        Capture capture = new Capture();
        int vertices = new PathAntiAliasing().stroke(new float[]{0, 0, 10, 0}, 2, false, 0.5f, 1.0f, capture);

        expect(vertices == 12, "thin line: only two fringe quads, no zero-width core");
        expectNear(0.5f, capture.maxCoverage(), "thin line: coverage = thickness / fringe");
        expectNear(1.0f, capture.maxY(0.0f), "thin line: widened to one pixel");
    }

    private static void closedStrokeUsesMiterJoins() {
        Capture capture = new Capture();
        int vertices = new PathAntiAliasing().stroke(square(0, 0, 10), 4, true, 2.0f, 1.0f, capture);

        expect(vertices == 72, "closed square stroke: 4 segments * 18 vertices");
        expectNear(-1.5f, capture.minX(0.0f), "miter join keeps the corner at the full outer offset");
        expectNear(11.5f, capture.maxX(0.0f), "miter join keeps the opposite corner symmetric");
    }

    private static void sharpAngleRespectsMiterLimit() {
        Capture capture = new Capture();
        float[] spike = {0, 0, 100, 0, 0, 1};
        new PathAntiAliasing().stroke(spike, 3, false, 2.0f, 1.0f, capture);

        float halfOuter = 1.5f;
        float limit = PathAntiAliasing.MITER_LIMIT * halfOuter + EPS;
        for (int i = 0; i < capture.size; i++) {
            float x = capture.x[i];
            expect(x <= 100.0f + limit, "sharp angle: spike must stay below the miter limit");
        }
    }

    private static void degenerateInputIsIgnored() {
        PathAntiAliasing aa = new PathAntiAliasing();
        Capture capture = new Capture();

        expect(aa.fillConvex(new float[]{1, 1, 1, 1, 1, 1}, 3, 1.0f, capture) == 0, "coincident points: no fill");
        expect(aa.fillConvex(new float[]{0, 0, 5, 0, 10, 0}, 3, 1.0f, capture) == 0, "collinear points: no fill");
        expect(aa.fillConvex(new float[]{0, 0, 5, 0}, 2, 1.0f, capture) == 0, "two points: no fill");
        expect(aa.stroke(new float[]{1, 1}, 1, false, 2.0f, 1.0f, capture) == 0, "single point: no stroke");
        expect(aa.stroke(new float[]{0, 0, Float.NaN, 3}, 2, false, 2.0f, 1.0f, capture) == 0,
                "non-finite point is skipped");
        expect(aa.fillConvex(null, 0, 1.0f, capture) == 0, "null input is safe");
        expect(capture.size == 0, "degenerate input must not emit vertices");

        // Повторное использование буферов после большого входа не должно ломать маленький.
        float[] large = new float[2000];
        for (int i = 0; i < 1000; i++) {
            double angle = Math.PI * 2.0 * i / 1000.0;
            large[i * 2] = (float) Math.cos(angle) * 50.0f;
            large[i * 2 + 1] = (float) Math.sin(angle) * 50.0f;
        }
        expect(aa.fillConvex(large, 1000, 1.0f, capture) > 0, "large polygon is tessellated");
        capture.size = 0;
        expect(aa.fillConvex(square(0, 0, 10), 4, 1.0f, capture) == 30, "buffers are reusable");
    }

    private static void pathCommandsDoNotShareBatchWithSdfPrimitives() {
        DrawList list = new DrawList();
        DefaultRenderContext context = new DefaultRenderContext(list);
        MutableColor color = new MutableColor();
        context.rect(0.0f, 0.0f, 20.0f, 10.0f, Paint.fill(color));
        context.circle(0.0f, 0.0f, 4.0f, 4.0f, Paint.fill(color));
        context.pathClear();
        context.pathLineTo(0.0f, 0.0f);
        context.pathLineTo(10.0f, 0.0f);
        context.pathLineTo(5.0f, 8.0f);
        context.pathFillConvex(color);
        context.line(0.0f, 12.0f, 20.0f, 12.0f, Paint.stroke(color, 1.0f));
        context.rect(0.0f, 14.0f, 20.0f, 4.0f, Paint.fill(color));

        var batches = SimpleDrawBatcher.INSTANCE.batch(list);
        expect(batches.size() == 3, "primitives, path and primitives must form three batches, got " + batches.size());
        expect(batches.get(0).size() == 2 && batches.get(1).size() == 1 && batches.get(2).size() == 2,
                "batch sizes must follow draw order");
    }

    private static float[] square(float x, float y, float size) {
        return new float[]{x, y, x + size, y, x + size, y + size, x, y + size};
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectNear(float expected, float actual, String message) {
        if (Math.abs(expected - actual) > EPS) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual);
        }
    }

    /** Накопитель вершин для проверок. */
    private static final class Capture implements PathAntiAliasing.VertexSink {
        private float[] x = new float[256];
        private float[] y = new float[256];
        private float[] coverage = new float[256];
        private int size;

        @Override
        public void vertex(float vx, float vy, float vc) {
            if (size == x.length) {
                x = java.util.Arrays.copyOf(x, size * 2);
                y = java.util.Arrays.copyOf(y, size * 2);
                coverage = java.util.Arrays.copyOf(coverage, size * 2);
            }
            x[size] = vx;
            y[size] = vy;
            coverage[size] = vc;
            size++;
        }

        private boolean allCoverageInRange() {
            for (int i = 0; i < size; i++) {
                if (coverage[i] < 0.0f || coverage[i] > 1.0f) return false;
            }
            return true;
        }

        private float maxCoverage() {
            float max = 0.0f;
            for (int i = 0; i < size; i++) max = Math.max(max, coverage[i]);
            return max;
        }

        private float minX(float level) {
            float min = Float.POSITIVE_INFINITY;
            for (int i = 0; i < size; i++) if (near(coverage[i], level)) min = Math.min(min, x[i]);
            return min;
        }

        private float maxX(float level) {
            float max = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < size; i++) if (near(coverage[i], level)) max = Math.max(max, x[i]);
            return max;
        }

        private float minY(float level) {
            float min = Float.POSITIVE_INFINITY;
            for (int i = 0; i < size; i++) if (near(coverage[i], level)) min = Math.min(min, y[i]);
            return min;
        }

        private float maxY(float level) {
            float max = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < size; i++) if (near(coverage[i], level)) max = Math.max(max, y[i]);
            return max;
        }

        private static boolean near(float a, float b) {
            return Math.abs(a - b) < 1.0e-5f;
        }
    }
}
