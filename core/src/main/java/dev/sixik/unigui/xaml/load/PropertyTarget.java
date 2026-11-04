package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.api.widget.Widget;

/**
 * Свойство конкретного виджета, в которое markup extension записывает результат.
 *
 * <p>Интерфейс скрывает дженерики схемы: расширению нужно лишь знать, куда писать и что это за свойство.</p>
 */
public interface PropertyTarget {
    /** @return виджет, свойство которого устанавливается */
    Widget widget();

    /** @return имя свойства в документе */
    String name();

    /** @return человекочитаемое описание {@code Тип.свойство} для сообщений и диагностики */
    String describe();

    /** @return {@code true}, если свойство допускает привязку к источнику данных */
    boolean bindable();

    /** @return тип значения источника для привязки или {@code null} */
    Class<?> bindingType();

    /**
     * Разбирает текст конвертером свойства и применяет результат.
     *
     * @param text текст значения
     */
    void setText(String text);

    /**
     * Применяет уже типизированное значение (например, пришедшее от binding).
     *
     * @param value значение подходящего типа
     */
    void setValue(Object value);
}
