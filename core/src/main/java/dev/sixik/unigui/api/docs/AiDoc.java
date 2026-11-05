package dev.sixik.unigui.api.docs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds implementation guidance to the separate generated AI documentation collection.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface AiDoc {

    String summary() default "";

    String purpose() default "";

    String[] useWhen() default {};

    String constraints() default "";

    /**
     * Critical behavioral rules that an AI assistant must preserve.
     */
    String[] criticalRules() default {};

    String[] related() default {};

    /**
     * Markdown section used to group a documented public member.
     */
    String section() default "API";

    /**
     * Sort order of a section relative to other member sections.
     */
    int sectionOrder() default 0;

    /**
     * Sort order within a section, or between type-level examples.
     */
    int order() default 0;

    /**
     * Optional hand-written examples kept in the AI documentation collection.
     */
    Example[] examples() default {};

    /**
     * A self-contained example embedded in an {@link AiDoc} declaration.
     */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Example {
        String title();

        String code();

        String language() default "java";

        int order() default 0;
    }
}
