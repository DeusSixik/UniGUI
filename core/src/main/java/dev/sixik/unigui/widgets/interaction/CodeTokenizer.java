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
        summary = "Functional interface converting source code text into colored syntax tokens.",
        purpose = "Enables custom lexical tokenization and syntax highlighting rules for CodeEditor.",
        useWhen = {
                "Implementing syntax highlighting for custom domain-specific languages.",
                "Customizing token coloring for existing languages."
        },
        criticalRules = {
                "Tokens must have non-overlapping, monotonically increasing index spans."
        },
        related = {"CodeToken", "TokenStyle", "CodeTokenizationContext", "CodeEditor"})
@WikiPage(
        title = "<key:code.tokenizer.title>",
        slug = "code-tokenizer",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:code.tokenizer.summary>",
        order = 300)
@WikiSection(
        title = "<key:code.tokenizer.section.overview.title>",
        markdown = "<key:code.tokenizer.section.overview.body>",
        order = 10)
public interface CodeTokenizer {
    CodeTokenizer NONE = context -> List.of();

    @WikiMethod(
            title = "<key:code.tokenizer.method.tokenize.title>",
            summary = "<key:code.tokenizer.method.tokenize.summary>")
    List<CodeToken> tokenize(CodeTokenizationContext context);
}