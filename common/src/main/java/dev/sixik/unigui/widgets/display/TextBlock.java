package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;

/**
 * Plain multiline paragraph widget.
 *
 * <p>TextBlock intentionally flattens RichText input to plain text. Use
 * {@link RichTextView} when segment-level font/style runs must be preserved.</p>
 */
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
