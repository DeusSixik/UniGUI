package dev.sixik.unigui.api.xml.editor;

/** Notification emitted whenever the shared XML editor session state changes. */
public record XmlEditorSessionChange(
        XmlEditorSession session,
        Kind kind,
        XmlEditorMode previousMode,
        XmlEditorMode mode,
        String description) {
    public XmlEditorSessionChange {
        if (session == null) throw new IllegalArgumentException("XML editor session change requires a session");
        kind = kind == null ? Kind.DOCUMENT_CHANGED : kind;
        previousMode = previousMode == null ? mode : previousMode;
        mode = mode == null ? XmlEditorMode.DESIGN : mode;
        description = description == null ? "" : description.trim();
    }

    public enum Kind {
        DOCUMENT_CHANGED,
        TEXT_CHANGED,
        SELECTION_CHANGED,
        MODE_CHANGED,
        DIAGNOSTICS_CHANGED,
        RUNTIME_OPTIONS_CHANGED,
        SOURCE_CHANGED,
        SAVED,
        REVERTED,
        UNDO_STACK_CHANGED
    }
}
