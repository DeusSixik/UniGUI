package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.docs.AiDoc;

import java.util.Objects;

/**
 * Атомарный non-text span внутри {@link RichText}.
 *
 * <p>Span занимает фиксированную ширину и высоту в строке, имеет plain fallback и рисуется
 * через {@link InlineContentRenderer}. Он не участвует в дереве widgets, не получает события
 * ввода и не хранит layout-состояние. Для text engine это один неделимый layout-атом, похожий
 * на один code point.</p>
 *
 * <p>{@code fallbackText} нужен для clipboard, search, debug text и сериализации. По умолчанию
 * используется Unicode object replacement character.</p>
 *
 * <h2>Размер в em</h2>
 * <p>Span может быть привязан к размеру текста: {@link #em(String, float, float, float, InlineContentRenderer)}
 * хранит коэффициенты {@link #emWidth()}/{@link #emHeight()}, а {@link #width()}/{@link #height()} — пиксели
 * для текущего размера. {@link RichText#withTextStyle(Float, Float, TextTransform)} и
 * {@link RichText.Builder} пересчитывают такие span'ы под новый {@code pixelSize}, поэтому иконка растёт вместе
 * со шрифтом. Span с абсолютным размером не меняется.</p>
 *
 * <h2>Равенство</h2>
 * <p>Renderer обычно лямбда, поэтому два одинаковых span'а из разных вызовов {@link RichText#resolve(String)}
 * имели бы разные renderer'ы. Если {@link #id()} не пустой, он считается идентификатором содержимого:
 * span'ы с одинаковым id, размерами, отступами и выравниванием равны независимо от renderer'а. Span без id
 * сравнивает renderer по ссылке.</p>
 *
 * <h2>Отступы</h2>
 * <p>{@link #marginLeft()} и {@link #marginRight()} добавляют пустое место по бокам: ширина в строке равна
 * {@link #advance()}, а renderer получает bounds без отступов.</p>
 */
@AiDoc(
        summary = "Atomic non-text layout item (icon, texture, badge) embedded in a RichText sequence.",
        purpose = "Participates in line breaking and text alignment, providing fallback plain text.",
        useWhen = {"Embedding inline graphics into text without creating separate widget nodes."},
        criticalRules = {
                "A non-empty id identifies the content: spans with equal id, size, margins and alignment are equal regardless of renderer.",
                "em-sized spans (emWidth/emHeight > 0) are rescaled by RichText.withTextStyle and RichText.Builder to follow the text size.",
                "The line advance is marginLeft + width + marginRight; the renderer receives bounds without margins."
        },
        related = {"RichText", "InlineContentRenderer", "InlineContentAlignment", "InlineMarkers"}
)
public final class InlineContentSpan implements RichTextSpan {
    /**
     * Стандартный fallback для atomic inline object.
     */
    public static final String DEFAULT_FALLBACK_TEXT = "￼";

    private final String id;
    private final String fallbackText;
    private final float width;
    private final float height;
    private final InlineContentAlignment alignment;
    private final InlineContentRenderer renderer;
    private final float marginLeft;
    private final float marginRight;
    private final float emWidth;
    private final float emHeight;

