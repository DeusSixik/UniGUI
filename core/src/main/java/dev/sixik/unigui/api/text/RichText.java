package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.TextureHandle;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Неизменяемый rich-text, собранный из текстовых run'ов и опционального inline-контента.
 *
 * <p>{@code RichText} хранит две проекции одного значения:</p>
 *
 * <ul>
 *     <li>{@link #spans()} — полный поток layout-атомов, включая {@link TextRun} и {@link InlineContentSpan};</li>
 *     <li>{@link #runs()} — совместимая старая проекция только текстовых run'ов.</li>
 * </ul>
 *
 * <p>Inline-content не является widget'ом и не хранит backend-специфичное состояние. Он измеряется
 * как атомарный span, а рисование выполняется позднее через {@link InlineContentRenderer}. Для plain
 * сценариев можно продолжать использовать {@link #plain(String)} и {@link #of(String, FontFace, float)}.</p>
 *
 * <p>Если строка пришла из XML или обычного {@code text(String)} API, используйте {@link #resolve(String)}.
 * Этот метод берёт текущий {@link InlineContentResolver} из {@link InlineContentResolvers} и позволяет
 * внешнему коду подключить marker'ы вида {@code {icon:...}} без хардкода в виджетах.</p>
 *
 * <pre>{@code
 * RichText text = RichText.builder()
 *         .append("Status ")
 *         .icon("status:ok", texture, 10.0f)
 *         .append(" Ready")
 *         .build();
 * }</pre>
 *
 * @see RichTextSpan
 * @see InlineContentSpan
 * @see InlineContentResolvers
 */
@AiDoc(
        summary = "Immutable multi-style rich text model supporting styled runs, fonts, brushes, gradients, and inline content spans.",
        purpose = "Provides a unified, immutable representation of text and embedded inline atoms (icons, badges, images) for UI layout, line wrapping, and rendering.",
        useWhen = {
                "Rendering formatted labels, buttons, tooltips, or chat logs with mixed fonts, sizes, and colors.",
                "Embedding icons, badges, or procedural inline graphics directly into continuous text flows.",
                "Applying multi-stop gradients or custom brushes across text runs."
        },
        criticalRules = {
                "RichText is strictly immutable. Methods like withBrush, withTextStyle, append, and slice return new instances.",
                "Inline content spans participate in line wrapping and alignment but do not receive direct UI input events.",
                "Plain string operations can use RichText.plain(String) or RichText.resolve(String) for automatic icon marker resolution."
        },
        related = {"TextRun", "RichTextSpan", "InlineContentSpan", "FontFace", "TextBrush", "TextEngine"},
        examples = {
                @AiDoc.Example(
                        title = "Building Rich Text with Text and Inline Icons",
                        order = 10,
                        code = """
                                RichText text = RichText.builder()
                                        .append("Status: ")
                                        .icon("icon:ok", checkTexture, 12.0f)
                                        .color(ColorView.rgb(80, 220, 100))
                                        .append(" All systems online")
                                        .build();
                                """),
                @AiDoc.Example(
                        title = "Applying Linear Gradient Across Text",
                        order = 20,
                        code = """
                                RichText gradientTitle = RichText.gradient(
                                        "UniGUI Engine",
                                        Fonts.TITLE,
                                        18.0f,
                                        ColorView.rgb(255, 100, 100),
                                        ColorView.rgb(100, 150, 255),
                                        45.0f
                                );
                                """)
        })
@WikiPage(
        title = "<key:rich.text.title>",
        slug = "rich-text",
        category = "<key:rich.text.category>",
        summary = "<key:rich.text.summary>",
        order = 10)
@WikiSection(
        title = "<key:rich.text.section.overview.title>",
        markdown = "<key:rich.text.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:rich.text.section.spans.title>",
        markdown = "<key:rich.text.section.spans.body>",
        order = 20)
@WikiSection(
        title = "<key:rich.text.section.inline.title>",
        markdown = "<key:rich.text.section.inline.body>",
        order = 30)
@WikiSection(
        title = "<key:rich.text.section.brushes.title>",
        markdown = "<key:rich.text.section.brushes.body>",
        order = 40)
@WikiSection(
        title = "<key:rich.text.section.transform.title>",
        markdown = "<key:rich.text.section.transform.body>",
        order = 50)
@WikiSection(
        title = "<key:rich.text.section.builder.title>",
        markdown = "<key:rich.text.section.builder.body>",
        order = 60)
@WikiExample(
        title = "<key:rich.text.example.mixed.title>",
        code = "<key:rich.text.example.mixed.code>",
        order = 10)
@WikiExample(
        title = "<key:rich.text.example.gradient.title>",
        code = "<key:rich.text.example.gradient.code>",
        order = 20)
public final class RichText {
    private final List<RichTextSpan> spans;
    private final List<TextRun> runs;
    private final String plainText;
    private final boolean hasInlineContent;

    /**
     * Создаёт rich-text из старого списка текстовых run'ов.
     *
     * @param runs текстовые run'ы; {@code null} и пустые run'ы игнорируются
     */
    public RichText(List<TextRun> runs) {
        this(asSpans(runs), true);
    }

    private RichText(List<? extends RichTextSpan> spans, boolean ignored) {
        ObjectArrayList<RichTextSpan> normalizedSpans = new ObjectArrayList<>();
        ObjectArrayList<TextRun> normalizedRuns = new ObjectArrayList<>();
        StringBuilder text = new StringBuilder();
        boolean inline = false;
        if (spans != null) {
            for (RichTextSpan span : spans) {
                if (span == null || span.isEmpty()) continue;
                normalizedSpans.add(span);
                text.append(span.fallbackText());
                if (span instanceof TextRun run) {
                    normalizedRuns.add(run);
                } else {
                    inline = true;
                }
            }
        }
        this.spans = Collections.unmodifiableList(normalizedSpans);
        this.runs = Collections.unmodifiableList(normalizedRuns);
        this.plainText = text.toString();
        this.hasInlineContent = inline;
    }

    /**
     * Создаёт plain rich-text без inline-контента и без явного font face.
     *
     * @param text исходный текст
     * @return rich-text из одного {@link TextRun}
     */
    @WikiMethod(
            title = "<key:rich.text.method.plain.title>",
            summary = "<key:rich.text.method.plain.summary>",
            order = 10)
    @WikiReturn("<key:rich.text.method.plain.return>")
    public static RichText plain(
            @WikiParameter(name = "text", description = "<key:rich.text.method.plain.text>")
            String text) {
        return new RichText(List.of(new TextRun(text, null, TextRun.DEFAULT_PIXEL_SIZE)));
    }

    /**
     * Разбирает строку через текущий {@link InlineContentResolver}.
     *
     * <p>Если resolver не установлен, результат совпадает с {@link #plain(String)}. Метод нужен для
     * обычных string-based API: виджет может принимать строку, а активный экран или XML loader решает,
     * надо ли превращать marker'ы в inline icons.</p>
     *
     * @param text исходная строка
     * @return rich-text после resolver'а
     */
    @WikiMethod(
            title = "<key:rich.text.method.resolve.title>",
            summary = "<key:rich.text.method.resolve.summary>",
            order = 20)
    @WikiReturn("<key:rich.text.method.resolve.return>")
    public static RichText resolve(
            @WikiParameter(name = "text", description = "<key:rich.text.method.resolve.text>")
            String text) {
        return InlineContentResolvers.resolve(text);
    }

    /**
     * Создаёт rich-text из одного текстового run'а с заданным шрифтом и размером.
     *
     * @param text исходный текст
     * @param font font face или {@code null}, чтобы backend выбрал default
     * @param pixelSize размер текста в UI-пикселях
     * @return rich-text из одного {@link TextRun}
     */
    @WikiMethod(
            title = "<key:rich.text.method.of_font.title>",
            summary = "<key:rich.text.method.of_font.summary>",
            order = 30)
    @WikiReturn("<key:rich.text.method.of_font.return>")
    public static RichText of(
            @WikiParameter(name = "text", description = "<key:rich.text.method.of_font.text>")
            String text,
            @WikiParameter(name = "font", description = "<key:rich.text.method.of_font.font>")
            FontFace font,
            @WikiParameter(name = "pixelSize", description = "<key:rich.text.method.of_font.pixel_size>")
            float pixelSize) {
        return new RichText(List.of(new TextRun(text, font, pixelSize)));
    }

    /**
     * Создаёт rich-text из одного текстового run'а с цветом.
     *
     * @param text исходный текст
     * @param font font face или {@code null}, чтобы backend выбрал default
     * @param pixelSize размер текста в UI-пикселях
     * @param color цвет run'а или {@code null}, чтобы renderer использовал paint виджета
     * @return rich-text из одного {@link TextRun}
     */
    @WikiMethod(
            title = "<key:rich.text.method.of_color.title>",
            summary = "<key:rich.text.method.of_color.summary>",
            order = 40)
    @WikiReturn("<key:rich.text.method.of_color.return>")
    public static RichText of(
            @WikiParameter(name = "text", description = "<key:rich.text.method.of_color.text>")
            String text,
            @WikiParameter(name = "font", description = "<key:rich.text.method.of_color.font>")
            FontFace font,
            @WikiParameter(name = "pixelSize", description = "<key:rich.text.method.of_color.pixel_size>")
            float pixelSize,
            @WikiParameter(name = "color", description = "<key:rich.text.method.of_color.color>")
            ColorView color) {
        return new RichText(List.of(new TextRun(text, font, pixelSize, color)));
    }

    /**
     * Создаёт rich-text из одного текстового run'а с brush-заливкой.
     *
     * @param text исходный текст
     * @param font font face или {@code null}, чтобы backend выбрал default
     * @param pixelSize размер текста в UI-пикселях
     * @param brush brush-заливка текста или {@code null} для обычного цвета
     * @return rich-text из одного {@link TextRun}
     */
    @WikiMethod(
            title = "<key:rich.text.method.brushed.title>",
            summary = "<key:rich.text.method.brushed.summary>",
            order = 50)
    @WikiReturn("<key:rich.text.method.brushed.return>")
    public static RichText brushed(
            @WikiParameter(name = "text", description = "<key:rich.text.method.brushed.text>")
            String text,
            @WikiParameter(name = "font", description = "<key:rich.text.method.brushed.font>")
            FontFace font,
            @WikiParameter(name = "pixelSize", description = "<key:rich.text.method.brushed.pixel_size>")
            float pixelSize,
            @WikiParameter(name = "brush", description = "<key:rich.text.method.brushed.brush>")
            TextBrush brush) {
        return new RichText(List.of(new TextRun(text, font, pixelSize, null, brush)));
    }

    /**
     * Создаёт rich-text из одного run'а с линейным градиентом.
     *
     * @param text исходный текст
     * @param font font face или {@code null}, чтобы backend выбрал default
     * @param pixelSize размер текста в UI-пикселях
     * @param startColor цвет начала градиента
     * @param endColor цвет конца градиента
     * @param angleDegrees угол направления в градусах
     * @return rich-text с {@link LinearGradientTextBrush}
     */
    @WikiMethod(
            title = "<key:rich.text.method.gradient.title>",
            summary = "<key:rich.text.method.gradient.summary>",
            order = 60)
    @WikiReturn("<key:rich.text.method.gradient.return>")
    public static RichText gradient(
            @WikiParameter(name = "text", description = "<key:rich.text.method.gradient.text>")
            String text,
            @WikiParameter(name = "font", description = "<key:rich.text.method.gradient.font>")
            FontFace font,
            @WikiParameter(name = "pixelSize", description = "<key:rich.text.method.gradient.pixel_size>")
            float pixelSize,
            @WikiParameter(name = "startColor", description = "<key:rich.text.method.gradient.start_color>")
            ColorView startColor,
            @WikiParameter(name = "endColor", description = "<key:rich.text.method.gradient.end_color>")
            ColorView endColor,
            @WikiParameter(name = "angleDegrees", description = "<key:rich.text.method.gradient.angle_degrees>")
            float angleDegrees) {
        return brushed(text, font, pixelSize, TextBrush.linearGradient(startColor, endColor, angleDegrees));
    }

    /**
     * Создаёт rich-text из полного списка layout-span'ов.
     *
     * @param spans текстовые и inline span'ы
     * @return нормализованный immutable rich-text
     */
    @WikiMethod(
            title = "<key:rich.text.method.of_spans.title>",
            summary = "<key:rich.text.method.of_spans.summary>",
            order = 70)
    @WikiReturn("<key:rich.text.method.of_spans.return>")
    public static RichText ofSpans(
            @WikiParameter(name = "spans", description = "<key:rich.text.method.of_spans.spans>")
            List<? extends RichTextSpan> spans) {
        return new RichText(spans, true);
    }

    /**
     * @return builder для fluent-сборки mixed text + inline content
     */
    @WikiMethod(
            title = "<key:rich.text.method.builder.title>",
            summary = "<key:rich.text.method.builder.summary>",
            order = 80)
    @WikiReturn("<key:rich.text.method.builder.return>")
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Совместимая проекция только текстовых run'ов.
     *
     * @return immutable список {@link TextRun}; inline span'ы сюда не попадают
     */
    @WikiMethod(
            title = "<key:rich.text.method.runs.title>",
            summary = "<key:rich.text.method.runs.summary>",
            order = 90)
    @WikiReturn("<key:rich.text.method.runs.return>")
    public List<TextRun> runs() {
        return runs;
    }

    /**
     * Полный поток layout-атомов.
     *
     * @return immutable список {@link TextRun} и {@link InlineContentSpan}
     */
    @WikiMethod(
            title = "<key:rich.text.method.spans.title>",
            summary = "<key:rich.text.method.spans.summary>",
            order = 100)
    @WikiReturn("<key:rich.text.method.spans.return>")
    public List<RichTextSpan> spans() {
        return spans;
    }

    /**
     * Plain fallback всего rich-text.
     *
     * @return текстовые run'ы плюс fallback text inline-span'ов
     */
    @WikiMethod(
            title = "<key:rich.text.method.plain_text.title>",
            summary = "<key:rich.text.method.plain_text.summary>",
            order = 110)
    @WikiReturn("<key:rich.text.method.plain_text.return>")
    public String plainText() {
        return plainText;
    }

    /**
     * @return {@code true}, если значение содержит хотя бы один inline-span
     */
    @WikiMethod(
            title = "<key:rich.text.method.has_inline_content.title>",
            summary = "<key:rich.text.method.has_inline_content.summary>",
            order = 120)
    @WikiReturn("<key:rich.text.method.has_inline_content.return>")
    public boolean hasInlineContent() {
        return hasInlineContent;
    }

    /**
     * @return {@code true}, если после нормализации не осталось span'ов
     */
    @WikiMethod(
            title = "<key:rich.text.method.is_empty.title>",
            summary = "<key:rich.text.method.is_empty.summary>",
            order = 130)
    @WikiReturn("<key:rich.text.method.is_empty.return>")
    public boolean isEmpty() {
        return spans.isEmpty();
    }

    /**
     * Возвращает копию rich-text, где все текстовые run'ы используют заданный brush.
     *
     * <p>Inline-content span'ы сохраняются как есть. Цвет run'а не удаляется и продолжает работать как tint.
     * {@code null} сбрасывает brush и возвращает обычную solid-заливку через paint/run color.</p>
     *
     * @param brush новый brush для всех {@link TextRun}
     * @return новое immutable rich-text значение
     */
    @WikiMethod(
            title = "<key:rich.text.method.with_brush.title>",
            summary = "<key:rich.text.method.with_brush.summary>",
            order = 140)
    @WikiReturn("<key:rich.text.method.with_brush.return>")
    public RichText withBrush(
            @WikiParameter(name = "brush", description = "<key:rich.text.method.with_brush.brush>")
            TextBrush brush) {
        if (spans.isEmpty()) return this;
        ObjectArrayList<RichTextSpan> updated = new ObjectArrayList<>(spans.size());
        for (RichTextSpan span : spans) {
            if (span instanceof TextRun run) {
                updated.add(new TextRun(
                        run.text(),
                        run.font(),
                        run.pixelSize(),
                        run.color(),
                        brush,
                        run.tracking(),
                        run.transform()));
            } else {
                updated.add(span);
            }
        }
        return new RichText(updated, true);
    }

    /**
     * Возвращает копию текста с переопределёнными параметрами текстовых run'ов.
     * Inline-содержимое сохраняется без изменений.
     *
     * @param pixelSize новый размер текста или {@code null}, чтобы сохранить размер run'а
     * @param tracking новый tracking или {@code null}, чтобы сохранить tracking run'а
     * @param transform новое преобразование или {@code null}, чтобы сохранить transform run'а
     * @return обновлённый immutable rich-text или текущий объект, если переопределений нет
     */
    @WikiMethod(
            title = "<key:rich.text.method.with_text_style.title>",
            summary = "<key:rich.text.method.with_text_style.summary>",
            order = 150)
    @WikiReturn("<key:rich.text.method.with_text_style.return>")
    public RichText withTextStyle(
            @WikiParameter(name = "pixelSize", description = "<key:rich.text.method.with_text_style.pixel_size>")
            Float pixelSize,
            @WikiParameter(name = "tracking", description = "<key:rich.text.method.with_text_style.tracking>")
            Float tracking,
            @WikiParameter(name = "transform", description = "<key:rich.text.method.with_text_style.transform>")
            TextTransform transform) {
        if (spans.isEmpty() || (pixelSize == null && tracking == null && transform == null)) return this;
        ObjectArrayList<RichTextSpan> updated = new ObjectArrayList<>(spans.size());
        for (RichTextSpan span : spans) {
            if (span instanceof TextRun run) {
                updated.add(new TextRun(
                        run.text(),
                        run.font(),
                        pixelSize == null ? run.pixelSize() : pixelSize,
                        run.color(),
                        run.brush(),
                        tracking == null ? run.tracking() : tracking,
                        transform == null ? run.transform() : transform));
            } else {
                updated.add(span);
            }
        }
        return new RichText(updated, true);
    }

    /**
     * Возвращает копию rich-text с линейным градиентом на всех текстовых run'ах.
     */
    @WikiMethod(
            title = "<key:rich.text.method.with_linear_gradient.title>",
            summary = "<key:rich.text.method.with_linear_gradient.summary>",
            order = 160)
    @WikiReturn("<key:rich.text.method.with_linear_gradient.return>")
    public RichText withLinearGradient(
            @WikiParameter(name = "startColor", description = "<key:rich.text.method.with_linear_gradient.start_color>")
            ColorView startColor,
            @WikiParameter(name = "endColor", description = "<key:rich.text.method.with_linear_gradient.end_color>")
            ColorView endColor,
            @WikiParameter(name = "angleDegrees", description = "<key:rich.text.method.with_linear_gradient.angle_degrees>")
            float angleDegrees) {
        return withBrush(TextBrush.linearGradient(startColor, endColor, angleDegrees));
    }

    /**
     * Добавляет другое rich-text значение в конец текущего.
     *
     * @param other добавляемый rich-text или {@code null}
     * @return новое immutable значение; текущий объект не изменяется
     */
    @WikiMethod(
            title = "<key:rich.text.method.append.title>",
            summary = "<key:rich.text.method.append.summary>",
            order = 170)
    @WikiReturn("<key:rich.text.method.append.return>")
    public RichText append(
            @WikiParameter(name = "other", description = "<key:rich.text.method.append.other>")
            RichText other) {
        if (other == null || other.isEmpty()) return this;
        List<RichTextSpan> combined = new ObjectArrayList<>(spans.size() + other.spans.size());
        combined.addAll(spans);
        combined.addAll(other.spans);
        return new RichText(combined, true);
    }

    /**
     * Возвращает срез по индексам plain fallback text.
     *
     * <p>Если срез полностью захватывает inline-span, он сохраняется как inline-content. Если срез
     * попадает внутрь fallback text inline-span'а, такой фрагмент деградирует до обычного {@link TextRun}.
     * Это сохраняет корректное plain-поведение для search/copy/debug и не создаёт частично обрезанные
     * draw-команды.</p>
     *
     * @param startInclusive начало среза включительно
     * @param endExclusive конец среза исключительно
     * @return новый rich-text с выбранным диапазоном
     */
    @WikiMethod(
            title = "<key:rich.text.method.slice.title>",
            summary = "<key:rich.text.method.slice.summary>",
            order = 180)
    @WikiReturn("<key:rich.text.method.slice.return>")
    public RichText slice(
            @WikiParameter(name = "startInclusive", description = "<key:rich.text.method.slice.start_inclusive>")
            int startInclusive,
            @WikiParameter(name = "endExclusive", description = "<key:rich.text.method.slice.end_exclusive>")
            int endExclusive) {
        int start = Math.max(0, Math.min(startInclusive, plainText.length()));
        int end = Math.max(start, Math.min(endExclusive, plainText.length()));
        if (start == 0 && end == plainText.length()) return this;
        if (start == end) return RichText.plain("");

        List<RichTextSpan> sliced = new ObjectArrayList<>();
        int spanStart = 0;
        for (RichTextSpan span : spans) {
            String fallback = span.fallbackText();
            int spanEnd = spanStart + fallback.length();
            int overlapStart = Math.max(start, spanStart);
            int overlapEnd = Math.min(end, spanEnd);
            if (overlapStart < overlapEnd) {
                if (span instanceof TextRun run) {
                    sliced.add(sliceRun(run, overlapStart - spanStart, overlapEnd - spanStart));
                } else if (overlapStart == spanStart && overlapEnd == spanEnd) {
                    sliced.add(span);
                } else {
                    String fallbackSlice = fallback.substring(overlapStart - spanStart, overlapEnd - spanStart);
                    sliced.add(new TextRun(fallbackSlice, null, TextRun.DEFAULT_PIXEL_SIZE));
                }
            }
            spanStart = spanEnd;
            if (spanStart >= end) break;
        }
        return new RichText(sliced, true);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof RichText richText && spans.equals(richText.spans);
    }

    @Override
    public int hashCode() {
        return spans.hashCode();
    }

    @Override
    public String toString() {
        return plainText;
    }

    private static TextRun sliceRun(TextRun run, int start, int end) {
        String value = run.text();
        int safeStart = Math.max(0, Math.min(start, value.length()));
        int safeEnd = Math.max(safeStart, Math.min(end, value.length()));
        return new TextRun(
                value.substring(safeStart, safeEnd),
                run.font(),
                run.pixelSize(),
                run.color(),
                run.brush(),
                run.tracking(),
                run.transform());
    }

    private static List<RichTextSpan> asSpans(List<TextRun> runs) {
        ObjectArrayList<RichTextSpan> spans = new ObjectArrayList<>();
        if (runs != null) {
            for (TextRun run : runs) {
                if (run != null) spans.add(run);
            }
        }
        return spans;
    }

    /**
     * Fluent builder для {@link RichText}.
     *
     * <p>Builder хранит текущие параметры text-run'а: font, size, color, brush, tracking и transform.
     * Они применяются к последующим {@link #append(String)}. Inline-span'ы добавляются как отдельные
     * атомы и не наследуют эти параметры, кроме тех случаев, когда renderer сам использует paint.</p>
     */
    @AiDoc(
            summary = "Fluent builder for assembling multi-style RichText with text runs and inline content atoms.",
            purpose = "Maintains persistent style attributes (font, size, color, brush, tracking, transform) that apply across chained append() calls, alongside dedicated icon() and inline() helpers.",
            useWhen = {
                    "Constructing formatted text containing multiple colors, fonts, or inline graphic spans.",
                    "Embedding icons or procedural badges into rich text."
            },
            criticalRules = {
                    "Style attributes set on the builder apply only to subsequent append() calls; they do not retroactively alter previously added spans.",
                    "Inline spans do not inherit text styling attributes except when the inline renderer explicitly utilizes the context color tint."
            },
            related = {"RichText", "TextRun", "InlineContentSpan", "FontFace"}
    )
    public static final class Builder {
        private final List<RichTextSpan> spans = new ObjectArrayList<>();
        private FontFace font;
        private float pixelSize = TextRun.DEFAULT_PIXEL_SIZE;
        private ColorView color;
        private TextBrush brush;
        private float tracking;
        private TextTransform transform = TextTransform.NONE;

        /**
         * Задаёт font face для следующих текстовых run'ов.
         *
         * @param font font face или {@code null}
         * @return этот builder
         */
        public Builder font(FontFace font) {
            this.font = font;
            return this;
        }

        /**
         * Задаёт размер следующих текстовых run'ов.
         *
         * @param pixelSize размер в UI-пикселях
         * @return этот builder
         */
        public Builder size(float pixelSize) {
            this.pixelSize = pixelSize;
            return this;
        }

        /**
         * Задаёт цвет следующих текстовых run'ов.
         *
         * @param color цвет или {@code null}, чтобы renderer использовал paint виджета
         * @return этот builder
         */
        public Builder color(ColorView color) {
            this.color = color;
            return this;
        }

        /**
         * Задаёт brush-заливку для следующих текстовых run'ов.
         *
         * <p>Цвет run'а остаётся tint'ом: итоговый цвет умножается на paint color, run color и brush color.</p>
         *
         * @param brush brush или {@code null}, чтобы использовать обычную solid-заливку
         * @return этот builder
         */
        public Builder brush(TextBrush brush) {
            this.brush = brush;
            return this;
        }

        /**
         * Задаёт линейный градиент для следующих текстовых run'ов.
         *
         * @param startColor цвет начала градиента
         * @param endColor цвет конца градиента
         * @param angleDegrees угол направления в градусах
         * @return этот builder
         */
        public Builder linearGradient(ColorView startColor, ColorView endColor, float angleDegrees) {
            return brush(TextBrush.linearGradient(startColor, endColor, angleDegrees));
        }

        /**
         * Сбрасывает brush и возвращает обычную solid-заливку через paint/run color.
         *
         * @return этот builder
         */
        public Builder clearBrush() {
            return brush(null);
        }

        /**
         * Задаёт дополнительный tracking для следующих текстовых run'ов.
         *
         * @param tracking дополнительный отступ между glyph'ами в UI-пикселях
         * @return этот builder
         */
        public Builder tracking(float tracking) {
            this.tracking = Float.isFinite(tracking) ? Math.max(0.0f, tracking) : 0.0f;
            return this;
        }

        /**
         * Задаёт transform для следующих текстовых run'ов.
         *
         * @param transform text transform или {@code null} для {@link TextTransform#NONE}
         * @return этот builder
         */
        public Builder transform(TextTransform transform) {
            this.transform = transform == null ? TextTransform.NONE : transform;
            return this;
        }

        /**
         * Включает uppercase transform для следующих текстовых run'ов.
         *
         * @return этот builder
         */
        public Builder uppercase() {
            return transform(TextTransform.UPPERCASE);
        }

        /**
         * Добавляет текстовый run с текущими параметрами builder'а.
         *
         * @param text текст run'а
         * @return этот builder
         */
        public Builder append(String text) {
            TextRun run = new TextRun(text, font, pixelSize, color, brush, tracking, transform);
            if (!run.isEmpty()) spans.add(run);
            return this;
        }

        /**
         * Добавляет все span'ы другого {@link RichText}.
         *
         * @param text rich-text или {@code null}
         * @return этот builder
         */
        public Builder append(RichText text) {
            if (text != null) spans.addAll(text.spans);
            return this;
        }

        /**
         * Добавляет готовый inline-span.
         *
         * @param span inline-span или {@code null}
         * @return этот builder
         */
        public Builder appendInline(InlineContentSpan span) {
            if (span != null && !span.isEmpty()) spans.add(span);
            return this;
        }

        /**
         * Добавляет кастомный inline-content renderer.
         *
         * @param id стабильный id span'а для debug/diagnostics
         * @param fallbackText plain fallback для search/copy/debug
         * @param width ширина inline-контента в UI-пикселях
         * @param height высота inline-контента в UI-пикселях
         * @param alignment вертикальное выравнивание внутри строки
         * @param renderer renderer, который получит финальные bounds после layout
         * @return этот builder
         */
        public Builder inline(String id, String fallbackText, float width, float height,
                              InlineContentAlignment alignment, InlineContentRenderer renderer) {
            return appendInline(new InlineContentSpan(id, fallbackText, width, height, alignment, renderer));
        }

        /**
         * Добавляет кастомный inline-content renderer с center alignment.
         *
         * @param id стабильный id span'а
         * @param fallbackText plain fallback
         * @param width ширина в UI-пикселях
         * @param height высота в UI-пикселях
         * @param renderer renderer inline-контента
         * @return этот builder
         */
        public Builder inline(String id, String fallbackText, float width, float height,
                              InlineContentRenderer renderer) {
            return inline(id, fallbackText, width, height, InlineContentAlignment.CENTER, renderer);
        }

        /**
         * Добавляет кастомный inline-content renderer со стандартным object replacement fallback.
         *
         * @param id стабильный id span'а
         * @param width ширина в UI-пикселях
         * @param height высота в UI-пикселях
         * @param renderer renderer inline-контента
         * @return этот builder
         */
        public Builder inline(String id, float width, float height, InlineContentRenderer renderer) {
            return inline(id, InlineContentSpan.DEFAULT_FALLBACK_TEXT, width, height, renderer);
        }

        /**
         * Добавляет square texture icon.
         *
         * @param id стабильный id span'а
         * @param texture texture-handle для рендера
         * @param size ширина и высота в UI-пикселях
         * @return этот builder
         */
        public Builder icon(String id, TextureHandle texture, float size) {
            return icon(id, texture, size, size);
        }

        /**
         * Добавляет texture icon произвольного размера.
         *
         * <p>Icon renderer использует цвет текущего paint'а как tint. Если texture отсутствует,
         * span всё равно участвует в layout, но ничего не рисует.</p>
         *
         * @param id стабильный id span'а
         * @param texture texture-handle для рендера
         * @param width ширина в UI-пикселях
         * @param height высота в UI-пикселях
         * @return этот builder
         */
        public Builder icon(String id, TextureHandle texture, float width, float height) {
            return inline(id, InlineContentSpan.DEFAULT_FALLBACK_TEXT, width, height,
                    (draw, context) -> {
                        if (texture != null) {
                            draw.texture(texture, context.x(), context.y(), context.width(), context.height(),
                                    Paint.fill(context.color()));
                        }
                    });
        }

        /**
         * Собирает immutable rich-text.
         *
         * @return новый {@link RichText}
         */
        public RichText build() {
            return new RichText(spans, true);
        }
    }
}
