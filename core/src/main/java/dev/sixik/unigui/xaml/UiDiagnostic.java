package dev.sixik.unigui.xaml;

/**
 * Замечание к документу или виджету: сообщение и, если известна, позиция в исходном тексте.
 *
 * <p>Один и тот же тип используется везде, где нужно показать пользователю проблему с местом: ошибки загрузки,
 * разбор стилей, диагностика редактора. Это замена прежнего {@code XmlWidgetDiagnostic}, у которого позиция была
 * двумя отдельными числами.</p>
 *
 * @param message  описание проблемы без позиции
 * @param location позиция или {@link SourceLocation#UNKNOWN}
 */
public record UiDiagnostic(String message, SourceLocation location) {
    public UiDiagnostic {
        message = message == null ? "" : message;
        location = location == null ? SourceLocation.UNKNOWN : location;
    }

    /**
     * Создаёт замечание без позиции.
     *
     * @param message описание
     */
    public UiDiagnostic(String message) {
        this(message, SourceLocation.UNKNOWN);
    }

    /**
     * Создаёт замечание с позицией, заданной числами.
     *
     * @param message описание
     * @param line    строка или отрицательное число, если неизвестна
     * @param column  колонка или отрицательное число, если неизвестна
     */
    public UiDiagnostic(String message, int line, int column) {
        this(message, line > 0 && column > 0 ? new SourceLocation(line, column) : SourceLocation.UNKNOWN);
    }

    /** @return {@code true}, если позиция известна */
    public boolean hasLocation() {
        return location.known();
    }

    /** @return номер строки или {@code -1} */
    public int line() {
        return location.line();
    }

    /** @return номер колонки или {@code -1} */
    public int column() {
        return location.column();
    }

    @Override
    public String toString() {
        return hasLocation() ? message + " (line " + line() + ", column " + column() + ")" : message;
    }
}
