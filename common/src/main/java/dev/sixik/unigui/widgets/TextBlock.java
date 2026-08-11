package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.text.RichText;

/**
 * Plain multiline paragraph widget.
 *
 * <p>TextBlock intentionally flattens RichText input to plain text. Use
 * {@link RichTextView} when segment-level font/style runs must be preserved.</p>
 */
public final class TextBlock extends TextWidget {
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
