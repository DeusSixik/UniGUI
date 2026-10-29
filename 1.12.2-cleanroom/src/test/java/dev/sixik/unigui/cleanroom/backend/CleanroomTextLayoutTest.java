package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextBrush;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.impl.text.SdfGlyph;
import dev.sixik.unigui.impl.text.SdfGlyphProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanroomTextLayoutTest {
    private static final float EPS = 1.0e-4f;
    private static final float[] IDENTITY = {1, 0, 0, 1, 0, 0};

    /** Vanilla-like face: every glyph is 6 wide at size 10, ascent 8, line height 10 at size 10. */
    private static final class FixedFace implements FontFace {
        private final String id;
        private final float ascentRatio;

        private FixedFace(String id, float ascentRatio) {
            this.id = id;
            this.ascentRatio = ascentRatio;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public FontMetrics metrics(float pixelSize) {
            return new FontMetrics(pixelSize * ascentRatio, pixelSize * (1.0f - ascentRatio), 0.0f, pixelSize);
        }

        @Override
        public float advance(int codePoint, float pixelSize) {
            return pixelSize * 0.6f;
        }
    }

    /** Face whose glyphs come from the test atlas: advance 48 / bearing at base size. */
    private static final class SdfFace implements SdfGlyphProvider {
        @Override
        public String id() {
            return "sdf";
        }

        @Override
        public FontMetrics metrics(float pixelSize) {
            return new FontMetrics(pixelSize * 0.8f, pixelSize * 0.2f, 0.0f, pixelSize);
        }

        @Override
        public float advance(int codePoint, float pixelSize) {
            return pixelSize * 0.5f;
        }

        @Override
        public SdfGlyph sdfGlyph(int codePoint, int pixelSize, int spread) {
            return null;
        }
    }

    private record Vanilla(String text, FontFace face, float x, float top, boolean perGlyph) {
    }

    private record Quad(Object page, float x, float y, float w, float h, float u0, float v0, float u1, float v1) {
    }

    private static final class Recorder implements CleanroomTextLayout.Sink {
        private final List<Vanilla> vanilla = new ArrayList<>();
        private final List<Quad> quads = new ArrayList<>();
        private final List<Object> order = new ArrayList<>();

        @Override
        public void sdfGlyph(Object page, float x, float y, float width, float height,
                             float u0, float v0, float u1, float v1, TextRun run) {
            Quad quad = new Quad(page, x, y, width, height, u0, v0, u1, v1);
            quads.add(quad);
            order.add(quad);
        }

        @Override
        public void vanilla(String text, FontFace face, TextRun run, float x, float top, boolean perGlyph) {
            Vanilla segment = new Vanilla(text, face, x, top, perGlyph);
            vanilla.add(segment);
            order.add(segment);
        }
    }

    private final CleanroomTextLayout layout = new CleanroomTextLayout();
    private final FixedFace vanillaFace = new FixedFace("vanilla", 0.8f);
    private final FixedFace fallbackFace = new FixedFace("fallback", 0.8f);
    private final Object page = new Object();

    private final CleanroomTextLayout.Atlas atlas = (face, codePoint) -> codePoint == '?'
            ? null
            : new CleanroomTextLayout.Glyph(page, 24, 40, 30.0f, 2.0f, -36.0f, 0.1f, 0.2f, 0.3f, 0.4f);

    @Test
    void vanillaRun_sitsAtBoundsAndAdvancesByFaceMetrics() {
        Recorder recorder = new Recorder();
        run(command("ab", 5, 7), RichText.of("ab", vanillaFace, 10.0f), null, recorder);

        assertEquals(1, recorder.vanilla.size());
        Vanilla segment = recorder.vanilla.get(0);
        assertEquals("ab", segment.text());
        assertEquals(5.0f, segment.x(), EPS);
        // The only face defines the baseline, so its top equals the bounds top.
        assertEquals(7.0f, segment.top(), EPS);
        assertEquals(false, segment.perGlyph());
    }

    @Test
    void newline_splitsSegmentsAndMovesDownByLineHeight() {
        Recorder recorder = new Recorder();
        run(command("ab\ncd", 5, 7), RichText.of("ab\ncd", vanillaFace, 10.0f), null, recorder);

        assertEquals(2, recorder.vanilla.size());
        assertEquals("ab", recorder.vanilla.get(0).text());
        assertEquals("cd", recorder.vanilla.get(1).text());
        assertEquals(5.0f, recorder.vanilla.get(1).x(), EPS);
        assertEquals(17.0f, recorder.vanilla.get(1).top(), EPS);
    }

    @Test
    void tracking_drawsGlyphByGlyphWithExtraSpacing() {
        Recorder recorder = new Recorder();
        RichText text = RichText.builder().font(vanillaFace).size(10.0f).tracking(0.5f).append("abc").build();
        run(command("abc", 0, 0), text, null, recorder);

        assertEquals(3, recorder.vanilla.size());
        // advance 6 + tracking 0.5 * size 10 = 5 between glyphs.
        assertEquals(0.0f, recorder.vanilla.get(0).x(), EPS);
        assertEquals(11.0f, recorder.vanilla.get(1).x(), EPS);
        assertEquals(22.0f, recorder.vanilla.get(2).x(), EPS);
        assertTrue(recorder.vanilla.get(0).perGlyph());
    }

    @Test
    void mixedFaces_tallestAscentSetsBaselineAndShorterFaceHangsFromIt() {
        Recorder recorder = new Recorder();
        FixedFace tall = new FixedFace("tall", 0.8f);
        FixedFace small = new FixedFace("small", 0.8f);
        RichText text = RichText.builder().font(small).size(10.0f).append("a")
                .font(tall).size(20.0f).append("b").build();
        run(command("ab", 0, 0), text, null, recorder);

        // Baseline = 0 + 16 (ascent of the 20 px run); the 10 px run has ascent 8, so its top is 8.
        assertEquals(8.0f, recorder.vanilla.get(0).top(), EPS);
        assertEquals(0.0f, recorder.vanilla.get(1).top(), EPS);
        // Second run starts after the first glyph: 10 * 0.6.
        assertEquals(6.0f, recorder.vanilla.get(1).x(), EPS);
    }

    @Test
    void sdfRun_placesQuadFromBearingsAndScalesFromBaseSize() {
        Recorder recorder = new Recorder();
        SdfFace face = new SdfFace();
        run(command("a", 10, 20), RichText.of("a", face, 24.0f), atlas, recorder);

        assertEquals(1, recorder.quads.size());
        Quad quad = recorder.quads.get(0);
        float scale = 24.0f / CleanroomTextLayout.BASE_PIXEL_SIZE;
        assertSame(page, quad.page());
        assertEquals(10.0f + 2.0f * scale, quad.x(), EPS);
        // baseline = 20 + ascent (24 * 0.8) ; top = baseline + bearingY * scale
        assertEquals(20.0f + 19.2f - 36.0f * scale, quad.y(), EPS);
        assertEquals(24.0f * scale, quad.w(), EPS);
        assertEquals(40.0f * scale, quad.h(), EPS);
        assertEquals(0.1f, quad.u0(), EPS);
        assertEquals(0.4f, quad.v1(), EPS);
    }

    @Test
    void sdfRun_advancesByAtlasAdvance() {
        Recorder recorder = new Recorder();
        run(command("aa", 0, 0), RichText.of("aa", new SdfFace(), 48.0f), atlas, recorder);

        assertEquals(2, recorder.quads.size());
        // scale 1: second glyph is 30 (atlas advance) to the right of the first.
        assertEquals(30.0f, recorder.quads.get(1).x() - recorder.quads.get(0).x(), EPS);
    }

    @Test
    void sdfRun_glyphMissingFromFace_advancesWithoutQuad() {
        Recorder recorder = new Recorder();
        run(command("?a", 0, 0), RichText.of("?a", new SdfFace(), 48.0f), atlas, recorder);

        assertEquals(1, recorder.quads.size());
        // '?' has no glyph: pen moves by face.advance (48 * 0.5) before the next glyph.
        assertEquals(24.0f + 2.0f, recorder.quads.get(0).x(), EPS);
    }

    @Test
    void sdfUnavailable_sdfRunFallsBackToTheVanillaFace() {
        Recorder recorder = new Recorder();
        run(command("ab", 0, 0), RichText.of("ab", new SdfFace(), 10.0f), null, recorder);

        assertTrue(recorder.quads.isEmpty());
        assertEquals(1, recorder.vanilla.size());
        assertSame(fallbackFace, recorder.vanilla.get(0).face());
    }

    @Test
    void mixedSdfAndVanilla_keepRunOrder() {
        Recorder recorder = new Recorder();
        RichText text = RichText.builder().font(vanillaFace).size(10.0f).append("a")
                .font(new SdfFace()).size(10.0f).append("b")
                .font(vanillaFace).size(10.0f).append("c").build();
        run(command("abc", 0, 0), text, atlas, recorder);

        assertEquals(3, recorder.order.size());
        assertTrue(recorder.order.get(0) instanceof Vanilla);
        assertTrue(recorder.order.get(1) instanceof Quad);
        assertTrue(recorder.order.get(2) instanceof Vanilla);
    }

    @Test
    void pixelSnap_roundsToWholePhysicalPixels() {
        Recorder recorder = new Recorder();
        // 2 physical pixels per UI unit: 5.3 -> 5.5, 7.8 -> 8.0.
        layout.layout(command("a", 5.3f, 7.8f), RichText.of("a", vanillaFace, 10.0f), vanillaFace, fallbackFace,
                null, IDENTITY, 2.0f, 2.0f, recorder);
        assertEquals(5.5f, recorder.vanilla.get(0).x(), EPS);
        assertEquals(8.0f, recorder.vanilla.get(0).top(), EPS);
    }

    @Test
    void pixelSnap_disabledByCommandFlagLargeTextAndVisualTransforms() {
        Recorder flagOff = new Recorder();
        DrawCommand command = command("a", 5.3f, 7.8f).textPixelSnap(false);
        layout.layout(command, RichText.of("a", vanillaFace, 10.0f), vanillaFace, fallbackFace, null, IDENTITY,
                2.0f, 2.0f, flagOff);
        assertEquals(5.3f, flagOff.vanilla.get(0).x(), EPS);

        Recorder large = new Recorder();
        layout.layout(command("a", 5.3f, 7.8f), RichText.of("a", vanillaFace, 40.0f), vanillaFace, fallbackFace,
                null, IDENTITY, 2.0f, 2.0f, large);
        assertEquals(5.3f, large.vanilla.get(0).x(), EPS);

        Recorder moved = new Recorder();
        Transform shift = new Transform();
        shift.position().set(0.25f, 0.0f);
        layout.layout(command("a", 5.3f, 7.8f).transform(shift), RichText.of("a", vanillaFace, 10.0f), vanillaFace,
                fallbackFace, null, IDENTITY, 2.0f, 2.0f, moved);
        assertEquals(5.3f, moved.vanilla.get(0).x(), EPS);
    }

    @Test
    void snapX_accountsForTranslationAndSkipsRotatedTransforms() {
        float[] translated = {1, 0, 0, 1, 0.25f, 0};
        // screen = 3.1 + 0.25 = 3.35 -> *2 = 6.7 -> round 7 -> 3.5 -> local 3.25.
        assertEquals(3.25f, CleanroomTextLayout.snapX(3.1f, translated, 2.0f), EPS);

        float[] rotated = {0, 1, -1, 0, 0, 0};
        assertEquals(3.1f, CleanroomTextLayout.snapX(3.1f, rotated, 2.0f), EPS);
        assertEquals(3.1f, CleanroomTextLayout.snapY(3.1f, rotated, 2.0f), EPS);
    }

    @Test
    void affine_collapsesScaleAndStackIntoSixNumbers() {
        Transform scale = new Transform();
        scale.scale().set(2.0f, 3.0f);
        Transform shift = new Transform();
        shift.position().set(10.0f, 20.0f);
        MutableRect layerBounds = new MutableRect(0, 0, 50, 50);
        Object[] layers = {new dev.sixik.unigui.api.render.TransformLayer(layerBounds, shift)};
        float[] out = new float[6];

        CleanroomRenderMath.affine(new MutableRect(0, 0, 10, 10), scale, layers, 1, new float[2], out);

        // x' = 2x + 10, y' = 3y + 20
        assertEquals(2.0f, out[0], EPS);
        assertEquals(0.0f, out[1], EPS);
        assertEquals(0.0f, out[2], EPS);
        assertEquals(3.0f, out[3], EPS);
        assertEquals(10.0f, out[4], EPS);
        assertEquals(20.0f, out[5], EPS);
    }

    @Test
    void gradientBrush_forcesPerGlyphDrawing() {
        Recorder recorder = new Recorder();
        TextBrush gradient = TextBrush.linearGradient(new MutableColor(1, 0, 0, 1), new MutableColor(0, 0, 1, 1), 0.0f);
        RichText text = RichText.builder().font(vanillaFace).size(10.0f).brush(gradient).append("ab").build();
        run(command("ab", 0, 0), text, null, recorder);

        assertEquals(2, recorder.vanilla.size());
        assertTrue(recorder.vanilla.get(0).perGlyph());
    }

    private void run(DrawCommand command, RichText text, CleanroomTextLayout.Atlas atlas, CleanroomTextLayout.Sink sink) {
        // Scale 1 with an identity transform and a 1 px grid: integer test coordinates are unaffected by snapping.
        layout.layout(command, text, vanillaFace, fallbackFace, atlas, IDENTITY, 1.0f, 1.0f, sink);
    }

    private static DrawCommand command(String text, float x, float y) {
        return DrawCommand.text(text, new MutableRect(x, y, 100, 20), Paint.fill(new MutableColor(1, 1, 1, 1)));
    }
}
