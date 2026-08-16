package dev.sixik.unigui.api.xml;

/** Диагностика, полученная при загрузке дерева виджетов из XML. */
public record XmlWidgetDiagnostic(String message, int line, int column) {
    public XmlWidgetDiagnostic(String message) {
        this(message, -1, -1);
    }

    public boolean hasLocation() {
        return line >= 0 && column >= 0;
    }

    @Override
    public String toString() {
        if (!hasLocation()) return message;
        return message + " at line " + line + ", column " + column;
    }
}
