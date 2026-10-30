package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;

/**
 * Rich multiline text view that preserves RichText runs.
 *
 * <p>Use {@link TextBlock} for plain multiline paragraphs where rich styling
 * should be flattened.</p>
 */
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
