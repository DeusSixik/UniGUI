package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.TextureHandle;

/** Общие парсеры значений XML-атрибутов виджетов. */
public final class XmlValueParsers {
    public static final XmlValueParser<String> STRING = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.STRING.parse(value);
    public static final XmlValueParser<Boolean> BOOLEAN = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.BOOLEAN.parse(value);
    public static final XmlValueParser<Integer> INT = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.INT.parse(value);
    public static final XmlValueParser<Float> FLOAT = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.FLOAT.parse(value);
    public static final XmlValueParser<Double> DOUBLE = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.DOUBLE.parse(value);
    public static final XmlValueParser<MutableColor> COLOR = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.COLOR.parse(value);
    public static final XmlValueParser<TextureHandle> TEXTURE = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.TEXTURE.parse(value);
    public static final XmlValueParser<SizeValue> SIZE = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.SIZE.parse(value);
    public static final XmlValueParser<EdgeInsets> INSETS = value -> dev.sixik.unigui.impl.xml.XmlValueParsers.INSETS.parse(value);

    private XmlValueParsers() {
    }

    public static <E extends Enum<E>> XmlValueParser<E> enumValue(Class<E> type) {
        dev.sixik.unigui.impl.xml.XmlValueParser<E> parser = dev.sixik.unigui.impl.xml.XmlValueParsers.enumValue(type);
        return parser::parse;
    }
}
