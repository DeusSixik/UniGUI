package dev.sixik.unigui.xaml.bind;

import dev.sixik.unigui.api.event.EventSubscription;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Список, который сообщает о своих изменениях.
 *
 * <p>Источник данных для списков в интерфейсе ({@code <VirtualListView Items="{Binding catalog}">}): добавили
 * товар в список — карточка появилась, убрали — исчезла, без ручных {@code addChild}. Список рассчитан на поток
 * интерфейса, как и остальные виджеты; слушатели вызываются синхронно, после изменения.</p>
 *
 * @param <T> тип элемента
 */
public final class XamlObservableList<T> extends AbstractList<T> {
    /**
     * Что произошло со списком.
     */
    public enum Kind {
        /**
         * Вставлены элементы: {@code count} штук, начиная с {@code index}.
         */
        ADDED,
        /**
         * Удалены элементы: {@code count} штук, начиная с {@code index}.
         */
        REMOVED,
        /**
         * Заменены элементы: {@code count} штук, начиная с {@code index}.
         */
        REPLACED,
        /**
         * Содержимое изменилось целиком; индексы не имеют смысла.
         */
        RESET
    }

    /**
     * Описание одного изменения.
     *
     * @param kind  что произошло
     * @param index первый затронутый индекс (для {@link Kind#RESET} — 0)
     * @param count сколько элементов затронуто (для {@link Kind#RESET} — новый размер)
     */
    public record Change(Kind kind, int index, int count) {
    }

    private final List<T> items = new ArrayList<>();
    private final List<Consumer<Change>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Создаёт пустой список.
     */
    public XamlObservableList() {
    }

    /**
     * Создаёт список с начальным содержимым; слушателей пока нет, поэтому событий не будет.
     *
     * @param initial начальные элементы
     */
    public XamlObservableList(Collection<? extends T> initial) {
        items.addAll(initial);
    }

    /**
     * Подписывается на изменения.
     *
     * @param listener получает описание каждого изменения
     * @return подписка
     */
    public EventSubscription onChanged(Consumer<Change> listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /**
     * Заменяет всё содержимое одним событием {@link Kind#RESET}.
     *
     * @param next новое содержимое
     */
    public void setAll(Collection<? extends T> next) {
        items.clear();
        items.addAll(next);
        modCount++;
        fire(new Change(Kind.RESET, 0, items.size()));
    }

    @Override
    public T get(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public T set(int index, T element) {
        T previous = items.set(index, element);
        fire(new Change(Kind.REPLACED, index, 1));
        return previous;
    }

    @Override
    public void add(int index, T element) {
        items.add(index, element);
        modCount++;
        fire(new Change(Kind.ADDED, index, 1));
    }

    @Override
    public boolean addAll(Collection<? extends T> added) {
        if (added.isEmpty()) return false;
        int start = items.size();
        items.addAll(added);
        modCount++;
        fire(new Change(Kind.ADDED, start, added.size()));
        return true;
    }

    @Override
    public T remove(int index) {
        T removed = items.remove(index);
        modCount++;
        fire(new Change(Kind.REMOVED, index, 1));
        return removed;
    }

    @Override
    public void clear() {
        if (items.isEmpty()) return;
        items.clear();
        modCount++;
        fire(new Change(Kind.RESET, 0, 0));
    }

    private void fire(Change change) {
        for (Consumer<Change> listener : listeners) listener.accept(change);
    }
}
