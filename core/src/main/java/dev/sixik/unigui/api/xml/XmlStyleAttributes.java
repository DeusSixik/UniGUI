package dev.sixik.unigui.api.xml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Маркер базовых виджетов, которые добавляют общие XML-атрибуты стиля и состояния.
 *
 * <p>Маркер подключает common widget attributes: {@code id}, {@code enabled}, {@code visibility},
 * transform/opacity и другие свойства, которые повторяются у большинства widget-ов. Это снижает
 * количество ручной регистрации одинаковых атрибутов в built-in XML registry.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface XmlStyleAttributes {
}
