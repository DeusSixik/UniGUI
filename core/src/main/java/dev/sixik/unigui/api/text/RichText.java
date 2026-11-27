package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.TextureHandle;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

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
                "Plain string operations can use RichText.plain(String) or RichText.resolve(String) for automatic icon marker resolution.",
                "String markers like {key:jump} resolve everywhere once their type is registered in InlineMarkers; widgets' text(String) uses RichText.resolve.",
                "Use textRuns() only to read styles; rebuild text with mapTextRuns() or withFont() so inline spans are kept (runs() is deprecated).",
                "Empty results are the shared RichText.EMPTY instance; plain(\"\") does not allocate."
        },
        related = {"TextRun", "RichTextSpan", "InlineContentSpan", "InlineMarkers", "FontFace", "TextBrush", "TextEngine"},
        examples = {
                @AiDoc.Example(
                        title = "Building Rich Text with Text and Inline Icons",
                        order = 10,
                        code = """
                                RichText text = RichText.builder()
                                        .append("Status: ")
                                        .icon("icon:ok", checkTexture)          // 1em, follows the text size
                                        .append(" All systems online", s -> s.color(MutableColor.rgba255(80, 220, 100, 255)))
                                        .build();
                                """),
                @AiDoc.Example(
                        title = "Applying Linear Gradient Across Text",
                        order = 20,
                        code = """
                                RichText gradientTitle = RichText.gradient(
                                        "UniGUI Engine",
                                        Fonts.defaultFace(),
                                        18.0f,
                                        MutableColor.rgba255(255, 100, 100, 255),
                                        MutableColor.rgba255(100, 150, 255, 255),
                                        45.0f
                                );
                                """),
                @AiDoc.Example(
                        title = "Global String Markers",
                        order = 30,
                        code = """
                                InlineMarkers.register("key", (argument, size) -> {
                                    TextureHandle icon = KeyIcons.forBinding(argument);
                                    return icon == null ? null
                                            : InlineContentSpan.sized("key:" + argument, size, InlineContentRenderer.texture(icon))
                                                    .withAlignment(InlineContentAlignment.BASELINE)
                                                    .withMargins(1, 1);
                                });
                                new Label("Hold {key:key.attack} to pan the map.");
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
        title = "<key:rich.text.section.markers.title>",
        markdown = "<key:rich.text.section.markers.body>",
        order = 35)
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
@WikiExample(
        title = "<key:rich.text.example.scoped.title>",
        code = "<key:rich.text.example.scoped.code>",
        order = 30)
public final class RichText {
    /**
     * Пустой rich-text. {@link #plain(String)} с пустой строкой и любые фабрики, после нормализации которых
     * не осталось span'ов, возвращают этот экземпляр, поэтому {@code RichText.plain("")} ничего не аллоцирует.
     */
    public static final RichText EMPTY = new RichText(Normalized.of(null));

    private final List<RichTextSpan> spans;
    private final List<TextRun> runs;
    private final String plainText;
    private final boolean hasInlineContent;

    /**
     * Создаёт rich-text из старого списка текстовых run'ов.
     *
     * @param runs текстовые run'ы; {@code null} и пустые run'ы игнорируются
     * @deprecated используйте {@link #ofSpans(List)} (принимает и {@code List<TextRun>}) или {@link #builder()}
     */
    @Deprecated
    public RichText(List<TextRun> runs) {
        this(Normalized.of(runs));
    }

    private RichText(Normalized normalized) {
        this.spans = normalized.spans();
        this.runs = normalized.runs();
        this.plainText = normalized.plainText();
        this.hasInlineContent = normalized.hasInlineContent();
    }

    /**
     * Единая точка создания: нормализует span'ы и возвращает {@link #EMPTY}, если ничего не осталось.
     */
    private static RichText create(List<? extends RichTextSpan> spans) {
        Normalized normalized = Normalized.of(spans);
        return normalized.spans().isEmpty() ? EMPTY : new RichText(normalized);
    }

    /**
     * Результат нормализации списка span'ов: без {@code null} и пустых span'ов.
     */
    private record Normalized(List<RichTextSpan> spans, List<TextRun> runs, String plainText,
                              boolean hasInlineContent) {
        static Normalized of(List<? extends RichTextSpan> source) {
            ObjectArrayList<RichTextSpan> spans = new ObjectArrayList<>();
            ObjectArrayList<TextRun> runs = new ObjectArrayList<>();
            StringBuilder text = new StringBuilder();
            boolean inline = false;
            if (source != null) {
                for (RichTextSpan span : source) {
                    if (span == null || span.isEmpty()) continue;
                    spans.add(span);
                    text.append(span.fallbackText());
                    if (span instanceof TextRun run) {
                        runs.add(run);
                    } else {
                        inline = true;
                    }
                }
            }
            return new Normalized(Collections.unmodifiableList(spans), Collections.unmodifiableList(runs),
                    text.toString(), inline);
        }
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
        return create(List.of(new TextRun(text, null, TextRun.DEFAULT_PIXEL_SIZE)));
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
     * @param text      исходный текст
     * @param font      font face или {@code null}, чтобы backend выбрал default
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
        return create(List.of(new TextRun(text, font, pixelSize)));
    }

    /**
     * Создаёт rich-text из одного текстового run'а с цветом.
     *
     * @param text      исходный текст
     * @param font      font face или {@code null}, чтобы backend выбрал default
     * @param pixelSize размер текста в UI-пикселях
     * @param color     цвет run'а или {@code null}, чтобы renderer использовал paint виджета
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
        return create(List.of(new TextRun(text, font, pixelSize, color)));
    }

    /**
     * Создаёт rich-text из одного текстового run'а с brush-заливкой.
     *
     * @param text      исходный текст
     * @param font      font face или {@code null}, чтобы backend выбрал default
     * @param pixelSize размер текста в UI-пикселях
     * @param brush     brush-заливка текста или {@code null} для обычного цвета
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
        return create(List.of(new TextRun(text, font, pixelSize, null, brush)));
    }

    /**
     * Создаёт rich-text из одного run'а с линейным градиентом.
     *
     * @param text         исходный текст
     * @param font         font face или {@code null}, чтобы backend выбрал default
     * @param pixelSize    размер текста в UI-пикселях
     * @param startColor   цвет начала градиента
     * @param endColor     цвет конца градиента
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
        return create(spans);
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
     * @deprecated имя скрывает, что inline-span'ы теряются. Используйте {@link #textRuns()}, если нужны только
     * run'ы, {@link #spans()} для полного потока или {@link #mapTextRuns(UnaryOperator)} для пересборки
     * с сохранением inline-контента.
     */
    @Deprecated
    @WikiMethod(
            title = "<key:rich.text.method.runs.title>",
            summary = "<key:rich.text.method.runs.summary>",
            order = 90)
    @WikiReturn("<key:rich.text.method.runs.return>")
    public List<TextRun> runs() {
        return runs;
    }

    /**
     * Только текстовые run'ы, без inline-span'ов.
     *
     * <p>Подходит для чтения стиля или для рендереров, которые получают заведомо plain-текст
     * ({@link #hasInlineContent()} == {@code false}). Чтобы пересобрать текст, не теряя иконки,
     * используйте {@link #mapTextRuns(UnaryOperator)}.</p>
     *
     * @return immutable список {@link TextRun}
     */
    @WikiMethod(
            title = "<key:rich.text.method.text_runs.title>",
            summary = "<key:rich.text.method.text_runs.summary>",
            order = 95)
    @WikiReturn("<key:rich.text.method.text_runs.return>")
    public List<TextRun> textRuns() {
        return runs;
    }

    /**
     * Возвращает копию, где каждый текстовый run заменён результатом {@code mapper}; inline-span'ы
     * сохраняются на своих местах.
     *
     * @param mapper преобразование run'а; {@code null}-результат удаляет run
     * @return новый rich-text или этот объект, если ни один run не изменился
     */
    @WikiMethod(
            title = "<key:rich.text.method.map_text_runs.title>",
            summary = "<key:rich.text.method.map_text_runs.summary>",
            order = 97)
    @WikiReturn("<key:rich.text.method.map_text_runs.return>")
    public RichText mapTextRuns(
            @WikiParameter(name = "mapper", description = "<key:rich.text.method.map_text_runs.mapper>")
            UnaryOperator<TextRun> mapper) {
        if (mapper == null || runs.isEmpty()) return this;
        ObjectArrayList<RichTextSpan> updated = new ObjectArrayList<>(spans.size());
        boolean changed = false;
        for (RichTextSpan span : spans) {
            if (span instanceof TextRun run) {
                TextRun mapped = mapper.apply(run);
                changed |= mapped != run;
                if (mapped != null) updated.add(mapped);
            } else {
                updated.add(span);
            }
        }
        return changed ? create(updated) : this;
    }

    /**
     * Возвращает копию, где все текстовые run'ы используют заданный шрифт; inline-span'ы сохраняются.
     *
     * @param font font face или {@code null}, чтобы backend выбрал default
     * @return новый rich-text или этот объект, если шрифт уже такой
     */
    @WikiMethod(
            title = "<key:rich.text.method.with_font.title>",
            summary = "<key:rich.text.method.with_font.summary>",
            order = 155)
    @WikiReturn("<key:rich.text.method.with_font.return>")
    public RichText withFont(
            @WikiParameter(name = "font", description = "<key:rich.text.method.with_font.font>")
            FontFace font) {
        return mapTextRuns(run -> run.withFont(font));
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
        return create(updated);
    }

    /**
     * Возвращает копию текста с переопределёнными параметрами текстовых run'ов.
     * Inline-span'ы с размером в em ({@link InlineContentSpan#scalesWithText()}) пересчитываются под новый
     * {@code pixelSize}; остальное inline-содержимое сохраняется без изменений.
     *
     * @param pixelSize новый размер текста или {@code null}, чтобы сохранить размер run'а
     * @param tracking  новый tracking или {@code null}, чтобы сохранить tracking run'а
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
            } else if (pixelSize != null && span instanceof InlineContentSpan inline) {
                updated.add(inline.withTextSize(pixelSize));
            } else {
                updated.add(span);
            }
        }
        return create(updated);
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
        return create(combined);
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
     * @param endExclusive   конец среза исключительно
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
        TextRun lastStyle = runs.isEmpty() ? null : runs.get(0);
        for (RichTextSpan span : spans) {
            String fallback = span.fallbackText();
            int spanEnd = spanStart + fallback.length();
            int overlapStart = Math.max(start, spanStart);
            int overlapEnd = Math.min(end, spanEnd);
            if (span instanceof TextRun run) lastStyle = run;
            if (overlapStart < overlapEnd) {
                if (span instanceof TextRun run) {
                    sliced.add(sliceRun(run, overlapStart - spanStart, overlapEnd - spanStart));
                } else if (overlapStart == spanStart && overlapEnd == spanEnd) {
                    sliced.add(span);
                } else {
                    // Частично захваченный inline-span становится текстом в стиле соседнего run'а.
                    String fallbackSlice = fallback.substring(overlapStart - spanStart, overlapEnd - spanStart);
                    sliced.add(lastStyle == null
                            ? new TextRun(fallbackSlice, null, TextRun.DEFAULT_PIXEL_SIZE)
                            : lastStyle.withText(fallbackSlice));
                }
            }
            spanStart = spanEnd;
            if (spanStart >= end) break;
        }
        return create(sliced);
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

    /**
     * Fluent builder для {@link RichText}.
     *
     * <p>Builder хранит текущие параметры text-run'а: font, size, color, brush, tracking и transform.
     * Они применяются к последующим {@link #append(String)}. Inline-span'ы добавляются как отдельные
     * атомы; от параметров builder'а они берут только размер, если заданы в em
     * ({@link InlineContentSpan#scalesWithText()}).</p>
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
                    "Use append(text, style) or styled(style, body) for a style that should apply to one fragment only; the previous style is restored afterwards.",
                    "Consecutive append() calls with an identical style are merged into one TextRun.",
                    "Inline spans take only the builder size, and only when they are em-sized (icon(id, texture) without a size, or markers without @size).",
                    "appendResolved(String) expands {type:argument} markers using the builder style for the text parts."
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
         * @param startColor   цвет начала градиента
         * @param endColor     цвет конца градиента
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
            if (run.isEmpty()) return this;
            // Подряд идущие append'ы с одинаковым стилем склеиваются в один run: меньше draw-команд и span'ов.
            int last = spans.size() - 1;
            if (last >= 0 && spans.get(last) instanceof TextRun previous && previous.hasSameStyle(run)) {
                spans.set(last, previous.withText(previous.text() + run.text()));
            } else {
                spans.add(run);
            }
            return this;
        }

        /**
         * Добавляет текст со стилем, действующим только для него.
         *
         * <pre>{@code
         * RichText.builder()
         *         .append("HP: ")
         *         .append("20", s -> s.color(RED).size(12))
         *         .append(" / 20")   // снова стиль builder'а
         *         .build();
         * }</pre>
         *
         * @param text  текст run'а
         * @param style настройка стиля (font, size, color, brush, tracking, transform) поверх текущего
         * @return этот builder
         */
        public Builder append(String text, Consumer<Builder> style) {
            return styled(style, builder -> builder.append(text));
        }

        /**
         * Выполняет {@code body} со стилем, изменённым {@code style}, и затем восстанавливает прежний стиль.
         *
         * <pre>{@code
         * builder.styled(s -> s.color(GOLD), b -> b
         *         .append("Reward: ")
         *         .icon("coin", coinTexture)
         *         .append(" 50"));
         * }</pre>
         *
         * @param style настройка стиля поверх текущего; {@code null} — без изменений
         * @param body  содержимое, добавляемое в этом стиле
         * @return этот builder
         */
        public Builder styled(Consumer<Builder> style, Consumer<Builder> body) {
            FontFace savedFont = font;
            float savedSize = pixelSize;
            ColorView savedColor = color;
            TextBrush savedBrush = brush;
            float savedTracking = tracking;
            TextTransform savedTransform = transform;
            try {
                if (style != null) style.accept(this);
                if (body != null) body.accept(this);
            } finally {
                font = savedFont;
                pixelSize = savedSize;
                color = savedColor;
                brush = savedBrush;
                tracking = savedTracking;
                transform = savedTransform;
            }
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
         * Добавляет строку, разбирая inline-маркеры ({@code {icon:...}}, {@code {key:...}} и другие типы из
         * {@link InlineMarkers}) через {@link RichText#resolve(String)}.
         *
         * <p>Текстовые части получают текущие параметры builder'а, em-иконки — текущий размер.</p>
         *
         * @param text строка с маркерами
         * @return этот builder
         */
        public Builder appendResolved(String text) {
            for (RichTextSpan span : RichText.resolve(text).spans()) {
                if (span instanceof TextRun run) {
                    append(run.text());
                } else if (span instanceof InlineContentSpan inline) {
                    appendInline(inline);
                }
            }
            return this;
        }

        /**
         * Добавляет готовый inline-span.
         *
         * <p>Span с размером в em пересчитывается под текущий размер builder'а.</p>
         *
         * @param span inline-span или {@code null}
         * @return этот builder
         */
        public Builder appendInline(InlineContentSpan span) {
            if (span != null && !span.isEmpty()) spans.add(span.withTextSize(pixelSize));
            return this;
        }

        /**
         * Добавляет готовый inline-span; синоним {@link #appendInline(InlineContentSpan)}.
         *
         * <pre>{@code
         * builder.inline(InlineContentSpan.em("key:jump", 1.0f, 1.0f, 10.0f, InlineContentRenderer.texture(keyTexture))
         *         .withAlignment(InlineContentAlignment.BASELINE)
         *         .withMargins(1.0f, 1.0f));
         * }</pre>
         *
         * @param span inline-span или {@code null}
         * @return этот builder
         */
        public Builder inline(InlineContentSpan span) {
            return appendInline(span);
        }

        /**
         * Добавляет кастомный inline-content renderer.
         *
         * @param id           стабильный id span'а для debug/diagnostics
         * @param fallbackText plain fallback для search/copy/debug
         * @param width        ширина inline-контента в UI-пикселях
         * @param height       высота inline-контента в UI-пикселях
         * @param alignment    вертикальное выравнивание внутри строки
         * @param renderer     renderer, который получит финальные bounds после layout
         * @return этот builder
         */
        public Builder inline(String id, String fallbackText, float width, float height,
                              InlineContentAlignment alignment, InlineContentRenderer renderer) {
            return appendInline(new InlineContentSpan(id, fallbackText, width, height, alignment, renderer));
        }

        /**
         * Добавляет кастомный inline-content renderer с center alignment.
         *
         * @param id           стабильный id span'а
         * @param fallbackText plain fallback
         * @param width        ширина в UI-пикселях
         * @param height       высота в UI-пикселях
         * @param renderer     renderer inline-контента
         * @return этот builder
         */
        public Builder inline(String id, String fallbackText, float width, float height,
                              InlineContentRenderer renderer) {
            return inline(id, fallbackText, width, height, InlineContentAlignment.CENTER, renderer);
        }

        /**
         * Добавляет кастомный inline-content renderer со стандартным object replacement fallback.
         *
         * @param id       стабильный id span'а
         * @param width    ширина в UI-пикселях
         * @param height   высота в UI-пикселях
         * @param renderer renderer inline-контента
         * @return этот builder
         */
        public Builder inline(String id, float width, float height, InlineContentRenderer renderer) {
            return inline(id, InlineContentSpan.DEFAULT_FALLBACK_TEXT, width, height, renderer);
        }

        /**
         * Добавляет квадратную texture-иконку размером с текст (1em).
         *
         * <p>Иконка берёт текущий размер builder'а и пересчитывается в
         * {@link RichText#withTextStyle(Float, Float, TextTransform)}, то есть растёт вместе со шрифтом.
         * Цвета texture сохраняются; для окраски цветом текста используйте
         * {@link #icon(String, TextureHandle, float, float, boolean)} или {@link #inline(InlineContentSpan)}.</p>
         *
         * @param id      стабильный id содержимого
         * @param texture texture-handle для рендера
         * @return этот builder
         */
        public Builder icon(String id, TextureHandle texture) {
            return appendInline(InlineContentSpan.em(id, 1.0f, 1.0f, pixelSize, InlineContentRenderer.texture(texture)));
        }

        /**
         * Добавляет квадратную texture-иконку с абсолютным размером.
         *
         * @param id      стабильный id span'а
         * @param texture texture-handle для рендера
         * @param size    ширина и высота в UI-пикселях
         * @return этот builder
         */
        public Builder icon(String id, TextureHandle texture, float size) {
            return icon(id, texture, size, size);
        }

        /**
         * Добавляет texture-иконку произвольного абсолютного размера без окраски цветом текста.
         *
         * <p>Если texture отсутствует, span всё равно участвует в layout, но ничего не рисует.</p>
         *
         * @param id      стабильный id span'а
         * @param texture texture-handle для рендера
         * @param width   ширина в UI-пикселях
         * @param height  высота в UI-пикселях
         * @return этот builder
         */
        public Builder icon(String id, TextureHandle texture, float width, float height) {
            return icon(id, texture, width, height, false);
        }

        /**
         * Добавляет texture-иконку произвольного абсолютного размера.
         *
         * @param id      стабильный id span'а
         * @param texture texture-handle для рендера
         * @param width   ширина в UI-пикселях
         * @param height  высота в UI-пикселях
         * @param tint    {@code true} — умножать texture на цвет текста (одноцветные glyph-иконки),
         *                {@code false} — сохранить цвета texture
         * @return этот builder
         */
        public Builder icon(String id, TextureHandle texture, float width, float height, boolean tint) {
            return appendInline(InlineContentSpan.of(id, width, height, InlineContentRenderer.texture(texture, tint)));
        }

        /**
         * Собирает immutable rich-text.
         *
         * @return новый {@link RichText}
         */
        public RichText build() {
            return create(spans);
        }
    }
}
