package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.Map;

/**
 * Неизменяемое представление набора style-свойств виджета.
 *
 * <p>{@code Style} хранит значения по типизированным ключам {@link StyleKey} и состояниям
 * {@link WidgetState}. Один и тот же ключ может иметь обычное значение для {@link WidgetState#NORMAL}
 * и override для состояний вроде {@link WidgetState#HOVERED} или {@link WidgetState#PRESSED}.</p>
 *
 * <p>Виджет обычно не создаёт {@code Style} напрямую. Он получает его из {@link Theme},
 * {@link StylePack} или локального style-scope, а затем читает нужные значения через
 * {@link #get(StyleKey, WidgetState, Object)}.</p>
 *
 * <pre>{@code
 * ColorView color = style.get(StyleKeys.TEXT_COLOR, WidgetState.HOVERED, fallbackColor);
 * }</pre>
 *
 * @see MutableStyle
 * @see ResolvedStyle
 * @see StylePack
 */
@AiDoc(
        summary = "Read-only, typed style values with optional widget-state overrides.",
        purpose = "Represents one style layer that widgets, themes, and style packs can query without casting arbitrary maps.",
        useWhen = {
                "Read a visual property with an explicit fallback.",
                "Parse a standalone XML `<Style>` element or inline style declaration."
        },
        criticalRules = {
                "Always query with the matching `StyleKey<T>`; the key defines the Java value type.",
                "State lookup may fall back to `WidgetState.NORMAL`, then to the caller-supplied fallback.",
                "`Style.from` parses one `<Style>` only; use `StylePack.from` for definitions, selectors, bindings, and animations.",
                "Treat maps returned by `values` as read-only snapshots."
        },
        related = {"MutableStyle", "StyleKey", "StyleKeys", "WidgetState", "StylePack"},
        examples = @AiDoc.Example(
                title = "Read a state-aware value",
                code = """
                        ColorView color = style.get(
                                StyleKeys.TEXT_COLOR,
                                WidgetState.HOVERED,
                                fallbackColor);
                        """))
@WikiPage(
        title = "Style",
        slug = "style",
        category = "<key:style.category>",
        summary = "<key:style.summary>",
        order = 10)
@WikiSection(title = "<key:style.section.overview.title>",
        markdown = "<key:style.section.overview.body>", order = 10)
@WikiSection(title = "<key:style.section.cascade.title>",
        markdown = "<key:style.section.cascade.body>", order = 20)
@WikiExample(title = "<key:style.example.inline.title>",
        code = "<key:style.example.inline.code>", order = 10)
public interface Style {
    /** Пустой стиль, который всегда возвращает переданный fallback. */
    Style EMPTY = new Style() {
    };

    /**
     * Парсит одиночный XML-элемент {@code <Style>} через стандартный registry свойств.
     *
     * <p>Метод читает только style-свойства: {@code <Setter>} и {@code <State>}. Атрибуты уровня
     * {@link StyleDefinition}, {@code <Event>} и связи с renderer'ом не попадают в {@code Style};
     * для полного style pack'а используй {@link StylePack#from(String)}.</p>
     *
     * @param xml XML с корневым элементом {@code <Style>}
     * @return загруженный стиль
     */
    @AiDoc(section = "Parsing", sectionOrder = 10, order = 10,
            summary = "Parses one XML `<Style>` element with the built-in property registry.")
    @WikiMethod(title = "from(String)", summary = "<key:style.method.from.summary>",
            section = "<key:style.api.section.creation>", sectionOrder = 10, order = 10)
    @WikiReturn("<key:style.method.from.return>")
    static Style from(@WikiParameter(name = "xml", description = "<key:style.method.from.xml>") String xml) {
        return StylePackXml.parseStyle(xml);
    }

