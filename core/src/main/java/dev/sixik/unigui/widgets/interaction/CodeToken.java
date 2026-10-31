package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/** Syntax token range in UTF-16 text indexes. Boundaries must be code-point safe. */
@AiDoc(
        summary = "Immutable range token associating character indices with a TokenStyle.",
        purpose = "Represents a single highlighted syntax token range in source text.",
        useWhen = {
                "Emitted by CodeTokenizer implementations."
        },
        criticalRules = {
                "startIndex is inclusive, endIndex is exclusive."
        },
        related = {"TokenStyle", "CodeTokenizer"})
@WikiPage(
        title = "<key:code.token.title>",
        slug = "code-token",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:code.token.summary>",
        order = 310)
@WikiSection(
        title = "<key:code.token.section.overview.title>",
        markdown = "<key:code.token.section.overview.body>",
        order = 10)
public record CodeToken(int startIndex, int endIndex, TokenStyle style) {
    public CodeToken {
        int start = Math.max(0, startIndex);
        int end = Math.max(start, endIndex);
        startIndex = start;
        endIndex = end;
        style = style == null ? TokenStyle.NONE : style;
    }

    public boolean isEmpty() {
        return endIndex <= startIndex;
    }
}