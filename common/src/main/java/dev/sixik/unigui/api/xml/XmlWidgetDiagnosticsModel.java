package dev.sixik.unigui.api.xml;

import java.util.List;

/**
 * Редакторская list-модель для отображения XML-диагностик в панели.
 *
 * <p>Модель отделяет сырые {@link XmlWidgetDiagnostic} от UI: каждая строка уже содержит
 * severity, порядковый номер и location label. Сейчас parser/validator создаёт только ошибки,
 * но enum severity оставлен шире, чтобы позже добавить warnings и info без смены API.</p>
 */
public final class XmlWidgetDiagnosticsModel {
    private static final XmlWidgetDiagnosticsModel EMPTY = new XmlWidgetDiagnosticsModel(List.of());

    private final List<Entry> entries;

    private XmlWidgetDiagnosticsModel(List<Entry> entries) {
        this.entries = List.copyOf(entries == null ? List.of() : entries);
    }

    /**
     * Возвращает общий пустой экземпляр модели.
     *
     * @return empty diagnostics model
     */
    public static XmlWidgetDiagnosticsModel empty() {
        return EMPTY;
    }

    /**
     * Создаёт модель из результата editor parse/validation.
     *
     * @param result результат документа или {@code null}
     * @return модель diagnostics panel
     */
    public static XmlWidgetDiagnosticsModel from(XmlWidgetDocumentResult result) {
        if (result == null) return empty();
        return errors(result.diagnostics());
    }

    /**
     * Создаёт модель из статуса hot-reload preview.
     *
     * @param status статус preview или {@code null}
     * @return модель diagnostics panel
     */
    public static XmlWidgetDiagnosticsModel from(XmlWidgetHotReloadPreview.Status status) {
        if (status == null || !status.hasDiagnostics()) return empty();
        return errors(status.diagnostics());
    }

    /**
     * Создаёт модель, где каждая диагностика считается ошибкой.
     *
     * @param diagnostics список XML diagnostics
     * @return модель с error entries или empty model
     */
    public static XmlWidgetDiagnosticsModel errors(List<XmlWidgetDiagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) return empty();
        List<Entry> entries = new java.util.ArrayList<>(diagnostics.size());
        for (int i = 0; i < diagnostics.size(); i++) {
            XmlWidgetDiagnostic diagnostic = diagnostics.get(i);
            entries.add(new Entry(
                    i + 1,
                    Severity.ERROR,
                    diagnostic.message(),
                    diagnostic.line(),
                    diagnostic.column()));
        }
        return new XmlWidgetDiagnosticsModel(entries);
    }

    /**
     * Возвращает строки модели.
     *
     * @return immutable список entries
     */
    public List<Entry> entries() {
        return entries;
    }

    /**
     * Проверяет, что diagnostics отсутствуют.
     *
     * @return {@code true}, если entries пусты
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Проверяет наличие error entries.
     *
     * @return {@code true}, если есть хотя бы одна ошибка
     */
    public boolean hasErrors() {
        return entries.stream().anyMatch(entry -> entry.severity() == Severity.ERROR);
    }

    /**
     * Возвращает количество ошибок.
     *
     * @return число entries с severity {@link Severity#ERROR}
     */
    public int errorCount() {
        return (int) entries.stream().filter(entry -> entry.severity() == Severity.ERROR).count();
    }

    /**
     * Возвращает короткую сводку для заголовка панели.
     *
     * @return человекочитаемая summary строка
     */
    public String summary() {
        if (entries.isEmpty()) return "No XML diagnostics.";
        int count = entries.size();
        return count == 1 ? "1 XML diagnostic." : count + " XML diagnostics.";
    }

    /**
     * Severity одной diagnostics строки.
     */
    public enum Severity {
        /** Ошибка, из-за которой XML нельзя считать валидным. */
        ERROR,
        /** Предупреждение: XML загружается, но требует внимания. */
        WARNING,
        /** Информационное сообщение редактора. */
        INFO
    }

    /**
     * Одна строка diagnostics panel.
     *
     * @param index порядковый номер строки, начиная с 1
     * @param severity важность сообщения
     * @param message текст диагностики
     * @param line номер строки или {@code -1}
     * @param column номер колонки или {@code -1}
     */
    public record Entry(int index, Severity severity, String message, int line, int column) {
        public Entry {
            if (index < 1) throw new IllegalArgumentException("XML diagnostic entry index must be positive");
            severity = severity == null ? Severity.ERROR : severity;
            message = message == null ? "" : message.trim();
        }

        /**
         * Проверяет наличие source location.
         *
         * @return {@code true}, если line и column заданы
         */
        public boolean hasLocation() {
            return line >= 0 && column >= 0;
        }

        /**
         * Возвращает короткую подпись позиции.
         *
         * @return {@code line N, column M} или пустая строка
         */
        public String locationLabel() {
            return hasLocation() ? "line " + line + ", column " + column : "";
        }

        /**
         * Возвращает текст строки для простого list-view.
         *
         * @return message с добавленной source location, если она есть
         */
        public String displayText() {
            String label = locationLabel();
            return label.isEmpty() ? message : message + " at " + label;
        }
    }
}
