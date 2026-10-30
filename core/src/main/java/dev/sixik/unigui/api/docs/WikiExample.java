package dev.sixik.unigui.api.docs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

/** Adds a source example to a generated wiki page. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(WikiExamples.class)
public @interface WikiExample {
    String title();

    String code();

    String language() default "java";

    int order() default 0;
}
