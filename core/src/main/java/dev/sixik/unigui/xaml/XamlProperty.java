package dev.sixik.unigui.xaml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает fluent-setter как XAML-свойство виджета.
 *
 * <p>Аннотация считывается {@link dev.sixik.unigui.xaml.schema.Schema#defineAnnotated(Class)} при
 * регистрации типа и при построении встроенной схемы.
 * Метод должен быть нестатическим и принимать ровно один параметр поддерживаемого
 * типа: строку, число, boolean, enum, цвет, size/insets, rect или texture handle.</p>
 *
 * <pre>{@code
 * @XamlProperty(
 *         value = "selectedIndex",
 *         category = "Behavior",
 *         defaultValue = "0",
 *         description = "Initial selected item index.")
 * public TabControl selectedIndex(int selectedIndex) { ... }
 * }</pre>
 *
 * <p>{@code value} — это имя свойства в документе. Остальные поля идут в editor metadata
 * и не влияют на runtime-применение атрибута.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface XamlProperty {
    /**
     * Имя свойства.
     *
     * @return имя свойства в документе
     */
    String value();

    /**
     * Человекочитаемое имя для инспектора.
     *
     * @return display name; пустая строка означает автоматическую генерацию из {@link #value()}
     */
    String displayName() default "";

    /**
     * Категория атрибута в редакторе.
     *
     * @return category, например {@code "Layout"}, {@code "Behavior"} или {@code "Assets"}
     */
    String category() default "";

    /**
     * Текстовое значение по умолчанию для подсказок редактора.
     *
     * @return default value в XML-синтаксисе или пустая строка
     */
    String defaultValue() default "";

    /**
     * Короткое описание атрибута для inspector UI.
     *
     * @return описание назначения и единиц измерения атрибута
     */
    String description() default "";
}
