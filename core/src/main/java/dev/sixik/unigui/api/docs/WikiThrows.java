package dev.sixik.unigui.api.docs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

/** Describes an exception contract of a documented wiki method or constructor. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(WikiThrowsList.class)
public @interface WikiThrows {
    Class<? extends Throwable> type();

    String description();
}
