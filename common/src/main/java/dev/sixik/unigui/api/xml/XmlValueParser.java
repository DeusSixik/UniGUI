package dev.sixik.unigui.api.xml;

/**
 * Преобразует строковое значение XML-атрибута в типизированное значение свойства.
 *
 * <p>Parser вызывается XML-loader'ом перед setter-ом атрибута. Если значение
 * невозможно разобрать, parser должен выбросить {@link IllegalArgumentException}
 * или другое runtime-исключение: loader обернёт его в диагностичную ошибку с
 * именем атрибута и координатами XML.</p>
 *
 * @param <T> тип значения, которое получит {@link XmlPropertySetter}
 */
@FunctionalInterface
public interface XmlValueParser<T> {
    /**
     * Разбирает строку из XML.
     *
     * @param value исходное строковое значение атрибута
     * @return типизированное значение
     */
    T parse(String value);
}
