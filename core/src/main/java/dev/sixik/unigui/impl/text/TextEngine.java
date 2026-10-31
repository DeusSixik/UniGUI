package dev.sixik.unigui.impl.text;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
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
import dev.sixik.unigui.api.text.SolidTextBrush;
import dev.sixik.unigui.api.text.TextBrush;
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
@AiDoc(
        summary = "Core text layout, line-wrapping, dimension measurement, and inline-content rendering engine.",
        purpose = "Centralizes character metrics, word wrapping, alignment offsets, and atomic inline content positioning across all widgets.",
        useWhen = {
                "Calculating preferred or min-content size for text-containing widgets in layout phases.",
                "Wrapping multi-line rich text blocks to fit constrained widths.",
                "Drawing formatted text with alignment or mixed inline content into RenderContext or DrawScope."
        },
        criticalRules = {
                "TextEngine is stateless; all state is passed via RenderContext, DrawScope, or parameter arguments.",
                "Plain rich text is dispatched directly to RenderBackend; rich text with inline content is decomposed into local atomic layout boxes.",
                "minContentWidth returns the length of the longest unbreakable word."
        },
        related = {"RichText", "FontFace", "RenderContext", "DrawScope", "InlineContentSpan"}
)
@WikiPage(
        title = "<key:text.engine.title>",
        slug = "text-engine",
        category = "<key:text.engine.category>",
        summary = "<key:text.engine.summary>",
        order = 30)
@WikiSection(
        title = "<key:text.engine.section.overview.title>",
        markdown = "<key:text.engine.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:text.engine.section.measurement.title>",
        markdown = "<key:text.engine.section.measurement.body>",
        order = 20)
@WikiSection(
        title = "<key:text.engine.section.wrapping.title>",
        markdown = "<key:text.engine.section.wrapping.body>",
        order = 30)
@WikiSection(
        title = "<key:text.engine.section.rendering.title>",
        markdown = "<key:text.engine.section.rendering.body>",
        order = 40)
@WikiExample(
        title = "<key:text.engine.example.wrap.title>",
        code = "<key:text.engine.example.wrap.code>",
        order = 10)
@WikiExample(
        title = "<key:text.engine.example.draw.title>",
        code = "<key:text.engine.example.draw.code>",
        order = 20)
public final class TextEngine {
    public static final float APPROX_CHAR_WIDTH = 6.0f;
    public static final float LINE_HEIGHT = 10.0f;

    private TextEngine() {
    }

    @WikiMethod(
            title = "<key:text.engine.method.measure_line_width_str.title>",
            summary = "<key:text.engine.method.measure_line_width_str.summary>",
            order = 10)
    @WikiReturn("<key:text.engine.method.measure_line_width_str.return>")
    public static float measureLineWidth(
            @WikiParameter(name = "text", description = "<key:text.engine.method.measure_line_width_str.text>")
            String text) {
        if (text == null || text.isEmpty()) return 0.0f;
        return text.codePointCount(0, text.length()) * APPROX_CHAR_WIDTH;
    }

    /**
     * Измеряет минимальную контентную ширину: самое длинное слово без переносов.
     *
     * @param text rich text или {@code null}
     * @return ширина самого длинного слова
     */
    @WikiMethod(
            title = "<key:text.engine.method.min_content_width.title>",
            summary = "<key:text.engine.method.min_content_width.summary>",
            order = 20)
    @WikiReturn("<key:text.engine.method.min_content_width.return>")
    public static float minContentWidth(
            @WikiParameter(name = "text", description = "<key:text.engine.method.min_content_width.text>")
            RichText text) {
        if (text == null || text.isEmpty()) return 0.0f;
        float longest = 0.0f;
        for (String word : text.plainText().split("\\s+")) {
            if (!word.isEmpty()) {
                longest = Math.max(longest, measureLineWidth(word));
            }
        }
        return longest;
    }

    @WikiMethod(
            title = "<key:text.engine.method.measure_line_width_ctx_str.title>",
            summary = "<key:text.engine.method.measure_line_width_ctx_str.summary>",
            order = 30)
    @WikiReturn("<key:text.engine.method.measure_line_width_ctx_str.return>")
    public static float measureLineWidth(
            @WikiParameter(name = "context", description = "<key:text.engine.method.measure_line_width_ctx_str.context>")
            RenderContext context,
            @WikiParameter(name = "text", description = "<key:text.engine.method.measure_line_width_ctx_str.text>")
            String text) {
        if (text == null || text.isEmpty()) return 0.0f;
        RenderBackend backend = context == null ? null : context.backend();
        return backend == null ? measureLineWidth(text) : Math.max(0.0f, backend.measureTextWidth(text));
    }

