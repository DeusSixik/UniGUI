package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.xaml.SourceLocation;
import dev.sixik.unigui.xaml.UiDiagnostic;

import java.util.List;

/**
 * Ошибка загрузки документа: разбор текста, неизвестный тип или свойство, некорректное значение.
 *
 * <p>Каждая ошибка несёт позицию в исходном тексте, поэтому редактор может подсветить точное место.
 * Сообщение исключения уже содержит {@code (line L, column C)}.</p>
 */
public final class XamlLoadException extends RuntimeException {
    private final List<UiDiagnostic> diagnostics;

    /**
     * Создаёт ошибку с одной диагностикой.
     *
     * @param message  описание
     * @param location позиция
     * @param cause    исходная причина или {@code null}
     */
    public XamlLoadException(String message, SourceLocation location, Throwable cause) {
        this(List.of(new UiDiagnostic(message, location)), cause);
    }

    /**
     * Создаёт ошибку без позиции.
     *
     * @param message описание
     */
    public XamlLoadException(String message) {
        this(message, SourceLocation.UNKNOWN, null);
    }

    /**
     * Создаёт ошибку из нескольких диагностик (например, при разборе файла стилей, где проблем может быть много).
     *
     * @param diagnostics диагностики; не должен быть пустым
     */
    public XamlLoadException(List<UiDiagnostic> diagnostics) {
        this(diagnostics, null);
    }

    private XamlLoadException(List<UiDiagnostic> diagnostics, Throwable cause) {
        super(describe(diagnostics), cause);
        this.diagnostics = List.copyOf(diagnostics);
    }

    private static String describe(List<UiDiagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) {
            throw new IllegalArgumentException("an exception needs at least one diagnostic");
        }
        if (diagnostics.size() == 1) return diagnostics.get(0).toString();
        StringBuilder builder = new StringBuilder(diagnostics.get(0).toString());
        builder.append(" (+").append(diagnostics.size() - 1).append(" more)");
        return builder.toString();
    }

    /**
     * Возвращает диагностики.
     *
     * @return неизменяемый список, как минимум с одним элементом
     */
    public List<UiDiagnostic> diagnostics() {
        return diagnostics;
    }
}
