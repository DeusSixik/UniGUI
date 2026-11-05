package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

/**
 * Событие структурного изменения набора строк таблицы: вставка либо удаление.
 *
 * <p>Индексы строк относятся к модели данных (source rows), а не к порядку после сортировки.
 * Владелец данных получает событие и синхронизирует собственное хранилище.</p>
 */
public final class TableRowsChangedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<TableRowsChangedEvent> TYPE = EventType.create("table.rows_changed");

    private final Widget target;
    private final int index;
    private final int count;
    private final boolean inserted;

    /**
     * Создаёт событие изменения строк.
     *
     * @param target   таблица-источник
     * @param index    индекс первой затронутой строки модели
     * @param count    число вставленных либо удалённых строк
     * @param inserted {@code true} для вставки, {@code false} для удаления
     */
    public TableRowsChangedEvent(Widget target, int index, int count, boolean inserted) {
        this.target = Objects.requireNonNull(target, "target");
        this.index = index;
        this.count = count;
        this.inserted = inserted;
    }

    @Override
    public EventType<TableRowsChangedEvent> type() {
        return TYPE;
    }

    @Override
    public Widget target() {
        return target;
    }

    @Override
    public Widget currentTarget() {
        return target;
    }

    @Override
    public EventPhase phase() {
        return EventPhase.TARGET;
    }

    /**
     * Возвращает индекс первой затронутой строки модели.
     *
     * @return индекс строки
     */
    public int index() {
        return index;
    }

    /**
     * Возвращает число затронутых строк.
     *
     * @return количество строк
     */
    public int count() {
        return count;
    }

    /**
     * Возвращает признак вставки.
     *
     * @return {@code true}, если строки добавлены
     */
    public boolean inserted() {
        return inserted;
    }

    /**
     * Возвращает признак удаления.
     *
     * @return {@code true}, если строки удалены
     */
    public boolean removed() {
        return !inserted;
    }
}
