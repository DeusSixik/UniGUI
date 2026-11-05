package dev.sixik.unigui.xaml.bind;

import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Команда, которой разметка передаёт параметр: {@code onClick="{Command shop.buy, Parameter={Item id}}"}.
 *
 * <p>Параметр вычисляется один раз, когда загружается виджет (для строки списка — когда создаётся строка), поэтому
 * одна и та же команда получает у каждой кнопки своё значение. Обычная команда без параметра остаётся
 * {@link XamlCommand}; параметр можно передать только команде, объявленной этим интерфейсом, иначе загрузка
 * сообщит об ошибке.</p>
 */
@FunctionalInterface
public interface XamlParameterCommand extends XamlCommand {
    /**
     * Выполняет команду.
     *
     * @param source    виджет-источник события
     * @param event     исходное событие
     * @param parameter значение параметра из разметки; может быть {@code null}
     */
    void execute(Widget source, Event event, Object parameter);

    @Override
    default void execute(Widget source, Event event) {
        execute(source, event, null);
    }
}
