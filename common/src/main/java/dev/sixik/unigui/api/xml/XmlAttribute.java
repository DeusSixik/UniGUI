package dev.sixik.unigui.api.xml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Аннотация для fluent-setter-ов, которые можно регистрировать как XML-атрибуты. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface XmlAttribute {
    String value();

    String category() default "";

    String defaultValue() default "";

    String description() default "";
}
