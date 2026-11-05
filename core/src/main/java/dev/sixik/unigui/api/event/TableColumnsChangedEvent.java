package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

/**
 * Событие добавления либо удаления колонки таблицы.
 *
 * <p>{@link #index()} — позиция колонки в текущем порядке отображения, а {@link #modelIndex()} —
 * её стабильный индекс данных, который передаётся провайдерам ячеек. Модельный индекс не меняется
 * при перемещении и удалении соседних колонок.</p>
 */
public final class TableColumnsChangedEvent extends BaseEvent implements WidgetEvent {
    public static final EventType<TableColumnsChangedEvent> TYPE = EventType.create("table.columns_changed");

    private final Widget target;
    private final int index;
    private final int modelIndex;
    private final boolean inserted;

    /**
     * Создаёт событие изменения набора колонок.
     *
     * @param target     таблица-источник
     * @param index      позиция колонки в порядке отображения
     * @param modelIndex стабильный индекс данных колонки
     * @param inserted   {@code true} для добавления, {@code false} для удаления
     */
    public TableColumnsChangedEvent(Widget target, int index, int modelIndex, boolean inserted) {
        this.target = Objects.requireNonNull(target, "target");
        this.index = index;
        this.modelIndex = modelIndex;
        this.inserted = inserted;
    }

    @Override
    public EventType<TableColumnsChangedEvent> type() {
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
     * Возвращает позицию колонки в порядке отображения.
     *
     * @return индекс колонки
     */
    public int index() {
        return index;
    }

    /**
     * Возвращает стабильный индекс данных колонки.
     *
     * @return модельный индекс
     */
    public int modelIndex() {
        return modelIndex;
    }

    /**
     * Возвращает признак добавления.
     *
     * @return {@code true}, если колонка добавлена
     */
    public boolean inserted() {
        return inserted;
    }

    /**
     * Возвращает признак удаления.
     *
     * @return {@code true}, если колонка удалена
     */
    public boolean removed() {
        return !inserted;
    }
}
