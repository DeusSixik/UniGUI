package dev.sixik.unigui.api.docs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type as a generated Markdown wiki page.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WikiPage {
    /**
     * @return the nonblank display title, which may include localization keys
     */
    String title();

    /**
     * @return the unique, stable URL segment made of lowercase letters or digits with optional hyphens between words
     */
    String slug();

    /**
     * @return the slash-separated category, whose English value determines the output directory
     */
    String category() default "API";

    /**
     * @return a localizable page description for the page and index, or an empty string
     */
    String summary() default "";

    /**
     * @return the sort position within the category, with smaller values listed first
     */
    int order() default 0;
}
