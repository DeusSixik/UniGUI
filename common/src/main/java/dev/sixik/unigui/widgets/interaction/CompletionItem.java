package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.text.RichText;

/** A single completion proposal returned by {@link CompletionProvider}. */
public record CompletionItem(RichText label,
                             String insertText,
                             RichText detail,
                             int replaceStart,
                             int replaceEnd) {
    public CompletionItem(String label, String insertText) {
        this(label, insertText, "", -1, -1);
    }

    public CompletionItem(String label, String insertText, String detail, int replaceStart, int replaceEnd) {
        this(RichText.resolve(label == null ? "" : label.trim()), insertText,
                RichText.resolve(detail == null ? "" : detail.trim()), replaceStart, replaceEnd);
    }

    public CompletionItem(RichText label, String insertText) {
        this(label, insertText, RichText.plain(""), -1, -1);
    }

    public CompletionItem {
        label = label == null ? RichText.plain("") : label;
        insertText = insertText == null ? label.plainText() : insertText;
        detail = detail == null ? RichText.plain("") : detail;
        if (replaceStart < 0 || replaceEnd < replaceStart) {
            replaceStart = -1;
            replaceEnd = -1;
        }
    }

    public String labelPlainText() {
        return label.plainText();
    }

    public String detailPlainText() {
        return detail.plainText();
    }

    public boolean hasReplacementRange() {
        return replaceStart >= 0 && replaceEnd >= replaceStart;
    }
}