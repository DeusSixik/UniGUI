package dev.sixik.unigui.api.xml;

import java.util.List;

/** Редакторская list-модель для отображения XML-диагностик в панели. */
public final class XmlWidgetDiagnosticsModel {
    private static final XmlWidgetDiagnosticsModel EMPTY = new XmlWidgetDiagnosticsModel(List.of());

    private final List<Entry> entries;

    private XmlWidgetDiagnosticsModel(List<Entry> entries) {
        this.entries = List.copyOf(entries == null ? List.of() : entries);
    }

    public static XmlWidgetDiagnosticsModel empty() {
        return EMPTY;
    }

    public static XmlWidgetDiagnosticsModel from(XmlWidgetDocumentResult result) {
        if (result == null) return empty();
        return errors(result.diagnostics());
    }

    public static XmlWidgetDiagnosticsModel from(XmlWidgetHotReloadPreview.Status status) {
        if (status == null || !status.hasDiagnostics()) return empty();
        return errors(status.diagnostics());
    }

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

    public List<Entry> entries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean hasErrors() {
        return entries.stream().anyMatch(entry -> entry.severity() == Severity.ERROR);
    }

    public int errorCount() {
        return (int) entries.stream().filter(entry -> entry.severity() == Severity.ERROR).count();
    }

    public String summary() {
        if (entries.isEmpty()) return "No XML diagnostics.";
        int count = entries.size();
        return count == 1 ? "1 XML diagnostic." : count + " XML diagnostics.";
    }

    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }

    public record Entry(int index, Severity severity, String message, int line, int column) {
        public Entry {
            if (index < 1) throw new IllegalArgumentException("XML diagnostic entry index must be positive");
            severity = severity == null ? Severity.ERROR : severity;
            message = message == null ? "" : message.trim();
        }

        public boolean hasLocation() {
            return line >= 0 && column >= 0;
        }

        public String locationLabel() {
            return hasLocation() ? "line " + line + ", column " + column : "";
        }

        public String displayText() {
            String label = locationLabel();
            return label.isEmpty() ? message : message + " at " + label;
        }
    }
}