    @WikiMethod(
            title = "<key:text.engine.method.measure_line_width_ctx_rich.title>",
            summary = "<key:text.engine.method.measure_line_width_ctx_rich.summary>",
            order = 40)
    @WikiReturn("<key:text.engine.method.measure_line_width_ctx_rich.return>")
    public static float measureLineWidth(
            @WikiParameter(name = "context", description = "<key:text.engine.method.measure_line_width_ctx_rich.context>")
            RenderContext context,
            @WikiParameter(name = "text", description = "<key:text.engine.method.measure_line_width_ctx_rich.text>")
            RichText text) {
        if (text == null || text.isEmpty()) return 0.0f;
        RenderBackend backend = context == null ? null : context.backend();
        if (backend == null || text.hasInlineContent()) {
            // Backend умеет быстро измерять plain rich text, но inline-span'ы знает только TextEngine.
            FontFace defaultFace = backend == null ? null : backend.defaultTextFace();
            return measureLineWidth(text, defaultFace);
        }
        return Math.max(0.0f, backend.measureTextWidth(text));
    }

    @WikiMethod(
            title = "<key:text.engine.method.measure_line_width_rich.title>",
            summary = "<key:text.engine.method.measure_line_width_rich.summary>",
            order = 50)
    @WikiReturn("<key:text.engine.method.measure_line_width_rich.return>")
    public static float measureLineWidth(
            @WikiParameter(name = "text", description = "<key:text.engine.method.measure_line_width_rich.text>")
            RichText text) {
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

    @WikiMethod(
            title = "<key:text.engine.method.measure_text_height_rich.title>",
            summary = "<key:text.engine.method.measure_text_height_rich.summary>",
            order = 60)
    @WikiReturn("<key:text.engine.method.measure_text_height_rich.return>")
    public static float measureTextHeight(
            @WikiParameter(name = "text", description = "<key:text.engine.method.measure_text_height_rich.text>")
            RichText text) {
        return measureTextHeight(text, null);
    }

    /**
     * Измеряет высоту текста с учётом default font face текущего backend'а.
     *
     * <p>Это важно для run'ов без явно заданного шрифта: fallback-метрика нужна
     * layout-коду без render context, но при отрисовке высота должна совпадать
     * с метриками шрифта, который действительно выберет backend.</p>
     */
    @WikiMethod(
            title = "<key:text.engine.method.measure_text_height_ctx_rich.title>",
            summary = "<key:text.engine.method.measure_text_height_ctx_rich.summary>",
            order = 70)
    @WikiReturn("<key:text.engine.method.measure_text_height_ctx_rich.return>")
    public static float measureTextHeight(
            @WikiParameter(name = "context", description = "<key:text.engine.method.measure_text_height_ctx_rich.context>")
            RenderContext context,
            @WikiParameter(name = "text", description = "<key:text.engine.method.measure_text_height_ctx_rich.text>")
            RichText text) {
        RenderBackend backend = context == null ? null : context.backend();
        FontFace defaultFace = backend == null ? null : backend.defaultTextFace();
        return measureTextHeight(text, defaultFace);
    }

    private static float measureTextHeight(RichText text, FontFace defaultFace) {
        if (text == null || text.isEmpty()) return 0.0f;
        float total = 0.0f;
        float lineHeight = 0.0f;
        for (RichTextSpan span : text.spans()) {
            if (span instanceof TextRun run) {
                FontFace face = run.font() == null ? defaultFace : run.font();
                float runLineHeight = face == null
                        ? fallbackLineHeight(run.pixelSize())
                        : face.metrics(run.pixelSize()).lineHeight();
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

    @WikiMethod(
            title = "<key:text.engine.method.wrap_lines_rich.title>",
            summary = "<key:text.engine.method.wrap_lines_rich.summary>",
            order = 80)
    @WikiReturn("<key:text.engine.method.wrap_lines_rich.return>")
    public static List<RichText> wrapLines(
            @WikiParameter(name = "text", description = "<key:text.engine.method.wrap_lines_rich.text>")
            RichText text,
            @WikiParameter(name = "maxWidth", description = "<key:text.engine.method.wrap_lines_rich.max_width>")
            float maxWidth) {
        return wrapLines(null, text, maxWidth);
    }

    @WikiMethod(
            title = "<key:text.engine.method.wrap_lines_ctx_rich.title>",
            summary = "<key:text.engine.method.wrap_lines_ctx_rich.summary>",
            order = 90)
    @WikiReturn("<key:text.engine.method.wrap_lines_ctx_rich.return>")
    public static List<RichText> wrapLines(
            @WikiParameter(name = "context", description = "<key:text.engine.method.wrap_lines_ctx_rich.context>")
            RenderContext context,
            @WikiParameter(name = "text", description = "<key:text.engine.method.wrap_lines_ctx_rich.text>")
            RichText text,
            @WikiParameter(name = "maxWidth", description = "<key:text.engine.method.wrap_lines_ctx_rich.max_width>")
            float maxWidth) {
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

    @WikiMethod(
            title = "<key:text.engine.method.line_height_rich.title>",
            summary = "<key:text.engine.method.line_height_rich.summary>",
            order = 100)
    @WikiReturn("<key:text.engine.method.line_height_rich.return>")
    public static float lineHeight(
            @WikiParameter(name = "line", description = "<key:text.engine.method.line_height_rich.line>")
            RichText line) {
        return line == null || line.isEmpty()
                ? LINE_HEIGHT
                : Math.max(LINE_HEIGHT, measureTextHeight(line));
    }

    @WikiMethod(
            title = "<key:text.engine.method.line_height_ctx_rich.title>",
            summary = "<key:text.engine.method.line_height_ctx_rich.summary>",
            order = 110)
    @WikiReturn("<key:text.engine.method.line_height_ctx_rich.return>")
    public static float lineHeight(
            @WikiParameter(name = "context", description = "<key:text.engine.method.line_height_ctx_rich.context>")
            RenderContext context,
            @WikiParameter(name = "line", description = "<key:text.engine.method.line_height_ctx_rich.line>")
            RichText line) {
        return line == null || line.isEmpty()
                ? LINE_HEIGHT
                : Math.max(LINE_HEIGHT, measureTextHeight(context, line));
    }

    @WikiMethod(
            title = "<key:text.engine.method.lines_height_list.title>",
            summary = "<key:text.engine.method.lines_height_list.summary>",
            order = 120)
    @WikiReturn("<key:text.engine.method.lines_height_list.return>")
    public static float linesHeight(
            @WikiParameter(name = "lines", description = "<key:text.engine.method.lines_height_list.lines>")
            List<RichText> lines) {
        if (lines == null || lines.isEmpty()) return 0.0f;
        float height = 0.0f;
        for (RichText line : lines) height += lineHeight(line);
        return height;
    }

    @WikiMethod(
            title = "<key:text.engine.method.lines_height_ctx_list.title>",
            summary = "<key:text.engine.method.lines_height_ctx_list.summary>",
            order = 130)
    @WikiReturn("<key:text.engine.method.lines_height_ctx_list.return>")
    public static float linesHeight(
            @WikiParameter(name = "context", description = "<key:text.engine.method.lines_height_ctx_list.context>")
            RenderContext context,
            @WikiParameter(name = "lines", description = "<key:text.engine.method.lines_height_ctx_list.lines>")
            List<RichText> lines) {
        if (lines == null || lines.isEmpty()) return 0.0f;
        float height = 0.0f;
        for (RichText line : lines) height += lineHeight(context, line);
        return height;
    }

    @WikiMethod(
            title = "<key:text.engine.method.draw_str.title>",
            summary = "<key:text.engine.method.draw_str.summary>",
            order = 140)
    public static void draw(
            @WikiParameter(name = "context", description = "<key:text.engine.method.draw_str.context>")
            RenderContext context,
            @WikiParameter(name = "text", description = "<key:text.engine.method.draw_str.text>")
            String text,
            @WikiParameter(name = "x", description = "<key:text.engine.method.draw_str.x>")
            float x,
            @WikiParameter(name = "y", description = "<key:text.engine.method.draw_str.y>")
            float y,
            @WikiParameter(name = "width", description = "<key:text.engine.method.draw_str.width>")
            float width,
            @WikiParameter(name = "height", description = "<key:text.engine.method.draw_str.height>")
            float height,
                            @WikiParameter(name = "paint", description = "<key:text.engine.method.draw_str.paint>")
            Paint paint,
            @WikiParameter(name = "transform", description = "<key:text.engine.method.draw_str.transform>")
            Transform transform,
            @WikiParameter(name = "horizontalAlignment", description = "<key:text.engine.method.draw_str.h_align>")
            Alignment horizontalAlignment,
            @WikiParameter(name = "verticalAlignment", description = "<key:text.engine.method.draw_str.v_align>")
            Alignment verticalAlignment) {
        if (text == null || text.isEmpty()) return;
        // String API тоже проходит через resolver, чтобы XML/модовый scope мог подменить marker'ы.
        draw(context, RichText.resolve(text), x, y, width, height, paint, transform, horizontalAlignment, verticalAlignment);
    }

    @WikiMethod(
            title = "<key:text.engine.method.draw_rich.title>",
            summary = "<key:text.engine.method.draw_rich.summary>",
            order = 150)
    public static void draw(
            @WikiParameter(name = "context", description = "<key:text.engine.method.draw_rich.context>")
            RenderContext context,
            @WikiParameter(name = "text", description = "<key:text.engine.method.draw_rich.text>")
            RichText text,
            @WikiParameter(name = "x", description = "<key:text.engine.method.draw_rich.x>")
            float x,
            @WikiParameter(name = "y", description = "<key:text.engine.method.draw_rich.y>")
            float y,
            @WikiParameter(name = "width", description = "<key:text.engine.method.draw_rich.width>")
            float width,
            @WikiParameter(name = "height", description = "<key:text.engine.method.draw_rich.height>")
            float height,
            @WikiParameter(name = "paint", description = "<key:text.engine.method.draw_rich.paint>")
            Paint paint,
            @WikiParameter(name = "transform", description = "<key:text.engine.method.draw_rich.transform>")
            Transform transform,
            @WikiParameter(name = "horizontalAlignment", description = "<key:text.engine.method.draw_rich.h_align>")
            Alignment horizontalAlignment,
            @WikiParameter(name = "verticalAlignment", description = "<key:text.engine.method.draw_rich.v_align>")
            Alignment verticalAlignment) {
        if (context == null || text == null || text.isEmpty()) return;

        float textWidth = Math.min(Math.max(0.0f, width), measureLineWidth(context, text));
        float textHeight = Math.min(Math.max(0.0f, height), measureTextHeight(context, text));
        float drawX = alignedStart(x, Math.max(0.0f, width), textWidth, horizontalAlignment);
        float drawY = alignedStart(y, Math.max(0.0f, height), textHeight, verticalAlignment);
        if (text.hasInlineContent()) {
            // Inline-rich path раскладывает span'ы вручную, потому что backend.text не знает custom draw atoms.
            drawInline(new DrawScope(context, transform), text, drawX, drawY,
                    Math.max(0.0f, width - (drawX - x)), textHeight, paint);
        } else {
            // Градиент считается по реальному extent'у текста, а не по оставшейся ширине контейнера.
            boolean brushScope = pushBrushScope(context, text, drawX, drawY);
            try {
                context.text(text, drawX, drawY, Math.max(0.0f, width - (drawX - x)), textHeight, paint, transform);
            } finally {
                if (brushScope) context.popTextBrushBounds();
            }
        }
    }

    /**
     * Рисует rich text с поддержкой inline-content.
     *
     * <p>Для plain rich text метод делегирует в {@link DrawScope#text(RichText, float, float, float, float, Paint)}.
     * Для mixed content он последовательно рисует текстовые chunks и вызывает renderer каждого
     * {@link InlineContentSpan}. Переносы внутри text-run'ов поддерживаются, но редактирование inline-span'ов
     * остаётся задачей будущего editor layout. Если inline-span выше шрифта, обычные glyph'ы
     * автоматически центрируются внутри общей line box.</p>
     */
    @WikiMethod(
            title = "<key:text.engine.method.draw_inline.title>",
            summary = "<key:text.engine.method.draw_inline.summary>",
            order = 160)
    public static void drawInline(
            @WikiParameter(name = "draw", description = "<key:text.engine.method.draw_inline.draw>")
            DrawScope draw,
            @WikiParameter(name = "text", description = "<key:text.engine.method.draw_inline.text>")
            RichText text,
            @WikiParameter(name = "x", description = "<key:text.engine.method.draw_inline.x>")
            float x,
            @WikiParameter(name = "y", description = "<key:text.engine.method.draw_inline.y>")
            float y,
            @WikiParameter(name = "width", description = "<key:text.engine.method.draw_inline.width>")
            float width,
            @WikiParameter(name = "height", description = "<key:text.engine.method.draw_inline.height>")
            float height,
            @WikiParameter(name = "paint", description = "<key:text.engine.method.draw_inline.paint>")
            Paint paint) {
        drawInline(draw, text, x, y, width, height, paint,
                Float.NaN);
    }

    /**
     * Рисует mixed RichText, позволяя сдвинуть только обычные glyph'ы вниз.
     * Inline-content сохраняет собственное выравнивание по line box.
     */
    @WikiMethod(
            title = "<key:text.engine.method.draw_inline_offset.title>",
            summary = "<key:text.engine.method.draw_inline_offset.summary>",
            order = 170)
    public static void drawInline(
            @WikiParameter(name = "draw", description = "<key:text.engine.method.draw_inline_offset.draw>")
            DrawScope draw,
            @WikiParameter(name = "text", description = "<key:text.engine.method.draw_inline_offset.text>")
            RichText text,
            @WikiParameter(name = "x", description = "<key:text.engine.method.draw_inline_offset.x>")
            float x,
            @WikiParameter(name = "y", description = "<key:text.engine.method.draw_inline_offset.y>")
            float y,
            @WikiParameter(name = "width", description = "<key:text.engine.method.draw_inline_offset.width>")
            float width,
            @WikiParameter(name = "height", description = "<key:text.engine.method.draw_inline_offset.height>")
            float height,
            @WikiParameter(name = "paint", description = "<key:text.engine.method.draw_inline_offset.paint>")
            Paint paint,
            @WikiParameter(name = "textVisualOffset", description = "<key:text.engine.method.draw_inline_offset.offset>")
            float textVisualOffset) {
        if (draw == null || text == null || text.isEmpty()) return;
        if (!text.hasInlineContent()) {
            RenderContext plainContext = draw.context();
            boolean plainBrushScope = pushBrushScope(plainContext, text, x, y);
            try {
                draw.text(text, x, y, width, height, paint);
            } finally {
                if (plainBrushScope) plainContext.popTextBrushBounds();
            }
            return;
        }

        float lineHeight = Math.max(LINE_HEIGHT,
                height > 0.0f ? height : measureTextHeight(draw.context(), text));
        float cursorX = x;
        float cursorY = y;
        FontFace defaultFace = draw.context().backend() == null ? null : draw.context().backend().defaultTextFace();
        List<RichTextSpan> spans = text.spans();
        TrailingInline trailing = trailingInline(spans, defaultFace);
        boolean pinTrailing = trailing != null
                && width > 0.0f
                && measureLineWidth(text, defaultFace) > width;
        float trailingX = pinTrailing
                ? x + Math.max(0.0f, width - trailing.inline().width())
                : 0.0f;
        float textClipWidth = pinTrailing
                ? Math.max(0.0f, trailingX - x - trailing.gapWidth())
                : 0.0f;
        // Все chunk'и и inline-span'ы одного текста делят общий прямоугольник градиента.
        boolean brushScope = pushBrushScope(draw.context(), text, x, y,
                pinTrailing ? width : measureLineWidth(text, defaultFace),
                Math.max(lineHeight, measureTextHeight(draw.context(), text)));
        boolean textClipPushed = false;
        if (pinTrailing) {
            draw.pushTextClip(x, y, textClipWidth, lineHeight);
            textClipPushed = true;
        }

        try {
            for (int spanIndex = 0; spanIndex < spans.size(); spanIndex++) {
                RichTextSpan span = spans.get(spanIndex);
                if (pinTrailing && spanIndex == trailing.gapSpanIndex()) {
                    continue;
                }
                if (pinTrailing && spanIndex == trailing.inlineSpanIndex()) {
                    if (textClipPushed) {
                        draw.popClip();
                        textClipPushed = false;
                    }
                    cursorX = trailingX;
                }

                if (span instanceof TextRun run) {
                    float runOffset = Float.isFinite(textVisualOffset)
                            ? Math.max(0.0f, textVisualOffset)
                            : centeredRunOffset(run, lineHeight, defaultFace);
                    TextRunDrawResult result = drawTextRun(draw, run, cursorX,
                            cursorY + runOffset,
                            lineHeight, paint, defaultFace, x);
                    cursorX = result.cursorX();
                    cursorY = result.cursorY() - runOffset;
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
        } finally {
            if (textClipPushed) draw.popClip();
            if (brushScope) draw.context().popTextBrushBounds();
        }
    }

    /** Находит завершающий inline-span и пробел перед ним, который служит отступом. */
    private static TrailingInline trailingInline(List<RichTextSpan> spans, FontFace defaultFace) {
        if (spans == null || spans.isEmpty()) return null;
        int inlineIndex = spans.size() - 1;
        if (!(spans.get(inlineIndex) instanceof InlineContentSpan inline)) return null;

        int gapIndex = -1;
        float gapWidth = 0.0f;
        if (inlineIndex > 0 && spans.get(inlineIndex - 1) instanceof TextRun gap
                && !gap.text().isEmpty() && gap.text().isBlank()) {
            gapIndex = inlineIndex - 1;
            gapWidth = measureRunWidth(gap, defaultFace);
        }
        return new TrailingInline(inlineIndex, inline, gapIndex, gapWidth);
    }

    /**
     * Вычисляет оптическое смещение текстового run'а внутри line box.
     *
     * <p>Inline-контент может быть выше шрифта. В таком случае обычный текст нужно
     * опустить на половину разницы высот, иначе он визуально оказывается выше центра.
     * Для явной перегрузки {@code drawInline(..., textVisualOffset)} эта автоматическая
     * коррекция не используется.</p>
     */
    private static float centeredRunOffset(TextRun run, float lineHeight, FontFace defaultFace) {
        if (run == null) return 0.0f;
        FontFace face = run.font() == null ? defaultFace : run.font();
        float runHeight = face == null
                ? fallbackLineHeight(run.pixelSize())
                : positiveLineHeight(face.metrics(run.pixelSize()).lineHeight());
        return Math.max(0.0f, lineHeight - runHeight) * 0.5f;
    }

    /**
     * Проверяет, есть ли в тексте run с brush-заливкой, зависящей от положения (градиентом).
     *
     * @param text rich-text или {@code null}
     * @return {@code true}, если нужен общий прямоугольник заливки
     */
    @WikiMethod(
            title = "<key:text.engine.method.uses_positional_brush.title>",
            summary = "<key:text.engine.method.uses_positional_brush.summary>",
            order = 180)
    @WikiReturn("<key:text.engine.method.uses_positional_brush.return>")
    public static boolean usesPositionalBrush(
            @WikiParameter(name = "text", description = "<key:text.engine.method.uses_positional_brush.text>")
            RichText text) {
        if (text == null) return false;
        List<TextRun> runs = text.runs();
        for (int i = 0, size = runs.size(); i < size; i++) {
            TextBrush brush = runs.get(i).brush();
            if (brush != null && !(brush instanceof SolidTextBrush)) return true;
        }
        return false;
    }

    /**
     * Открывает общий brush-scope по измеренному extent'у текста, если он нужен и ещё не открыт снаружи.
     *
     * @return {@code true}, если scope открыт и вызывающий обязан вызвать {@code popTextBrushBounds()}
     */
    private static boolean pushBrushScope(RenderContext context, RichText text, float x, float y) {
        if (context == null || !usesPositionalBrush(text) || context.textBrushBounds() != null) return false;
        context.pushTextBrushBounds(x, y,
                measureLineWidth(context, text), measureTextHeight(context, text));
        return true;
    }

    /** Вариант {@link #pushBrushScope(RenderContext, RichText, float, float)} с уже известным extent'ом. */
    private static boolean pushBrushScope(RenderContext context, RichText text,
                                          float x, float y, float width, float height) {
        if (context == null || !usesPositionalBrush(text) || context.textBrushBounds() != null) return false;
        context.pushTextBrushBounds(x, y, width, height);
        return true;
    }

    @WikiMethod(
            title = "<key:text.engine.method.aligned_start.title>",
            summary = "<key:text.engine.method.aligned_start.summary>",
            order = 190)
    @WikiReturn("<key:text.engine.method.aligned_start.return>")
    public static float alignedStart(
            @WikiParameter(name = "start", description = "<key:text.engine.method.aligned_start.start>")
            float start,
            @WikiParameter(name = "available", description = "<key:text.engine.method.aligned_start.available>")
            float available,
            @WikiParameter(name = "size", description = "<key:text.engine.method.aligned_start.size>")
            float size,
            @WikiParameter(name = "alignment", description = "<key:text.engine.method.aligned_start.alignment>")
            Alignment alignment) {
        return switch (alignment == null ? Alignment.START : alignment) {
            case CENTER -> start + Math.max(0.0f, available - size) * 0.5f;
            case END -> start + Math.max(0.0f, available - size);
            case START, STRETCH, BASELINE -> start;
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

    private record TrailingInline(int inlineSpanIndex,
                                  InlineContentSpan inline,
                                  int gapSpanIndex,
                                  float gapWidth) {
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
