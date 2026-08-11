package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.text.RichText;

/**
 * Generic display text widget.
 *
 * <p>Use {@link Label} when the text captions another control and should focus
 * that control on click. Use {@link TextBlock} for plain multiline paragraphs
 * and {@link RichTextView} for rich multiline content.</p>
 */
public final class Text extends TextWidget {
    public Text() {
    }

    public Text(String text) {
        super(text);
    }

    public Text(RichText text) {
        richText(text);
    }
}
