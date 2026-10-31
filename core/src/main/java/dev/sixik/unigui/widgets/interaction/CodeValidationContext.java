package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/** Immutable input passed to a {@link CodeValidator}. */
@AiDoc(
        summary = "Context record passed to code validators containing document text and language ID.",
        purpose = "Provides document source code and language identifier to CodeValidator.",
        useWhen = {
                "Executing static analysis passes in CodeEditor."
        },
        criticalRules = {
                "text is the complete current document string."
        },
        related = {"CodeValidator", "CodeDiagnostic"})
@WikiPage(
        title = "<key:code.validation.context.title>",
        slug = "code-validation-context",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:code.validation.context.summary>",
        order = 374)
@WikiSection(
        title = "<key:code.validation.context.section.overview.title>",
        markdown = "<key:code.validation.context.section.overview.body>",
        order = 10)
public record CodeValidationContext(String text, String languageId) {
    public CodeValidationContext {
        text = text == null ? "" : text;
        languageId = languageId == null ? "" : languageId.trim();
    }
}