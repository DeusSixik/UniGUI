package dev.sixik.unigui.api.xml;

import java.util.List;

/**
 * Исключение выбрасывается, когда XML нельзя материализовать в валидное дерево виджетов UniGUI.
 *
 * <p>Exception хранит список diagnostics, а {@link #getMessage()} строится по первой диагностике.
 * Это позволяет runtime-коду бросать обычное исключение, а editor/hot-reload UI получать полный
 * список проблем для отображения пользователю.</p>
 */
public final class XmlWidgetLoadException extends RuntimeException {
    private final List<XmlWidgetDiagnostic> diagnostics;

    /**
     * Создаёт исключение с одной диагностикой без source location.
     *
     * @param message текст ошибки
     */
    public XmlWidgetLoadException(String message) {
        this(List.of(new XmlWidgetDiagnostic(message)));
    }

    /**
     * Создаёт исключение с одной диагностикой и cause.
     *
     * @param message текст ошибки
     * @param cause исходное исключение
     */
    public XmlWidgetLoadException(String message, Throwable cause) {
        this(List.of(new XmlWidgetDiagnostic(message)), cause);
    }

    /**
     * Создаёт исключение из списка diagnostics.
     *
     * @param diagnostics diagnostics загрузки XML
     */
    public XmlWidgetLoadException(List<XmlWidgetDiagnostic> diagnostics) {
        super(message(diagnostics));
        this.diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    /**
     * Создаёт исключение из списка diagnostics и cause.
     *
     * @param diagnostics diagnostics загрузки XML
     * @param cause исходное исключение
     */
    public XmlWidgetLoadException(List<XmlWidgetDiagnostic> diagnostics, Throwable cause) {
        super(message(diagnostics), cause);
        this.diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    /**
     * Возвращает diagnostics, связанные с ошибкой загрузки.
     *
     * @return immutable список diagnostics
     */
    public List<XmlWidgetDiagnostic> diagnostics() {
        return diagnostics;
    }

    private static String message(List<XmlWidgetDiagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) {
            return "XML widget load failed.";
        }
        return diagnostics.get(0).toString();
    }
}
