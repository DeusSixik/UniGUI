package dev.sixik.unigui.xaml.bind;

/**
 * Слушатель изменения XML observable-значения.
 *
 * <p>Вызывается синхронно из {@link XamlMutableObservableValue#set(Object)} после
 * фактического изменения значения. Listener должен быть коротким и не блокировать
 * UI/update-поток.</p>
 *
 * @param <T> тип значения observable-источника
 */
@FunctionalInterface
public interface XamlBindingListener<T> {
    /**
     * Обрабатывает snapshot изменения.
     *
     * @param change имя источника, тип, старое и новое значение
     */
    void changed(XamlBindingChange<T> change);
}
