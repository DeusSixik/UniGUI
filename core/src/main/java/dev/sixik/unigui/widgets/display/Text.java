package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.xml.XmlWidgetName;

/**
 * Generic display text widget.
 *
 * <p>Use {@link Label} when the text captions another control and should focus
 * that control on click. Use {@link TextBlock} for plain multiline paragraphs
 * and {@link RichTextView} for rich multiline content.</p>
 */
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
