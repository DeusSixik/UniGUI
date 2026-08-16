package dev.sixik.unigui.api.xml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Маркер базовых виджетов, которые добавляют общий набор layout XML-атрибутов. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface XmlLayoutAttributes {
}
