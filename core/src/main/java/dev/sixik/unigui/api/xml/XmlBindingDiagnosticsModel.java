package dev.sixik.unigui.api.xml;

import java.util.List;

/**
 * Редакторская list-модель для статусов binding-ов и диагностик.
 *
 * <p>Модель превращает набор {@link XmlBindingStatus} в плоский immutable список строк,
 * который удобно отдавать в list/table widget без знания внутренней структуры binding API.
 * Каждый entry хранит индекс, state, path и уже готовый display message.</p>
 */
public final class XmlBindingDiagnosticsModel {
    private static final XmlBindingDiagnosticsModel EMPTY = new XmlBindingDiagnosticsModel(List.of());

    private final List<Entry> entries;

    private XmlBindingDiagnosticsModel(List<Entry> entries) {
        this.entries = List.copyOf(entries == null ? List.of() : entries);
    }

    /**
     * Возвращает общий пустой экземпляр модели.
     *
     * @return empty diagnostics model
     */
    public static XmlBindingDiagnosticsModel empty() {
        return EMPTY;
    }

    /**
     * Создаёт модель из одного binding status.
     *
     * @param status статус binding-а или {@code null}
     * @return модель с одной строкой либо пустая модель
     */
    public static XmlBindingDiagnosticsModel from(XmlBindingStatus status) {
        if (status == null) return empty();
        return from(List.of(status));
    }

    /**
     * Создаёт модель из списка binding status-ов.
     *
     * <p>{@code null} элементы игнорируются, а индексы entries пересчитываются заново
     * в порядке входного списка.</p>
     *
     * @param statuses статусы binding-ов
     * @return immutable модель диагностик
     */
    public static XmlBindingDiagnosticsModel from(List<XmlBindingStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) return empty();
        java.util.ArrayList<Entry> entries = new java.util.ArrayList<>(statuses.size());
        for (XmlBindingStatus status : statuses) {
            if (status == null) continue;
            entries.add(new Entry(entries.size() + 1, status.state(), status.path(), status.summary()));
        }
        return entries.isEmpty() ? empty() : new XmlBindingDiagnosticsModel(entries);
    }

    /**
     * Возвращает строки модели в порядке отображения.
     *
     * @return immutable список entries
     */
    public List<Entry> entries() {
        return entries;
    }

    /**
     * Проверяет, что в модели нет строк.
     *
     * @return {@code true}, если entries пусты
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Проверяет наличие error status-ов.
     *
     * @return {@code true}, если хотя бы одна строка является ошибкой
     */
    public boolean hasErrors() {
        return entries.stream().anyMatch(entry -> entry.state() == XmlBindingStatus.State.ERROR);
    }

    /**
     * Возвращает количество error status-ов.
     *
     * @return число строк со state {@link XmlBindingStatus.State#ERROR}
     */
    public int errorCount() {
        return (int) entries.stream().filter(entry -> entry.state() == XmlBindingStatus.State.ERROR).count();
    }

    /**
     * Возвращает короткую сводку для status bar или заголовка панели.
     *
     * @return человекочитаемая summary строка
     */
    public String summary() {
        if (entries.isEmpty()) return "No XML binding diagnostics.";
        int errors = errorCount();
        if (errors == 0) return entries.size() == 1 ? "1 XML binding status." : entries.size() + " XML binding statuses.";
        return errors == 1 ? "1 XML binding diagnostic." : errors + " XML binding diagnostics.";
    }

    /**
     * Одна строка diagnostics list-а.
     *
     * @param index порядковый номер строки, начиная с 1
     * @param state состояние binding-а
     * @param path binding path или имя observable value
     * @param message готовый текст для отображения
     */
    public record Entry(int index, XmlBindingStatus.State state, String path, String message) {
        public Entry {
            if (index < 1) throw new IllegalArgumentException("XML binding entry index must be positive");
            state = state == null ? XmlBindingStatus.State.ERROR : state;
            path = path == null ? "" : path.trim();
            message = message == null ? "" : message.trim();
        }

        /**
         * Проверяет, что строка описывает ошибку.
         *
         * @return {@code true}, если state равен {@link XmlBindingStatus.State#ERROR}
         */
        public boolean error() {
            return state == XmlBindingStatus.State.ERROR;
        }

        /**
         * Возвращает текст строки для простого list-view.
         *
         * @return {@code path: message}, если path задан, иначе только message
         */
        public String displayText() {
            return path.isEmpty() ? message : path + ": " + message;
        }
    }
}
