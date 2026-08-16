package dev.sixik.unigui.api.xml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация объявляет публичное XML-имя типа виджета.
 *
 * <p>Имя используется {@link XmlWidgetRegistry#registerAnnotated(Class)} как tag name,
 * который можно писать в XML: {@code <Button />}, {@code <VBox />}, {@code <MyWidget />}. Без этой
 * аннотации annotated-регистрация не знает, под каким именем публиковать класс.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface XmlWidgetName {
    /**
     * Возвращает XML tag name виджета.
     *
     * @return непустое имя XML-элемента
     */
    String value();
}
