package dev.sixik.unigui.widgets.interaction;

import java.util.List;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@FunctionalInterface
@AiDoc(
        summary = "Interface producing autocompletion proposals based on editor cursor context.",
        purpose = "Provides contextual autocomplete candidates when typing in CodeEditor.",
        useWhen = {
                "Implementing tag, keyword, or identifier autocompletion."
        },
        criticalRules = {
                "Returns list of CompletionItem suggestions displayed in an overlay popup."
        },
        related = {"CompletionItem", "CompletionContext", "CodeEditor"})
@WikiPage(
        title = "<key:completion.provider.title>",
        slug = "completion-provider",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:completion.provider.summary>",
        order = 350)
@WikiSection(
        title = "<key:completion.provider.section.overview.title>",
        markdown = "<key:completion.provider.section.overview.body>",
        order = 10)
public interface CompletionProvider {
    CompletionProvider NONE = context -> List.of();

    @WikiMethod(
            title = "<key:completion.provider.method.complete.title>",
            summary = "<key:completion.provider.method.complete.summary>")
    List<CompletionItem> complete(CompletionContext context);
}