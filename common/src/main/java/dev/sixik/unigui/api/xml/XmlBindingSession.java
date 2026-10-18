package dev.sixik.unigui.api.xml;

import java.util.ArrayList;
import java.util.List;

/**
 * Владелец binding-ов одного загруженного XML-экрана.
 *
 * <p>Сессия позволяет закрыть все подписки одним вызовом при hot reload или
 * удалении экрана. В неё попадают также неактивные binding-ы, чтобы редактор
 * мог получить их диагностику до закрытия экрана.</p>
 */
public final class XmlBindingSession implements AutoCloseable {
    /**
     * Список binding-ов в порядке их создания.
     */
    private final List<XmlBinding<?>> bindings = new ArrayList<>();
    /**
     * Флаг завершённого lifecycle сессии.
     */
    private boolean closed;

    /**
     * Добавляет binding в сессию.
     *
     * @param binding активный или диагностический binding; {@code null} игнорируется
     * @param <T>     тип значения binding-а
     * @return этот объект для chained-вызовов
     */
    public <T> XmlBindingSession add(XmlBinding<T> binding) {
        if (binding == null) return this;
        if (closed) {
            binding.close();
            return this;
        }
        bindings.add(binding);
        return this;
    }

    /**
     * Возвращает snapshot статусов всех binding-ов сессии.
     *
     * @return неизменяемый список статусов
     */
    public List<XmlBindingStatus> statuses() {
        return bindings.stream().map(binding -> binding.status()).toList();
    }

    /**
     * Возвращает только диагностические статусы сессии.
     *
     * @return неизменяемый список ошибок binding-ов
     */
    public List<XmlBindingStatus> diagnostics() {
        return statuses().stream().filter(status -> status.hasDiagnostics()).toList();
    }

    /**
     * Проверяет, закрыта ли сессия.
     *
     * @return {@code true}, если подписки уже закрыты
     */
    public boolean closed() {
        return closed;
    }

    /**
     * Закрывает все binding-и сессии.
     *
     * <p>Метод идемпотентен и безопасен для повторного вызова.</p>
     */
    @Override
    public void close() {
        if (closed) return;
        closed = true;
        for (XmlBinding<?> binding : List.copyOf(bindings)) {
            binding.close();
        }
    }
}
