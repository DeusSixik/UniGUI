package dev.sixik.unigui.api.xml.editor;

/**
 * Read/write endpoint for an XML editor document.
 *
 * <p>The source describes where the saved XML text lives. It deliberately works
 * with text instead of parsed documents because XML remains the saved artifact.</p>
 */
public interface XmlEditorDocumentSource {
    /** Stable source id for recent-project lists and command status. */
    String id();

    /** Human-readable label for UI panels. */
    String displayName();

    /** Whether {@link #writeText(String)} can persist changes. */
    boolean writable();

    /** Reads UTF-8 XML text from this source. */
    String readText();

    /** Writes UTF-8 XML text to this source, or throws if it is read-only. */
    void writeText(String text);
}
