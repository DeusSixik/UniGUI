package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.BlendMode;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextTransform;

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
