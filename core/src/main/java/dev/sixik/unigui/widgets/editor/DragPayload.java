package dev.sixik.unigui.widgets.editor;

/**
 * Immutable payload, передаваемый между drag source и drop target.
 *
 * <p>Строковые {@code id}, {@code type} и {@code preview} подходят для сериализуемых сценариев
 * (палитры, XML). Поле {@code data} несёт произвольный объект в рамках одного UI-процесса: модель
 * данных, виджет-источник, ссылку на секцию списка. Payload никогда не сериализуется
 * автоматически, поэтому в {@code data} безопасно класть любые Java-объекты.</p>
 *
 * @param id      стабильный идентификатор перетаскиваемого объекта
 * @param type    тип payload, по которому drop target фильтрует принимаемое
 * @param preview короткая текстовая подпись для ghost'а
 * @param data    произвольные данные или {@code null}
 */
public record DragPayload(String id, String type, String preview, Object data) {
    public DragPayload {
        id = normalize(id, "");
        type = normalize(type, "generic");
        preview = normalize(preview, id);
    }

    /**
     * Совместимый конструктор без произвольных данных.
     */
    public DragPayload(String id, String type, String preview) {
        this(id, type, preview, null);
    }

    public static DragPayload of(String id, String type, String preview) {
        return new DragPayload(id, type, preview, null);
    }

    public static DragPayload of(String id, String type, String preview, Object data) {
        return new DragPayload(id, type, preview, data);
    }

    public boolean matchesType(String expectedType) {
        String normalized = normalize(expectedType, "");
        return normalized.equals("*") || type.equals(normalized);
    }

    /**
     * @return {@code true}, если payload несёт произвольные данные
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Возвращает данные, приведённые к нужному типу.
     *
     * @param type ожидаемый тип данных
     * @param <T>  тип данных
     * @return данные или {@code null}, если их нет или тип не совпал
     */
    public <T> T data(Class<T> type) {
        return type != null && type.isInstance(data) ? type.cast(data) : null;
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
