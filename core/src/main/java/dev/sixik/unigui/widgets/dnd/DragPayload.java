package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Immutable payload passed from a drag source to a drop target.
 *
 * @param id stable identifier of the dragged value
 * @param type payload type used by drop-target filters
 * @param preview short label used by drag previews
 * @param data optional in-process data
 */
@AiDoc(summary = "Immutable transfer payload conveying identifier, type, preview text, and optional user object between drag source and drop target."
)
@WikiPage(
        title = "DragPayload",
        slug = "drag-payload",
        category = "<key:dnd.category>",
        summary = "<key:drag.payload.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:drag.payload.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:drag.payload.example.basic.title>",
        code = "DragPayload payload = DragPayload.of(\"item-42\", \"item.stack\", \"Iron Ingot\", stackItem);"
)
@WikiLink(
        label = "DragController",
        target = "page:drag-controller",
        description = "<key:drag.controller.summary>"
)
@WikiLink(
        label = "DropController",
        target = "page:drop-controller",
        description = "<key:drop.controller.summary>"
)
public record DragPayload(
        @WikiParameter(description = "<key:drag.payload.param.id>")
        String id,
        @WikiParameter(description = "<key:drag.payload.param.type>")
        String type,
        @WikiParameter(description = "<key:drag.payload.param.preview>")
        String preview,
        @WikiParameter(description = "<key:drag.payload.param.data>")
        Object data
) {
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
    @AiDoc(summary = "Constructs a drag payload without extra object data.")
    @WikiMethod(summary = "<key:drag.payload.method.ctor.basic>")
    public DragPayload(
            @WikiParameter(description = "<key:drag.payload.param.id>")
            String id,
            @WikiParameter(description = "<key:drag.payload.param.type>")
            String type,
            @WikiParameter(description = "<key:drag.payload.param.preview>")
            String preview
    ) {
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
    @AiDoc(summary = "Factory method to create a payload without extra data.")
    @WikiMethod(summary = "<key:drag.payload.method.of.basic>")
    @WikiReturn("<key:drag.payload.return.payload>")
    public static DragPayload of(
            @WikiParameter(description = "<key:drag.payload.param.id>")
            String id,
            @WikiParameter(description = "<key:drag.payload.param.type>")
            String type,
            @WikiParameter(description = "<key:drag.payload.param.preview>")
            String preview
    ) {
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
    @AiDoc(summary = "Factory method to create a payload with arbitrary in-process data.")
    @WikiMethod(summary = "<key:drag.payload.method.of.data>")
    @WikiReturn("<key:drag.payload.return.payload>")
    public static DragPayload of(
            @WikiParameter(description = "<key:drag.payload.param.id>")
            String id,
            @WikiParameter(description = "<key:drag.payload.param.type>")
            String type,
            @WikiParameter(description = "<key:drag.payload.param.preview>")
            String preview,
            @WikiParameter(description = "<key:drag.payload.param.data>")
            Object data
    ) {
        return new DragPayload(id, type, preview, data);
    }

    /**
     * Проверяет совместимость типа с фильтром drop-target'а.
     *
     * @param expectedType точный ожидаемый тип или {@code *} для любого типа
     * @return {@code true}, если тип совпадает либо используется wildcard
     */
    @AiDoc(summary = "Checks whether the payload type matches the expected filter.")
    @WikiMethod(summary = "<key:drag.payload.method.matchesType>")
    @WikiReturn("<key:drag.payload.return.matchesType>")
    public boolean matchesType(
            @WikiParameter(description = "<key:drag.payload.param.expectedType>")
            String expectedType
    ) {
        String normalized = normalize(expectedType, "");
        return normalized.equals("*") || type.equals(normalized);
    }

    /**
     * Проверяет наличие дополнительных объектных данных.
     *
     * @return {@code true}, если {@link #data()} не равен {@code null}
     */
    @AiDoc(summary = "Checks whether this payload carries attached data.")
    @WikiMethod(summary = "<key:drag.payload.method.hasData>")
    @WikiReturn("<key:drag.payload.return.hasData>")
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
    @AiDoc(summary = "Returns the attached data cast to the specified class, or null if absent or type mismatch.")
    @WikiMethod(summary = "<key:drag.payload.method.dataClass>")
    @WikiReturn("<key:drag.payload.return.dataClass>")
    public <T> T data(
            @WikiParameter(description = "<key:drag.payload.param.dataClass>")
            Class<T> type
    ) {
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
        if (value == null) return fallback;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
