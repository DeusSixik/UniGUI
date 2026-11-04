package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.xaml.SourceLocation;

import java.util.List;

/**
 * Ошибка загрузки документа: разбор текста, неизвестный тип или свойство, некорректное значение.
 *
 * <p>Каждая ошибка несёт позицию в исходном тексте, поэтому редактор может подсветить точное место.
 * Сообщение исключения уже содержит {@code (line L, column C)}.</p>
 */
public final class XamlLoadException extends RuntimeException {
    private final List<Diagnostic> diagnostics;

    /**
     * Одна диагностика.
     *
     * @param message  описание проблемы без позиции
     * @param location позиция или {@link SourceLocation#UNKNOWN}
     */
    public record Diagnostic(String message, SourceLocation location) {
        public Diagnostic {
            location = location == null ? SourceLocation.UNKNOWN : location;
        }

        /**
         * Показывает, известна ли позиция.
         *
         * @return {@code true}, если есть строка и колонка
         */
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

    /**
     * Создаёт ошибку с одной диагностикой.
     *
     * @param message  описание
     * @param location позиция
     * @param cause    исходная причина или {@code null}
     */
    public XamlLoadException(String message, SourceLocation location, Throwable cause) {
        this(List.of(new Diagnostic(message, location)), cause);
    }

    /**
     * Создаёт ошибку без позиции.
     *
     * @param message описание
     */
    public XamlLoadException(String message) {
        this(message, SourceLocation.UNKNOWN, null);
    }

    private XamlLoadException(List<Diagnostic> diagnostics, Throwable cause) {
        super(diagnostics.get(0).toString(), cause);
        this.diagnostics = List.copyOf(diagnostics);
    }

    /**
     * Возвращает диагностики.
     *
     * @return неизменяемый список, как минимум с одним элементом
     */
    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }
}
