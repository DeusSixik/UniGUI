package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/** A single completion proposal returned by {@link CompletionProvider}. */
@AiDoc(
        summary = "Individual autocompletion suggestion with rich label, insertion text, and replacement range.",
        purpose = "Represents a single candidate item within the CodeEditor autocomplete popup menu.",
        useWhen = {
                "Emitted by CompletionProvider implementations."
        },
        criticalRules = {
                "insertText defines the characters inserted on confirmation."
        },
        related = {"CompletionProvider", "CompletionContext"})
@WikiPage(
        title = "<key:completion.item.title>",
        slug = "completion-item",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:completion.item.summary>",
        order = 360)
@WikiSection(
        title = "<key:completion.item.section.overview.title>",
        markdown = "<key:completion.item.section.overview.body>",
        order = 10)
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