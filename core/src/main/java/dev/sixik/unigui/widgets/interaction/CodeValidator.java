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
        summary = "Functional interface performing static analysis and reporting code diagnostics.",
        purpose = "Allows plugging in real-time linting and syntax error checkers into CodeEditor.",
        useWhen = {
                "Validating configuration schemas, script syntax, or expression correctness."
        },
        criticalRules = {
                "Returns list of CodeDiagnostic items displayed as squiggles and tooltips in the editor."
        },
        related = {"CodeDiagnostic", "CodeValidationContext", "CodeEditor"})
@WikiPage(
        title = "<key:code.validator.title>",
        slug = "code-validator",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:code.validator.summary>",
        order = 330)
@WikiSection(
        title = "<key:code.validator.section.overview.title>",
        markdown = "<key:code.validator.section.overview.body>",
        order = 10)
public interface CodeValidator {
    CodeValidator NONE = context -> List.of();

    @WikiMethod(
            title = "<key:code.validator.method.validate.title>",
            summary = "<key:code.validator.method.validate.summary>")
    List<CodeDiagnostic> validate(CodeValidationContext context);
}