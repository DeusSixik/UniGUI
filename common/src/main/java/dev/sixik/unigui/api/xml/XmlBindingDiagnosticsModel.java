package dev.sixik.unigui.api.xml;

import java.util.List;

/** Редакторская list-модель для статусов binding-ов и диагностик. */
public final class XmlBindingDiagnosticsModel {
    private static final XmlBindingDiagnosticsModel EMPTY = new XmlBindingDiagnosticsModel(List.of());

    private final List<Entry> entries;

    private XmlBindingDiagnosticsModel(List<Entry> entries) {
        this.entries = List.copyOf(entries == null ? List.of() : entries);
    }

    public static XmlBindingDiagnosticsModel empty() {
        return EMPTY;
    }

    public static XmlBindingDiagnosticsModel from(XmlBindingStatus status) {
        if (status == null) return empty();
        return from(List.of(status));
    }

    public static XmlBindingDiagnosticsModel from(List<XmlBindingStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) return empty();
        java.util.ArrayList<Entry> entries = new java.util.ArrayList<>(statuses.size());
        for (XmlBindingStatus status : statuses) {
            if (status == null) continue;
            entries.add(new Entry(entries.size() + 1, status.state(), status.path(), status.summary()));
        }
        return entries.isEmpty() ? empty() : new XmlBindingDiagnosticsModel(entries);
    }

    public List<Entry> entries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean hasErrors() {
        return entries.stream().anyMatch(entry -> entry.state() == XmlBindingStatus.State.ERROR);
    }

    public int errorCount() {
        return (int) entries.stream().filter(entry -> entry.state() == XmlBindingStatus.State.ERROR).count();
    }

    public String summary() {
        if (entries.isEmpty()) return "No XML binding diagnostics.";
        int errors = errorCount();
        if (errors == 0) return entries.size() == 1 ? "1 XML binding status." : entries.size() + " XML binding statuses.";
        return errors == 1 ? "1 XML binding diagnostic." : errors + " XML binding diagnostics.";
    }

    public record Entry(int index, XmlBindingStatus.State state, String path, String message) {
        public Entry {
            if (index < 1) throw new IllegalArgumentException("XML binding entry index must be positive");
            state = state == null ? XmlBindingStatus.State.ERROR : state;
            path = path == null ? "" : path.trim();
            message = message == null ? "" : message.trim();
        }

        public boolean error() {
            return state == XmlBindingStatus.State.ERROR;
        }

        public String displayText() {
            return path.isEmpty() ? message : path + ": " + message;
        }
    }
}
