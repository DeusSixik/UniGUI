package dev.sixik.unigui.api.docs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds a localized link to a generated wiki page.
 *
 * <p>Use an absolute {@code https://}, {@code http://}, or {@code mailto:} URL for an external
 * destination, or {@code page:<slug>} to link to another generated wiki page.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(WikiLinks.class)
public @interface WikiLink {
    String label();

    String target();

    String description() default "";

    int order() default 0;
}
