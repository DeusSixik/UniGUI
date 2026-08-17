package dev.sixik.unigui.api.xml.editor;

/** Source bucket used when the editor merges diagnostics for visible panes. */
public enum XmlEditorDiagnosticChannel {
    PARSE,
    VALIDATION,
    LOADING,
    EDIT,
    BINDING,
    SCRIPT,
    RUNTIME
}
