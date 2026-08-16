package dev.sixik.unigui.api.xml;

import java.util.List;

/** Исключение выбрасывается, когда XML нельзя материализовать в валидное дерево виджетов UniGUI. */
public final class XmlWidgetLoadException extends RuntimeException {
    private final List<XmlWidgetDiagnostic> diagnostics;

    public XmlWidgetLoadException(String message) {
        this(List.of(new XmlWidgetDiagnostic(message)));
    }

    public XmlWidgetLoadException(String message, Throwable cause) {
        this(List.of(new XmlWidgetDiagnostic(message)), cause);
    }

    public XmlWidgetLoadException(List<XmlWidgetDiagnostic> diagnostics) {
        super(message(diagnostics));
        this.diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    public XmlWidgetLoadException(List<XmlWidgetDiagnostic> diagnostics, Throwable cause) {
        super(message(diagnostics), cause);
        this.diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

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
