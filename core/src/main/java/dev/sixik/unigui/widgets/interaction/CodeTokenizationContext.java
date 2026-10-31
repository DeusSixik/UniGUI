package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/** Immutable input passed to a {@link CodeTokenizer}. */
@AiDoc(
        summary = "Context record passed to syntax tokenizers containing source text and language ID.",
        purpose = "Provides raw source string and language identifier to CodeTokenizer.",
        useWhen = {
                "Invoking syntax tokenizers in CodeEditor."
        },
        criticalRules = {
                "languageId can be null if untyped text is edited."
        },
        related = {"CodeTokenizer", "CodeToken"})
@WikiPage(
        title = "<key:code.tokenization.context.title>",
        slug = "code-tokenization-context",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:code.tokenization.context.summary>",
        order = 372)
@WikiSection(
        title = "<key:code.tokenization.context.section.overview.title>",
        markdown = "<key:code.tokenization.context.section.overview.body>",
        order = 10)
public record CodeTokenizationContext(String text, String languageId) {
    public CodeTokenizationContext {
        text = text == null ? "" : text;
        languageId = languageId == null ? "" : languageId.trim();
    }
}