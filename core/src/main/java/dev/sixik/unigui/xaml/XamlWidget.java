package dev.sixik.unigui.xaml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация объявляет публичное XAML-имя типа виджета.
 *
 * <p>Имя используется {@link dev.sixik.unigui.xaml.schema.Schema#defineAnnotated(Class)} как tag name,
 * который можно писать в XAML: {@code <Button />}, {@code <VBox />}, {@code <MyWidget />}. Без этой
 * аннотации annotated-регистрация не знает, под каким именем публиковать класс.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface XamlWidget {
    /**
     * Возвращает XML tag name виджета.
     *
     * @return непустое имя XML-элемента
     */
    String value();
}
