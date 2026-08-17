package dev.sixik.unigui.api.xml.editor;

/** Editing mode for the XML UI editor session. */
public enum XmlEditorMode {
    /** Edit source XML through hierarchy, properties and design handles. */
    DESIGN,
    /** Edit raw XML text. */
    CODE,
    /** Run a disposable runtime tree built from the current XML snapshot. */
    RUNTIME
}
