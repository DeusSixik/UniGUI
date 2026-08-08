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

import java.util.ArrayList;
import java.util.List;

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

    public static List<RichText> wrapLines(RichText text, float maxWidth) {
        return wrapLines(null, text, maxWidth);
    }

    public static List<RichText> wrapLines(RenderContext context, RichText text, float maxWidth) {
        List<RichText> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        if (!Float.isFinite(maxWidth) || maxWidth <= 0.0f) {
            appendParagraphLines(lines, text);
            return lines;
        }

        String plain = text.plainText();
        FontFace defaultFace = context == null || context.backend() == null
                ? Fonts.defaultFace()
                : context.backend().defaultTextFace();
        int paragraphStart = 0;
        while (paragraphStart <= plain.length()) {
            int paragraphEnd = nextLineBreak(plain, paragraphStart);
            appendWrappedParagraph(lines, text, plain, paragraphStart, paragraphEnd, maxWidth, defaultFace);
            if (paragraphEnd >= plain.length()) break;
            paragraphStart = skipLineBreak(plain, paragraphEnd);
        }
        return lines;
    }

    public static float lineHeight(RichText line) {
        return line == null || line.isEmpty()
                ? LINE_HEIGHT
                : Math.max(LINE_HEIGHT, measureTextHeight(line));
    }

    public static float linesHeight(List<RichText> lines) {
        if (lines == null || lines.isEmpty()) return 0.0f;
        float height = 0.0f;
        for (RichText line : lines) height += lineHeight(line);
        return height;
    }

    public static void draw(RenderContext context, String text,
                            float x, float y, float width, float height,
                            Paint paint, Transform transform,
                            Alignment horizontalAlignment, Alignment verticalAlignment) {
        if (text == null || text.isEmpty()) return;
        draw(context, RichText.plain(text), x, y, width, height, paint, transform, horizontalAlignment, verticalAlignment);
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

    private static void appendParagraphLines(List<RichText> lines, RichText text) {
        String plain = text.plainText();
        int paragraphStart = 0;
        while (paragraphStart <= plain.length()) {
            int paragraphEnd = nextLineBreak(plain, paragraphStart);
            lines.add(text.slice(paragraphStart, paragraphEnd));
            if (paragraphEnd >= plain.length()) break;
            paragraphStart = skipLineBreak(plain, paragraphEnd);
        }
    }

    private static void appendWrappedParagraph(List<RichText> lines,
                                               RichText richText,
                                               String plain,
                                               int start,
                                               int end,
                                               float maxWidth,
                                               FontFace defaultFace) {
        if (start >= end) {
            lines.add(RichText.plain(""));
            return;
        }

        int lineStart = skipLeadingWhitespace(plain, start, end);
        while (lineStart < end) {
            int index = lineStart;
            int lastFittingEnd = lineStart;
            int lastBreakStart = -1;
            float lineWidth = 0.0f;
            boolean emitted = false;
            WidthCursor widthCursor = new WidthCursor(richText, lineStart, defaultFace);

            while (index < end) {
                int codePoint = plain.codePointAt(index);
                int next = plain.offsetByCodePoints(index, 1);
                lineWidth += widthCursor.advance(index, codePoint);
                if (Character.isWhitespace(codePoint)) {
                    lastBreakStart = index;
                }

                if (lineWidth > maxWidth) {
                    if (lastBreakStart > lineStart) {
                        int breakEnd = stripTrailingWhitespace(plain, lineStart, lastBreakStart);
                        if (breakEnd > lineStart) {
                            lines.add(richText.slice(lineStart, breakEnd));
                        }
                        lineStart = skipLeadingWhitespace(plain, lastBreakStart, end);
                    } else if (lastFittingEnd > lineStart) {
                        lines.add(richText.slice(lineStart, lastFittingEnd));
                        lineStart = skipLeadingWhitespace(plain, lastFittingEnd, end);
                    } else {
                        lines.add(richText.slice(lineStart, next));
                        lineStart = skipLeadingWhitespace(plain, next, end);
                    }
                    emitted = true;
                    break;
                }

                if (!Character.isWhitespace(codePoint)) {
                    lastFittingEnd = next;
                }
                index = next;
            }

            if (!emitted) {
                int lineEnd = stripTrailingWhitespace(plain, lineStart, end);
                if (lineEnd > lineStart) lines.add(richText.slice(lineStart, lineEnd));
                break;
            }
        }
    }

    private static int nextLineBreak(String text, int start) {
        for (int index = start; index < text.length(); index++) {
            char value = text.charAt(index);
            if (value == '\n' || value == '\r') return index;
        }
        return text.length();
    }

    private static int skipLineBreak(String text, int index) {
        if (index < text.length() && text.charAt(index) == '\r') {
            index++;
            if (index < text.length() && text.charAt(index) == '\n') index++;
            return index;
        }
        return index < text.length() && text.charAt(index) == '\n' ? index + 1 : index;
    }

    private static int skipLeadingWhitespace(String text, int start, int end) {
        int index = start;
        while (index < end && Character.isWhitespace(text.codePointAt(index))) {
            index = text.offsetByCodePoints(index, 1);
        }
        return index;
    }

    private static int stripTrailingWhitespace(String text, int start, int end) {
        int index = end;
        while (index > start) {
            int previous = text.offsetByCodePoints(index, -1);
            if (!Character.isWhitespace(text.codePointAt(previous))) break;
            index = previous;
        }
        return index;
    }

    private static FontFace resolvedFace(TextRun run) {
        return run.font() == null ? Fonts.defaultFace() : run.font();
    }

    private static float positiveLineHeight(float value) {
        return value > 0.0f ? value : LINE_HEIGHT;
    }

    private static final class WidthCursor {
        private final RichText text;
        private int runIndex;
        private int runStart;
        private int runEnd;
        private TextRun run;

        private final FontFace defaultFace;

        private WidthCursor(RichText text, int startIndex, FontFace defaultFace) {
            this.text = text;
            this.defaultFace = defaultFace == null ? Fonts.defaultFace() : defaultFace;
            this.runIndex = -1;
            advanceRun();
            seek(startIndex);
        }

        private float advance(int charIndex, int codePoint) {
            seek(charIndex);
            if (run == null || codePoint == 10 || codePoint == 13) return 0.0f;
            FontFace face = run.font() == null ? defaultFace : run.font();
            return Math.max(0.0f, face.advance(codePoint, run.pixelSize()));
        }

        private void seek(int charIndex) {
            while (run != null && charIndex >= runEnd) advanceRun();
        }

        private void advanceRun() {
            runIndex++;
            if (runIndex >= text.runs().size()) {
                run = null;
                return;
            }
            runStart = run == null ? 0 : runEnd;
            run = text.runs().get(runIndex);
            runEnd = runStart + run.text().length();
        }
    }
}
