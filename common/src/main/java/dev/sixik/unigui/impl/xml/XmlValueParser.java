package dev.sixik.unigui.impl.xml;

/** Преобразует строковое значение XML-атрибута в типизированное значение свойства. */
@FunctionalInterface
public interface XmlValueParser<T> {
    T parse(String value);
}
