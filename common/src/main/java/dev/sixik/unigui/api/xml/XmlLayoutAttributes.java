package dev.sixik.unigui.api.xml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Маркер базовых виджетов, которые добавляют общий набор layout XML-атрибутов.
 *
 * <p>При annotated-регистрации {@link XmlWidgetAnnotations} добавляет к типу стандартные layout
 * атрибуты вроде {@code width}, {@code height}, {@code margin}, {@code padding}, {@code flexGrow}
 * и absolute-position insets. Аннотация обычно ставится на базовые widget classes, а не на каждый
 * конкретный setter.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface XmlLayoutAttributes {
}
