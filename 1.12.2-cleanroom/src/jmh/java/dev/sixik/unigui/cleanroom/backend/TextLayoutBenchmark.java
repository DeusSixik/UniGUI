package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.impl.text.SdfGlyph;
import dev.sixik.unigui.impl.text.SdfGlyphProvider;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Cost of laying out one text command (positions of every glyph) with a counting sink, for SDF glyphs and for the
 * vanilla font, with and without {@code §} codes and glyph-by-glyph drawing (tracking).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class TextLayoutBenchmark {
    private static final String LINE = "The quick brown fox jumps over 13 lazy dogs";

    private static final class Face implements SdfGlyphProvider {
        private final FontMetrics metrics = new FontMetrics(9.6f, 2.4f, 0.0f, 12.0f);

        @Override
        public String id() {
            return "bench";
        }

        @Override
        public FontMetrics metrics(float pixelSize) {
            return metrics;
        }

        @Override
        public float advance(int codePoint, float pixelSize) {
            return pixelSize * 0.55f;
        }

        @Override
        public SdfGlyph sdfGlyph(int codePoint, int pixelSize, int spread) {
            return null;
        }
    }

    /** A vanilla-like (non SDF) face. */
    private static final class Plain implements FontFace {
        private final FontMetrics metrics = new FontMetrics(7.0f, 2.0f, 0.0f, 9.0f);

        @Override
        public String id() {
            return "plain";
        }

        @Override
        public FontMetrics metrics(float pixelSize) {
            return metrics;
        }

        @Override
        public float advance(int codePoint, float pixelSize) {
            return 6.0f;
        }
    }

    @Param({"sdf", "vanilla", "vanillaCodes", "vanillaTracking"})
    public String mode;

    private final CleanroomTextLayout layout = new CleanroomTextLayout();
    private final Object page = new Object();
    private final CleanroomTextLayout.Glyph glyph =
            new CleanroomTextLayout.Glyph(page, 24, 40, 30.0f, 2.0f, -36.0f, 0.1f, 0.2f, 0.3f, 0.4f);
    private final float[] identity = {1, 0, 0, 1, 0, 0};
    private final Face sdfFace = new Face();
    private final Plain plainFace = new Plain();
    private DrawCommand command;
    private RichText text;
    private FontFace face;
    private CleanroomTextLayout.Atlas atlas;
    private int sink;

    private final CleanroomTextLayout.Sink counting = new CleanroomTextLayout.Sink() {
        @Override
        public void sdfGlyph(Object p, float x, float y, float w, float h, float u0, float v0, float u1, float v1,
                             TextRun run) {
            sink++;
        }

        @Override
        public void vanilla(String s, FontFace f, TextRun run, float x, float top, boolean perGlyph) {
            sink += s.length();
        }
    };

    @Setup
    public void setup() {
        command = DrawCommand.text("x", new MutableRect(0, 0, 300, 20), Paint.fill(new MutableColor(1, 1, 1, 1)));
        atlas = mode.equals("sdf") ? (f, codePoint) -> glyph : null;
        face = mode.equals("sdf") ? sdfFace : plainFace;
        text = switch (mode) {
            case "vanillaCodes" -> RichText.of("\u00a7cThe quick \u00a7lbrown\u00a7r fox jumps over 13 lazy dogs", face, 9.0f);
            case "vanillaTracking" -> new RichText(java.util.List.of(new TextRun(LINE, face, 9.0f, null, 0.5f)));
            default -> RichText.of(LINE, face, 9.0f);
        };
    }

    @Benchmark
    public int layout(Blackhole blackhole) {
        sink = 0;
        layout.layout(command, text, face, plainFace, atlas, identity, 2.0f, 2.0f, counting);
        return sink;
    }
}