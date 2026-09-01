package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.BlendMode;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextTransform;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.text.TextEngine;

public final class RenderPolishSelfTest {
    private static final float EPSILON = 0.0005f;

    public static void main(String[] args) {
        new RenderPolishSelfTest().run();
        System.out.println("RenderPolishSelfTest passed");
    }

    private void run() {
        paintCopyKeepsDashAndBlend();
        richTextAppliesTrackingAndUppercase();
        dashedLineExpandsToSolidSegments();
        textPixelSnapScopeAffectsTextCommands();
        contextHeightUsesBackendDefaultFont();
    }

    private void paintCopyKeepsDashAndBlend() {
        Paint paint = Paint.stroke(new MutableColor(0.1f, 0.2f, 0.3f, 0.4f), 2.0f)
                .dash(4.0f, 2.0f)
                .dashOffset(1.5f)
                .blend(BlendMode.ADDITIVE);
        Paint copy = paint.copy();

        assertTrue(copy.dashed(), "copied paint should stay dashed");
        assertClose(4.0f, copy.dashLength(), "copied dash length");
        assertClose(2.0f, copy.dashGap(), "copied dash gap");
        assertClose(1.5f, copy.dashOffset(), "copied dash offset");
        assertTrue(copy.blendMode() == BlendMode.ADDITIVE, "copied blend mode should stay additive");
    }

    private void richTextAppliesTrackingAndUppercase() {
        RichText text = RichText.builder()
                .uppercase()
                .tracking(0.18f)
                .append("camp")
                .build();

        assertTrue("CAMP".equals(text.plainText()), "uppercase transform should affect plain text");
        assertClose(0.18f, text.runs().get(0).tracking(), "run tracking");
        assertTrue(text.runs().get(0).transform() == TextTransform.UPPERCASE, "run transform");
    }

    private void dashedLineExpandsToSolidSegments() {
        NoopRenderContext context = new NoopRenderContext();
        Paint paint = Paint.stroke(new MutableColor(1.0f, 1.0f, 1.0f, 1.0f), 1.0f)
                .dash(2.0f, 1.0f)
                .blend(BlendMode.ADDITIVE);

        context.line(0.0f, 0.0f, 10.0f, 0.0f, paint);

        assertTrue(context.drawList().size() > 1, "dashed line should emit multiple draw commands");
        for (DrawCommand command : context.drawList().commands()) {
            assertTrue(command.type() == DrawCommandType.LINE, "dash segment should be a line command");
            assertTrue(!command.paint().dashed(), "dash segment paint should be solid");
            assertTrue(command.paint().blendMode() == BlendMode.ADDITIVE, "dash segment should keep additive blend");
        }
    }

    private void textPixelSnapScopeAffectsTextCommands() {
        DefaultRenderContext context = new DefaultRenderContext(new dev.sixik.unigui.api.render.DrawList());

        context.text("static", 0.0f, 0.0f, 40.0f, 10.0f, Paint.fill(new MutableColor(1.0f, 1.0f, 1.0f, 1.0f)));
        context.pushTextPixelSnap(false);
        try {
            context.text("moving", 0.0f, 1.0f, 40.0f, 10.0f, Paint.fill(new MutableColor(1.0f, 1.0f, 1.0f, 1.0f)));
        } finally {
            context.popTextPixelSnap();
        }
        context.text("static", 0.0f, 2.0f, 40.0f, 10.0f, Paint.fill(new MutableColor(1.0f, 1.0f, 1.0f, 1.0f)));

        assertTrue(context.drawList().commands().get(0).textPixelSnap(), "text snap should default to enabled");
        assertTrue(!context.drawList().commands().get(1).textPixelSnap(), "disabled scope should mark text commands unsnapped");
        assertTrue(context.drawList().commands().get(2).textPixelSnap(), "text snap should restore after scope");
    }

    private void contextHeightUsesBackendDefaultFont() {
        FontFace face = new FixedFontFace(24.0f);
        DefaultRenderContext context = new DefaultRenderContext(new DrawList())
                .backend(new MetricsBackend(face));
        RichText text = RichText.plain("centered");

        assertClose(10.0f, TextEngine.measureTextHeight(text),
                "context-free height should preserve fallback metrics");
        assertClose(24.0f, TextEngine.measureTextHeight(context, text),
                "render-time height should use backend default font metrics");
    }

    private record FixedFontFace(float lineHeight) implements FontFace {
        @Override
        public String id() {
            return "fixed-metrics";
        }

        @Override
        public FontMetrics metrics(float pixelSize) {
            return new FontMetrics(lineHeight * 0.75f, lineHeight * 0.25f, 0.0f, lineHeight);
        }

        @Override
        public float advance(int codePoint, float pixelSize) {
            return pixelSize * 0.5f;
        }
    }

    private record MetricsBackend(FontFace defaultFace) implements RenderBackend {
        @Override
        public FontFace defaultTextFace() {
            return defaultFace;
        }

        @Override
        public void beginFrame(FrameContext frame) {
        }

        @Override
        public void render(DrawList drawList, RenderTarget target) {
        }

        @Override
        public void endFrame() {
        }
    }

    private static void assertClose(float expected, float actual, String message) {
        if (Math.abs(expected - actual) > EPSILON) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
