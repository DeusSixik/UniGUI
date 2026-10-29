package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.text.LinearGradientTextBrush;
import dev.sixik.unigui.api.text.SolidTextBrush;
import dev.sixik.unigui.api.text.TextBrush;

/**
 * Resolves the final color of a text glyph from the draw command paint, the run color and the run brush.
 *
 * <p>Rules (identical to the 1.20 backend): the paint and run alpha always multiply; a {@link SolidTextBrush}
 * replaces the RGB and multiplies its own alpha; a {@link LinearGradientTextBrush} interpolates RGB and alpha at the
 * glyph position; without a brush the RGB of paint and run multiply. A non-finite channel counts as {@code 1}.</p>
 */
final class CleanroomTextColors {
    private CleanroomTextColors() {
    }

    /**
     * Writes {@code r, g, b, a} (each in {@code [0, 1]}) of the color at a position.
     *
     * @param out     destination array
     * @param offset  index of the red channel in {@code out}
     * @param x       position used by gradient brushes
     * @param y       position used by gradient brushes
     * @param base    paint color of the command, may be {@code null}
     * @param run     color of the run, may be {@code null}
     * @param brush   brush of the run, may be {@code null}
     * @param bounds  bounds the gradient is relative to
     * @param opacity extra alpha multiplier of the surface
     */
    static void rgba(float[] out, int offset, float x, float y, ColorView base, ColorView run, TextBrush brush,
                     RectView bounds, float opacity) {
        float a = (base == null ? 1.0f : clamp01(base.a())) * (run == null ? 1.0f : clamp01(run.a()))
                * clamp01(opacity);
        float r;
        float g;
        float b;
        if (brush instanceof SolidTextBrush solid) {
            ColorView color = solid.color();
            r = clamp01(color.r());
            g = clamp01(color.g());
            b = clamp01(color.b());
            a *= clamp01(color.a());
        } else if (brush instanceof LinearGradientTextBrush gradient) {
            float t = gradient.factor(x, y, bounds);
            ColorView start = gradient.startColor();
            ColorView end = gradient.endColor();
            r = lerp(clamp01(start.r()), clamp01(end.r()), t);
            g = lerp(clamp01(start.g()), clamp01(end.g()), t);
            b = lerp(clamp01(start.b()), clamp01(end.b()), t);
            a *= lerp(clamp01(start.a()), clamp01(end.a()), t);
        } else {
            r = (base == null ? 1.0f : clamp01(base.r())) * (run == null ? 1.0f : clamp01(run.r()));
            g = (base == null ? 1.0f : clamp01(base.g())) * (run == null ? 1.0f : clamp01(run.g()));
            b = (base == null ? 1.0f : clamp01(base.b())) * (run == null ? 1.0f : clamp01(run.b()));
        }
        out[offset] = r;
        out[offset + 1] = g;
        out[offset + 2] = b;
        out[offset + 3] = a;
    }

    /**
     * Packs {@code r, g, b, a} written by {@link #rgba} into {@code 0xAARRGGBB}.
     *
     * @param rgba   four channels in {@code [0, 1]}
     * @param offset index of the red channel
     * @return packed color
     */
    static int pack(float[] rgba, int offset) {
        return channel(rgba[offset + 3]) << 24 | channel(rgba[offset]) << 16
                | channel(rgba[offset + 1]) << 8 | channel(rgba[offset + 2]);
    }

    /**
     * Most opaque alpha the text can have, as an 8-bit channel. A gradient counts with its more opaque end so that a
     * text fading in from one side is not culled.
     *
     * @param base    paint color
     * @param run     run color
     * @param brush   run brush
     * @param opacity extra alpha multiplier
     * @return alpha in {@code 0..255}
     */
    static int maxAlphaChannel(ColorView base, ColorView run, TextBrush brush, float opacity) {
        float alpha = (base == null ? 1.0f : clamp01(base.a())) * (run == null ? 1.0f : clamp01(run.a()))
                * clamp01(opacity);
        if (brush instanceof SolidTextBrush solid) {
            alpha *= clamp01(solid.color().a());
        } else if (brush instanceof LinearGradientTextBrush gradient) {
            alpha *= Math.max(clamp01(gradient.startColor().a()), clamp01(gradient.endColor().a()));
        }
        return channel(alpha);
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static float clamp01(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, Math.min(1.0f, value)) : 1.0f;
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
    }
}
