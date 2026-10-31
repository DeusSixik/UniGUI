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
 * Plain multiline paragraph widget.
 *
 * <p>TextBlock intentionally flattens RichText input to plain text. Use
 * {@link RichTextView} when segment-level font/style runs must be preserved.</p>
 */
@AiDoc(
        summary = "Multiline paragraph text display widget supporting line wrapping, line height, and text alignments.",
        purpose = "Renders longer descriptive text blocks and paragraphs with automatic word wrapping.",
        useWhen = {
                "Multi-line item descriptions, lore text, or story dialogue.",
                "Documentation or article excerpt paragraphs within GUIs."
        },
        criticalRules = {
                "Automatically wraps words based on available layout width.",
                "Supports RichText formatting runs."
        },
        related = {"TextWidget", "RichTextView", "Label"})
@WikiPage(
        title = "<key:text.block.title>",
        slug = "text-block",
        category = "<key:core.widgets.display.category>",
        summary = "<key:text.block.summary>",
        order = 40)
@WikiSection(
        title = "<key:text.block.section.overview.title>",
        markdown = "<key:text.block.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:text.block.example.basic.title>",
        code = "<key:text.block.example.basic.code>",
        order = 10)
@XmlWidgetName("TextBlock")
public final class TextBlock extends TextWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.TEXT_BLOCK;

    public TextBlock() {
        wrap(true);
    }

    public TextBlock(String text) {
        super(text);
        wrap(true);
    }

    public TextBlock(RichText text) {
        text(text == null ? "" : text.plainText());
        wrap(true);
    }

    @Override
    @XmlAttribute(value = "text", category = "Content", defaultValue = "", description = "Plain text content displayed by the text block.")
    public TextBlock text(String text) {
        super.text(text);
        return this;
    }

    @Override
    public TextBlock richText(RichText richText) {
        super.text(richText == null ? "" : richText.plainText());
        return this;
    }

    @Override
    protected Alignment textVerticalAlignment() {
        return Alignment.START;
    }
}