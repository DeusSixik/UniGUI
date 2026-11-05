package dev.sixik.unigui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Что сейчас выбрано в редакторе.
 *
 * <p>Выбор общий для всех панелей: иерархия выбирает узел, инспектор показывает его свойства, вьюпорт рисует рамку.
 * Хранятся произвольные объекты, а панель берёт только те, что умеет показать ({@link #first(Class)}). Изменение
 * объявляется событием {@link Changed} на шине.</p>
 */
public final class EditorSelection {
    /**
     * Выбор изменился.
     *
     * @param items новое содержимое выбора
     */
    public record Changed(List<Object> items) {
        public Changed {
            items = List.copyOf(items);
        }
    }

    private final EditorEvents events;
    private List<Object> items = List.of();

    EditorSelection(EditorEvents events) {
        this.events = events;
    }

    /** @return выбранные объекты в порядке выбора */
    public List<Object> items() {
        return items;
    }

    /** @return {@code true}, если ничего не выбрано */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** @return первый выбранный объект */
    public Optional<Object> first() {
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    /**
     * Первый выбранный объект нужного типа.
     *
     * @param type класс
     * @param <T>  тип
     * @return объект или пусто
     */
    public <T> Optional<T> first(Class<T> type) {
        for (Object item : items) if (type.isInstance(item)) return Optional.of(type.cast(item));
        return Optional.empty();
    }

    /**
     * @param item объект
     * @return {@code true}, если объект выбран
     */
    public boolean contains(Object item) {
        return items.contains(item);
    }

    /**
     * Заменяет выбор. Если новое содержимое совпадает с прежним, событие не посылается.
     *
     * @param selected новые объекты; {@code null}-элементы недопустимы
     */
    public void set(List<?> selected) {
        List<Object> next = new ArrayList<>(selected.size());
        for (Object item : selected) next.add(Objects.requireNonNull(item, "selected item"));
        if (next.equals(items)) return;
        items = List.copyOf(next);
        events.post(new Changed(items));
    }

    /**
     * Заменяет выбор одним объектом.
     *
     * @param item объект
     */
    public void set(Object item) {
        set(List.of(item));
    }

    /** Снимает выбор. */
    public void clear() {
        set(List.of());
    }
}
