package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/** Diagnostic reported by a pluggable {@link CodeValidator}. */
@AiDoc(
        summary = "Diagnostic report entry representing an error, warning, or hint at a specific code range.",
        purpose = "Represents a single static analysis finding reported to the code editor user.",
        useWhen = {
                "Emitted by CodeValidator implementations."
        },
        criticalRules = {
                "Severity determines icon, squiggle color, and status bar impact."
        },
        related = {"CodeValidator", "CodeEditor"})
@WikiPage(
        title = "<key:code.diagnostic.title>",
        slug = "code-diagnostic",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:code.diagnostic.summary>",
        order = 340)
@WikiSection(
        title = "<key:code.diagnostic.section.overview.title>",
        markdown = "<key:code.diagnostic.section.overview.body>",
        order = 10)
public record CodeDiagnostic(CodeEditor.Severity severity,
                             int line,
                             int column,
                             int endLine,
                             int endColumn,
                             String message) {
    public CodeDiagnostic(CodeEditor.Severity severity, int line, int column, String message) {
        this(severity, line, column, line, column + 1, message);
    }

    public CodeDiagnostic {
        severity = severity == null ? CodeEditor.Severity.ERROR : severity;
        endLine = Math.max(line, endLine);
        endColumn = endLine == line ? Math.max(column, endColumn) : Math.max(1, endColumn);
        message = message == null ? "" : message.trim();
    }

    public boolean hasLocation() {
        return line >= 1 && column >= 1;
    }
}