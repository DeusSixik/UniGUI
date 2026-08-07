package dev.sixik.unigui.impl.text;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.api.text.Fonts;

/**
 * Small shared text layout helper for retained widgets.
 *
 * <p>The current renderer still delegates glyph drawing to the backend, but widgets should not each guess
 * their own baseline. Keep these constants and alignment math in one place until a richer font metrics
 * backend is wired in.</p>
 */
public final class TextEngine {
    public static final float APPROX_CHAR_WIDTH = 6.0f;
    public static final float LINE_HEIGHT = 10.0f;

    private TextEngine() {
    }

    public static float measureLineWidth(String text) {
        if (text == null || text.isEmpty()) return 0.0f;
        return text.codePointCount(0, text.length()) * APPROX_CHAR_WIDTH;
    }

    public static float measureLineWidth(RenderContext context, String text) {
        if (text == null || text.isEmpty()) return 0.0f;
        RenderBackend backend = context == null ? null : context.backend();
        return backend == null ? measureLineWidth(text) : Math.max(0.0f, backend.measureTextWidth(text));
    }

    public static float measureLineWidth(RenderContext context, RichText text) {
        if (text == null || text.isEmpty()) return 0.0f;
        RenderBackend backend = context == null ? null : context.backend();
        return backend == null ? measureLineWidth(text) :
                Math.max(0.0f, backend.measureTextWidth(text));
    }

    public static float measureLineWidth(RichText text) {
        if (text == null || text.isEmpty()) return 0.0f;
        float maximum = 0.0f;
        float current = 0.0f;
        for (TextRun run : text.runs()) {
            FontFace face = resolvedFace(run);
            String value = run.text();
            for (int index = 0; index < value.length(); ) {
                int codePoint = value.codePointAt(index);
                index += Character.charCount(codePoint);
                if (codePoint == '\n') {
                    maximum = Math.max(maximum, current);
                    current = 0.0f;
                } else {
                    current += Math.max(0.0f, face.advance(codePoint, run.pixelSize()));
                }
            }
        }
        return Math.max(maximum, current);
    }

    public static float measureTextHeight(RichText text) {
        if (text == null || text.isEmpty()) return 0.0f;
        float total = 0.0f;
        float lineHeight = 0.0f;
        for (TextRun run : text.runs()) {
            FontMetrics metrics = resolvedFace(run).metrics(run.pixelSize());
            lineHeight = Math.max(lineHeight, metrics.lineHeight());
            String value = run.text();
            for (int index = 0; index < value.length(); ) {
                int codePoint = value.codePointAt(index);
                index += Character.charCount(codePoint);
                if (codePoint == '\n') {
                    total += positiveLineHeight(lineHeight);
                    lineHeight = metrics.lineHeight();
                }
            }
        }
        return total + positiveLineHeight(lineHeight);
    }

    public static void draw(RenderContext context, String text,
                            float x, float y, float width, float height,
                            Paint paint, Transform transform,
                            Alignment horizontalAlignment, Alignment verticalAlignment) {
        if (context == null || text == null || text.isEmpty()) return;

        float textWidth = Math.min(Math.max(0.0f, width), measureLineWidth(context, text));
        float textHeight = Math.min(Math.max(0.0f, height), LINE_HEIGHT);
        float drawX = alignedStart(x, Math.max(0.0f, width), textWidth, horizontalAlignment);
        float drawY = alignedStart(y, Math.max(0.0f, height), textHeight, verticalAlignment);
        context.text(text, drawX, drawY, Math.max(0.0f, width - (drawX - x)), textHeight, paint, transform);
    }

    public static void draw(RenderContext context, RichText text,
                            float x, float y, float width, float height,
                            Paint paint, Transform transform,
                            Alignment horizontalAlignment, Alignment verticalAlignment) {
        if (context == null || text == null || text.isEmpty()) return;

        float textWidth = Math.min(Math.max(0.0f, width), measureLineWidth(context, text));
        float textHeight = Math.min(Math.max(0.0f, height), measureTextHeight(text));
        float drawX = alignedStart(x, Math.max(0.0f, width), textWidth, horizontalAlignment);
        float drawY = alignedStart(y, Math.max(0.0f, height), textHeight, verticalAlignment);
        context.text(text, drawX, drawY, Math.max(0.0f, width - (drawX - x)), textHeight, paint, transform);
    }

    public static float alignedStart(float start, float available, float size, Alignment alignment) {
        return switch (alignment == null ? Alignment.START : alignment) {
            case CENTER -> start + Math.max(0.0f, available - size) * 0.5f;
            case END -> start + Math.max(0.0f, available - size);
            case START, STRETCH -> start;
        };
    }

    private static FontFace resolvedFace(TextRun run) {
        return run.font() == null ? Fonts.defaultFace() : run.font();
    }

    private static float positiveLineHeight(float value) {
        return value > 0.0f ? value : LINE_HEIGHT;
    }
}
