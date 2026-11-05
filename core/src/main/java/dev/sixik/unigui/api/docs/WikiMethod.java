package dev.sixik.unigui.api.docs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Publishes a method or constructor in the API section of its type's wiki page.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface WikiMethod {
    String title() default "";

    String summary() default "";

    String markdown() default "";

    String section() default "";

    int sectionOrder() default 0;

    int order() default 0;
}
