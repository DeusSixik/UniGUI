package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.widget.Widget;

import java.util.function.Consumer;

/**
 * Сообщает, что пользователь изменил значение свойства в виджете (ввёл текст, сдвинул ползунок).
 *
 * <p>Свойство с уведомителем можно привязать в обе стороны: {@code {Binding path, Mode=TwoWay}}.</p>
 *
 * @param <T> тип виджета
 */
@FunctionalInterface
public interface ChangeNotifier<T extends Widget> {
    /**
     * Подписывается на изменения.
     *
     * @param widget   виджет
     * @param onChange получает новое значение свойства
     * @return подписка
     */
    EventSubscription subscribe(T widget, Consumer<Object> onChange);
}
