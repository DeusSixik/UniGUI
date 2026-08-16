package dev.sixik.unigui.api.xml;

/**
 * Слушатель изменения XML observable-значения.
 *
 * <p>Вызывается синхронно из {@link XmlMutableObservableValue#set(Object)} после
 * фактического изменения значения. Listener должен быть коротким и не блокировать
 * UI/update-поток.</p>
 *
 * @param <T> тип значения observable-источника
 */
@FunctionalInterface
public interface XmlBindingListener<T> {
    /**
     * Обрабатывает snapshot изменения.
     *
     * @param change имя источника, тип, старое и новое значение
     */
    void changed(XmlBindingChange<T> change);
}
