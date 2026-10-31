package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Built-in language presets for {@link CodeEditor}.
 *
 * <p>A preset configures the stable language id and the default tokenizer. More advanced
 * validators and completion providers remain pluggable and can be set separately.</p>
 */
@AiDoc(
        summary = "Enumeration of built-in language presets for syntax highlighting and validation.",
        purpose = "Supplies language identifiers for predefined tokenizer and validator presets.",
        useWhen = {
                "Specifying language preset on CodeEditor.",
                "Selecting syntax highlight mode in UI tools."
        },
        criticalRules = {
                "Presets map to tokenizers supplied by CodeLanguagePresets."
        },
        related = {"CodeLanguagePresets", "CodeEditor"})
@WikiPage(
        title = "<key:code.language.preset.title>",
        slug = "code-language-preset",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:code.language.preset.summary>",
        order = 280)
@WikiSection(
        title = "<key:code.language.preset.section.overview.title>",
        markdown = "<key:code.language.preset.section.overview.body>",
        order = 10)
public enum CodeLanguagePreset {
    /** No built-in tokenizer is installed. */
    NONE,

    /** XML/XAML markup tokenizer preset. */
    XAML
}