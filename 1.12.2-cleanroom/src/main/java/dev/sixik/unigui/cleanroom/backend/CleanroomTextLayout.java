package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.TransformLayer;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import dev.sixik.unigui.api.text.LinearGradientTextBrush;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.SolidTextBrush;
import dev.sixik.unigui.api.text.TextTransform;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.impl.render.shaders.SdfTextShaderSources;
import dev.sixik.unigui.impl.text.SdfGlyphProvider;

import java.util.Arrays;

/**
 * Positions the glyphs of one text command and hands them to a {@link Sink}.
 *
 * <p>Pure layout: no OpenGL and no Minecraft access, so it is covered by unit tests with fake atlases. It decides
 * <em>where</em> each glyph goes (baseline from the tallest face of the line, tracking, newlines, pixel snapping);
 * what is drawn there is up to the sink. Runs may mix faces freely. Faces that provide SDF glyphs
 * ({@link SdfGlyphProvider}) are emitted as atlas quads; every other face (the vanilla font) is emitted as text
 * segments, so the draw order of a mixed text is preserved.</p>
 *
 * <p>Conventions match the 1.20 backend: coordinates are in command-local UI units, the first baseline is
 * {@code bounds.y + max ascent of line 1}, a vanilla segment is placed at {@code baseline - face ascent}, tracking is
 * {@code max(0, tracking) * pixelSize} added before every glyph except the first of a line.</p>
 *
 * <p>An instance owns scratch arrays and is not thread safe.</p>
 */
final class CleanroomTextLayout {
    /**
     * Pixel size SDF glyphs are rasterized at, see {@link SdfTextShaderSources#BASE_PIXEL_SIZE}.
     */
    static final int BASE_PIXEL_SIZE = SdfTextShaderSources.BASE_PIXEL_SIZE;
    /**
     * Text above this size is not snapped to the pixel grid (it would visibly shift glyph spacing).
     */
    static final float PIXEL_SNAP_MAX_SIZE = 18.0f;
    private static final float EPSILON = 0.0001f;

    /**
     * A glyph already uploaded to an SDF atlas page. Sizes are in base-size pixels, uv in {@code [0, 1]}.
     */
    record Glyph(Object page, int width, int height, float advance, float bearingX, float bearingY,
                 float u0, float v0, float u1, float v1) {
    }

    /**
     * Source of atlas glyphs, implemented by the OpenGL atlas.
     */
    interface Atlas {
        /**
         * @param face      SDF-capable face
         * @param codePoint character
         * @return placement in the atlas, or {@code null} if the face has no glyph for it
         */
        Glyph glyph(FontFace face, int codePoint);
    }

    /**
     * Receiver of laid-out glyphs. Coordinates are command-local, before the command transform.
     */
    interface Sink {
        /**
         * One SDF glyph quad with its top-left corner at {@code (x, y)}.
         */
        void sdfGlyph(Object page, float x, float y, float width, float height,
                      float u0, float v0, float u1, float v1, TextRun run);

        /**
         * A string of the same run drawn with a non-SDF face; {@code (x, top)} is the top-left of the first
         * glyph. When {@code perGlyph} is set the string is a single glyph (tracking or gradient brush).
         */
        void vanilla(String text, FontFace face, TextRun run, float x, float top, boolean perGlyph);
    }

    private float[] lineAscent = new float[8];
    private float[] lineHeight = new float[8];
    private int lineCount;
    private final StringBuilder segment = new StringBuilder();
    private final CleanroomLegacyFormatting.State format = new CleanroomLegacyFormatting.State();
    private boolean segmentHasGlyph;

