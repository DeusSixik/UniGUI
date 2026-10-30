package dev.sixik.unigui.api.xml;

import java.util.List;
import java.util.Optional;

/**
 * Результат editor-mode парсинга XML: исходный документ и нефатальные диагностики.
 *
 * <p>Синтаксически валидный XML может содержать неизвестные виджеты, атрибуты или property-child
 * элементы. В editor mode такие проблемы возвращаются как diagnostics, чтобы пользователь мог
 * видеть документ и исправлять его постепенно.</p>
 *
 * @param document parsed source document
 * @param diagnostics immutable список нефатальных диагностик
 */
public record XmlWidgetDocumentResult(XmlWidgetDocument document, List<XmlWidgetDiagnostic> diagnostics) {
    public XmlWidgetDocumentResult {
        if (document == null) throw new IllegalArgumentException("XML widget document result must contain a document");
        diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    /**
     * Проверяет наличие диагностик.
     *
     * @return {@code true}, если validation нашла хотя бы одну проблему
     */
    public boolean hasDiagnostics() {
        return !diagnostics.isEmpty();
    }

    /**
     * Проверяет, что документ прошёл editor validation без диагностик.
     *
     * @return {@code true}, если diagnostics пусты
     */
    public boolean valid() {
        return diagnostics.isEmpty();
    }

    /**
     * Возвращает первую диагностику для status bar или quick-fix UI.
     *
     * @return первая диагностика или empty
     */
    public Optional<XmlWidgetDiagnostic> firstDiagnostic() {
        return diagnostics.isEmpty() ? Optional.empty() : Optional.of(diagnostics.get(0));
    }

    /**
     * Возвращает только тексты диагностик.
     *
     * @return immutable список сообщений
     */
    public List<String> diagnosticMessages() {
        return diagnostics.stream().map(XmlWidgetDiagnostic::message).toList();
    }

    /**
     * Превращает нефатальные диагностики в исключение.
     *
     * <p>Метод удобен для runtime-сценариев, где editor tolerance не нужна и документ должен
     * загружаться только полностью валидным.</p>
     *
     * @return этот результат, если diagnostics пусты
     */
    public XmlWidgetDocumentResult throwIfDiagnostics() {
        if (hasDiagnostics()) {
            throw new XmlWidgetLoadException(diagnostics);
        }
        return this;
    }
}
