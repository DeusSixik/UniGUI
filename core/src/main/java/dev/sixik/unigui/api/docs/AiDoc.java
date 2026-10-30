package dev.sixik.unigui.api.docs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Adds implementation guidance to the separate generated AI documentation collection. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface AiDoc {

    String summary() default "";

    String purpose() default "";

    String[] useWhen() default {};

    String constraints() default "";

    String[] related() default {};
}
