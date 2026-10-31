package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/** Immutable input passed to a {@link CompletionProvider}. */
@AiDoc(
        summary = "Context record supplied to completion providers containing editor text and cursor position.",
        purpose = "Supplies cursor position, line numbers, and text to CompletionProvider queries.",
        useWhen = {
                "Calculating autocomplete choices for CodeEditor."
        },
        criticalRules = {
                "caretIndex is the zero-based offset in text where completion was invoked."
        },
        related = {"CompletionProvider", "CompletionItem"})
@WikiPage(
        title = "<key:completion.context.title>",
        slug = "completion-context",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:completion.context.summary>",
        order = 370)
@WikiSection(
        title = "<key:completion.context.section.overview.title>",
        markdown = "<key:completion.context.section.overview.body>",
        order = 10)
public record CompletionContext(String text,
                                String languageId,
                                int cursorIndex,
                                int selectionStart,
                                int selectionEnd) {
    public CompletionContext {
        text = text == null ? "" : text;
        languageId = languageId == null ? "" : languageId.trim();
        cursorIndex = clamp(cursorIndex, 0, text.length());
        selectionStart = clamp(selectionStart, 0, text.length());
        selectionEnd = clamp(selectionEnd, 0, text.length());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}