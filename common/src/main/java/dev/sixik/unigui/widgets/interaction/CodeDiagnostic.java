package dev.sixik.unigui.widgets.interaction;

/** Diagnostic reported by a pluggable {@link CodeValidator}. */
public record CodeDiagnostic(CodeEditor.Severity severity,
                             int line,
                             int column,
                             int endLine,
                             int endColumn,
                             String message) {
    public CodeDiagnostic(CodeEditor.Severity severity, int line, int column, String message) {
        this(severity, line, column, line, column + 1, message);
    }

    public CodeDiagnostic {
        severity = severity == null ? CodeEditor.Severity.ERROR : severity;
        endLine = Math.max(line, endLine);
        endColumn = endLine == line ? Math.max(column, endColumn) : Math.max(1, endColumn);
        message = message == null ? "" : message.trim();
    }

    public boolean hasLocation() {
        return line >= 1 && column >= 1;
    }
}