    /**
     * Lays out one command.
     *
     * @param command      source command (bounds, transform and pixel-snap flag are read)
     * @param text         text to lay out
     * @param defaultFace  face used by runs without one
     * @param fallbackFace non-SDF face used for SDF runs when {@code atlas} is {@code null}
     * @param atlas        SDF glyph source, {@code null} if SDF rendering is unavailable
     * @param affine       command transform chain collapsed by {@link CleanroomRenderMath#affine}
     * @param scaleX       physical pixels per UI unit along x, used for pixel snapping
     * @param scaleY       physical pixels per UI unit along y
     * @param sink         receiver of glyphs
     */
    void layout(DrawCommand command, RichText text, FontFace defaultFace, FontFace fallbackFace, Atlas atlas,
                float[] affine, float scaleX, float scaleY, Sink sink) {
        RectView bounds = command.bounds();
        measureLines(text, defaultFace);
        boolean visualTransform = hasVisualTransform(command);

        float penX = bounds.x();
        float lineTop = bounds.y();
        int lineIndex = 0;
        float baseline = lineTop + lineAscent[0];
        int glyphsInLine = 0;
        boolean lineSnapReady = false;
        float lineSnapX = 0.0f;
        float lineSnapY = 0.0f;

        for (TextRun run : text.runs()) {
            FontFace face = run.font() == null ? defaultFace : run.font();
            boolean sdf = atlas != null && face instanceof SdfGlyphProvider;
            FontFace drawFace = face instanceof SdfGlyphProvider && !sdf ? fallbackFace : face;
            float size = run.pixelSize();
            float sdfScale = size / BASE_PIXEL_SIZE;
            boolean perGlyph = run.tracking() > 0.0f || run.brush() instanceof LinearGradientTextBrush;
            boolean snapSdf = sdf && canSnap(command, visualTransform, size) && axisAligned(affine);
            boolean snapVanilla = !sdf && canSnap(command, visualTransform, size);
            FontMetrics drawMetrics = sdf ? null : drawFace.metrics(size);
            segment.setLength(0);
            segmentHasGlyph = false;
            float segmentX = 0.0f;
            float segmentTop = 0.0f;
            format.reset();
            // SDF runs get the code colour through a derived run; it is rebuilt only when a code changes it.
            TextRun glyphRun = run;

            String value = run.text();
            for (int index = 0; index < value.length(); ) {
                if (CleanroomLegacyFormatting.isCode(value, index)) {
                    char code = value.charAt(index + 1);
                    index += 2;
                    if (!sdf) {
                        // The vanilla font draws codes itself. Per-glyph strings get the state as a prefix instead.
                        if (!perGlyph) {
                            if (segment.length() == 0) {
                                segmentX = penX;
                                segmentTop = baseline - drawMetrics.ascent();
                                format.appendPrefix(segment);
                            }
                            segment.append(CleanroomLegacyFormatting.SIGN).append(code);
                        }
                        format.apply(code);
                    } else {
                        format.apply(code);
                        glyphRun = format.color() < 0 ? run : colored(run, format.color());
                    }
                    continue;
                }
                int codePoint = value.codePointAt(index);
                index += Character.charCount(codePoint);
                if (codePoint == '\n') {
                    flushSegment(sink, drawFace, run, segmentX, segmentTop, perGlyph, snapVanilla, affine, scaleX, scaleY);
                    penX = bounds.x();
                    lineTop += lineHeight[lineIndex];
                    lineIndex = Math.min(lineIndex + 1, lineCount - 1);
                    baseline = lineTop + lineAscent[lineIndex];
                    glyphsInLine = 0;
                    lineSnapReady = false;
                    lineSnapX = 0.0f;
                    lineSnapY = 0.0f;
                    continue;
                }

                if (glyphsInLine > 0) penX += trackingAdvance(run);
                if (sdf) {
                    Glyph glyph = atlas.glyph(face, codePoint);
                    if (glyph == null) {
                        penX += Math.max(0.0f, face.advance(codePoint, size));
                        glyphsInLine++;
                        continue;
                    }
                    float left = penX + glyph.bearingX() * sdfScale;
                    float top = baseline + glyph.bearingY() * sdfScale;
                    if (snapSdf) {
                        if (!lineSnapReady) {
                            lineSnapX = snapX(left, affine, scaleX) - left;
                            lineSnapY = snapY(top, affine, scaleY) - top;
                            lineSnapReady = true;
                        }
                        left += lineSnapX;
                        top += lineSnapY;
                    }
                    sink.sdfGlyph(glyph.page(), left, top, glyph.width() * sdfScale, glyph.height() * sdfScale,
                            glyph.u0(), glyph.v0(), glyph.u1(), glyph.v1(), glyphRun);
                    penX += glyph.advance() * sdfScale;
                } else {
                    if (segment.length() == 0) {
                        segmentX = penX;
                        segmentTop = baseline - drawMetrics.ascent();
                        format.appendPrefix(segment);
                    }
                    segment.appendCodePoint(codePoint);
                    segmentHasGlyph = true;
                    penX += Math.max(0.0f, drawFace.advance(codePoint, size));
                    if (perGlyph) {
                        flushSegment(sink, drawFace, run, segmentX, segmentTop, true, snapVanilla, affine, scaleX, scaleY);
                    }
                }
                glyphsInLine++;
            }
            flushSegment(sink, drawFace, run, segmentX, segmentTop, perGlyph, snapVanilla, affine, scaleX, scaleY);
        }
    }

    private void flushSegment(Sink sink, FontFace face, TextRun run, float x, float top, boolean perGlyph,
                              boolean snap, float[] affine, float scaleX, float scaleY) {
        // A segment made only of codes draws nothing.
        if (segment.length() == 0 || !segmentHasGlyph) {
            segment.setLength(0);
            segmentHasGlyph = false;
            return;
        }
        segmentHasGlyph = false;
        float drawX = snap ? snapX(x, affine, scaleX) : x;
        float drawY = snap ? snapY(top, affine, scaleY) : top;
        sink.vanilla(segment.toString(), face, run, drawX, drawY, perGlyph);
        segment.setLength(0);
    }