    /**
     * Парсит одиночный XML-элемент {@code <Style>} через указанный registry свойств.
     *
     * @param xml XML с корневым элементом {@code <Style>}
     * @param registry registry известных style-свойств
     * @return загруженный стиль
     */
    @AiDoc(section = "Parsing", sectionOrder = 10, order = 20,
            summary = "Parses one XML `<Style>` element with a custom property registry.")
    @WikiMethod(title = "from(String, StyleKeyRegistry)", summary = "<key:style.method.from_registry.summary>",
            section = "<key:style.api.section.creation>", sectionOrder = 10, order = 20)
    @WikiReturn("<key:style.method.from.return>")
    static Style from(
            @WikiParameter(name = "xml", description = "<key:style.method.from.xml>") String xml,
            @WikiParameter(name = "registry", description = "<key:style.method.from_registry.registry>") StyleKeyRegistry registry) {
        return StylePackXml.parseStyle(xml, registry);
    }

    /**
     * Разбирает встроенные объявления, например {@code "background: #7a1f1f; radius: 8;"}.
     * Короткая форма пропускает ошибочные объявления; для получения
     * диагностики используй {@link StyleTextParser#parseEditor(String)}.
     *
     * @param text объявления встроенного стиля
     * @return разобранный стиль
     */
    @AiDoc(section = "Parsing", sectionOrder = 10, order = 30,
            summary = "Parses semicolon-separated inline declarations and skips invalid entries.")
    @WikiMethod(title = "fromText(String)", summary = "<key:style.method.from_text.summary>",
            section = "<key:style.api.section.creation>", sectionOrder = 10, order = 30)
    @WikiReturn("<key:style.method.from_text.return>")
    static Style fromText(@WikiParameter(name = "text", description = "<key:style.method.from_text.text>") String text) {
        return StyleTextParser.parse(text);
    }

    /**
     * Возвращает версию стиля для кэшей и invalidation.
     *
     * <p>Версия должна меняться при любом изменении значений. Неизменяемые стили могут
     * возвращать {@code 0}.</p>
     *
     * @return монотонный или агрегированный номер версии
     */
    default long version() {
        return 0L;
    }

    /**
     * Возвращает значение свойства для указанного состояния.
     *
     * <p>Реализация может сама делать fallback на {@link WidgetState#NORMAL}. Если значение
     * не найдено, возвращается {@code fallback}.</p>
     *
     * @param key типизированный ключ свойства
     * @param state состояние виджета; {@code null} трактуется как {@link WidgetState#NORMAL}
     * @param fallback значение по умолчанию
     * @return найденное значение или {@code fallback}
     * @param <T> Java-тип свойства
     */
    @AiDoc(section = "Reading", sectionOrder = 20, order = 10,
            summary = "Reads a typed property for a widget state, using the supplied fallback when absent.")
    @WikiMethod(title = "get(StyleKey, WidgetState, T)", summary = "<key:style.method.get.summary>",
            section = "<key:style.api.section.reading>", sectionOrder = 20, order = 10)
    @WikiReturn("<key:style.method.get.return>")
    default <T> T get(
            @WikiParameter(name = "key", description = "<key:style.method.get.key>") StyleKey<T> key,
            @WikiParameter(name = "state", description = "<key:style.method.get.state>") WidgetState state,
            @WikiParameter(name = "fallback", description = "<key:style.method.get.fallback>") T fallback) {
        return fallback;
    }

    /**
     * Возвращает снимок всех значений, сгруппированных по состояниям.
     *
     * <p>Метод нужен редактору, сериализации и отладочным инспекторам. Возвращаемые карты
     * должны рассматриваться как read-only снимок.</p>
     *
     * @return значения стиля по состояниям
     */
    default Map<WidgetState, Map<StyleKey<?>, Object>> values() {
        return Map.of();
    }

    /**
     * Возвращает значения только для одного состояния.
     *
     * @param state состояние виджета; {@code null} трактуется как {@link WidgetState#NORMAL}
     * @return read-only карта значений или пустая карта
     */
    default Map<StyleKey<?>, Object> values(WidgetState state) {
        WidgetState normalized = state == null ? WidgetState.NORMAL : state;
        Map<StyleKey<?>, Object> values = values().get(normalized);
        return values == null ? Map.of() : values;
    }
}
