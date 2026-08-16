package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;

/**
 * Набор стандартных parser-ов для XML-атрибутов виджетов.
 *
 * <p>Эти parser-ы используются как ручной registry API, так и reflection-регистрация
 * {@link XmlAttribute}. Они намеренно принимают только компактный XML-синтаксис:
 * числа через стандартные Java parser-ы, boolean через common aliases, цвета в hex,
 * размеры как {@code auto}, {@code px}, проценты или голые числа.</p>
 *
 * <p>Парсинг texture id использует resolver из текущего {@link XmlWidgetOptions}.
 * Во время обычной загрузки loader сам устанавливает этот контекст.</p>
 */
public final class XmlValueParsers {
    /** Parser строк без дополнительной нормализации, кроме замены {@code null} на пустую строку. */
    public static final XmlValueParser<String> STRING = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.STRING.parse(value);
    /** Parser boolean-значений: {@code true/false}, {@code 1/0}, {@code yes/no}, {@code on/off}. */
    public static final XmlValueParser<Boolean> BOOLEAN = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.BOOLEAN.parse(value);
    /** Parser целых чисел. */
    public static final XmlValueParser<Integer> INT = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.INT.parse(value);
    /** Parser float-чисел. */
    public static final XmlValueParser<Float> FLOAT = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.FLOAT.parse(value);
    /** Parser float-чисел и специального значения {@code auto} для старых layout-полей. */
    public static final XmlValueParser<Float> FLOAT_OR_AUTO = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.FLOAT_OR_AUTO.parse(value);
    /** Parser double-чисел. */
    public static final XmlValueParser<Double> DOUBLE = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.DOUBLE.parse(value);
    /** Parser цвета в формате {@code RRGGBB}, {@code RRGGBBAA} или с ведущим {@code #}. */
    public static final XmlValueParser<MutableColor> COLOR = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.COLOR.parse(value);
    /** Parser rect-значения вида {@code x y width height}. */
    public static final XmlValueParser<MutableRect> RECT = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.RECT.parse(value);
    /** Parser texture id через текущий {@link XmlTextureResolver}. */
    public static final XmlValueParser<TextureHandle> TEXTURE = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.TEXTURE.parse(value);
    /** Parser layout-size значений: {@code auto}, {@code 10px}, {@code 50%} или {@code 10}. */
    public static final XmlValueParser<SizeValue> SIZE = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.SIZE.parse(value);
    /** Parser edge insets: одно значение, вертикаль/горизонталь или четыре стороны. */
    public static final XmlValueParser<EdgeInsets> INSETS = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.INSETS.parse(value);

    private XmlValueParsers() {
    }

    /**
     * Создаёт parser enum-значений.
     *
     * <p>Parser нечувствителен к регистру и принимает {@code kebab-case}, пробелы
     * и underscore как один стиль имени. Например, {@code "clamp-to-edge"}
     * совпадает с {@code CLAMP_TO_EDGE}.</p>
     *
     * @param type enum-класс
     * @param <E> тип enum
     * @return parser для XML-строк
     */
    public static <E extends Enum<E>> XmlValueParser<E> enumValue(Class<E> type) {
        dev.sixik.unigui.impl.xml.XmlValueParser<E> parser = dev.sixik.unigui.impl.xml.XmlValueParsers.enumValue(type);
        return parser::parse;
    }

    /**
     * Разрешает texture id через resolver текущего XML-load контекста.
     *
     * <p>Метод нужен setter-ам, которые меняют ширину/высоту/options уже созданного
     * texture handle. Если вызван вне XML-загрузки, используется дефолтный simple resolver.</p>
     *
     * @param id id текстуры из XML
     * @param width желаемая ширина source texture
     * @param height желаемая высота source texture
     * @param options параметры sampling/wrap/mipmaps
     * @return texture handle, созданный текущим resolver-ом
     */
    public static TextureHandle resolveTexture(String id, int width, int height, TextureOptions options) {
        return dev.sixik.unigui.impl.xml.XmlValueParsers.resolveTexture(id, width, height, options);
    }
}
