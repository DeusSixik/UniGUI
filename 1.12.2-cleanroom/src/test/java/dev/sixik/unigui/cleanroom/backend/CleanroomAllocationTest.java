package dev.sixik.unigui.cleanroom.backend;

import com.sun.management.ThreadMXBean;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.VectorPath;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hot-path allocation budget: once warmed up, tessellating a shape or laying out a text command must not create
 * garbage. The frame loop runs these per command per frame, so a few bytes per call are thousands of objects a
 * second. The measurement is the allocated-bytes counter of the current thread; the budget leaves room for the
 * odd JIT artifact, but any real {@code new} per call (24+ bytes) fails.
 */
class CleanroomAllocationTest {
    private static final int WARMUP = 30_000;
    private static final int RUNS = 30_000;
    private static final double BUDGET_BYTES_PER_CALL = 1.0;
    private static final MutableColor WHITE = new MutableColor(1, 1, 1, 1);

    private final CleanroomShapeTessellator tessellator = new CleanroomShapeTessellator();
    private final CleanroomVertexList out = new CleanroomVertexList();
    private final CleanroomTextLayout layout = new CleanroomTextLayout();

    private static double bytesPerCall(IntConsumer body) {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        for (int i = 0; i < WARMUP; i++) body.accept(i);
        long before = bean.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < RUNS; i++) body.accept(i);
        long after = bean.getCurrentThreadAllocatedBytes();
        return (after - before) / (double) RUNS;
    }

    private void assertNoGarbage(String what, DrawCommand command) {
        double bytes = bytesPerCall(i -> {
            out.clear();
            tessellator.begin(out, 2.0f);
            tessellator.append(command, 1.0f);
        });
        assertTrue(bytes <= BUDGET_BYTES_PER_CALL, what + " allocates " + bytes + " bytes per call");
    }

    @Test
    void rectsAndStrokes() {
        assertNoGarbage("rect", DrawCommand.rect(new MutableRect(0, 0, 20, 10), Paint.fill(WHITE)));
        assertNoGarbage("rect stroke", DrawCommand.rect(new MutableRect(0, 0, 20, 10), Paint.stroke(WHITE, 2.0f)));
    }

    @Test
    void roundedRectsCirclesAndLines() {
        assertNoGarbage("rounded rect", new DrawCommand(dev.sixik.unigui.api.render.DrawCommandType.ROUNDED_RECT)
                .bounds(new MutableRect(0, 0, 40, 20)).paint(Paint.fill(WHITE)).radius(6.0f));
        assertNoGarbage("circle", DrawCommand.circle(new MutableRect(0, 0, 20, 20), Paint.fill(WHITE)).segments(24));
        assertNoGarbage("line", DrawCommand.line(new MutableRect(0, 0, 30, 12), Paint.stroke(WHITE, 3.0f)));
    }

    @Test
    void pathsWithCurves() {
        VectorPath path = new VectorPath().moveTo(0, 0).lineTo(30, 0).quadraticTo(40, 10, 30, 20)
                .cubicTo(20, 30, 10, 30, 0, 20).close();
        assertNoGarbage("path fill", DrawCommand.path(path, new MutableRect(0, 0, 40, 30), Paint.fill(WHITE)));
        assertNoGarbage("path stroke", DrawCommand.path(path, new MutableRect(0, 0, 40, 30), Paint.stroke(WHITE, 2.0f)));
    }

    /** Fixed-width SDF face: only the glyph quads of the atlas are produced, nothing else may be allocated. */
    private static final class SdfFace implements dev.sixik.unigui.impl.text.SdfGlyphProvider {
        @Override
        public String id() {
            return "sdf";
        }

        // Real faces cache their metrics too, so the test measures the layout and not the face.
        private final FontMetrics metrics = new FontMetrics(9.6f, 2.4f, 0.0f, 12.0f);

        @Override
        public FontMetrics metrics(float pixelSize) {
            return metrics;
        }

        @Override
        public float advance(int codePoint, float pixelSize) {
            return pixelSize * 0.5f;
        }

        @Override
        public dev.sixik.unigui.impl.text.SdfGlyph sdfGlyph(int codePoint, int pixelSize, int spread) {
            return null;
        }
    }

    @Test
    void vanillaTextDrawnGlyphByGlyphMakesNoGarbage() {
        FontFace face = new SdfFace();
        // A tracked run is drawn one glyph at a time; the face is not an atlas face here, so the glyphs go to the
        // vanilla sink like they do for the vanilla font.
        RichText text = new RichText(java.util.List.of(new TextRun("Hello, wor\u043c\u0438\u0440", face, 12.0f, null, 0.5f)));
        DrawCommand command = DrawCommand.text("x", new MutableRect(0, 0, 200, 20), Paint.fill(WHITE));
        float[] identity = {1, 0, 0, 1, 0, 0};
        int[] drawn = new int[1];
        CleanroomTextLayout.Sink sink = new CleanroomTextLayout.Sink() {
            @Override
            public void sdfGlyph(Object p, float x, float y, float w, float h, float u0, float v0, float u1, float v1,
                                 TextRun run) {
            }

            @Override
            public void vanilla(String s, FontFace f, TextRun run, float x, float top, boolean perGlyph) {
                drawn[0] += s.length();
            }
        };
        double bytes = bytesPerCall(i -> layout.layout(command, text, face, face, null, identity, 1.0f, 1.0f, sink));
        assertTrue(drawn[0] > 0);
        assertTrue(bytes <= BUDGET_BYTES_PER_CALL, "glyph-by-glyph vanilla layout allocates " + bytes + " bytes per call");
    }

    @Test
    void singleCharStringsAreSharedForCommonScripts() {
        assertTrue(CleanroomTextLayout.singleChar('a') == CleanroomTextLayout.singleChar('a'));
        assertTrue(CleanroomTextLayout.singleChar('\u043c') == CleanroomTextLayout.singleChar('\u043c'));
        org.junit.jupiter.api.Assertions.assertEquals("\u4e2d", CleanroomTextLayout.singleChar('\u4e2d'));
    }

    @Test
    void sdfTextLayout() {
        SdfFace face = new SdfFace();
        Object page = new Object();
        CleanroomTextLayout.Glyph glyph =
                new CleanroomTextLayout.Glyph(page, 24, 40, 30.0f, 2.0f, -36.0f, 0.1f, 0.2f, 0.3f, 0.4f);
        CleanroomTextLayout.Atlas atlas = (f, codePoint) -> glyph;
        RichText text = RichText.of("Hello, world 0123", face, 12.0f);
        DrawCommand command = DrawCommand.text("x", new MutableRect(0, 0, 200, 20), Paint.fill(WHITE));
        float[] identity = {1, 0, 0, 1, 0, 0};
        int[] glyphs = new int[1];
        CleanroomTextLayout.Sink sink = new CleanroomTextLayout.Sink() {
            @Override
            public void sdfGlyph(Object p, float x, float y, float w, float h, float u0, float v0, float u1, float v1,
                                 TextRun run) {
                glyphs[0]++;
            }

            @Override
            public void vanilla(String s, FontFace f, TextRun run, float x, float top, boolean perGlyph) {
            }
        };
        double bytes = bytesPerCall(i -> layout.layout(command, text, face, face, atlas, identity, 1.0f, 1.0f, sink));
        assertTrue(glyphs[0] > 0);
        assertTrue(bytes <= BUDGET_BYTES_PER_CALL, "sdf text layout allocates " + bytes + " bytes per call");
    }
}