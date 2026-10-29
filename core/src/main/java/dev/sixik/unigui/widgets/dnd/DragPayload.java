package dev.sixik.unigui.widgets.dnd;

/**
 * Immutable payload passed from a drag source to a drop target.
 *
 * @param id stable identifier of the dragged value
 * @param type payload type used by drop-target filters
 * @param preview short label used by drag previews
 * @param data optional in-process data
 */
public record DragPayload(String id, String type, String preview, Object data) {
    /**
     * Нормализует компоненты создаваемого payload.
     *
     * @param id стабильный идентификатор; пустое значение разрешено
     * @param type тип payload; пустое значение становится {@code generic}
     * @param preview подпись drag-preview; пустое значение заменяется идентификатором
     * @param data произвольные данные текущего процесса либо {@code null}
     */
    public DragPayload {
        id = normalize(id, "");
        type = normalize(type, "generic");
        preview = normalize(preview, id);
    }

    /**
     * Создаёт payload без дополнительных объектных данных.
     *
     * @param id стабильный идентификатор; пробелы по краям удаляются
     * @param type тип для фильтрации drop-target'ом; пустое значение становится {@code generic}
     * @param preview подпись drag-preview; пустое значение заменяется идентификатором
     */
    public DragPayload(String id, String type, String preview) {
        this(id, type, preview, null);
    }

    /**
     * Создаёт payload без дополнительных объектных данных.
     *
     * @param id стабильный идентификатор
     * @param type тип для фильтрации drop-target'ом
     * @param preview подпись drag-preview
     * @return нормализованный неизменяемый payload
     */
    public static DragPayload of(String id, String type, String preview) {
        return new DragPayload(id, type, preview, null);
    }

    /**
     * Создаёт payload с произвольными данными текущего UI-процесса.
     *
     * @param id стабильный идентификатор
     * @param type тип для фильтрации drop-target'ом
     * @param preview подпись drag-preview
     * @param data произвольные данные либо {@code null}
     * @return нормализованный неизменяемый payload
     */
    public static DragPayload of(String id, String type, String preview, Object data) {
        return new DragPayload(id, type, preview, data);
    }

    /**
     * Проверяет совместимость типа с фильтром drop-target'а.
     *
     * @param expectedType точный ожидаемый тип или {@code *} для любого типа
     * @return {@code true}, если тип совпадает либо используется wildcard
     */
    public boolean matchesType(String expectedType) {
        String normalized = normalize(expectedType, "");
        return normalized.equals("*") || type.equals(normalized);
    }

    /**
     * Проверяет наличие дополнительных объектных данных.
     *
     * @return {@code true}, если {@link #data()} не равен {@code null}
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Возвращает дополнительные данные только при совпадении их типа.
     *
     * @param type ожидаемый класс данных; может быть {@code null}
     * @param <T> ожидаемый тип данных
     * @return приведённые данные либо {@code null}, если данных нет или тип не совпал
     */
    public <T> T data(Class<T> type) {
        return type != null && type.isInstance(data) ? type.cast(data) : null;
    }

    /**
     * Нормализует строковое поле payload.
     *
     * @param value исходное значение; может быть {@code null}
     * @param fallback значение для пустой строки
     * @return строка без пробелов по краям либо {@code fallback}
     */
    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
