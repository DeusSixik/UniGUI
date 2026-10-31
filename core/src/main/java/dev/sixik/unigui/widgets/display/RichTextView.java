package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Rich multiline text view that preserves RichText runs.
 *
 * <p>Use {@link TextBlock} for plain multiline paragraphs where rich styling
 * should be flattened.</p>
 */
@AiDoc(
        summary = "Formatted rich text widget rendering styled runs, inline icons, color spans, and links.",
        purpose = "Displays structured RichText models with formatted styles, badges, icons, and interactive links.",
        useWhen = {
                "Chat messages, lore tooltips, or announcements with multi-colored spans.",
                "Interactive text with embedded icon graphics or clickable URLs."
        },
        criticalRules = {
                "Renders RichText runs with full fidelity across line wraps.",
                "Supports hover and click interactions for linked runs."
        },
        related = {"RichText", "TextWidget", "TextBlock"})
@WikiPage(
        title = "<key:rich.text.view.title>",
        slug = "rich-text-view",
        category = "<key:core.widgets.display.category>",
        summary = "<key:rich.text.view.summary>",
        order = 50)
@WikiSection(
        title = "<key:rich.text.view.section.overview.title>",
        markdown = "<key:rich.text.view.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:rich.text.view.example.basic.title>",
        code = "<key:rich.text.view.example.basic.code>",
        order = 10)
@XmlWidgetName("RichTextView")
public final class RichTextView extends TextWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.RICH_TEXT_VIEW;

    public RichTextView() {
        wrap(true);
    }

    public RichTextView(RichText text) {
        this();
        richText(text);
    }

    @Override
    public RichTextView richText(RichText richText) {
        super.richText(richText);
        return this;
    }

    @Override
    @XmlAttribute(value = "text", category = "Content", defaultValue = "", description = "Plain text fallback content displayed by the rich text view.")
    public RichTextView text(String text) {
        super.text(text);
        return this;
    }

    @Override
    protected Alignment textVerticalAlignment() {
        return Alignment.START;
    }
}