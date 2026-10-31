package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Generic display text widget.
 *
 * <p>Use {@link Label} when the text captions another control and should focus
 * that control on click. Use {@link TextBlock} for plain multiline paragraphs
 * and {@link RichTextView} for rich multiline content.</p>
 */
@AiDoc(
        summary = "Lightweight text display widget for static or dynamic strings and RichText.",
        purpose = "Provides simple, non-interactive text rendering for headers, status messages, and read-only labels.",
        useWhen = {
                "Displaying titles, status readouts, or notifications without form focus semantics.",
                "Lightweight alternative to Label when no associated control exists."
        },
        criticalRules = {
                "Does not delegate input focus on click.",
                "Inherits all styling capabilities from TextWidget."
        },
        related = {"TextWidget", "Label", "TextBlock", "RichTextView"})
@WikiPage(
        title = "<key:text.display.title>",
        slug = "text",
        category = "<key:core.widgets.display.category>",
        summary = "<key:text.display.summary>",
        order = 30)
@WikiSection(
        title = "<key:text.display.section.overview.title>",
        markdown = "<key:text.display.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:text.display.example.basic.title>",
        code = "<key:text.display.example.basic.code>",
        order = 10)
@XmlWidgetName("Text")
public final class Text extends TextWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.TEXT;

    public Text() {
    }

    public Text(String text) {
        super(text);
    }

    public Text(RichText text) {
        richText(text);
    }
}