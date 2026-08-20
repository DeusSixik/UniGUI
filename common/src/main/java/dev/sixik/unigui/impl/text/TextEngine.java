package dev.sixik.unigui.impl.text;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.InlineContentAlignment;
import dev.sixik.unigui.api.text.InlineContentContext;
import dev.sixik.unigui.api.text.InlineContentSpan;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.RichTextSpan;
import dev.sixik.unigui.api.text.TextRun;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;

/**
 * Общий helper для измерения, переноса и рендера текста в retained widgets.
 *
 * <p>Обычный текст по-прежнему отдаётся backend'у как один {@link RichText}. Если значение содержит
 * {@link InlineContentSpan}, engine переключается на локальную раскладку: проходит по span'ам,
 * измеряет текстовые run'ы через {@link FontFace}, ставит inline-content как атомарные блоки и
 * вызывает их renderer с уже рассчитанными bounds.</p>
 *
 * <p>Важно: inline-content здесь не превращается в widget. Это только часть текстового потока,
 * поэтому он наследует clip/transform родительского текста и не получает input events.</p>
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
        if (backend == null || text.hasInlineContent()) {
            // Backend умеет быстро измерять plain rich text, но inline-span'ы знает только TextEngine.
            FontFace defaultFace = backend == null ? null : backend.defaultTextFace();
            return measureLineWidth(text, defaultFace);
        }
        return Math.max(0.0f, backend.measureTextWidth(text));
    }

    public static float measureLineWidth(RichText text) {
        return measureLineWidth(text, null);
    }

    private static float measureLineWidth(RichText text, FontFace defaultFace) {
        if (text == null || text.isEmpty()) return 0.0f;
        float maximum = 0.0f;
        float current = 0.0f;
        int glyphsInLine = 0;
        for (RichTextSpan span : text.spans()) {
            if (span instanceof TextRun run) {
                String value = run.text();
                for (int index = 0; index < value.length(); ) {
                    int codePoint = value.codePointAt(index);
                    index += Character.charCount(codePoint);
                    if (codePoint == 10) {
                        maximum = Math.max(maximum, current);
                        current = 0.0f;
                        glyphsInLine = 0;
                    } else {
                        if (glyphsInLine > 0) current += trackingAdvance(run);
                        current += textAdvance(run, codePoint, defaultFace);
                        glyphsInLine++;
                    }
                }
            } else if (span instanceof InlineContentSpan inline) {
                if (glyphsInLine > 0) current += 0.0f;
                current += inline.width();
                glyphsInLine++;
            }
        }
        return Math.max(maximum, current);
    }

    public static float measureTextHeight(RichText text) {
        if (text == null || text.isEmpty()) return 0.0f;
        float total = 0.0f;
        float lineHeight = 0.0f;
        for (RichTextSpan span : text.spans()) {
            if (span instanceof TextRun run) {
                float runLineHeight = run.font() == null
                        ? fallbackLineHeight(run.pixelSize())
                        : run.font().metrics(run.pixelSize()).lineHeight();
                lineHeight = Math.max(lineHeight, runLineHeight);
                String value = run.text();
                for (int index = 0; index < value.length(); ) {
                    int codePoint = value.codePointAt(index);
                    index += Character.charCount(codePoint);
                    if (codePoint == '\n') {
                        total += positiveLineHeight(lineHeight);
                        lineHeight = runLineHeight;
                    }
                }
            } else if (span instanceof InlineContentSpan inline) {
                lineHeight = Math.max(lineHeight, inline.height());
            }
        }
        return total + positiveLineHeight(lineHeight);
    }

    public static List<RichText> wrapLines(RichText text, float maxWidth) {
        return wrapLines(null, text, maxWidth);
    }

    public static List<RichText> wrapLines(RenderContext context, RichText text, float maxWidth) {
        List<RichText> lines = new ObjectArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        if (!Float.isFinite(maxWidth) || maxWidth <= 0.0f) {
            appendParagraphLines(lines, text);
            return lines;
        }

        String plain = text.plainText();
        FontFace defaultFace = context == null || context.backend() == null
                ? null
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
        // String API тоже проходит через resolver, чтобы XML/модовый scope мог подменить marker'ы.
        draw(context, RichText.resolve(text), x, y, width, height, paint, transform, horizontalAlignment, verticalAlignment);
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
        if (text.hasInlineContent()) {
            // Inline-rich path раскладывает span'ы вручную, потому что backend.text не знает custom draw atoms.
            drawInline(new DrawScope(context, transform), text, drawX, drawY,
                    Math.max(0.0f, width - (drawX - x)), textHeight, paint);
        } else {
            context.text(text, drawX, drawY, Math.max(0.0f, width - (drawX - x)), textHeight, paint, transform);
        }
    }

    /**
     * Рисует rich text с поддержкой inline-content.
     *
     * <p>Для plain rich text метод делегирует в {@link DrawScope#text(RichText, float, float, float, float, Paint)}.
     * Для mixed content он последовательно рисует текстовые chunks и вызывает renderer каждого
     * {@link InlineContentSpan}. Переносы внутри text-run'ов поддерживаются, но редактирование inline-span'ов
     * остаётся задачей будущего editor layout.</p>
     */
    public static void drawInline(DrawScope draw, RichText text,
                                  float x, float y, float width, float height,
                                  Paint paint) {
        if (draw == null || text == null || text.isEmpty()) return;
        if (!text.hasInlineContent()) {
            draw.text(text, x, y, width, height, paint);
            return;
        }

        float lineHeight = Math.max(LINE_HEIGHT, height > 0.0f ? height : measureTextHeight(text));
        float cursorX = x;
        float cursorY = y;
        FontFace defaultFace = draw.context().backend() == null ? null : draw.context().backend().defaultTextFace();
        for (RichTextSpan span : text.spans()) {
            if (span instanceof TextRun run) {
                TextRunDrawResult result = drawTextRun(draw, run, cursorX, cursorY, lineHeight, paint, defaultFace, x);
                cursorX = result.cursorX();
                cursorY = result.cursorY();
            } else if (span instanceof InlineContentSpan inline) {
                float inlineY = inlineY(cursorY, lineHeight, inline);
                InlineContentContext context = new InlineContentContext(
                        inline,
                        new MutableRect(cursorX, inlineY, inline.width(), inline.height()),
                        lineHeight,
                        paint == null ? new Paint() : paint.copy());
                inline.renderer().render(draw, context);
                cursorX += inline.width();
            }
        }
    }

    public static float alignedStart(float start, float available, float size, Alignment alignment) {
        return switch (alignment == null ? Alignment.START : alignment) {
            case CENTER -> start + Math.max(0.0f, available - size) * 0.5f;
            case END -> start + Math.max(0.0f, available - size);
            case START, STRETCH -> start;
        };
    }

    private static TextRunDrawResult drawTextRun(DrawScope draw, TextRun run,
                                                 float cursorX, float cursorY, float lineHeight,
                                                 Paint paint, FontFace defaultFace, float lineStartX) {
        String value = run.text();
        int chunkStart = 0;
        float currentX = cursorX;
        float currentY = cursorY;
        for (int index = 0; index < value.length(); ) {
            int codePoint = value.codePointAt(index);
            int next = value.offsetByCodePoints(index, 1);
            if (codePoint == '\n') {
                currentX = drawTextChunk(draw, run, chunkStart, index, currentX, currentY, lineHeight, paint, defaultFace);
                currentX = lineStartX;
                currentY += lineHeight;
                chunkStart = next;
            }
            index = next;
        }
        currentX = drawTextChunk(draw, run, chunkStart, value.length(), currentX, currentY, lineHeight, paint, defaultFace);
        return new TextRunDrawResult(currentX, currentY);
    }

    private static float drawTextChunk(DrawScope draw, TextRun run,
                                       int start, int end,
                                       float x, float y, float lineHeight,
                                       Paint paint, FontFace defaultFace) {
        if (start >= end) return x;
        String chunk = run.text().substring(start, end);
        TextRun chunkRun = new TextRun(chunk, run.font(), run.pixelSize(), run.color(), run.brush(), run.tracking(), run.transform());
        RichText chunkText = new RichText(List.of(chunkRun));
        float chunkWidth = measureRunWidth(chunkRun, defaultFace);
        // Текстовые chunks всё ещё рисуются штатным backend renderer'ом, чтобы сохранить font/tint поведение.
        draw.text(chunkText, x, y, chunkWidth, lineHeight, paint);
        return x + chunkWidth;
    }

    private static float inlineY(float lineY, float lineHeight, InlineContentSpan inline) {
        float remaining = Math.max(0.0f, lineHeight - inline.height());
        InlineContentAlignment alignment = inline.alignment();
        return switch (alignment) {
            case TEXT_TOP -> lineY;
            case TEXT_BOTTOM -> lineY + remaining;
            case CENTER -> lineY + remaining * 0.5f;
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
            // WidthCursor синхронизирует plain-index из fallback text с реальным span-потоком.
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

    private static float measureRunWidth(TextRun run, FontFace defaultFace) {
        if (run == null || run.text().isEmpty()) return 0.0f;
        float width = 0.0f;
        int glyphs = 0;
        String value = run.text();
        for (int index = 0; index < value.length(); ) {
            int codePoint = value.codePointAt(index);
            index += Character.charCount(codePoint);
            if (codePoint == '\n' || codePoint == '\r') {
                break;
            }
            if (glyphs > 0) width += trackingAdvance(run);
            width += textAdvance(run, codePoint, defaultFace);
            glyphs++;
        }
        return width;
    }

    private static float textAdvance(TextRun run, int codePoint, FontFace defaultFace) {
        FontFace face = run.font() == null ? defaultFace : run.font();
        return face == null
                ? fallbackAdvance(run.pixelSize())
                : Math.max(0.0f, face.advance(codePoint, run.pixelSize()));
    }

    private static float fallbackAdvance(float pixelSize) {
        return APPROX_CHAR_WIDTH * fallbackScale(pixelSize);
    }

    private static float fallbackLineHeight(float pixelSize) {
        return LINE_HEIGHT * fallbackScale(pixelSize);
    }

    private static float fallbackScale(float pixelSize) {
        return Float.isFinite(pixelSize) && pixelSize > 0.0f
                ? pixelSize / TextRun.DEFAULT_PIXEL_SIZE
                : 1.0f;
    }

    private static float trackingAdvance(TextRun run) {
        return run == null ? 0.0f : Math.max(0.0f, run.tracking()) * run.pixelSize();
    }

    private static float positiveLineHeight(float value) {
        return value > 0.0f ? value : LINE_HEIGHT;
    }

    private record TextRunDrawResult(float cursorX, float cursorY) {
    }

    /**
     * Cursor для измерения переносов по plain fallback индексам.
     *
     * <p>Wrap algorithm идёт по {@link RichText#plainText()}, потому что переносы и slicing работают
     * с обычными индексами строки. Этот cursor держит соответствующий {@link RichTextSpan} и отдаёт
     * ширину либо конкретного code point внутри {@link TextRun}, либо всей inline-span ширины на
     * первом fallback-индексе inline-content.</p>
     */
    private static final class WidthCursor {
        private final RichText text;
        private final FontFace defaultFace;
        private int spanIndex;
        private int spanStart;
        private int spanEnd;
        private RichTextSpan span;
        private int lineGlyphCount;

        private WidthCursor(RichText text, int startIndex, FontFace defaultFace) {
            this.text = text;
            this.defaultFace = defaultFace == null ? Fonts.defaultFace() : defaultFace;
            this.spanIndex = -1;
            advanceSpan();
            seek(startIndex);
        }

        private float advance(int charIndex, int codePoint) {
            seek(charIndex);
            if (span == null) return 0.0f;
            if (codePoint == 10 || codePoint == 13) {
                lineGlyphCount = 0;
                return 0.0f;
            }
            if (span instanceof InlineContentSpan inline) {
                if (charIndex != spanStart) return 0.0f;
                lineGlyphCount++;
                return inline.width();
            }
            if (!(span instanceof TextRun run)) return 0.0f;
            float width = lineGlyphCount > 0 ? trackingAdvance(run) : 0.0f;
            width += textAdvance(run, codePoint, defaultFace);
            lineGlyphCount++;
            return width;
        }

        private void seek(int charIndex) {
            while (span != null && charIndex >= spanEnd) advanceSpan();
        }

        private void advanceSpan() {
            spanIndex++;
            if (spanIndex >= text.spans().size()) {
                span = null;
                return;
            }
            spanStart = span == null ? 0 : spanEnd;
            span = text.spans().get(spanIndex);
            spanEnd = spanStart + span.fallbackText().length();
        }
    }
}
