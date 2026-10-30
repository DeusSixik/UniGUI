package dev.sixik.unigui.api.animation;

import dev.sixik.unigui.api.widget.Widget;

/**
 * Скомпилированный доступ к одному свойству виджета.
 *
 * <p>Accessor создаётся или регистрируется один раз. Storyboard player хранит готовую ссылку
 * и не разбирает property path во время кадрового обновления.</p>
 *
 * @param <T> тип свойства
 */
public interface PropertyAccessor<T> {
    /** @return Java-тип значения для проверки совместимости track'а */
    Class<T> valueType();

    /** Читает текущее значение свойства. */
    T get(Widget widget);

    /** Записывает значение свойства. */
    void set(Widget widget, T value);
}
