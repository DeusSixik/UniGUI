package dev.sixik.unigui.api.docs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

/** Adds authored Markdown content to a generated wiki page. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(WikiSections.class)
public @interface WikiSection {
    String title();

    String markdown();

    int order() default 0;
}