    /**
     * Fills {@link #lineAscent} / {@link #lineHeight}: per line, the tallest face decides.
     */
    private void measureLines(RichText text, FontFace defaultFace) {
        lineCount = 0;
        float ascent = 0.0f;
        float height = 0.0f;
        for (TextRun run : text.runs()) {
            FontFace face = run.font() == null ? defaultFace : run.font();
            FontMetrics metrics = face.metrics(run.pixelSize());
            ascent = Math.max(ascent, metrics.ascent());
            height = Math.max(height, metrics.lineHeight());
            String value = run.text();
            for (int index = 0; index < value.length(); ) {
                int codePoint = value.codePointAt(index);
                index += Character.charCount(codePoint);
                if (codePoint == '\n') {
                    addLine(positive(ascent), positive(height));
                    ascent = metrics.ascent();
                    height = metrics.lineHeight();
                }
            }
        }
        addLine(positive(ascent), positive(height));
    }

    private void addLine(float ascent, float height) {
        if (lineCount == lineAscent.length) {
            lineAscent = Arrays.copyOf(lineAscent, lineCount * 2);
            lineHeight = Arrays.copyOf(lineHeight, lineCount * 2);
        }
        lineAscent[lineCount] = ascent;
        lineHeight[lineCount] = height;
        lineCount++;
    }

    /**
     * Copy of {@code run} whose colour is the code colour: a solid brush replaces RGB and keeps the alpha of the
     * paint and the run, which is what the vanilla font does.
     */
    private static TextRun colored(TextRun run, int colorIndex) {
        int rgb = CleanroomLegacyFormatting.rgb(colorIndex);
        MutableColor color = MutableColor.rgba(((rgb >> 16) & 255) / 255.0f, ((rgb >> 8) & 255) / 255.0f,
                (rgb & 255) / 255.0f, 1.0f);
        return new TextRun("", run.font(), run.pixelSize(), run.color(), new SolidTextBrush(color), run.tracking(),
                TextTransform.NONE);
    }

    private static float positive(float value) {
        return value > 0.0f ? value : TextRun.DEFAULT_PIXEL_SIZE;
    }

    private static float trackingAdvance(TextRun run) {
        return Math.max(0.0f, run.tracking()) * run.pixelSize();
    }

    private static boolean canSnap(DrawCommand command, boolean visualTransform, float size) {
        return command.textPixelSnap() && !visualTransform && Float.isFinite(size) && size > 0.0f
                && size <= PIXEL_SNAP_MAX_SIZE;
    }

    private static boolean axisAligned(float[] affine) {
        return Math.abs(affine[2]) <= EPSILON && Math.abs(affine[1]) <= EPSILON
                && Math.abs(affine[0]) > EPSILON && Math.abs(affine[3]) > EPSILON;
    }

    /**
     * Snaps a local x so its physical position is a whole pixel.
     *
     * @param x      local x
     * @param affine collapsed transform, {@code a, b, c, d, tx, ty}
     * @param scale  physical pixels per UI unit along x
     * @return snapped local x, or {@code x} unchanged for rotated / skewed transforms
     */
    static float snapX(float x, float[] affine, float scale) {
        if (Math.abs(affine[2]) > EPSILON || Math.abs(affine[0]) <= EPSILON) return x;
        float s = CleanroomRenderMath.sanitizeScale(scale);
        float screen = x * affine[0] + affine[4];
        return ((Math.round(screen * s) / s) - affine[4]) / affine[0];
    }

    /**
     * Snaps a local y so its physical position is a whole pixel.
     *
     * @param y      local y
     * @param affine collapsed transform, {@code a, b, c, d, tx, ty}
     * @param scale  physical pixels per UI unit along y
     * @return snapped local y, or {@code y} unchanged for rotated / skewed transforms
     */
    static float snapY(float y, float[] affine, float scale) {
        if (Math.abs(affine[1]) > EPSILON || Math.abs(affine[3]) <= EPSILON) return y;
        float s = CleanroomRenderMath.sanitizeScale(scale);
        float screen = y * affine[3] + affine[5];
        return ((Math.round(screen * s) / s) - affine[5]) / affine[3];
    }

    /**
     * A text with a real position / rotation / scale transform is drawn exactly, never snapped.
     */
    private static boolean hasVisualTransform(DrawCommand command) {
        if (isVisual(command.transform())) return true;
        Object[] layers = command.transformStackElements();
        for (int i = 0, size = command.transformStackSize(); i < size; i++) {
            if (layers[i] instanceof TransformLayer layer && isVisual(layer.transform())) return true;
        }
        return false;
    }

    private static boolean isVisual(Transform transform) {
        if (transform == null) return false;
        return Math.abs(transform.position().x()) > EPSILON
                || Math.abs(transform.position().y()) > EPSILON
                || Math.abs(transform.rotationDegrees()) > EPSILON
                || Math.abs(transform.scale().x() - 1.0f) > EPSILON
                || Math.abs(transform.scale().y() - 1.0f) > EPSILON;
    }
}
