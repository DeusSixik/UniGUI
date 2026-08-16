package dev.sixik.unigui.api.xml;

/** Преобразует строковое значение XML-атрибута в типизированное значение свойства. */
@FunctionalInterface
public interface XmlValueParser<T> {
    T parse(String value);
}
