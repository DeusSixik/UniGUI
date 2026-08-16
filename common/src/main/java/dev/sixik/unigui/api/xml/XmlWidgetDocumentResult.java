package dev.sixik.unigui.api.xml;

import java.util.List;
import java.util.Optional;

/** Результат editor-mode парсинга XML: исходный документ и нефатальные диагностики. */
public record XmlWidgetDocumentResult(XmlWidgetDocument document, List<XmlWidgetDiagnostic> diagnostics) {
    public XmlWidgetDocumentResult {
        if (document == null) throw new IllegalArgumentException("XML widget document result must contain a document");
        diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    public boolean hasDiagnostics() {
        return !diagnostics.isEmpty();
    }

    public boolean valid() {
        return diagnostics.isEmpty();
    }

    public Optional<XmlWidgetDiagnostic> firstDiagnostic() {
        return diagnostics.isEmpty() ? Optional.empty() : Optional.of(diagnostics.get(0));
    }

    public List<String> diagnosticMessages() {
        return diagnostics.stream().map(XmlWidgetDiagnostic::message).toList();
    }

    public XmlWidgetDocumentResult throwIfDiagnostics() {
        if (hasDiagnostics()) {
            throw new XmlWidgetLoadException(diagnostics);
        }
        return this;
    }
}
