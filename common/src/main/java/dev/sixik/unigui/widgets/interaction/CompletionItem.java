package dev.sixik.unigui.widgets.interaction;

/** A single completion proposal returned by {@link CompletionProvider}. */
public record CompletionItem(String label,
                             String insertText,
                             String detail,
                             int replaceStart,
                             int replaceEnd) {
    public CompletionItem(String label, String insertText) {
        this(label, insertText, "", -1, -1);
    }

    public CompletionItem {
        label = label == null ? "" : label.trim();
        insertText = insertText == null ? label : insertText;
        detail = detail == null ? "" : detail.trim();
        if (replaceStart < 0 || replaceEnd < replaceStart) {
            replaceStart = -1;
            replaceEnd = -1;
        }
    }

    public boolean hasReplacementRange() {
        return replaceStart >= 0 && replaceEnd >= replaceStart;
    }
}