    /**
     * Создаёт span с абсолютным размером и без отступов.
     *
     * <p>Размеры приводятся к неотрицательным значениям, пустой fallback заменяется на
     * {@link #DEFAULT_FALLBACK_TEXT}, а отсутствующий renderer превращается в безопасный NOOP.</p>
     *
     * @param id           стабильный id содержимого (см. раздел «Равенство»)
     * @param fallbackText plain-text fallback span'а
     * @param width        ширина в UI-пикселях
     * @param height       высота в UI-пикселях
     * @param alignment    вертикальное выравнивание внутри строки
     * @param renderer     renderer, который получит финальные bounds после layout
     */
    public InlineContentSpan(String id, String fallbackText, float width, float height,
                             InlineContentAlignment alignment, InlineContentRenderer renderer) {
        this(id, fallbackText, width, height, alignment, renderer, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    /**
     * Создаёт square inline-span с абсолютным размером.
     *
     * @param id           стабильный id span'а
     * @param fallbackText plain fallback
     * @param size         ширина и высота в UI-пикселях
     * @param renderer     renderer inline-контента
     */
    public InlineContentSpan(String id, String fallbackText, float size, InlineContentRenderer renderer) {
        this(id, fallbackText, size, size, InlineContentAlignment.CENTER, renderer);
    }

    private InlineContentSpan(String id, String fallbackText, float width, float height,
                              InlineContentAlignment alignment, InlineContentRenderer renderer,
                              float marginLeft, float marginRight, float emWidth, float emHeight) {
        this.id = id == null ? "" : id;
        this.fallbackText = normalizeFallback(fallbackText);
        this.width = sanitize(width);
        this.height = sanitize(height);
        this.alignment = alignment == null ? InlineContentAlignment.CENTER : alignment;
        this.renderer = renderer == null ? InlineContentRenderer.NOOP : renderer;
        this.marginLeft = sanitize(marginLeft);
        this.marginRight = sanitize(marginRight);
        this.emWidth = sanitize(emWidth);
        this.emHeight = sanitize(emHeight);
    }

    /**
     * Создаёт span с абсолютным размером, center-выравниванием и стандартным fallback.
     *
     * @param id       стабильный id содержимого
     * @param width    ширина в UI-пикселях
     * @param height   высота в UI-пикселях
     * @param renderer renderer inline-контента
     * @return новый span
     */
    public static InlineContentSpan of(String id, float width, float height, InlineContentRenderer renderer) {
        return new InlineContentSpan(id, DEFAULT_FALLBACK_TEXT, width, height, InlineContentAlignment.CENTER, renderer);
    }

    /**
     * Создаёт span, размер которого задан в долях размера текста.
     *
     * @param id        стабильный id содержимого
     * @param emWidth   ширина в em (1.0 = {@code pixelSize})
     * @param emHeight  высота в em
     * @param pixelSize текущий размер текста, от которого считаются пиксели
     * @param renderer  renderer inline-контента
     * @return новый span, который будет масштабироваться вместе с текстом
     */
    public static InlineContentSpan em(String id, float emWidth, float emHeight, float pixelSize,
                                       InlineContentRenderer renderer) {
        float size = textSize(pixelSize);
        return new InlineContentSpan(id, DEFAULT_FALLBACK_TEXT, emWidth * size, emHeight * size,
                InlineContentAlignment.CENTER, renderer, 0.0f, 0.0f, emWidth, emHeight);
    }

    /**
     * Создаёт span из размера, разобранного из маркера ({@code @16}, {@code @16x8}, {@code @1.2em}).
     *
     * @param id       стабильный id содержимого
     * @param size     размер; {@code null} означает {@link InlineSize#DEFAULT} (1em)
     * @param renderer renderer inline-контента
     * @return новый span
     */
    public static InlineContentSpan sized(String id, InlineSize size, InlineContentRenderer renderer) {
        InlineSize normalized = size == null ? InlineSize.DEFAULT : size;
        return normalized.em()
                ? em(id, normalized.width(), normalized.height(), TextRun.DEFAULT_PIXEL_SIZE, renderer)
                : of(id, normalized.width(), normalized.height(), renderer);
    }

    /**
     * @return стабильный id содержимого
     */
    public String id() {
        return id;
    }

    @Override
    public String fallbackText() {
        return fallbackText;
    }

    /**
     * @return ширина контента в UI-пикселях без отступов
     */
    public float width() {
        return width;
    }

    /**
     * @return высота контента в UI-пикселях
     */
    public float height() {
        return height;
    }

    /**
     * @return вертикальное выравнивание внутри строки
     */
    public InlineContentAlignment alignment() {
        return alignment;
    }

    /**
     * @return renderer контента
     */
    public InlineContentRenderer renderer() {
        return renderer;
    }

    /**
     * @return пустое место слева в UI-пикселях
     */
    public float marginLeft() {
        return marginLeft;
    }

    /**
     * @return пустое место справа в UI-пикселях
     */
    public float marginRight() {
        return marginRight;
    }

    /**
     * @return ширина в em или {@code 0}, если ширина абсолютная
     */
    public float emWidth() {
        return emWidth;
    }

    /**
     * @return высота в em или {@code 0}, если высота абсолютная
     */
    public float emHeight() {
        return emHeight;
    }

    /**
     * @return {@code true}, если размер span'а следует за размером текста
     */
    public boolean scalesWithText() {
        return emWidth > 0.0f || emHeight > 0.0f;
    }

    /**
     * @return сколько span занимает в строке: {@code marginLeft + width + marginRight}
     */
    public float advance() {
        return marginLeft + width + marginRight;
    }

    /**
     * Пересчитывает em-размер под заданный размер текста.
     *
     * @param pixelSize размер текста в UI-пикселях
     * @return этот же span, если он абсолютный или размер не меняется, иначе новый span
     */
    public InlineContentSpan withTextSize(float pixelSize) {
        if (!scalesWithText()) return this;
        float size = textSize(pixelSize);
        float nextWidth = emWidth > 0.0f ? emWidth * size : width;
        float nextHeight = emHeight > 0.0f ? emHeight * size : height;
        if (Float.compare(nextWidth, width) == 0 && Float.compare(nextHeight, height) == 0) return this;
        return new InlineContentSpan(id, fallbackText, nextWidth, nextHeight, alignment, renderer,
                marginLeft, marginRight, emWidth, emHeight);
    }

    /**
     * @param left  пустое место слева в UI-пикселях
     * @param right пустое место справа в UI-пикселях
     * @return копия с новыми отступами
     */
    public InlineContentSpan withMargins(float left, float right) {
        return new InlineContentSpan(id, fallbackText, width, height, alignment, renderer, left, right, emWidth, emHeight);
    }

    /**
     * @param alignment новое вертикальное выравнивание
     * @return копия с новым выравниванием
     */
    public InlineContentSpan withAlignment(InlineContentAlignment alignment) {
        return new InlineContentSpan(id, fallbackText, width, height, alignment, renderer,
                marginLeft, marginRight, emWidth, emHeight);
    }

    /**
     * @param fallbackText новый plain fallback
     * @return копия с новым fallback
     */
    public InlineContentSpan withFallbackText(String fallbackText) {
        return new InlineContentSpan(id, fallbackText, width, height, alignment, renderer,
                marginLeft, marginRight, emWidth, emHeight);
    }

    /**
     * @return {@code true}, если span не имеет полезной площади для layout
     */
    @Override
    public boolean isEmpty() {
        return advance() <= 0.0f && height <= 0.0f;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InlineContentSpan span)) return false;
        return id.equals(span.id)
                && fallbackText.equals(span.fallbackText)
                && Float.compare(width, span.width) == 0
                && Float.compare(height, span.height) == 0
                && Float.compare(marginLeft, span.marginLeft) == 0
                && Float.compare(marginRight, span.marginRight) == 0
                && Float.compare(emWidth, span.emWidth) == 0
                && Float.compare(emHeight, span.emHeight) == 0
                && alignment == span.alignment
                && (!id.isEmpty() || renderer == span.renderer);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, fallbackText, width, height, marginLeft, marginRight, emWidth, emHeight, alignment);
        return id.isEmpty() ? 31 * result + System.identityHashCode(renderer) : result;
    }

    @Override
    public String toString() {
        return "InlineContentSpan[id=" + id + ", " + width + "x" + height
                + (scalesWithText() ? " (" + emWidth + "x" + emHeight + "em)" : "")
                + ", " + alignment + "]";
    }

    private static String normalizeFallback(String fallbackText) {
        return fallbackText == null || fallbackText.isEmpty() ? DEFAULT_FALLBACK_TEXT : fallbackText;
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }

    private static float textSize(float pixelSize) {
        return Float.isFinite(pixelSize) && pixelSize > 0.0f ? pixelSize : TextRun.DEFAULT_PIXEL_SIZE;
    }
}
