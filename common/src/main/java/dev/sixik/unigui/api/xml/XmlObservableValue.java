package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.event.EventSubscription;

/**
 * Типизированное observable-значение для XML-binding и code-behind прототипов.
 *
 * <p>Это минимальный контракт источника данных: имя, runtime-тип, текущее значение
 * и подписка на изменения. Он не задаёт синтаксис binding expression внутри XML,
 * а предоставляет инфраструктуру, которой смогут пользоваться редактор и будущий
 * binding layer.</p>
 *
 * @param <T> тип значения источника
 */
public interface XmlObservableValue<T> {
    /**
     * Возвращает стабильное имя/path значения.
     *
     * @return имя, по которому значение регистрируется в {@link XmlBindingContext}
     */
    String name();

    /**
     * Возвращает runtime-тип значения.
     *
     * @return класс значения, используемый для type-check binding'ов
     */
    Class<T> valueType();

    /**
     * Возвращает текущее значение.
     *
     * @return текущее значение; может быть {@code null}, если это допустимо для типа
     */
    T get();

    /**
     * Подписывается на изменения значения.
     *
     * @param listener слушатель изменений
     * @return subscription для отписки
     */
    EventSubscription onChanged(XmlBindingListener<T> listener);

    /**
     * Возвращает активный статус этого observable-источника.
     *
     * @return статус для diagnostics UI
     */
    default XmlBindingStatus status() {
        return XmlBindingStatus.active(name(), valueType());
    }
}
